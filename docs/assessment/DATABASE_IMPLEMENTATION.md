# Database Implementation

PostgreSQL DDL is managed by Flyway. Common migrations V1-V9 create and extend
the nine base tables, views, and aggregate function. V10 is a dev-only sample
seed; V11 extends the contribution audit trigger and creates the summary
procedure; V12 adds user token-version state for logout invalidation.

Normal reads and writes use Spring Data JPA repositories and transactional
services. The summary procedure is called through `JdbcTemplate` by
`GET /api/admin/procedures/organization-summary/{databaseId}`. The V11 trigger
audits contribution INSERT, UPDATE, and DELETE operations.

The implementation includes database views and grouped aggregates; the
queries are listed in [SQL_QUERIES.md](./SQL_QUERIES.md). The application uses
Spring `@Transactional` in service methods. PostgreSQL migration, procedure,
trigger, and commit/rollback behavior were not executable in the inspected
environment because PostgreSQL client tools and Docker were unavailable.
Do not present these as database-tested.
