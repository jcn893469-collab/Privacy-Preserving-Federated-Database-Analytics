# Data Dictionary

> Detailed descriptions of every column in the Privacy-Preserving Federated Analytics system.

---

## `app_user`

| Column | Business Meaning |
|---|---|
| `id` | System-generated surrogate key. Never exposed in URLs. |
| `email` | User's institutional email address. Used as login credential and unique identifier. |
| `password_hash` | BCrypt (strength 12) one-way hash. Plain-text password never stored. |
| `full_name` | User's display name shown in dashboards and audit logs. |
| `role` | Access level: **ADMIN** (full system control), **DATA_OWNER** (can contribute and manage own databases), **RESEARCHER** (can query aggregated federated results only). |
| `organization` | Institution or company name (e.g., "Apollo Hospitals"). Shown in contribution summaries. |
| `created_at` | UTC timestamp when the account was created. |
| `updated_at` | UTC timestamp of last profile update. Maintained by application code. |

---

## `federated_database`

| Column | Business Meaning |
|---|---|
| `id` | Surrogate PK for the federated database group. |
| `name` | Human-readable dataset group name (e.g., "National Blood Pressure Study"). |
| `description` | Optional longer description of the analytics purpose. |
| `domain` | Analytics domain tag (e.g., `healthcare`, `finance`). Used for filtering. |
| `owner_id` | FK to the `DATA_OWNER` user who registered this database. |
| `privacy_epsilon_budget` | Total ε (epsilon) privacy budget allocated to this database. Default 10.0. Contributions deduct from this budget. |
| `privacy_delta_budget` | Total δ (delta) failure probability budget. Default 1×10⁻⁵. |
| `privacy_epsilon_spent` | Accumulated ε spent across all approved contributions. Budget exhaustion is enforced by the service layer. |
| `is_active` | Soft-delete flag. Inactive databases no longer accept contributions. |
| `created_at` | Registration timestamp. |

---

## `analytics_contribution`

| Column | Business Meaning |
|---|---|
| `id` | Surrogate PK for the contribution record. |
| `contributor_id` | FK to the `DATA_OWNER` who submitted this contribution. |
| `database_id` | FK to the `federated_database` this contribution belongs to. |
| `record_count` | Number of local data records used to compute the aggregate (no raw records stored). |
| `noisy_mean` | Gaussian-noisy mean value computed locally by the client. |
| `noisy_variance` | Gaussian-noisy variance computed locally. |
| `noisy_sum` | Gaussian-noisy sum computed locally. |
| `epsilon_used` | ε consumed by this query. Deducted from the database's `privacy_epsilon_spent`. |
| `delta_used` | δ consumed by this query. |
| `noise_scale` | Computed noise scale = `global_sensitivity / epsilon`. Used to verify DP parameters. |
| `noise_sigma` | Gaussian standard deviation σ = `sensitivity × √(2 ln(1.25/δ)) / ε`. |
| `clipping_lower` | Lower bound used for data clipping before noise addition. |
| `clipping_upper` | Upper bound used for data clipping. Determines L∞ global sensitivity. |
| `sensitivity` | Global L∞ sensitivity = `clipping_upper - clipping_lower`. |
| `anomaly_count` | Number of rows flagged as anomalies by Z-score detector (|z| > threshold). |
| `anomaly_rate` | `anomaly_count / record_count`. Stored for audit and research purposes. |
| `column_name` | Name of the CSV column that was analyzed. |
| `status` | Lifecycle status: `PENDING` → `APPROVED` / `REJECTED`. Federated aggregation only uses `APPROVED` contributions. |
| `submitted_at` | Client-submitted UTC timestamp. |

---

## `dataset_metadata`

| Column | Business Meaning |
|---|---|
| `id` | Surrogate PK. |
| `database_id` | FK to `federated_database`. A database may have multiple datasets (e.g., one per year). |
| `dataset_name` | Catalog name for the dataset (e.g., "BP_Study_2024"). |
| `description` | Description of what the dataset contains. |
| `row_count_estimate` | Approximate number of local records (not exact, for display purposes only). |
| `column_count` | Number of columns in the source CSV schema. |
| `registered_at` | When the dataset was registered in the catalog. |

---

## `dataset_column_schema`

| Column | Business Meaning |
|---|---|
| `id` | Surrogate PK. |
| `dataset_id` | FK to `dataset_metadata`. |
| `column_name` | Name of the column (matches CSV header). |
| `data_type` | PostgreSQL/SQL type hint: `NUMERIC`, `TEXT`, `BOOLEAN`, `DATE`, etc. |
| `is_sensitive` | `TRUE` if this column requires Gaussian DP protection before aggregation. |
| `min_value` | Expected minimum value. Used to suggest clipping bounds to users. |
| `max_value` | Expected maximum value. Used to suggest clipping upper bound. |
| `description` | Business description of the column (e.g., "Systolic blood pressure in mmHg"). |

---

## `privacy_budget_ledger`

| Column | Business Meaning |
|---|---|
| `id` | Surrogate PK. |
| `user_id` | FK to `app_user`. Budget is tracked per user per database. |
| `database_id` | FK to `federated_database`. |
| `contribution_id` | FK to the specific `analytics_contribution` that caused this charge. NULL for manual admin adjustments. |
| `epsilon_charged` | ε deducted from the budget in this transaction. |
| `delta_charged` | δ deducted in this transaction. |
| `query_type` | Label for the query type: e.g., `GAUSSIAN_MEAN`, `GAUSSIAN_VARIANCE`. |
| `recorded_at` | UTC timestamp when the charge was recorded. |

---

## `federated_result`

| Column | Business Meaning |
|---|---|
| `id` | Surrogate PK. |
| `database_id` | FK to `federated_database`. |
| `run_id` | UUID v4 uniquely identifying a federated aggregation run. Used for idempotency. |
| `participant_count` | Number of distinct contributing organisations included in this run. |
| `aggregated_mean` | Record-count–weighted mean of all participant noisy means. |
| `aggregated_variance` | Record-count–weighted variance. |
| `total_records` | Sum of all participant `record_count` values. |
| `computed_at` | UTC timestamp when the federated aggregation was computed. |

---

## `federated_run_participant`

| Column | Business Meaning |
|---|---|
| `id` | Surrogate PK. |
| `result_id` | FK to `federated_result`. |
| `user_id` | FK to the `app_user` (DATA_OWNER) who participated. |
| `weight` | Contribution weight = `participant.record_count / total_records`. Used for weighted averaging. |
| `record_count` | Local record count for this participant in this run. |

---

## `audit_log`

| Column | Business Meaning |
|---|---|
| `id` | Surrogate PK. |
| `user_id` | FK to `app_user` who performed the action. NULL for automated system events (e.g., trigger-generated entries). |
| `action` | Action code, e.g., `CONTRIBUTION_SUBMITTED`, `USER_DELETED`, `BUDGET_EXHAUSTED`. |
| `entity_type` | Type of entity affected: `analytics_contribution`, `app_user`, `federated_database`, etc. |
| `entity_id` | ID of the specific entity affected. |
| `details` | Free-text or JSON-serialized extra context (e.g., old vs new values). |
| `ip_address` | Client IP address at time of action, for security auditing. |
| `occurred_at` | UTC timestamp of the event. |
