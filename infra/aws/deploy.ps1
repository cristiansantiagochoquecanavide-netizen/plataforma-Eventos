[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $Region,
    [Parameter(Mandatory = $true)] [string] $PublicDomainName,
    [Parameter(Mandatory = $true)] [string] $AdminEmail,
    [Parameter(Mandatory = $true)] [string] $SesFrom,
    [Parameter(Mandatory = $true)] [string] $ApplicationSecretsArn,
    [string] $DeveloperAuditKeySecretArn,
    [string] $DeveloperAuditSecretKmsKeyArn,
    [Parameter(Mandatory = $true)] [string] $ImageTag,
    [string] $StackName = 'plataforma-eventos',
    [string] $AwsProfile = 'default',
    [ValidateSet('true', 'false')] [string] $EnableLegacyAuditPayloadDecrypt = 'false'
)

$ErrorActionPreference = 'Stop'
$env:AWS_PROFILE = $AwsProfile

foreach ($tool in @('aws', 'docker')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "No se encontró el comando requerido: $tool"
    }
}

$accountId = (& aws sts get-caller-identity --query Account --output text --region $Region).Trim()
if ($LASTEXITCODE -ne 0 -or $accountId -notmatch '^\d{12}$') {
    throw 'No se pudo identificar una sesión AWS válida. Configura AWS CLI/SSO antes de continuar.'
}

$template = Join-Path $PSScriptRoot 'cloudformation.yml'
& aws cloudformation validate-template --template-body "file://$template" --region $Region | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'CloudFormation rechazó la plantilla; no se crearon repositorios ni se publicaron imágenes.' }

if ([string]::IsNullOrWhiteSpace($DeveloperAuditKeySecretArn) -or [string]::IsNullOrWhiteSpace($DeveloperAuditSecretKmsKeyArn)) {
    $auditOutputs = & aws cloudformation describe-stacks `
        --stack-name plataforma-eventos-audit-secret `
        --query 'Stacks[0].Outputs' `
        --output json `
        --region $Region | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or -not $auditOutputs) {
        throw 'Falta el stack plataforma-eventos-audit-secret; no se iniciará el despliegue.'
    }
    if ([string]::IsNullOrWhiteSpace($DeveloperAuditKeySecretArn)) {
        $DeveloperAuditKeySecretArn = ($auditOutputs | Where-Object OutputKey -eq 'DeveloperAuditSecretArn').OutputValue
    }
    if ([string]::IsNullOrWhiteSpace($DeveloperAuditSecretKmsKeyArn)) {
        $DeveloperAuditSecretKmsKeyArn = ($auditOutputs | Where-Object OutputKey -eq 'DeveloperAuditSecretKmsKeyArn').OutputValue
    }
}

