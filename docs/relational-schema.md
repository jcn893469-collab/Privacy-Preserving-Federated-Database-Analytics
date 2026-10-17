# Relational Schema

> Full table definitions for the Privacy-Preserving Federated Analytics system.  
> All tables follow **Third Normal Form (3NF)**. See [`normalization.md`](normalization.md) for proof.

---

## Table: `app_user`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | Login email, globally unique |
| `password_hash` | `VARCHAR(255)` | NOT NULL | BCrypt hash (strength 12) |
| `full_name` | `VARCHAR(255)` | NOT NULL | Display name |
| `role` | `VARCHAR(50)` | NOT NULL | Enum: `ADMIN`, `DATA_OWNER`, `RESEARCHER` |
| `organization` | `VARCHAR(255)` | NULL | Organisation/institution name |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Row creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Last update timestamp |

**Indexes:** PK on `id`, UNIQUE on `email`

---

## Table: `federated_database`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `name` | `VARCHAR(255)` | NOT NULL | Database/dataset group name |
| `description` | `TEXT` | NULL | Human-readable description |
| `domain` | `VARCHAR(255)` | NULL | Analytics domain (e.g., healthcare) |
| `owner_id` | `BIGINT` | FK → `app_user.id`, NOT NULL | Data owner user |
| `privacy_epsilon_budget` | `DOUBLE PRECISION` | NOT NULL, DEFAULT 10.0 | Total ε budget |
| `privacy_delta_budget` | `DOUBLE PRECISION` | NOT NULL, DEFAULT 1e-5 | Total δ budget |
| `privacy_epsilon_spent` | `DOUBLE PRECISION` | NOT NULL, DEFAULT 0.0 | Spent ε (accumulated) |
| `is_active` | `BOOLEAN` | NOT NULL, DEFAULT TRUE | Soft-delete flag |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Row creation timestamp |

**Foreign Keys:** `owner_id` → `app_user(id)` ON DELETE RESTRICT

---

## Table: `analytics_contribution`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `contributor_id` | `BIGINT` | FK → `app_user.id`, NOT NULL | Contributing user |
| `database_id` | `BIGINT` | FK → `federated_database.id`, NOT NULL | Target federated database |
| `record_count` | `INTEGER` | NOT NULL | Number of local records |
| `noisy_mean` | `DOUBLE PRECISION` | NULL | Gaussian-noisy mean |
| `noisy_variance` | `DOUBLE PRECISION` | NULL | Gaussian-noisy variance |
| `noisy_sum` | `DOUBLE PRECISION` | NULL | Gaussian-noisy sum |
| `epsilon_used` | `DOUBLE PRECISION` | NOT NULL | ε consumed |
| `delta_used` | `DOUBLE PRECISION` | NOT NULL | δ consumed |
| `noise_scale` | `DOUBLE PRECISION` | NULL | Computed noise scale (GS/ε) |
| `noise_sigma` | `DOUBLE PRECISION` | NULL | Gaussian σ |
| `clipping_lower` | `DOUBLE PRECISION` | NULL | Lower clipping bound |
| `clipping_upper` | `DOUBLE PRECISION` | NULL | Upper clipping bound |
| `sensitivity` | `DOUBLE PRECISION` | NULL | L∞ global sensitivity |
| `anomaly_count` | `INTEGER` | NULL | Rows flagged by Z-score |
| `anomaly_rate` | `DOUBLE PRECISION` | NULL | Anomaly fraction [0, 1] |
| `column_name` | `VARCHAR(255)` | NULL | Column analyzed |
| `status` | `VARCHAR(50)` | NOT NULL, DEFAULT 'PENDING' | Status: PENDING, APPROVED, REJECTED |
| `submitted_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Submission timestamp |

**Foreign Keys:**  
- `contributor_id` → `app_user(id)` ON DELETE CASCADE  
- `database_id` → `federated_database(id)` ON DELETE CASCADE

---

## Table: `dataset_metadata`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `database_id` | `BIGINT` | FK → `federated_database.id`, NOT NULL | Parent database |
| `dataset_name` | `VARCHAR(255)` | NOT NULL | Dataset catalog name |
| `description` | `TEXT` | NULL | Description of the dataset |
| `row_count_estimate` | `BIGINT` | NULL | Estimated number of rows |
| `column_count` | `INTEGER` | NULL | Number of columns |
| `registered_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Registration timestamp |

---

