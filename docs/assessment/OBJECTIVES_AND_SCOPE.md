# Objectives and Scope

## Objectives

- Demonstrate local data processing and privacy-protected contributions.
- Persist users, participating databases, datasets, contributions, privacy
  spending, audit events, and federated run results in PostgreSQL.
- Provide role-aware APIs and dashboards for administrators, data owners, and
  researchers.
- Demonstrate normalized relational design, Flyway schema management,
  aggregate SQL, views, a stored procedure, and a database trigger.
- Provide reproducible local/Docker startup, database backup instructions,
  tests, and evidence-oriented documentation.

## Current scope

The current data boundary is `federated_database`; an organization is not a
separate entity. `federated_result` represents persisted aggregation runs.
Standalone organization CRUD, complete dataset-column CRUD, cloud deployment,
and Docker Hub publication are outside the verified current implementation.
See the gap analysis before treating any partial feature as rubric-complete.
