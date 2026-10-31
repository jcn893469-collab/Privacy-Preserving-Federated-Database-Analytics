# Data Dictionary

This dictionary reflects Flyway migrations V1-V8 and V12. `PK` is primary key, `FK`
foreign key, `UQ` unique, and `NULL` means nullable.

## `app_user`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | User identifier. |
| `full_name` | VARCHAR(150) | NOT NULL | Display name. |
| `email` | VARCHAR(255) | NOT NULL, UQ | Login email. |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt password hash. |
| `role` | VARCHAR(30) | NOT NULL, DEFAULT `RESEARCHER`, CHECK | `ADMIN`, `DATA_OWNER`, or `RESEARCHER`. |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Whether authentication is allowed. |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Account creation time. |
| `token_version` | INTEGER | NOT NULL, DEFAULT 0, CHECK >= 0 | Incremented on logout or role/active changes; tokens with older versions are rejected. |

## `federated_database`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Federated participant/database identifier. |
| `organization_name` | VARCHAR(150) | NOT NULL | Display name of the participating organization. |
| `db_identifier` | VARCHAR(100) | NOT NULL, UQ | Stable unique database identifier. |
| `description` | TEXT | NULL | Organization/database description. |
| `owner_user_id` | BIGINT | NOT NULL, FK → `app_user.id` | Owning account; deletion restricted. |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Active participation flag. |
| `registered_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Registration time. |
| `privacy_epsilon_budget` | NUMERIC(10,4) | NOT NULL, DEFAULT 10.0000, CHECK > 0 | Maximum epsilon allocation. |
| `privacy_delta_budget` | NUMERIC(20,15) | NOT NULL, DEFAULT 0.000010000000000, CHECK (0,1) | Delta allocation. |
| `privacy_epsilon_spent` | NUMERIC(10,4) | NOT NULL, DEFAULT 0.0000 | Epsilon spent to date. |
| `rdp_accountant_state` | TEXT | NULL | Serialized persisted RDP accountant state. |

## `analytics_contribution`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Contribution identifier. |
| `database_id` | BIGINT | NOT NULL, FK → `federated_database.id` | Source participant; deletion restricted. |
| `record_count` | INTEGER | NOT NULL, CHECK > 0 | Local row count used for the aggregate. |
| `noisy_sum` | NUMERIC(18,4) | NOT NULL | Client-computed privacy-protected sum. |
| `epsilon` | NUMERIC(10,4) | NOT NULL, CHECK > 0 | Query epsilon. |
| `submitted_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Submission time. |
| `delta_value` | NUMERIC(20,15) | NOT NULL, DEFAULT 0.000010000000000 | Query delta. |
| `mechanism` | VARCHAR(40) | NOT NULL, DEFAULT `GAUSSIAN`, CHECK | Implemented privacy mechanism. |
| `clipping_lower` | NUMERIC(18,4) | NULL | Lower clipping bound. |
| `clipping_upper` | NUMERIC(18,4) | NULL | Upper clipping bound. |
| `sensitivity` | NUMERIC(18,4) | NULL | Sensitivity used for noise calibration. |
| `noise_sigma` | NUMERIC(18,8) | NULL | Gaussian standard deviation. |
| `noise_scale` | NUMERIC(18,8) | NULL | Stored noise scale. |
| `rdp_epsilon_cost` | NUMERIC(10,4) | NULL | Accounted RDP epsilon cost. |
| `anomaly_count` | INTEGER | NULL | Local anomaly count metadata. |
| `anomaly_rate` | NUMERIC(10,6) | NULL | Local anomaly fraction metadata. |

## `dataset_metadata`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Dataset identifier. |
| `database_id` | BIGINT | NOT NULL, FK → `federated_database.id`, ON DELETE CASCADE | Owning federated database. |
| `dataset_name` | VARCHAR(150) | NOT NULL; UQ with `database_id` | Dataset catalog name. |
| `description` | TEXT | NULL | Dataset description. |
| `schema_version` | VARCHAR(30) | NOT NULL, DEFAULT `1.0` | Dataset schema version. |
| `total_records` | INTEGER | NOT NULL, DEFAULT 0, CHECK >= 0 | Catalogued record count. |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation time. |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update time. |

