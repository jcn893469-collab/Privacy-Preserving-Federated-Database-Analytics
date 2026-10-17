# Entity-Relationship Diagram

> All 9 main tables + 2 PostgreSQL views for the Privacy-Preserving Federated Analytics system.

---

## ER Diagram

```
┌──────────────────────┐         ┌──────────────────────────┐
│       app_user       │         │    federated_database     │
│─────────────────────-│         │──────────────────────────│
│ PK id (BIGSERIAL)    │◄───┐    │ PK id (BIGSERIAL)        │
│    email (UNIQUE)    │    │    │    name                  │
│    password_hash     │    │    │    description           │
│    full_name         │    │    │    domain                │
│    role (ENUM)       │    └────│ FK owner_id → app_user   │
│    organization      │         │    privacy_epsilon_budget│
│    created_at        │         │    privacy_delta_budget  │
│    updated_at        │         │    privacy_epsilon_spent │
└──────────────────────┘         │    is_active             │
           │                     │    created_at            │
           │                     └──────────┬───────────────┘
           │                                │
           │    ┌───────────────────────────┘
           │    │
           ▼    ▼
┌────────────────────────────────────────────────┐
│              analytics_contribution            │
│────────────────────────────────────────────────│
│ PK  id (BIGSERIAL)                             │
│ FK  contributor_id → app_user.id               │
│ FK  database_id    → federated_database.id     │
│     record_count                               │
│     noisy_mean                                 │
│     noisy_variance                             │
│     noisy_sum                                  │
│     epsilon_used                               │
│     delta_used                                 │
│     noise_scale                                │
│     noise_sigma                                │
│     clipping_lower                             │
│     clipping_upper                             │
│     sensitivity                                │
│     anomaly_count                              │
│     anomaly_rate                               │
│     column_name                                │
│     status (ENUM)                              │
│     submitted_at                               │
└────────────────────────────────────────────────┘
           │                    │
           │                    │
           ▼                    ▼
┌──────────────────┐  ┌────────────────────────────────┐
│  privacy_budget  │  │       federated_result         │
│    _ledger       │  │────────────────────────────────│
│──────────────────│  │ PK id (BIGSERIAL)              │
│ PK id            │  │ FK database_id                 │
│ FK user_id       │  │    run_id (UUID)               │
│ FK database_id   │  │    participant_count           │
│ FK contribution_id│  │    aggregated_mean            │
│    epsilon_charged│  │    aggregated_variance        │
│    delta_charged  │  │    total_records              │
│    query_type     │  │    computed_at                │
│    recorded_at    │  └──────────────┬───────────────┘
└──────────────────┘                 │
                                     │
                    ┌────────────────┘
                    ▼
     ┌─────────────────────────────────────┐
     │      federated_run_participant      │
     │─────────────────────────────────────│
     │ PK  id (BIGSERIAL)                  │
     │ FK  result_id → federated_result.id │
     │ FK  user_id   → app_user.id         │
     │     weight                          │
     │     record_count                    │
     │     UNIQUE (result_id, user_id)     │
     └─────────────────────────────────────┘

┌──────────────────────────────┐    ┌─────────────────────────────────┐
│       dataset_metadata       │    │       dataset_column_schema      │
│──────────────────────────────│    │─────────────────────────────────│
│ PK id (BIGSERIAL)            │    │ PK id (BIGSERIAL)               │
│ FK database_id               │    │ FK dataset_id → dataset_metadata│
│    dataset_name              │    │    column_name                  │
│    description               │    │    data_type                    │
│    row_count_estimate        │    │    is_sensitive (BOOL)          │
│    column_count              │    │    min_value                    │
│    registered_at             │    │    max_value                    │
└──────────────────────────────┘    │    description                  │
                                    └─────────────────────────────────┘

┌──────────────────────────────────────────────┐
│                  audit_log                   │
│──────────────────────────────────────────────│
│ PK id (BIGSERIAL)                            │
│ FK user_id (nullable)                        │
│    action (VARCHAR)                          │
│    entity_type                               │
│    entity_id                                 │
│    details (TEXT)                            │
│    ip_address                                │
│    occurred_at (TIMESTAMP)                   │
└──────────────────────────────────────────────┘
```

---

## PostgreSQL Views

### `v_organization_contribution_summary`
Aggregates per-organisation contribution statistics:
- `organization_name`, `contributor_email`
- `contribution_count`, `total_records`
- `avg_epsilon_used`, `avg_noisy_mean`
- `first_contribution`, `last_contribution`

### `v_federated_analytics_overview`
Cross-database overview for the admin dashboard:
- `database_name`, `owner_email`, `domain`
- `contribution_count`, `total_records`
- `avg_epsilon`, `privacy_epsilon_budget`, `privacy_epsilon_spent`
- `budget_utilization_pct`

---

## Stored Function

### `fn_calculate_federated_statistics(p_database_id BIGINT)`
Returns a table-valued result with:
- Weighted mean, variance, and standard deviation from all approved contributions
- Participant count and total record count
- Used by the `FederatedAggregationService` for ad-hoc federated analytics queries

---

## Relationships Summary

| Relationship | Cardinality |
|---|---|
| `app_user` → `federated_database` (owner) | 1 : N |
| `app_user` → `analytics_contribution` (contributor) | 1 : N |
| `federated_database` → `analytics_contribution` | 1 : N |
| `analytics_contribution` → `privacy_budget_ledger` | 1 : 1 |
| `federated_database` → `dataset_metadata` | 1 : N |
| `dataset_metadata` → `dataset_column_schema` | 1 : N |
| `federated_database` → `federated_result` | 1 : N |
| `federated_result` → `federated_run_participant` | 1 : N (M:N resolved) |
| `app_user` → `federated_run_participant` | 1 : N |
| `app_user` → `audit_log` | 1 : N (optional) |
