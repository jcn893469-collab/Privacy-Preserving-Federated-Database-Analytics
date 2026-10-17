# =============================================================
# Privacy Federated Analytics — PostgreSQL Restore Script
# Usage: .\scripts\restore-db.ps1 -BackupFile <path-to-backup>
# =============================================================
param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,

    [string]$Host     = $env:PGHOST     ?? "localhost",
    [string]$Port     = $env:PGPORT     ?? "5432",
    [string]$Database = $env:PGDATABASE ?? "fpa_db",
    [string]$User     = $env:PGUSER     ?? "fpa_user"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupFile)) {
    Write-Error "Backup file not found: $BackupFile"
    exit 1
}

Write-Host "=== Privacy Federated Analytics — DB Restore ===" -ForegroundColor Cyan
Write-Host "Host     : $Host`:$Port"
Write-Host "Database : $Database"
Write-Host "User     : $User"
Write-Host "File     : $BackupFile"
Write-Host ""

$confirm = Read-Host "This will OVERWRITE '$Database'. Type 'yes' to continue"
if ($confirm -ne "yes") {
    Write-Host "Aborted." -ForegroundColor Yellow
    exit 0
}

# Drop & recreate database (requires superuser or owner)
Write-Host "Dropping and recreating database..." -ForegroundColor Yellow
$env:PGPASSWORD = $env:PGPASSWORD
& pg_psql -h $Host -p $Port -U $User -c "DROP DATABASE IF EXISTS $Database;" postgres
& pg_psql -h $Host -p $Port -U $User -c "CREATE DATABASE $Database OWNER $User;" postgres

# Restore
Write-Host "Restoring from $BackupFile ..." -ForegroundColor Green
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile   = "restore_${timestamp}.log"

& pg_restore `
    --host=$Host `
    --port=$Port `
    --username=$User `
    --dbname=$Database `
    --no-owner `
    --role=$User `
    --verbose `
    $BackupFile 2>&1 | Tee-Object -FilePath $logFile

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Restore completed successfully." -ForegroundColor Green
    Write-Host "Log: $logFile"
} else {
    Write-Host ""
    Write-Host "Restore finished with warnings/errors. Check $logFile" -ForegroundColor Yellow
}
