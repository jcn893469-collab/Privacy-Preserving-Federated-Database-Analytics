# Privacy-Preserving Federated Database Analytics

VIT BCSE307L project demonstrating federated analytics over locally processed
data. CSV records are processed client-side; the backend receives protected
aggregate contributions, applies privacy-budget accounting, and persists
federated analytics results.

## Features

- Spring Boot 3.2.5 / Java 21 REST backend with Spring Data JPA and Flyway.
- PostgreSQL schema with nine base tables, two aggregate views, an aggregate
  function, a stored procedure, and a contribution audit trigger.
- JWT authentication and `ADMIN`, `DATA_OWNER`, and `RESEARCHER` role checks.
- BCrypt password hashing.
- Browser-based CSV processing, clipping, Gaussian noise, RDP accounting, and
  anomaly detection. Raw CSV rows are not sent to the backend.
- Federated database and dataset catalogs, contribution submission, analytics
  results, privacy budget tracking, and audit history.
- Docker Compose deployment with persistent PostgreSQL storage.

## Technology

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot 3.2.5, Spring Security, Spring Data JPA |
| Database | PostgreSQL 16, Flyway |
| Authentication | JWT (JJWT 0.12.5), BCrypt |
| Frontend | Static HTML, CSS, and JavaScript |
| Build/container | Maven, Docker, Docker Compose |

## Architecture

```text
Browser (local CSV processing)
  └─ protected aggregate over HTTPS/REST
       └─ Spring Security/JWT → controllers → services → JPA repositories
            └─ PostgreSQL (Flyway-managed schema, views, trigger, procedure)
```

The database treats `federated_database` as the organization/participant
boundary. `federated_result` stores completed federated runs. There is no
separate organization or analytics-job table in the current schema.

## Requirements

- Java 21 and Maven 3.9+
- PostgreSQL 16 for local execution, or Docker Desktop for Compose
- PostgreSQL client tools (`pg_dump`, `pg_restore`, and `psql`) for backup and
  recovery

## Configuration

The application intentionally has no default database password or JWT signing
key. Copy `.env.example` to `.env`, then fill in strong values:

```text
POSTGRES_DB=fpa_db
DB_USERNAME=fpa_user
DB_PASSWORD=<your-database-password>
JWT_SECRET=<random-secret-at-least-32-UTF8-bytes>
JWT_EXPIRATION_MS=28800000
POSTGRES_PORT=5432
BACKEND_PORT=8080
SPRING_PROFILE=prod
```

Do not commit `.env`. The same `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`
environment variables are required when running the application directly.
`JWT_SECRET` must contain at least 32 UTF-8 bytes.

## Run with Docker Compose

```powershell
Copy-Item .env.example .env
# Edit .env and set DB_USERNAME, DB_PASSWORD, and JWT_SECRET.
docker compose up -d --build
docker compose logs -f backend
```

The application and static frontend are served at `http://localhost:8080`.
Health checks use `/api/health`. PostgreSQL data is stored in the Compose
volume `postgres_data`.

To stop containers without deleting the database volume:

```powershell
docker compose down
```

`docker compose down -v` deletes the persistent database volume.

## Local backend execution

Create a PostgreSQL role and database using your own password:

```sql
CREATE USER fpa_user WITH PASSWORD '<your-database-password>';
CREATE DATABASE fpa_db OWNER fpa_user;
```

Set environment variables in PowerShell, then launch from the repository root:

