# Requirements Gap Analysis

This is the updated compliance status for the BCSE307L project prompt. Findings
are based on source, migrations, automated tests, browser layout checks, and
local Git metadata. PostgreSQL integration tests are included, but they were
skipped because Docker is unavailable in this environment; their presence is
not evidence of successful database execution.

## Summary

The application has nine migration-defined base tables and source-level support
for dataset/column CRUD, a researcher-only dashboard, HAVING query examples,
and PostgreSQL integration tests. The full Maven suite passes its runnable
tests; the four PostgreSQL integration tests are skipped without Docker, so
migrations, views, procedure, trigger, and rollback behavior remain unverified
against a live database. Cloud deployment and genuine submission screenshots
are not claimed.

## Database design and implementation

| Requirement | Status | Evidence and gap |
|---|---|---|
| At least 8 related tables | **Complete** | V1-V8 define `app_user`, `federated_database`, `analytics_contribution`, `dataset_metadata`, `dataset_column_schema`, `privacy_budget_ledger`, `federated_result`, `federated_run_participant`, and `audit_log`. |
| Primary/foreign keys and integrity | **Complete** | All nine tables have primary keys and relevant FKs. Unique, check, default, and not-null constraints are present across migrations. |
| Indexes | **Complete** | Migrations define indexes for frequently queried FK, timestamp, and lookup columns, as well as unique constraints. |
| Normalization and documentation match implementation | **Partial** | Current `docs/` design drafts describe obsolete columns and relationships (for example `contributor_id`, `name`, and `owner_id`), rather than the migration schema. Canonical migration-derived documents were added under `docs/database/`; the legacy files still need consolidation. |
| Organization and analytics-job entities | **Partial** | Organization identity is currently represented by `federated_database.organization_name`; persisted analytics runs are represented by `federated_result`. There is no separate `organization` or `analytics_job` table. These existing structures provide equivalents for the current workflow; CRUD for standalone organizations/jobs is not implemented. |
| CRUD coverage | **IMPLEMENTED BUT NOT TESTED** | Dataset metadata now has create/read/update/delete endpoints; column schema has create/read/update/delete endpoints. Service unit tests cover create/update/delete with mocked repositories. The PostgreSQL-backed CRUD path has not run. |
| JOIN and aggregate SQL | **IMPLEMENTED BUT NOT TESTED** | V9 views and documentation demonstrate joins and COUNT/SUM/AVG; `docs/assessment/SQL_QUERIES.md` includes a GROUP BY/HAVING query. The PostgreSQL integration test executes that query when Docker is available, but this run was skipped. |
| Database views | **Complete** | V9 creates `v_organization_contribution_summary` and `v_federated_analytics_overview`; a JPA read-only entity/repository maps the organization summary view. |
| Stored procedure | **Partial** | V11 adds `sp_generate_organization_analytics_summary`, wired to an admin API. PostgreSQL execution has not been tested in this environment. V9's separate aggregate function remains. |
| Trigger coverage | **Partial** | V11 replaces the V8 trigger binding to audit contribution INSERT, UPDATE, and DELETE. PostgreSQL execution has not been tested in this environment. |
| Transactions | **IMPLEMENTED BUT NOT TESTED** | Service methods use Spring `@Transactional`. A PostgreSQL integration test verifies that rollback removes both a contribution and its trigger-generated audit row; it was skipped because Docker is unavailable. |
| ORM integration | **Complete** | Spring Data JPA entities and repositories correspond to migration-backed tables and a view. |

## Application and frontend

| Requirement | Status | Evidence and gap |
|---|---|---|
| Login and dashboards | **IMPLEMENTED BUT NOT TESTED** | Login routes ADMIN, DATA_OWNER, and RESEARCHER to separate dashboards. The researcher page contains federated summary/run/history features and no admin navigation. Static browser layout was checked, but authenticated API behavior was not. |
| Responsive UI | **PARTIALLY IMPLEMENTED** | Shared responsive breakpoints and horizontally scrollable tables are in place. Researcher and owner pages were checked at a 390px viewport with no page overflow; API-backed full interaction and all-page device coverage remain untested. |
| CSV and local privacy processing | **Complete** | `org-portal.js` performs client-side CSV handling and protected aggregate submission; Java contains CSV and privacy-processing utilities. No raw-data upload API is present, consistent with the privacy design. |
| Organization/database and dataset management | **IMPLEMENTED BUT NOT TESTED** | Dataset metadata and column schema now have CRUD UI and typed API requests. Repository-level service tests pass; PostgreSQL persistence and full API/browser workflows remain untested. |
| Search, filtering, and pagination | **PARTIALLY IMPLEMENTED** | Dataset search now uses paged results when search/page/size parameters are supplied, with page-size bounds; the owner portal exposes search and paging. Other existing endpoints have their own filtering and paging. The database-backed UI flow was not run. |
| Form validation and exception handling | **PARTIALLY IMPLEMENTED** | Dataset and column API bodies now use typed DTOs and Bean Validation, including non-negative counts, field lengths, numeric precision, and clipping bounds. Other unrelated endpoints still need a broader validation pass. |
| API documentation | **PARTIALLY IMPLEMENTED** | README has an API overview with the current dataset and column CRUD routes; a complete OpenAPI/API specification is still absent. |

## Authentication and security

