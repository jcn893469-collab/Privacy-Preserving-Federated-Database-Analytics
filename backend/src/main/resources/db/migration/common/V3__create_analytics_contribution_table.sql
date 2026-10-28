-- Stores privacy-preserving aggregate contributions submitted by participating
-- federated databases (clients). This table NEVER stores individual/raw records —
-- only client-side-computed, already-noised aggregate statistics.
CREATE TABLE analytics_contribution (
    id              BIGSERIAL       PRIMARY KEY,
    database_id     BIGINT          NOT NULL,
    record_count    INTEGER         NOT NULL,
    noisy_sum       NUMERIC(18, 4)  NOT NULL,
    epsilon         NUMERIC(10, 4)  NOT NULL,
    submitted_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_analytics_contribution_database
        FOREIGN KEY (database_id)
        REFERENCES federated_database (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_analytics_contribution_record_count CHECK (record_count > 0),
    CONSTRAINT chk_analytics_contribution_epsilon CHECK (epsilon > 0)
);

CREATE INDEX idx_analytics_contribution_database_id ON analytics_contribution (database_id);