## `dataset_column_schema`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Column metadata identifier. |
| `dataset_id` | BIGINT | NOT NULL, FK → `dataset_metadata.id`, ON DELETE CASCADE | Parent dataset. |
| `column_name` | VARCHAR(100) | NOT NULL; UQ with `dataset_id` | CSV/schema column name. |
| `data_type` | VARCHAR(50) | NOT NULL, DEFAULT `NUMERIC` | Declared data type. |
| `clipping_lower` | NUMERIC(18,4) | NOT NULL, DEFAULT 0.0000 | Lower clipping boundary. |
| `clipping_upper` | NUMERIC(18,4) | NOT NULL, DEFAULT 100.0000, CHECK > lower | Upper clipping boundary. |
| `sensitivity` | NUMERIC(18,4) | NOT NULL, DEFAULT 100.0000, CHECK > 0 | Configured sensitivity. |
| `is_target_metric` | BOOLEAN | NOT NULL, DEFAULT TRUE | Whether selected for analytics. |

## `privacy_budget_ledger`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Ledger entry identifier. |
| `database_id` | BIGINT | NOT NULL, FK → `federated_database.id`, ON DELETE CASCADE | Budget owner. |
| `contribution_id` | BIGINT | NOT NULL, FK → `analytics_contribution.id`, ON DELETE CASCADE | Contribution charged. |
| `pre_epsilon_spent` | NUMERIC(10,4) | NOT NULL | Spend before this charge. |
| `round_epsilon_cost` | NUMERIC(10,4) | NOT NULL, CHECK >= 0 | Epsilon charged. |
| `post_epsilon_spent` | NUMERIC(10,4) | NOT NULL, CHECK >= pre-spend | Spend after this charge. |
| `noise_scale` | NUMERIC(18,8) | NOT NULL | Recorded noise scale. |
| `noise_sigma` | NUMERIC(18,8) | NOT NULL | Recorded Gaussian sigma. |
| `evaluated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Charge evaluation time. |

## `federated_result`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Persisted federated run ID. |
| `run_identifier` | VARCHAR(100) | NOT NULL, UQ | Human-readable unique run key. |
| `computation_type` | VARCHAR(50) | NOT NULL, DEFAULT `FEDERATED_MEAN` | Aggregation type. |
| `target_metric` | VARCHAR(100) | NOT NULL, DEFAULT `overall` | Metric being aggregated. |
| `participating_org_count` | INTEGER | NOT NULL, CHECK >= 0 | Distinct participant count. |
| `total_contribution_count` | INTEGER | NOT NULL | Contribution count in the run. |
| `protected_total_records` | INTEGER | NOT NULL, CHECK >= 0 | Summed protected record count. |
| `protected_aggregate_sum` | NUMERIC(18,4) | NOT NULL | Aggregated noisy sum. |
| `global_protected_mean` | NUMERIC(18,6) | NOT NULL | Protected global mean. |
| `privacy_mechanism` | VARCHAR(50) | NOT NULL, DEFAULT `GAUSSIAN + RDP` | Mechanism description. |
| `executed_by_user_id` | BIGINT | NULL, FK → `app_user.id`, ON DELETE SET NULL | Executing account. |
| `executed_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Run time. |

## `federated_run_participant`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Participant snapshot identifier. |
| `run_id` | BIGINT | NOT NULL, FK → `federated_result.id`, ON DELETE CASCADE | Parent run. |
| `contribution_id` | BIGINT | NOT NULL, FK → `analytics_contribution.id`, ON DELETE CASCADE; UQ with `run_id` | Source contribution. |
| `database_id` | BIGINT | NOT NULL, FK → `federated_database.id`, ON DELETE CASCADE | Participating database. |
| `organization_name` | VARCHAR(150) | NOT NULL | Organization display name snapshot at run time. |
| `record_count` | INTEGER | NOT NULL | Contribution record count snapshot. |
| `noisy_sum` | NUMERIC(18,4) | NOT NULL | Contribution sum snapshot. |
| `contributed_at` | TIMESTAMP | NOT NULL | Contribution timestamp snapshot. |

## `audit_log`

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Audit event identifier. |
| `user_id` | BIGINT | NULL, FK → `app_user.id`, ON DELETE SET NULL | Actor, if known. |
| `user_email` | VARCHAR(255) | NULL | Actor email snapshot. |
| `action` | VARCHAR(100) | NOT NULL | Event action. |
| `entity_type` | VARCHAR(80) | NOT NULL | Affected entity type. |
| `entity_id` | BIGINT | NULL | Affected entity identifier. |
| `details` | TEXT | NULL | Event context. |
| `ip_address` | VARCHAR(64) | NULL | Recorded client address. |
| `status` | VARCHAR(30) | NOT NULL, DEFAULT `SUCCESS` | Event result. |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Event time. |
