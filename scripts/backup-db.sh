#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-fpa_db}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"

if [[ -z "${DB_USERNAME:-}" || -z "${DB_PASSWORD:-}" ]]; then
  echo "Set DB_USERNAME and DB_PASSWORD before creating a backup." >&2
  exit 1
fi
if ! command -v pg_dump >/dev/null 2>&1; then
  echo "pg_dump was not found; install PostgreSQL client tools." >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
timestamp="$(date +"%Y%m%d_%H%M%S")"
backup_file="${BACKUP_DIR}/${DB_NAME}_backup_${timestamp}.dump"
export PGPASSWORD="$DB_PASSWORD"

if ! pg_dump --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USERNAME" \
  --dbname="$DB_NAME" --format=custom --blobs --verbose --file="$backup_file"; then
  rm -f -- "$backup_file"
  echo "pg_dump failed; partial backup removed." >&2
  exit 1
fi

unset PGPASSWORD
echo "Backup completed: $backup_file"