## Table: `dataset_column_schema`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `dataset_id` | `BIGINT` | FK → `dataset_metadata.id`, NOT NULL | Parent dataset |
| `column_name` | `VARCHAR(255)` | NOT NULL | Column identifier |
| `data_type` | `VARCHAR(50)` | NULL | SQL data type (NUMERIC, TEXT, etc.) |
| `is_sensitive` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | Whether column needs DP protection |
| `min_value` | `DOUBLE PRECISION` | NULL | Expected minimum value |
| `max_value` | `DOUBLE PRECISION` | NULL | Expected maximum value |
| `description` | `TEXT` | NULL | Column description |

---

## Table: `privacy_budget_ledger`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `user_id` | `BIGINT` | FK → `app_user.id`, NOT NULL | Budget holder |
| `database_id` | `BIGINT` | FK → `federated_database.id`, NOT NULL | Target database |
| `contribution_id` | `BIGINT` | FK → `analytics_contribution.id`, NULL | Linked contribution |
| `epsilon_charged` | `DOUBLE PRECISION` | NOT NULL | ε deducted for this query |
| `delta_charged` | `DOUBLE PRECISION` | NOT NULL | δ deducted for this query |
| `query_type` | `VARCHAR(100)` | NULL | Type of DP query |
| `recorded_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | When charged |

---

## Table: `federated_result`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `database_id` | `BIGINT` | FK → `federated_database.id`, NOT NULL | Source database |
| `run_id` | `UUID` | NOT NULL, UNIQUE | Federated run identifier |
| `participant_count` | `INTEGER` | NOT NULL | Number of contributing orgs |
| `aggregated_mean` | `DOUBLE PRECISION` | NULL | Weighted federated mean |
| `aggregated_variance` | `DOUBLE PRECISION` | NULL | Weighted federated variance |
| `total_records` | `BIGINT` | NULL | Total record count across participants |
| `computed_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Computation timestamp |

---

## Table: `federated_run_participant`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `result_id` | `BIGINT` | FK → `federated_result.id`, NOT NULL | Parent run result |
| `user_id` | `BIGINT` | FK → `app_user.id`, NOT NULL | Participating user |
| `weight` | `DOUBLE PRECISION` | NOT NULL | Contribution weight (records/total) |
| `record_count` | `INTEGER` | NOT NULL | Records from this participant |

**Unique constraint:** (`result_id`, `user_id`)

---

## Table: `audit_log`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Auto-increment primary key |
| `user_id` | `BIGINT` | FK → `app_user.id`, NULL | Actor (NULL for system events) |
| `action` | `VARCHAR(255)` | NOT NULL | Action type (e.g., CONTRIBUTION_SUBMITTED) |
| `entity_type` | `VARCHAR(100)` | NULL | Affected entity type |
| `entity_id` | `BIGINT` | NULL | Affected entity ID |
| `details` | `TEXT` | NULL | Free-text details/JSON |
| `ip_address` | `VARCHAR(45)` | NULL | Client IP address |
| `occurred_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Event timestamp |

---

## Views

### `v_organization_contribution_summary`
```sql
SELECT
    u.organization AS organization_name,
    u.email        AS contributor_email,
    COUNT(ac.id)   AS contribution_count,
    SUM(ac.record_count)  AS total_records,
    AVG(ac.epsilon_used)  AS avg_epsilon_used,
    AVG(ac.noisy_mean)    AS avg_noisy_mean,
    MIN(ac.submitted_at)  AS first_contribution,
    MAX(ac.submitted_at)  AS last_contribution
FROM analytics_contribution ac
JOIN app_user u ON u.id = ac.contributor_id
GROUP BY u.organization, u.email;
```

### `v_federated_analytics_overview`
```sql
SELECT
    fd.name  AS database_name,
    u.email  AS owner_email,
    fd.domain,
    COUNT(ac.id)           AS contribution_count,
    SUM(ac.record_count)   AS total_records,
    AVG(ac.epsilon_used)   AS avg_epsilon,
    fd.privacy_epsilon_budget,
    fd.privacy_epsilon_spent,
    ROUND((fd.privacy_epsilon_spent / NULLIF(fd.privacy_epsilon_budget, 0)) * 100, 2)
        AS budget_utilization_pct
FROM federated_database fd
JOIN app_user u ON u.id = fd.owner_id
LEFT JOIN analytics_contribution ac ON ac.database_id = fd.id
GROUP BY fd.id, fd.name, u.email, fd.domain,
         fd.privacy_epsilon_budget, fd.privacy_epsilon_spent;
```

---

## Audit Trigger

```sql
-- Function: fn_audit_contribution_change()
-- Trigger:  trg_analytics_contribution_audit
-- Fires:    AFTER INSERT OR UPDATE OR DELETE on analytics_contribution
-- Purpose:  Automatically logs row-level changes to audit_log table
```
