# SQL Queries and Advanced Database Features

The complete relational query examples and current column names are in
[docs/database/RELATIONAL_SCHEMA.md](../database/RELATIONAL_SCHEMA.md).

## Current database objects

- `v_organization_contribution_summary`: organization/database and owner
  details joined with contribution aggregates.
- `v_federated_analytics_overview`: global contribution and privacy aggregates.
- `fn_calculate_federated_statistics(BIGINT)`: existing set-returning
  PostgreSQL aggregate function.
- `sp_generate_organization_analytics_summary(BIGINT, JSONB)`: V11 stored
  procedure; called by the admin API and returns a JSONB summary.
- `trg_analytics_contribution_audit`: V11 row trigger for contribution
  insertion, update, and deletion.

## Examples

```sql
SELECT u.email, fd.organization_name, COUNT(ac.id) AS contribution_count,
       COALESCE(SUM(ac.record_count), 0) AS protected_records
FROM federated_database fd
JOIN app_user u ON u.id = fd.owner_user_id
LEFT JOIN analytics_contribution ac ON ac.database_id = fd.id
GROUP BY u.email, fd.id, fd.organization_name
HAVING COUNT(ac.id) > 0;

SELECT * FROM v_organization_contribution_summary
ORDER BY contribution_count DESC;

SELECT * FROM fn_calculate_federated_statistics(NULL);

CALL sp_generate_organization_analytics_summary(1, NULL);
```

These are schema-backed examples, not claims that they were run against a live
database in this session.
