# Database ER Diagram

This diagram reflects the common Flyway migrations V1-V9. `federated_database`
is the existing organization/database boundary; a separate organization table
does not exist. A persisted aggregation run is represented by
`federated_result`.

```mermaid
erDiagram
    app_user ||--o{ federated_database : owns
    federated_database ||--o{ analytics_contribution : receives
    federated_database ||--o{ dataset_metadata : catalogs
    dataset_metadata ||--o{ dataset_column_schema : defines
    federated_database ||--o{ privacy_budget_ledger : accounts
    analytics_contribution ||--o{ privacy_budget_ledger : incurs
    app_user o|--o{ federated_result : executes
    federated_result ||--o{ federated_run_participant : contains
    analytics_contribution ||--o{ federated_run_participant : included_as
    federated_database ||--o{ federated_run_participant : participates
    app_user o|--o{ audit_log : actor

    app_user {
        bigint id PK
        varchar email UK
        varchar role
        varchar password_hash
        integer token_version
    }
    federated_database {
        bigint id PK
        bigint owner_user_id FK
        varchar db_identifier UK
        varchar organization_name
    }
    analytics_contribution {
        bigint id PK
        bigint database_id FK
        integer record_count
        numeric noisy_sum
    }
    dataset_metadata {
        bigint id PK
        bigint database_id FK
        varchar dataset_name
    }
    dataset_column_schema {
        bigint id PK
        bigint dataset_id FK
        varchar column_name
    }
    privacy_budget_ledger {
        bigint id PK
        bigint database_id FK
        bigint contribution_id FK
    }
    federated_result {
        bigint id PK
        bigint executed_by_user_id FK
        varchar run_identifier UK
    }
    federated_run_participant {
        bigint id PK
        bigint run_id FK
        bigint contribution_id FK
        bigint database_id FK
    }
    audit_log {
        bigint id PK
        bigint user_id FK
        varchar action
    }
```

The V9 read-only views are `v_organization_contribution_summary` and
`v_federated_analytics_overview`. They are not base tables and are omitted from
the relationship diagram.
