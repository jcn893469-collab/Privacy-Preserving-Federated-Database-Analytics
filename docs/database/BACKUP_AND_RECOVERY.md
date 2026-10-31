# PostgreSQL Backup and Recovery

The PowerShell scripts use PostgreSQL's custom archive format (`pg_dump
--format=custom`), which can be restored with `pg_restore`. A successful backup
command is not proof of a tested recovery; no restoration has been performed
as part of this documentation.

## Requirements and credentials

Install PostgreSQL client tools matching the deployed PostgreSQL major version
or a compatible newer `pg_dump`/`pg_restore`, and make `pg_dump`, `pg_restore`,
and `psql` available on `PATH`.

Set connection values through environment variables; never place passwords in
the command line:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "fpa_db"
$env:DB_USERNAME = "fpa_user"
$env:DB_PASSWORD = "<database-password>"
```

In PowerShell, use `.\scripts\database\backup.ps1`. The default output is
`.\backups\<database>_backup_<timestamp>.dump`; the directory is git-ignored.
Override it with `-BackupDir`.

```powershell
.\scripts\database\backup.ps1
.\scripts\database\backup.ps1 -Database "fpa_db" -BackupDir ".\backups"
```

The script fails with a non-zero exit code if the client tool or credentials
are missing or `pg_dump` fails. It removes the partial archive created by that
failed invocation.

## Restore

Restore replaces objects and data in the selected existing database. It
requires explicit confirmation unless `-Force` is supplied. Ensure the target
database is the intended restore target before proceeding.

```powershell
.\scripts\database\restore.ps1 -BackupFile ".\backups\fpa_db_backup_YYYYMMDD_HHMMSS.dump"
```

To run non-interactively after independently confirming the target:

```powershell
.\scripts\database\restore.ps1 `
  -BackupFile ".\backups\fpa_db_backup_YYYYMMDD_HHMMSS.dump" `
  -Force
```

`pg_restore --clean --if-exists --exit-on-error` reports restore failures; on
success, the script runs a `psql` query against `information_schema.tables` to
verify that the restored database is reachable and report its public base-table
count. Review the output and compare expected application data before treating
a recovery as verified.

## Manual verification

```powershell
pg_restore --list ".\backups\fpa_db_backup_YYYYMMDD_HHMMSS.dump"
psql --host=$env:DB_HOST --port=$env:DB_PORT --username=$env:DB_USERNAME `
  --dbname=$env:DB_NAME --command="SELECT current_database(), COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
```

For assessment evidence, capture the real `pg_dump` output, the restore
confirmation and `pg_restore` output, and a post-restore SQL query. Do not
describe recovery as tested until those commands have actually succeeded on a
disposable or approved target database.
