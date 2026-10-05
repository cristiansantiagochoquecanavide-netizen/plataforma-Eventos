param()

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $repoRoot

$runningServices = @(docker compose ps --status running --services)
if ($LASTEXITCODE -ne 0) {
    throw 'No se pudo consultar Docker Compose.'
}
if ($runningServices -notcontains 'base-datos') {
    throw 'La base de datos local debe estar ejecutándose antes de crear el backup.'
}

$backupDirectory = Join-Path $repoRoot 'backups'
if (-not (Test-Path -LiteralPath $backupDirectory)) {
    New-Item -ItemType Directory -Path $backupDirectory | Out-Null
}

docker compose run --rm --no-deps backup-postgres
if ($LASTEXITCODE -ne 0) {
    throw 'Falló el backup o la comprobación de integridad.'
}
