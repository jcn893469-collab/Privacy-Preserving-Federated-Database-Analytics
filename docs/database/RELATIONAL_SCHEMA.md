# Relational Schema

Canonical schema for the nine base tables created by `db/migration/common`
V1-V8 and extended by V12. Numeric precision, nullability, unique keys, check constraints, foreign
keys, and indexes below come from those migrations. Migration V9 adds the two
views and aggregate function described after the table definitions.

## Relations

- **APP_USER**(`id` PK, `full_name`, `email` UQ, `password_hash`, `role`,
  `is_active`, `created_at`, `token_version`)
  - `role` CHECK: `ADMIN`, `DATA_OWNER`, `RESEARCHER`.
- **FEDERATED_DATABASE**(`id` PK, `organization_name`, `db_identifier` UQ,
  `description`, `owner_user_id` FK → APP_USER.`id`, `is_active`,
  `registered_at`, `privacy_epsilon_budget`, `privacy_delta_budget`,
  `privacy_epsilon_spent`, `rdp_accountant_state`)
  - Owner delete is RESTRICT. Epsilon budget > 0; delta budget is between 0 and
    1 (exclusive).
- **ANALYTICS_CONTRIBUTION**(`id` PK, `database_id` FK →
  FEDERATED_DATABASE.`id`, `record_count`, `noisy_sum`, `epsilon`,
  `submitted_at`, `delta_value`, `mechanism`, `clipping_lower`,
  `clipping_upper`, `sensitivity`, `noise_sigma`, `noise_scale`,
  `rdp_epsilon_cost`, `anomaly_count`, `anomaly_rate`)
  - Database delete is RESTRICT. `record_count` > 0; `epsilon` > 0;
    `mechanism` is `GAUSSIAN`.
- **DATASET_METADATA**(`id` PK, `database_id` FK →
  FEDERATED_DATABASE.`id`, `dataset_name`, `description`, `schema_version`,
  `total_records`, `created_at`, `updated_at`)
  - `(database_id, dataset_name)` UQ; database delete cascades;
    `total_records` >= 0.
- **DATASET_COLUMN_SCHEMA**(`id` PK, `dataset_id` FK →
  DATASET_METADATA.`id`, `column_name`, `data_type`, `clipping_lower`,
  `clipping_upper`, `sensitivity`, `is_target_metric`)
  - `(dataset_id, column_name)` UQ; dataset delete cascades;
    `clipping_upper` > `clipping_lower`; `sensitivity` > 0.
- **PRIVACY_BUDGET_LEDGER**(`id` PK, `database_id` FK →
  FEDERATED_DATABASE.`id`, `contribution_id` FK →
  ANALYTICS_CONTRIBUTION.`id`, `pre_epsilon_spent`, `round_epsilon_cost`,
  `post_epsilon_spent`, `noise_scale`, `noise_sigma`, `evaluated_at`)
  - Both parent deletes cascade; round cost >= 0; post-spend >= pre-spend.
- **FEDERATED_RESULT**(`id` PK, `run_identifier` UQ, `computation_type`,
  `target_metric`, `participating_org_count`, `total_contribution_count`,
  `protected_total_records`, `protected_aggregate_sum`,
  `global_protected_mean`, `privacy_mechanism`, `executed_by_user_id` FK →
  APP_USER.`id`, `executed_at`)
  - Executor delete sets the FK to NULL; protected total records and
    participating organization count are >= 0.
- **FEDERATED_RUN_PARTICIPANT**(`id` PK, `run_id` FK →
  FEDERATED_RESULT.`id`, `contribution_id` FK →
  ANALYTICS_CONTRIBUTION.`id`, `database_id` FK →
  FEDERATED_DATABASE.`id`, `organization_name`, `record_count`, `noisy_sum`,
  `contributed_at`)
  - Parent deletes cascade; `(run_id, contribution_id)` UQ.
- **AUDIT_LOG**(`id` PK, nullable `user_id` FK → APP_USER.`id`, `user_email`,
  `action`, `entity_type`, `entity_id`, `details`, `ip_address`, `status`,
  `created_at`)
  - User delete sets the FK to NULL; `status` defaults to `SUCCESS`.

## Indexes

Primary/unique keys are indexed by PostgreSQL. Explicit indexes are:

| Table | Index columns |
|---|---|
| `app_user` | `email` |
| `federated_database` | `owner_user_id` |
| `analytics_contribution` | `database_id` |
| `dataset_metadata` | `database_id` |
| `dataset_column_schema` | `dataset_id` |
| `privacy_budget_ledger` | `database_id`, `contribution_id`, `evaluated_at` |
| `federated_result` | `executed_at` |
| `federated_run_participant` | `run_id`, `contribution_id` |
| `audit_log` | `user_id`, `action`, (`entity_type`, `entity_id`), `created_at DESC` |

## Views and stored SQL

- `v_organization_contribution_summary`: joins database, owner, and
  contributions; computes count/sum/averages, privacy budget remaining, and
  latest contribution.
- `v_federated_analytics_overview`: global contribution and privacy aggregates.
- `fn_calculate_federated_statistics(BIGINT DEFAULT NULL)`: PostgreSQL
  set-returning **function** for global or database-scoped statistics. It is
  not a PostgreSQL `PROCEDURE`; that remains a rubric gap.
- `trg_analytics_contribution_audit`: AFTER INSERT, UPDATE, or DELETE trigger
  added by V11.
- `sp_generate_organization_analytics_summary(BIGINT, JSONB)`: V11
  organization summary procedure, called by the admin API.

## Query examples

These examples use parameter placeholders and the current migration columns.

```sql
-- INNER JOIN and grouped contribution totals per owner/database
SELECT u.email, fd.organization_name, COUNT(ac.id) AS contributions,
       COALESCE(SUM(ac.record_count), 0) AS protected_records
FROM federated_database fd
JOIN app_user u ON u.id = fd.owner_user_id
JOIN analytics_contribution ac ON ac.database_id = fd.id
GROUP BY u.email, fd.id, fd.organization_name
HAVING COUNT(ac.id) >= 1;

-- LEFT JOIN retains databases with no contributions; includes an aggregate
SELECT fd.id, fd.organization_name, COUNT(ac.id) AS contribution_count,
       COALESCE(AVG(ac.record_count), 0) AS avg_records,
       COALESCE(SUM(ac.epsilon), 0) AS submitted_epsilon
FROM federated_database fd
LEFT JOIN analytics_contribution ac ON ac.database_id = fd.id
WHERE fd.is_active = TRUE
GROUP BY fd.id, fd.organization_name;

-- Multi-table dataset inventory
SELECT fd.organization_name, dm.dataset_name, COUNT(cs.id) AS column_count
FROM federated_database fd
JOIN dataset_metadata dm ON dm.database_id = fd.id
LEFT JOIN dataset_column_schema cs ON cs.dataset_id = dm.id
GROUP BY fd.id, fd.organization_name, dm.id, dm.dataset_name;

-- View query
SELECT organization_name, contribution_count, privacy_budget_remaining
FROM v_organization_contribution_summary
ORDER BY contribution_count DESC;

-- Existing aggregate function
SELECT * FROM fn_calculate_federated_statistics(NULL);
```

Normal application writes use JPA repositories and transactional service
methods. There are no database integration tests in the inspected test tree to
demonstrate the above SQL, migration execution, or rollback behavior.
