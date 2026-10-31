param(
    [string]$HostName = $env:DB_HOST,
    [string]$Port = $env:DB_PORT,
    [string]$Database = $env:DB_NAME,
    [string]$Username = $env:DB_USERNAME,
    [string]$BackupDir = ".\backups"
)

& (Join-Path $PSScriptRoot "database\backup.ps1") `
    -HostName $HostName `
    -Port $Port `
    -Database $Database `
    -Username $Username `
    -BackupDir $BackupDir
