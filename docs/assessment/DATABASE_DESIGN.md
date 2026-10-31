# Database Design

The current schema has nine related base tables. The migration-derived ER
diagram, relational schema, data dictionary, and normalization notes are the
canonical design sources:

- [ER diagram](../database/ER_DIAGRAM.md)
- [Relational schema](../database/RELATIONAL_SCHEMA.md)
- [Data dictionary](../database/DATA_DICTIONARY.md)
- [Normalization notes](../database/NORMALIZATION.md)

The design contains more than four core entities and includes relationships
from users to federated databases, databases to contributions/datasets, runs
to participants, and other ledger/audit relationships. PK, FK, unique,
not-null, default, and check constraints are defined by the migrations.

Important caveats: organizations are currently represented by
`federated_database.organization_name`; the schema does not enforce that a
ledger's database matches its contribution's database, nor that a participant's
database matches its contribution's database. Those gaps are documented in
the normalization notes.
