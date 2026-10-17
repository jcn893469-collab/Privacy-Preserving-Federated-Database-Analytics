CREATE TABLE federated_database (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_name   VARCHAR(150)    NOT NULL,
    db_identifier       VARCHAR(100)    NOT NULL,
    description         TEXT,
    owner_user_id       BIGINT          NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    registered_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_federated_database_identifier UNIQUE (db_identifier),
    CONSTRAINT fk_federated_database_owner
        FOREIGN KEY (owner_user_id)
        REFERENCES app_user (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_federated_database_owner ON federated_database (owner_user_id);