| Requirement | Status | Evidence and gap |
|---|---|---|
| Single user table, BCrypt, JWT, three roles | **Complete** | `app_user`, `BCryptPasswordEncoder`, JWT service/filter, and `ADMIN`, `DATA_OWNER`, `RESEARCHER` role checks exist. |
| Role-based API access | **PARTIALLY IMPLEMENTED** | `SecurityConfig` and owner checks enforce the routes. MockMvc tests cover ADMIN, DATA_OWNER, and RESEARCHER dashboard/API permissions, including dataset routes. This is representative coverage, not an exhaustive matrix for every protected HTTP operation. |
| Login and current-user lookup | **Complete** | `/api/auth/login` and authenticated `/api/auth/me` are implemented. |
| Logout and token invalidation | **Not complete** | No logout endpoint or revocation mechanism exists; issued tokens remain valid until expiration. |
| Environment-based secrets | **Partial** | JWT and datasource secrets are now required via environment variables; a placeholder-only `.env.example` is tracked. Existing V10 development seed accounts remain intentionally limited to the dev migration. |
| Logout and token invalidation | **Partial** | `POST /api/auth/logout` increments a persisted `app_user.token_version`; the JWT filter rejects older versions and inactive users. Browser storage remains `localStorage`, so XSS-hardening and browser-level verification remain open. |
| SQL injection resistance | **Partial** | Normal data operations use JPA repositories and parameterized queries. This inventory does not establish a comprehensive injection test or review of every query surface. |
| User/role registration security | **Partial** | Public registration is exposed for a constrained flow; tests cover service login/registration behavior but not the complete API authorization matrix. |

## Backup, Docker, and professional delivery

| Requirement | Status | Evidence and gap |
|---|---|---|
| PostgreSQL backups | **Partial** | Requested PowerShell scripts and updated shell scripts use custom-format `pg_dump` archives, require credentials, fail explicitly, and use matching `pg_restore` behavior. No live database tools were available to execute them. |
| Tested recovery | **Not complete** | No restore execution evidence is available. The restore script reports tool and restore errors and runs a post-restore table-count query, but has not been exercised. |
| Requested database script paths/docs | **Complete** | Added `scripts/database/backup.ps1`, `scripts/database/restore.ps1`, and `docs/database/BACKUP_AND_RECOVERY.md`. |
| Docker and persistent PostgreSQL | **Partial** | A multi-stage backend Dockerfile and PostgreSQL Compose service with persistent volume exist. Compose/image execution was not available for verification. |
| Docker health checks | **Partial** | Health checks now target the existing `/api/health` endpoint and the runtime image installs `curl`; Docker is unavailable to verify the built image. |
| Secret-safe deployment configuration | **Partial** | Compose now requires `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`; `.env.example` contains blank placeholders. Docker was unavailable for runtime verification. |
| Git history and branches | **Not complete** | Git reports two commits total and only `main`/`origin/main`; no `dev` branch or ten-commit history is present. No history was rewritten. |
| README and `.gitignore` | **Partial** | README and `.gitignore` were updated for current setup, schema, tests, and backup paths. A complete API spec remains absent. |
| Docker Hub/cloud deployment | **Not evidenced** | No image publication or live deployment URL was found; do not claim these. |
| Private/public repository discrepancy | **Unverified** | The prompt reports conflicting instructions. This checkout does not provide the source documents to verify the conflict; repository visibility was not changed. |

## Testing and assessment evidence

| Requirement | Status | Evidence and gap |
|---|---|---|
| Unit tests | **COMPLETED AND TESTED** | `mvn -f backend\\pom.xml test` passed: 54 tests discovered, 0 failures, 0 errors, 4 skipped. The four skipped tests are PostgreSQL integration tests, skipped because Docker is unavailable. |
| Integration/database tests | **IMPLEMENTED BUT NOT TESTED** | PostgreSQL Testcontainers tests cover all 12 Flyway migrations, all nine tables/two views, dataset/column JPA CRUD/search/pagination, the HAVING query, stored procedure, INSERT/UPDATE/DELETE trigger rows, and rollback atomicity. All four database tests were skipped due to missing Docker; no live PostgreSQL test is claimed. |
| Role and dataset service tests | **PARTIALLY IMPLEMENTED** | Unit tests cover dataset/column create, update, and delete service paths; MockMvc covers representative authorization outcomes for all three roles. Full PostgreSQL/API CRUD and exhaustive role-route coverage remain outstanding. |
| Assessment documents | **PARTIALLY IMPLEMENTED** | Migration-derived database docs and the assessment set exist; legacy documents still need reconciliation, and actual runtime evidence is outstanding. |
| Team/contribution claims | **Not evidenced** | No team-responsibility data or verified contribution report is available. Do not invent it. |
| Screenshot evidence | **Not complete** | No screenshots are included or claimed. Capture the updated researcher and dataset/schema pages, responsive views, database evidence, and role outcomes from a real running environment using `docs/assessment/SCREENSHOT_CHECKLIST.md`. |

## Git evidence captured

The current branch is `main`; the listed history was:

| Commit | Date | Subject |
|---|---|---|
| `47968fb` | 2026-10-28 | Initial commit - Privacy-Preserving Federated Database Analytics |
| `40b9c9d` | 2026-10-17 | feat: complete privacy-preserving federated analytics full-stack platform |

These are the only commits visible in this checkout; they do not prove any
external team contribution or deployment.
