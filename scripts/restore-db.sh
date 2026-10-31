#!/usr/bin/env bash
set -euo pipefail

backup_file="${1:-}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-fpa_db}"

if [[ -z "$backup_file" || ! -f "$backup_file" ]]; then
  echo "Usage: $0 <custom-format-backup.dump>" >&2
  exit 1
fi
if [[ -z "${DB_USERNAME:-}" || -z "${DB_PASSWORD:-}" ]]; then
  echo "Set DB_USERNAME and DB_PASSWORD before restoring a backup." >&2
  exit 1
fi
for tool in pg_restore psql; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "$tool was not found; install PostgreSQL client tools." >&2
    exit 1
  fi
done

echo "WARNING: this replaces objects and data in database '${DB_NAME}'."
read -r -p "Type RESTORE to continue: " confirmation
if [[ "$confirmation" != "RESTORE" ]]; then
  echo "Restore cancelled; no database changes were made."
  exit 0
fi

export PGPASSWORD="$DB_PASSWORD"
pg_restore --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USERNAME" \
  --dbname="$DB_NAME" --clean --if-exists --no-owner --no-acl \
  --exit-on-error --verbose "$backup_file"
psql --no-psqlrc --host="$DB_HOST" --port="$DB_PORT" \
  --username="$DB_USERNAME" --dbname="$DB_NAME" --set=ON_ERROR_STOP=1 \
  --command="SELECT current_database() AS database_name, COUNT(*) AS public_tables FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
unset PGPASSWORD
