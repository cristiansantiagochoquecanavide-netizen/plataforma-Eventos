param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string] $BackupFile,

    [Parameter(Position = 1)]
    [string] $TargetDatabase
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $repoRoot

$backupDirectory = Join-Path $repoRoot 'backups'
$resolvedBackupDirectory = (Resolve-Path -LiteralPath $backupDirectory).Path
$resolvedBackupFile = (Resolve-Path -LiteralPath $BackupFile).Path
$resolvedParent = (Resolve-Path -LiteralPath (Split-Path -Parent $resolvedBackupFile)).Path
if ($resolvedParent -ne $resolvedBackupDirectory) {
    throw 'El backup debe estar dentro de la carpeta local backups.'
}
if ([IO.Path]::GetExtension($resolvedBackupFile) -ne '.dump') {
    throw 'Se requiere un archivo PostgreSQL custom-format con extensión .dump.'
}
if (-not (Test-Path -LiteralPath "$resolvedBackupFile.sha256")) {
    throw 'No se encontró el checksum SHA-256 del backup.'
}

if ([string]::IsNullOrWhiteSpace($TargetDatabase)) {
    $TargetDatabase = 'restore_validation_{0}' -f (Get-Date -Format 'yyyyMMdd_HHmmss')
}
if ($TargetDatabase -notmatch '^restore_validation_[A-Za-z0-9_]+$') {
    throw 'El nombre destino debe iniciar con restore_validation_; nunca se restaurará encima de la base de la aplicación.'
}

$runningServices = @(docker compose ps --status running --services)
if ($LASTEXITCODE -ne 0) {
    throw 'No se pudo consultar Docker Compose.'
}
if ($runningServices -notcontains 'base-datos') {
    throw 'La base de datos local debe estar ejecutándose antes de validar el restore.'
}

$fileName = Split-Path -Leaf $resolvedBackupFile
docker compose run --rm --no-deps `
    -e "BACKUP_FILE=/backups/$fileName" `
    -e "RESTORE_TARGET_DATABASE=$TargetDatabase" `
    restore-postgres
if ($LASTEXITCODE -ne 0) {
    throw 'Falló la restauración aislada o su validación.'
}

Write-Output "La base de validación $TargetDatabase se conserva en el PostgreSQL local para inspección."
