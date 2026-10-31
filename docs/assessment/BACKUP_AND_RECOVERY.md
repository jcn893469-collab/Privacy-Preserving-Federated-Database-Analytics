# Backup and Recovery

The operational commands and failure behavior are documented in
[docs/database/BACKUP_AND_RECOVERY.md](../database/BACKUP_AND_RECOVERY.md).
The PowerShell scripts are `scripts/database/backup.ps1` and
`scripts/database/restore.ps1`; they use `pg_dump`, `pg_restore`, and `psql`.

No actual backup or restoration was run in this environment because PostgreSQL
client tools and Docker were unavailable. Capture genuine command output and
verify restored application data before marking recovery tested.
