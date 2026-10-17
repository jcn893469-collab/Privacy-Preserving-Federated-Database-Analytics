#!/usr/bin/env bash
# backup-db.sh
# Backup PostgreSQL Database using pg_dump

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-fpa_db}"
DB_USERNAME="${DB_USERNAME:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_backup_${TIMESTAMP}.sql"

echo "=========================================="
echo "Starting PostgreSQL Database Backup..."
echo "Database: ${DB_NAME} on ${DB_HOST}:${DB_PORT}"
echo "Target:   ${BACKUP_FILE}"
echo "=========================================="

export PGPASSWORD="${DB_PASSWORD}"
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -F p -b -v -f "$BACKUP_FILE"
unset PGPASSWORD

echo "Backup completed successfully!"
echo "Backup saved to: ${BACKUP_FILE}"
