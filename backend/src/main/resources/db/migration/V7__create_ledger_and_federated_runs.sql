-- V7__create_ledger_and_federated_runs.sql
-- Privacy budget transaction ledger and persisted federated computation runs.
-- Enables transaction rollback auditing, historical analytics traceability, and M:N participation.

CREATE TABLE privacy_budget_ledger (
    id                  BIGSERIAL       PRIMARY KEY,
    database_id         BIGINT          NOT NULL,
    contribution_id     BIGINT          NOT NULL,
    pre_epsilon_spent   NUMERIC(10, 4)  NOT NULL,
    round_epsilon_cost  NUMERIC(10, 4)  NOT NULL,
    post_epsilon_spent  NUMERIC(10, 4)  NOT NULL,
    noise_scale         NUMERIC(18, 8)  NOT NULL,
    noise_sigma         NUMERIC(18, 8)  NOT NULL,
    evaluated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_database
        FOREIGN KEY (database_id)
        REFERENCES federated_database (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ledger_contribution
        FOREIGN KEY (contribution_id)
        REFERENCES analytics_contribution (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_ledger_cost_positive CHECK (round_epsilon_cost >= 0),
    CONSTRAINT chk_ledger_post_spent CHECK (post_epsilon_spent >= pre_epsilon_spent)
);

CREATE INDEX idx_privacy_ledger_database ON privacy_budget_ledger (database_id);
CREATE INDEX idx_privacy_ledger_contribution ON privacy_budget_ledger (contribution_id);
CREATE INDEX idx_privacy_ledger_evaluated_at ON privacy_budget_ledger (evaluated_at);

CREATE TABLE federated_result (
    id                          BIGSERIAL       PRIMARY KEY,
    run_identifier              VARCHAR(100)    NOT NULL,
    computation_type            VARCHAR(50)     NOT NULL DEFAULT 'FEDERATED_MEAN',
    target_metric               VARCHAR(100)    NOT NULL DEFAULT 'overall',
    participating_org_count     INTEGER         NOT NULL,
    total_contribution_count    INTEGER         NOT NULL,
    protected_total_records     INTEGER         NOT NULL,
    protected_aggregate_sum     NUMERIC(18, 4)  NOT NULL,
    global_protected_mean       NUMERIC(18, 6)  NOT NULL,
    privacy_mechanism           VARCHAR(50)     NOT NULL DEFAULT 'GAUSSIAN + RDP',
    executed_by_user_id         BIGINT,
    executed_at                 TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_federated_result_executor
        FOREIGN KEY (executed_by_user_id)
        REFERENCES app_user (id)
        ON DELETE SET NULL,

    CONSTRAINT uq_federated_run_identifier UNIQUE (run_identifier),
    CONSTRAINT chk_run_records_nonneg CHECK (protected_total_records >= 0),
    CONSTRAINT chk_run_orgs_nonneg CHECK (participating_org_count >= 0)
);

CREATE INDEX idx_federated_result_executed_at ON federated_result (executed_at);

CREATE TABLE federated_run_participant (
    id                  BIGSERIAL       PRIMARY KEY,
    run_id              BIGINT          NOT NULL,
    contribution_id     BIGINT          NOT NULL,
    database_id         BIGINT          NOT NULL,
    organization_name   VARCHAR(150)    NOT NULL,
    record_count        INTEGER         NOT NULL,
    noisy_sum           NUMERIC(18, 4)  NOT NULL,
    contributed_at      TIMESTAMP       NOT NULL,

    CONSTRAINT fk_participant_run
        FOREIGN KEY (run_id)
        REFERENCES federated_result (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_participant_contribution
        FOREIGN KEY (contribution_id)
        REFERENCES analytics_contribution (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_participant_database
        FOREIGN KEY (database_id)
        REFERENCES federated_database (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_run_contribution UNIQUE (run_id, contribution_id)
);

CREATE INDEX idx_participant_run ON federated_run_participant (run_id);
CREATE INDEX idx_participant_contribution ON federated_run_participant (contribution_id);
