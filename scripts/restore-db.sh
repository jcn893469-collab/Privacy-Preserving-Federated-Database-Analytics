#!/usr/bin/env bash
# =============================================================
# Privacy Federated Analytics — PostgreSQL Restore Script
# Usage: ./scripts/restore-db.sh <path-to-backup>
# =============================================================
set -euo pipefail

BACKUP_FILE="${1:-}"
HOST="${PGHOST:-localhost}"
PORT="${PGPORT:-5432}"
DATABASE="${PGDATABASE:-fpa_db}"
USER="${PGUSER:-fpa_user}"

if [[ -z "$BACKUP_FILE" ]]; then
  echo "Usage: $0 <backup-file>" >&2
  exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "ERROR: Backup file not found: $BACKUP_FILE" >&2
  exit 1
fi

echo "=== Privacy Federated Analytics — DB Restore ==="
echo "Host     : ${HOST}:${PORT}"
echo "Database : ${DATABASE}"
echo "User     : ${USER}"
echo "File     : ${BACKUP_FILE}"
echo ""

read -r -p "This will OVERWRITE '${DATABASE}'. Type 'yes' to continue: " confirm
if [[ "$confirm" != "yes" ]]; then
  echo "Aborted."
  exit 0
fi

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="restore_${TIMESTAMP}.log"

echo "Dropping and recreating database..."
psql -h "$HOST" -p "$PORT" -U "$USER" -c "DROP DATABASE IF EXISTS ${DATABASE};" postgres
psql -h "$HOST" -p "$PORT" -U "$USER" -c "CREATE DATABASE ${DATABASE} OWNER ${USER};" postgres

echo "Restoring from ${BACKUP_FILE} ..."
pg_restore \
  --host="$HOST" \
  --port="$PORT" \
  --username="$USER" \
  --dbname="$DATABASE" \
  --no-owner \
  --role="$USER" \
  --verbose \
  "$BACKUP_FILE" 2>&1 | tee "$LOG_FILE"

echo ""
echo "Restore complete. Log saved to: ${LOG_FILE}"