```powershell
$env:DB_USERNAME = "fpa_user"
$env:DB_PASSWORD = "<your-database-password>"
$env:JWT_SECRET = "<random-secret-at-least-32-UTF8-bytes>"
mvn -f backend\pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Common Flyway migrations V1-V9 and V11-V12 run in all profiles. V10 seeds
development demonstration accounts and sample metadata. Do not use the dev
profile or demo accounts in production.

## Frontend

There is no separate frontend package/build. HTML, CSS, and JavaScript are
packaged as Spring static resources. Open:

- `/org-login.html` for sign-in
- `/org-dashboard.html` for the data-owner portal
- `/admin-dashboard.html` for the administrator console
- `/researcher-dashboard.html` for federated summaries and run history
- `/` for the public landing page

## API overview

Protected API calls require `Authorization: Bearer <JWT>`.

| Method | Path | Access |
|---|---|---|
| `POST` | `/api/auth/login` | Public |
| `POST` | `/api/auth/logout` | Authenticated; invalidates all tokens for the account |
| `GET` | `/api/auth/me` | Authenticated |
| `POST` | `/api/users/register` | Public data-owner registration |
| `GET/POST` | `/api/databases` | Admin, data owner (owned records) |
| `GET/PUT/DELETE` | `/api/databases/{id}` | Admin, data owner (owned records) |
| `POST` | `/api/datasets` | Admin, data owner (owned records) |
| `GET` | `/api/datasets/database/{databaseId}` | Admin, data owner (owned records); optional `query`, `page`, `size` |
| `GET/PUT/DELETE` | `/api/datasets/{datasetId}` | Admin, data owner (owned records) |
| `GET/POST` | `/api/datasets/{datasetId}/columns` | Admin, data owner (owned records) |
| `PUT/DELETE` | `/api/datasets/{datasetId}/columns/{columnId}` | Admin, data owner (owned records) |
| `POST` | `/api/analytics/contributions` | Data owner |
| `GET/DELETE` | `/api/analytics/contributions/{id}` | Admin, data owner (owned records) |
| `GET/POST` | `/api/analytics/results/runs` | Admin, researcher |
| `GET` | `/api/analytics/results/global` | Admin, researcher |
| `GET` | `/api/admin/views/organization-summary` | Admin |
| `GET` | `/api/admin/procedures/organization-summary/{databaseId}` | Admin |
| `GET` | `/api/admin/audit-logs` | Admin |
| `GET` | `/api/public/dashboard` | Public, aggregate metadata only |
| `GET` | `/api/health` | Public |

Logout invalidates all tokens for the current account by incrementing the
persisted token version. Dataset metadata and column schema expose full CRUD;
the PostgreSQL-backed paths are covered by integration tests that require
Docker to run. See the
[requirements gap analysis](docs/assessment/REQUIREMENTS_GAP_ANALYSIS.md) for
remaining limits.

## Database design

The nine base tables are:

1. `app_user`
2. `federated_database`
3. `analytics_contribution`
4. `dataset_metadata`
5. `dataset_column_schema`
6. `privacy_budget_ledger`
7. `federated_result`
8. `federated_run_participant`
9. `audit_log`

The migration-derived ER diagram, schema, data dictionary, normalization notes,
and SQL examples are in [`docs/database/`](docs/database/). Migrations V1-V9
are the original schema; V10 is dev-only seed data; V11 extends the contribution
audit trigger to UPDATE and adds the organization-summary stored procedure;
V12 adds persisted JWT token-version invalidation. Never edit an already-applied
migration; add a new versioned migration.

## Backup and recovery

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "fpa_db"
$env:DB_USERNAME = "fpa_user"
$env:DB_PASSWORD = "<your-database-password>"
.\scripts\database\backup.ps1
.\scripts\database\restore.ps1 -BackupFile .\backups\fpa_db_backup_YYYYMMDD_HHMMSS.dump
```

Restore replaces objects and data in the selected database and asks for
confirmation. Read [`docs/database/BACKUP_AND_RECOVERY.md`](docs/database/BACKUP_AND_RECOVERY.md)
before running it. No successful restore is claimed without real database
execution and verification.

## Tests

Run the backend tests from the repository root:

```powershell
mvn -f backend\pom.xml test
```

The latest run discovered **54 tests: 0 failures, 0 errors, and 4 skipped**.
The skipped tests are Testcontainers PostgreSQL tests; Docker was unavailable,
so no database-backed pass is claimed. Run
`mvn -f backend\pom.xml -Dtest=PostgresMigrationIntegrationTest,PostgresDatasetJpaIntegrationTest test`
on a Docker-enabled machine to execute them. Static JavaScript syntax checks
passed; there is no separate frontend package/build.

## Repository and deployment evidence

The current checkout records two commits and only the `main` branch. No
`dev`-branch evidence, Docker Hub image, or live deployment URL was found.
Repository visibility is not changed here because the assignment instructions
report a private/public visibility discrepancy that must be resolved with the
course staff.

## Documentation

- [Build progress](docs/BUILD_PROGRESS.md)
- [Verified requirements gap analysis](docs/assessment/REQUIREMENTS_GAP_ANALYSIS.md)
- [Database ER diagram](docs/database/ER_DIAGRAM.md)
- [Database relational schema](docs/database/RELATIONAL_SCHEMA.md)
- [Database data dictionary](docs/database/DATA_DICTIONARY.md)
- [Normalization notes](docs/database/NORMALIZATION.md)
- [Backup and recovery](docs/database/BACKUP_AND_RECOVERY.md)
- [Deployment guidance](docs/assessment/DEPLOYMENT.md)
