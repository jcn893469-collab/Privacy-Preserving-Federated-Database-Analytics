# Testing and Results

## Executed backend tests

Command:

```powershell
mvn -f backend\pom.xml test
```

Result: **54 tests discovered, 0 failures, 0 errors, 4 skipped; Maven BUILD
SUCCESS**. The four skipped tests are Testcontainers PostgreSQL tests skipped
because Docker is unavailable; 50 non-PostgreSQL tests executed and passed.
Coverage includes authentication service behavior, JWT signing/validation,
dashboard and dataset authorization, dataset/column service CRUD, public
dashboard service, privacy math, local processing, and contribution behavior.

`org-portal.js` passed `node --check`; all four HTML pages' inline scripts
parsed with Node's JavaScript parser; and `git diff --check` passed. The owner and researcher pages were opened in a
browser at 390px width with no horizontal page overflow; the researcher page
was also checked at 1280px. The pages were loaded from local files, so their API
requests failed as expected; this was a layout check, not an end-to-end test.
There is no separate frontend package/build command.

## Not verified against PostgreSQL / Docker

- PostgreSQL migration, constraint, view, procedure, trigger, and
  commit/rollback tests.
- PostgreSQL-backed JPA dataset/column CRUD, search, and pagination.
- Backup/restore against a real PostgreSQL server.
- Docker build/Compose startup, authenticated browser workflows, or full
  viewport/device matrix.
- Cloud deployment.

Docker and PostgreSQL client tools were unavailable. Do not treat source
compilation or unit tests as evidence for those runtime behaviors.
