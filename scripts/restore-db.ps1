param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$HostName = $env:DB_HOST,
    [string]$Port = $env:DB_PORT,
    [string]$Database = $env:DB_NAME,
    [string]$Username = $env:DB_USERNAME,
    [switch]$Force
)

$parameters = @{
    BackupFile = $BackupFile
    HostName = $HostName
    Port = $Port
    Database = $Database
    Username = $Username
}
if ($Force) {
    $parameters.Force = $true
}

& (Join-Path $PSScriptRoot "database\restore.ps1") @parameters
