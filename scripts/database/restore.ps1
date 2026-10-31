param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$HostName = $env:DB_HOST,
    [string]$Port = $env:DB_PORT,
    [string]$Database = $env:DB_NAME,
    [string]$Username = $env:DB_USERNAME,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($HostName)) { $HostName = "localhost" }
if ([string]::IsNullOrWhiteSpace($Port)) { $Port = "5432" }
if ([string]::IsNullOrWhiteSpace($Database)) { $Database = "fpa_db" }
if ([string]::IsNullOrWhiteSpace($Username)) {
    throw "Set DB_USERNAME or pass -Username."
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw "Set DB_PASSWORD before restoring a backup."
}
if (-not (Get-Command pg_restore -ErrorAction SilentlyContinue)) {
    throw "pg_restore was not found. Install PostgreSQL client tools and add them to PATH."
}
if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "psql was not found. Install PostgreSQL client tools and add them to PATH."
}
if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) {
    throw "Backup file not found: $BackupFile"
}

$resolvedBackupFile = (Resolve-Path -LiteralPath $BackupFile).Path
if (-not $Force) {
    Write-Warning "This replaces objects and data in database '$Database'."
    $confirmation = Read-Host "Type RESTORE to continue"
    if ($confirmation -cne "RESTORE") {
        Write-Output "Restore cancelled; no database changes were made."
        exit 0
    }
}

$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $env:DB_PASSWORD
    & pg_restore --host=$HostName --port=$Port --username=$Username --dbname=$Database `
        --clean --if-exists --no-owner --no-acl --exit-on-error --verbose $resolvedBackupFile
    if ($LASTEXITCODE -ne 0) {
        throw "pg_restore failed with exit code $LASTEXITCODE."
    }

    & psql --no-psqlrc --host=$HostName --port=$Port --username=$Username --dbname=$Database `
        --set=ON_ERROR_STOP=1 --command="SELECT current_database() AS database_name, COUNT(*) AS public_tables FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
    if ($LASTEXITCODE -ne 0) {
        throw "Post-restore database verification failed with exit code $LASTEXITCODE."
    }
}
finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSWORD = $previousPassword
    }
}
