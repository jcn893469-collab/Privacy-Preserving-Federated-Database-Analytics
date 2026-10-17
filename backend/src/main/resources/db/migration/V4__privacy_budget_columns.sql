-- Per-organization privacy budget and persisted RDP accountant state.
-- Required so cumulative Gaussian / RDP spend survives restarts.

ALTER TABLE federated_database
    ADD COLUMN IF NOT EXISTS privacy_epsilon_budget NUMERIC(10, 4) NOT NULL DEFAULT 10.0000,
    ADD COLUMN IF NOT EXISTS privacy_delta_budget NUMERIC(20, 15) NOT NULL DEFAULT 0.000010000000000,
    ADD COLUMN IF NOT EXISTS privacy_epsilon_spent NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    ADD COLUMN IF NOT EXISTS rdp_accountant_state TEXT;

ALTER TABLE federated_database
    ADD CONSTRAINT chk_federated_database_epsilon_budget CHECK (privacy_epsilon_budget > 0),
    ADD CONSTRAINT chk_federated_database_delta_budget CHECK (privacy_delta_budget > 0 AND privacy_delta_budget < 1);
