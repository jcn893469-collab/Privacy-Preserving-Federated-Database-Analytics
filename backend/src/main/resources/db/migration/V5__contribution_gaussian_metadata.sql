-- Metadata for Gaussian-mechanism contributions. Still NO raw records.

ALTER TABLE analytics_contribution
    ADD COLUMN IF NOT EXISTS delta_value NUMERIC(20, 15) NOT NULL DEFAULT 0.000010000000000,
    ADD COLUMN IF NOT EXISTS mechanism VARCHAR(40) NOT NULL DEFAULT 'GAUSSIAN',
    ADD COLUMN IF NOT EXISTS clipping_lower NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS clipping_upper NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS sensitivity NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS noise_sigma NUMERIC(18, 8),
    ADD COLUMN IF NOT EXISTS noise_scale NUMERIC(18, 8),
    ADD COLUMN IF NOT EXISTS rdp_epsilon_cost NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS anomaly_count INTEGER,
    ADD COLUMN IF NOT EXISTS anomaly_rate NUMERIC(10, 6);

ALTER TABLE analytics_contribution
    ADD CONSTRAINT chk_analytics_contribution_mechanism CHECK (mechanism IN ('GAUSSIAN'));
