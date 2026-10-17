# backup-db.ps1
# Backup PostgreSQL Database using pg_dump
param(
    [string]$HostName = $env:DB_HOST,
    [string]$Port = $env:DB_PORT,
    [string]$Database = $env:DB_NAME,
    [string]$Username = $env:DB_USERNAME,
    [string]$BackupDir = "./backups"
)

if (-not $HostName) { $HostName = "localhost" }
if (-not $Port) { $Port = "5432" }
if (-not $Database) { $Database = "fpa_db" }
if (-not $Username) { $Username = "postgres" }

if (!(Test-Path -Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupFile = "$BackupDir/${Database}_backup_${Timestamp}.sql"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Starting PostgreSQL Database Backup..." -ForegroundColor Cyan
Write-Host "Database: $Database on ${HostName}:${Port}"
Write-Host "Target:   $BackupFile"
Write-Host "=========================================="

try {
    $env:PGPASSWORD = $env:DB_PASSWORD
    & pg_dump -h $HostName -p $Port -U $Username -d $Database -F p -b -v -f $BackupFile
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Backup completed successfully!" -ForegroundColor Green
        Write-Host "Backup saved to: $BackupFile" -ForegroundColor Green
    } else {
        Write-Host "pg_dump returned exit code $LASTEXITCODE" -ForegroundColor Red
    }
} catch {
    Write-Host "Backup failed: $_" -ForegroundColor Red
} finally {
    $env:PGPASSWORD = $null
}
