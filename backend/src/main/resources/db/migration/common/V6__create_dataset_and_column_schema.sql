-- V6__create_dataset_and_column_schema.sql
-- Normalized dataset catalog and column privacy configurations.
-- Satisfies 3NF: dataset-level metadata is separated from database and column schema.

CREATE TABLE dataset_metadata (
    id                  BIGSERIAL       PRIMARY KEY,
    database_id         BIGINT          NOT NULL,
    dataset_name        VARCHAR(150)    NOT NULL,
    description         TEXT,
    schema_version      VARCHAR(30)     NOT NULL DEFAULT '1.0',
    total_records       INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dataset_metadata_database
        FOREIGN KEY (database_id)
        REFERENCES federated_database (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_dataset_name_per_database UNIQUE (database_id, dataset_name),
    CONSTRAINT chk_dataset_total_records CHECK (total_records >= 0)
);

CREATE INDEX idx_dataset_metadata_database ON dataset_metadata (database_id);

CREATE TABLE dataset_column_schema (
    id                  BIGSERIAL       PRIMARY KEY,
    dataset_id          BIGINT          NOT NULL,
    column_name         VARCHAR(100)    NOT NULL,
    data_type           VARCHAR(50)     NOT NULL DEFAULT 'NUMERIC',
    clipping_lower      NUMERIC(18, 4)  NOT NULL DEFAULT 0.0000,
    clipping_upper      NUMERIC(18, 4)  NOT NULL DEFAULT 100.0000,
    sensitivity         NUMERIC(18, 4)  NOT NULL DEFAULT 100.0000,
    is_target_metric    BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_dataset_column_dataset
        FOREIGN KEY (dataset_id)
        REFERENCES dataset_metadata (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_column_per_dataset UNIQUE (dataset_id, column_name),
    CONSTRAINT chk_column_clipping_bounds CHECK (clipping_upper > clipping_lower),
    CONSTRAINT chk_column_sensitivity CHECK (sensitivity > 0)
);

CREATE INDEX idx_dataset_column_dataset ON dataset_column_schema (dataset_id);
