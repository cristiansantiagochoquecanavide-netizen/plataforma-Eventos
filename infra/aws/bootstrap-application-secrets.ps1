[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $AdminEmail,
    [string] $SesFrom = $AdminEmail,
    [string] $Region = 'us-east-1',
    [string] $AwsProfile = 'plataforma-eventos-deploy',
    [string] $SecretName = 'plataforma-eventos/application',
    [string] $SmtpUserName = 'plataforma-eventos-ses-smtp'
)

$ErrorActionPreference = 'Stop'
$env:AWS_PROFILE = $AwsProfile
$createdUser = $false
$accessKeyId = $null
$secretFile = $null
$policyFile = $null
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

function Get-HmacSha256([byte[]] $Key, [string] $Message) {
    $hmac = [System.Security.Cryptography.HMACSHA256]::new($Key)
    try {
        return ,($hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($Message)))
    }
    finally {
        $hmac.Dispose()
    }
}

try {
    $accountId = (& aws sts get-caller-identity --query Account --output text --region $Region).Trim()
    if ($LASTEXITCODE -ne 0 -or $accountId -notmatch '^\d{12}$') {
        throw 'No hay una sesión AWS válida para el perfil indicado.'
    }

    $verified = (& aws sesv2 get-email-identity `
        --email-identity $SesFrom `
        --query VerifiedForSendingStatus `
        --output text `
        --region $Region).Trim()
    if ($LASTEXITCODE -ne 0 -or $verified -ne 'True') {
        throw "SES aún no verifica el remitente $SesFrom. Completa primero la verificación en el correo."
    }

    $savedErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & aws secretsmanager describe-secret --secret-id $SecretName --region $Region *> $null
    $secretDescribeExitCode = $LASTEXITCODE
    & aws iam get-user --user-name $SmtpUserName --region $Region *> $null
    $userDescribeExitCode = $LASTEXITCODE
    $ErrorActionPreference = $savedErrorAction
    if ($secretDescribeExitCode -eq 0) {
        throw "El secreto $SecretName ya existe; no se sobrescribió para preservar credenciales existentes."
    }
    if ($userDescribeExitCode -eq 0) {
        throw "El usuario IAM $SmtpUserName ya existe; no se rotaron sus credenciales automáticamente."
    }

    $policy = @{
        Version = '2012-10-17'
        Statement = @(
            @{
                Sid       = 'SendMailOnlyFromVerifiedIdentity'
                Effect    = 'Allow'
                Action    = @('ses:SendRawEmail')
                Resource  = '*'
                Condition = @{ StringEquals = @{ 'ses:FromAddress' = $SesFrom } }
            }
        )
    } | ConvertTo-Json -Depth 8 -Compress

    $tempDirectory = 'C:\Users\ASUS\AppData\Local\Temp\opencode'
    if (-not (Test-Path -LiteralPath $tempDirectory -PathType Container)) {
        throw 'No está disponible el directorio temporal aprobado para la política SMTP.'
    }
    $policyFile = Join-Path $tempDirectory ("smtp-policy-" + [guid]::NewGuid().ToString('N') + '.json')
    [System.IO.File]::WriteAllText($policyFile, $policy, [System.Text.UTF8Encoding]::new($false))

    & aws iam create-user `
        --user-name $SmtpUserName `
        --path '/plataforma-eventos/' `
        --tags 'Key=Application,Value=plataforma-eventos' 'Key=Purpose,Value=smtp-sending' `
        --region $Region | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear el usuario IAM SMTP $SmtpUserName." }
    $createdUser = $true

    & aws iam put-user-policy `
        --user-name $SmtpUserName `
        --policy-name send-only-from-verified-address `
        --policy-document "file://$policyFile" `
        --region $Region | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo restringir el usuario SMTP a la dirección verificada.' }
    Remove-Item -LiteralPath $policyFile -Force
    $policyFile = $null

    $accessKeyJson = & aws iam create-access-key --user-name $SmtpUserName --region $Region --query AccessKey --output json
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la credencial SMTP de SES.' }
    $accessKey = $accessKeyJson | ConvertFrom-Json
    $accessKeyId = $accessKey.AccessKeyId

    $signingKey = [System.Text.Encoding]::UTF8.GetBytes("AWS4$($accessKey.SecretAccessKey)")
    $dateKey = Get-HmacSha256 $signingKey '11111111'
    $regionKey = Get-HmacSha256 $dateKey $Region
    $serviceKey = Get-HmacSha256 $regionKey 'ses'
    $signingKeyFinal = Get-HmacSha256 $serviceKey 'aws4_request'
    $signature = Get-HmacSha256 $signingKeyFinal 'SendRawEmail'
    $smtpPasswordBytes = [byte[]]::new(33)
    $smtpPasswordBytes[0] = 4
    [Array]::Copy($signature, 0, $smtpPasswordBytes, 1, 32)
    $smtpPassword = [Convert]::ToBase64String($smtpPasswordBytes)

    $jwtBytes = [byte[]]::new(32)
    $adminPasswordBytes = [byte[]]::new(32)
    $rng.GetBytes($jwtBytes)
    $rng.GetBytes($adminPasswordBytes)
    $jwtSecret = [Convert]::ToBase64String($jwtBytes)
    $adminPassword = [Convert]::ToBase64String($adminPasswordBytes) + 'Aa1!'
    $secretJson = [ordered]@{
        'jwt-secret'     = $jwtSecret
        'admin-password' = $adminPassword
        'smtp-user'      = $accessKey.AccessKeyId
        'smtp-password'  = $smtpPassword
    } | ConvertTo-Json -Compress

    $secretFile = Join-Path $tempDirectory ("app-secret-" + [guid]::NewGuid().ToString('N') + '.json')
    [System.IO.File]::WriteAllText($secretFile, $secretJson, [System.Text.UTF8Encoding]::new($false))

    $secretArn = & aws secretsmanager create-secret `
        --name $SecretName `
        --description 'Runtime credentials for Plataforma de Gestion de Eventos' `
        --secret-string "file://$secretFile" `
        --tags 'Key=Application,Value=plataforma-eventos' 'Key=Purpose,Value=runtime-credentials' `
        --query ARN `
        --output text `
        --region $Region
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear el secreto $SecretName." }

    [Array]::Clear($jwtBytes, 0, $jwtBytes.Length)
    [Array]::Clear($adminPasswordBytes, 0, $adminPasswordBytes.Length)
    [Array]::Clear($smtpPasswordBytes, 0, $smtpPasswordBytes.Length)
    [Array]::Clear($signature, 0, $signature.Length)
    [Array]::Clear($signingKey, 0, $signingKey.Length)
    $secretJson = $null
    $jwtSecret = $null
    $adminPassword = $null
    $smtpPassword = $null
    $accessKey.SecretAccessKey = $null
    Write-Output "ApplicationSecretsArn=$secretArn"
    Write-Output "AdminEmail=$AdminEmail"
    Write-Output 'Initial admin password is stored in Secrets Manager under admin-password; retrieve it there after deployment.'
}
catch {
    $savedErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    if ($createdUser) {
        if ($accessKeyId) {
            & aws iam delete-access-key --user-name $SmtpUserName --access-key-id $accessKeyId --region $Region *> $null
        }
        & aws iam delete-user-policy --user-name $SmtpUserName --policy-name send-only-from-verified-address --region $Region *> $null
        & aws iam delete-user --user-name $SmtpUserName --region $Region *> $null
    }
    $ErrorActionPreference = $savedErrorAction
    throw
}
finally {
    $rng.Dispose()
    if ($secretFile -and (Test-Path -LiteralPath $secretFile)) {
        Remove-Item -LiteralPath $secretFile -Force
    }
    if ($policyFile -and (Test-Path -LiteralPath $policyFile)) {
        Remove-Item -LiteralPath $policyFile -Force
    }
}
