# Build Progress

This log records work against the BCSE307L assessment prompt. Statuses reflect
the inspected repository and commands actually run; they are not claims of
deployment or external evidence.

| Phase | Status | Evidence / outcome |
|---|---|---|
| 1. Workspace inspection and gap analysis | Complete | Inventoried tracked files, Git history/branches, all Flyway migrations, entities, controllers, services, frontend files, scripts, configuration, and tests. Recorded findings in [REQUIREMENTS_GAP_ANALYSIS.md](assessment/REQUIREMENTS_GAP_ANALYSIS.md). |
| 2. Database design and documentation | Complete | Added migration-derived ER diagram, relational schema, data dictionary, and normalization notes under `docs/database/`. |
| 3. Schema and JPA changes | In progress | Added V11 procedure/trigger changes and V12 `app_user.token_version`; both are additive migrations. PostgreSQL migration execution is not available here. |
| 4. SQL features and transaction verification | Tests added; runtime pending | V11 adds and wires a summary procedure and audits INSERT/UPDATE/DELETE. Testcontainers tests cover procedure, views, trigger operations, HAVING, and rollback, but Docker is unavailable to run them. |
| 5. Frontend improvements | Partial | Added data-owner dataset/column management, a separate researcher dashboard, role redirects, and responsive breakpoints. Owner and researcher layouts were checked at 390px; API-backed interactions remain unverified. |
| 6. Authentication and configuration security | In progress | JWT/database secret fallbacks were removed. Logout increments persisted token version; the filter rejects revoked-version and inactive-user tokens. Browser token storage remains `localStorage`. |
| 7. Backup and recovery | In progress | Added PowerShell database scripts, docs, and updated shell scripts. No PostgreSQL client tools are installed, so backup/restore were not run. |
| 8. Docker and professional development | In progress | Compose now requires secrets and health checks target `/api/health`; Docker is unavailable for image execution. Git still has only `main` and two recorded commits. |
| 9. Testing and validation | Partial | `mvn -f backend\\pom.xml test` passed: 54 tests discovered, 0 failures, 0 errors, 4 PostgreSQL integration tests skipped because Docker is unavailable. JavaScript syntax and browser layout checks passed; no frontend package/build script exists. |
| 10. Assessment documentation and screenshots | In progress | Updated the gap analysis and screenshot checklist. Genuine database, Docker, deployment, and team evidence must still be captured from the user's actual environment. |
| 11. Dataset and column CRUD | Implemented; unit tested | Added typed create/update requests and non-sensitive response DTOs; implemented dataset and column read/update/delete operations; added owner-portal create/edit/delete and column-schema controls, search, and pagination. Service tests cover CRUD mutation paths. PostgreSQL/API end-to-end behavior remains untested. |
| 12. Researcher dashboard and role UI | Implemented; layout checked | Added a distinct researcher dashboard with federated summary, aggregation action, run search, and pagination. Login now routes each role to its own page, and the admin page redirects researchers. Researcher and owner layouts were checked in a browser at 390px width without horizontal overflow. |
| 13. HAVING, PostgreSQL migrations, and rollback tests | Tests added; not run against PostgreSQL | Added Testcontainers tests for all 12 Flyway migrations, dataset/column JPA CRUD/search/pagination, views, procedure, HAVING, all three trigger operations, and transactional rollback of contribution plus audit row. All 4 PostgreSQL tests were skipped because Docker is unavailable. |

## Repository evidence

- Current checked-out branch: `main`; only `main` and `origin/main` were listed.
- Recorded commits: `47968fb` (2026-10-28) and `40b9c9d` (2026-10-17).
- The repository contains neither an observed deployment URL nor Docker Hub
  publication evidence.
- The `docker`, `pg_dump`, `pg_restore`, and `psql` commands were unavailable
  in the current environment.
- PostgreSQL Testcontainers execution was skipped with the explicit reason
  `disabledWithoutDocker is true and Docker is not available`.
- There is no separate frontend package manifest or frontend build command.
  `node --check` was run for the shared portal script; responsive layout was
  manually checked in the browser at a 390px viewport for owner and researcher
  pages. API-backed frontend workflows were not verified.
- The private/public repository visibility discrepancy is user-reported; this
  inspection did not establish the question-paper or email text independently.
