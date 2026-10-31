param(
    [string]$HostName = $env:DB_HOST,
    [string]$Port = $env:DB_PORT,
    [string]$Database = $env:DB_NAME,
    [string]$Username = $env:DB_USERNAME,
    [string]$BackupDir = ".\backups"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($HostName)) { $HostName = "localhost" }
if ([string]::IsNullOrWhiteSpace($Port)) { $Port = "5432" }
if ([string]::IsNullOrWhiteSpace($Database)) { $Database = "fpa_db" }
if ([string]::IsNullOrWhiteSpace($Username)) {
    throw "Set DB_USERNAME or pass -Username."
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw "Set DB_PASSWORD before creating a backup."
}
if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    throw "pg_dump was not found. Install PostgreSQL client tools and add them to PATH."
}

$backupDirectory = [System.IO.Path]::GetFullPath($BackupDir)
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $backupDirectory "${Database}_backup_${timestamp}.dump"
$previousPassword = $env:PGPASSWORD

try {
    $env:PGPASSWORD = $env:DB_PASSWORD
    & pg_dump --host=$HostName --port=$Port --username=$Username --dbname=$Database `
        --format=custom --blobs --verbose --file=$backupFile
    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump failed with exit code $LASTEXITCODE."
    }

    Write-Output "Backup completed: $backupFile"
}
catch {
    if (Test-Path -LiteralPath $backupFile) {
        Remove-Item -LiteralPath $backupFile -Force
    }
    Write-Error $_
    exit 1
}
finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSWORD = $previousPassword
    }
}
