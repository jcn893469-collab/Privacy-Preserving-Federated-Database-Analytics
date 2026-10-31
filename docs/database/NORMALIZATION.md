# Normalization Notes

These notes describe the schema actually created by Flyway V1-V8, rather than
the superseded drafts in the original root-level database documents.

## First, second, and third normal forms

- **1NF:** Each row has a primary key and each stored field is scalar. Lists of
  columns, contributions, or participants are represented as separate rows.
- **2NF:** All declared primary keys are single-column surrogate keys, so there
  are no partial dependencies on a component of a composite primary key.
  Composite unique constraints (`database_id, dataset_name`,
  `dataset_id, column_name`, `run_id, contribution_id`) additionally prevent
  duplicate natural-key facts.
- **3NF:** Attributes describing users live in `app_user`; database ownership
  is represented by `owner_user_id`; contribution facts live in
  `analytics_contribution`; dataset and column facts are split between
  `dataset_metadata` and `dataset_column_schema`; run membership is in
  `federated_run_participant`; and budget events are in
  `privacy_budget_ledger`. The views derive aggregates rather than storing
  duplicate aggregate rows.

## Table-level rationale

| Relation | Normalization rationale |
|---|---|
| `app_user` | Account attributes depend on its key; email is a candidate key. Role values are constrained to the supported role set. |
| `federated_database` | The owner is referenced by FK rather than copying account data. A database's organization name and stable identifier describe that database registration. |
| `analytics_contribution` | Each row represents one protected aggregate contribution; owner/account attributes are obtained by joins through the database. Stored noise parameters document the actual released computation. |
| `dataset_metadata` | Dataset attributes are separated from database and column attributes; `(database_id, dataset_name)` prevents duplicate names within a database. |
| `dataset_column_schema` | Each row describes one dataset column; `(dataset_id, column_name)` is unique. |
| `privacy_budget_ledger` | Each row is an immutable charge event with before/after totals and the linked contribution. |
| `federated_result` | Each row identifies one persisted aggregate run. Its participant facts are stored separately. |
| `federated_run_participant` | Each row associates one contribution with a run. `organization_name`, `record_count`, `noisy_sum`, and `contributed_at` are snapshots used to preserve run-time evidence if source display data changes. |
| `audit_log` | Each row represents an event; user identity is an optional FK plus an email snapshot. |

## Integrity and modeling caveats

- `privacy_budget_ledger.database_id` and `contribution_id` are independently
  foreign-keyed. The current schema does not enforce that the contribution
  belongs to the same database named by the ledger row.
- `federated_run_participant.database_id` and `contribution_id` are also
  independent FKs; the database does not currently enforce that both refer to
  the same participant.
- `federated_run_participant.organization_name` and its contribution measures
  intentionally duplicate source values as historical snapshots; they are
  not used as authoritative current state.
- `federated_database.organization_name` represents the participant
  organization in the current design. A dedicated organization relation is
  not present.
- `privacy_epsilon_spent` is a maintained current-state total while the ledger
  records events. It is updated by application logic; no database trigger
  currently guarantees equality with a sum of ledger rows.
- `app_user.token_version` is per-account authentication state, not a copied
  user attribute; it is incremented to revoke existing JWTs on logout or
  privilege changes.

Therefore the design is normalized for the application's current workflow, but
the cross-FK consistency and maintained-budget-total caveats should be
addressed or explicitly tested before claiming full database-enforced
integrity.