# Preserve the current RDS instance before changing ECS or the stack.
$savedErrorAction = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& aws cloudformation describe-stacks --stack-name $StackName --query 'Stacks[0].StackStatus' --output text --region $Region *> $null
$stackDescribeExitCode = $LASTEXITCODE
$ErrorActionPreference = $savedErrorAction
if ($stackDescribeExitCode -eq 0) {
    $databaseIdentifier = (& aws cloudformation describe-stack-resource `
        --stack-name $StackName `
        --logical-resource-id Database `
        --query 'StackResourceDetail.PhysicalResourceId' `
        --output text `
        --region $Region).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($databaseIdentifier) -or $databaseIdentifier -eq 'None') {
        throw 'El stack ya existe pero no se pudo identificar RDS; se detiene para proteger los datos.'
    }

    & aws rds wait db-instance-available --db-instance-identifier $databaseIdentifier --region $Region
    if ($LASTEXITCODE -ne 0) { throw "RDS/$databaseIdentifier no está disponible; no se inicia el despliegue." }

    $safeStackName = ($StackName.ToLowerInvariant() -replace '[^a-z0-9-]', '-').Trim('-')
    if ($safeStackName.Length -gt 45) { $safeStackName = $safeStackName.Substring(0, 45).TrimEnd('-') }
    $snapshotIdentifier = "$safeStackName-predeploy-$([DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))"
    & aws rds create-db-snapshot `
        --db-instance-identifier $databaseIdentifier `
        --db-snapshot-identifier $snapshotIdentifier `
        --region $Region | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear el snapshot previo $snapshotIdentifier; despliegue cancelado." }

    & aws rds wait db-snapshot-available --db-snapshot-identifier $snapshotIdentifier --region $Region
    if ($LASTEXITCODE -ne 0) { throw "El snapshot $snapshotIdentifier no llegó a disponible; despliegue cancelado." }
    Write-Host "Snapshot RDS disponible: $snapshotIdentifier"
}

$registry = "$accountId.dkr.ecr.$Region.amazonaws.com"
$repositories = @(
    @{ Name = 'plataforma-eventos-backend'; Context = (Join-Path $PSScriptRoot '../../backend') },
    @{ Name = 'plataforma-eventos-frontend'; Context = (Join-Path $PSScriptRoot '../../frontend-web') },
    @{ Name = 'plataforma-eventos-ia'; Context = (Join-Path $PSScriptRoot '../../servicio-ia') }
)

foreach ($repository in $repositories) {
    $savedErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & aws ecr describe-repositories --repository-names $repository.Name --region $Region *> $null
    $repositoryDescribeExitCode = $LASTEXITCODE
    $ErrorActionPreference = $savedErrorAction
    if ($repositoryDescribeExitCode -ne 0) {
        & aws ecr create-repository `
            --repository-name $repository.Name `
            --image-tag-mutability IMMUTABLE `
            --image-scanning-configuration scanOnPush=true `
            --encryption-configuration encryptionType=AES256 `
            --region $Region | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "No se pudo crear ECR/$($repository.Name)" }
    }
    else {
        & aws ecr put-image-tag-mutability `
            --repository-name $repository.Name `
            --image-tag-mutability IMMUTABLE `
            --region $Region | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "No se pudo activar tags inmutables en ECR/$($repository.Name)" }

        & aws ecr put-image-scanning-configuration `
            --repository-name $repository.Name `
            --image-scanning-configuration scanOnPush=true `
            --region $Region | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "No se pudo activar el escaneo de imágenes en ECR/$($repository.Name)" }
    }
}

& aws ecr get-login-password --region $Region | & docker login --username AWS --password-stdin $registry
if ($LASTEXITCODE -ne 0) { throw 'Falló la autenticación de Docker contra ECR.' }

foreach ($repository in $repositories) {
    $image = "$registry/$($repository.Name):$ImageTag"
    & docker build --platform linux/amd64 --tag $image $repository.Context
    if ($LASTEXITCODE -ne 0) { throw "Falló la compilación de $($repository.Name)" }
    & docker push $image
    if ($LASTEXITCODE -ne 0) { throw "Falló la publicación de $($repository.Name)" }
}

$parameters = @(
    "BackendImage=$registry/plataforma-eventos-backend:$ImageTag",
    "FrontendImage=$registry/plataforma-eventos-frontend:$ImageTag",
    "IaImage=$registry/plataforma-eventos-ia:$ImageTag",
    "AdminEmail=$AdminEmail",
    "SesFrom=$SesFrom",
    "ApplicationSecretsArn=$ApplicationSecretsArn",
    "DeveloperAuditKeySecretArn=$DeveloperAuditKeySecretArn",
    "DeveloperAuditSecretKmsKeyArn=$DeveloperAuditSecretKmsKeyArn",
    "PublicDomainName=$PublicDomainName",
    "EnableLegacyAuditPayloadDecrypt=$EnableLegacyAuditPayloadDecrypt"
)

& aws cloudformation deploy `
    --template-file $template `
    --stack-name $StackName `
    --region $Region `
    --capabilities CAPABILITY_IAM `
    --parameter-overrides $parameters
if ($LASTEXITCODE -ne 0) { throw 'Falló el despliegue de CloudFormation.' }

& aws cloudformation describe-stacks `
    --stack-name $StackName `
    --query 'Stacks[0].Outputs' `
    --output table `
    --region $Region
if ($LASTEXITCODE -ne 0) { throw 'El stack se desplegó, pero no se pudieron consultar sus salidas.' }
