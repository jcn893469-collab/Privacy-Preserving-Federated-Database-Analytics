-- V8__create_audit_log_and_trigger.sql
-- Security audit logging table and database trigger for automated audit capture.

CREATE TABLE audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT,
    user_email      VARCHAR(255),
    action          VARCHAR(100)    NOT NULL,
    entity_type     VARCHAR(80)     NOT NULL,
    entity_id       BIGINT,
    details         TEXT,
    ip_address      VARCHAR(64),
    status          VARCHAR(30)     NOT NULL DEFAULT 'SUCCESS',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id)
        REFERENCES app_user (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_user ON audit_log (user_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);

-- Trigger Function: Automatically logs contribution insertions, updates, or deletions into audit_log
CREATE OR REPLACE FUNCTION fn_audit_contribution_change()
RETURNS TRIGGER AS $$
DECLARE
    v_db_name VARCHAR(150);
    v_owner_id BIGINT;
    v_owner_email VARCHAR(255);
BEGIN
    IF (TG_OP = 'INSERT') THEN
        SELECT fd.organization_name, u.id, u.email
        INTO v_db_name, v_owner_id, v_owner_email
        FROM federated_database fd
        LEFT JOIN app_user u ON fd.owner_user_id = u.id
        WHERE fd.id = NEW.database_id;

        INSERT INTO audit_log (
            user_id,
            user_email,
            action,
            entity_type,
            entity_id,
            details,
            status,
            created_at
        ) VALUES (
            v_owner_id,
            v_owner_email,
            'CONTRIBUTION_SUBMITTED',
            'ANALYTICS_CONTRIBUTION',
            NEW.id,
            CONCAT('Org: ', v_db_name, ', Records: ', NEW.record_count, ', DP Epsilon: ', NEW.epsilon, ', Noise Sigma: ', NEW.noise_sigma),
            'SUCCESS',
            CURRENT_TIMESTAMP
        );
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO audit_log (
            user_id,
            user_email,
            action,
            entity_type,
            entity_id,
            details,
            status,
            created_at
        ) VALUES (
            NULL,
            'SYSTEM',
            'CONTRIBUTION_DELETED',
            'ANALYTICS_CONTRIBUTION',
            OLD.id,
            CONCAT('Deleted contribution from database ID: ', OLD.database_id),
            'SUCCESS',
            CURRENT_TIMESTAMP
        );
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger binding on analytics_contribution table
DROP TRIGGER IF EXISTS trg_analytics_contribution_audit ON analytics_contribution;
CREATE TRIGGER trg_analytics_contribution_audit
AFTER INSERT OR DELETE ON analytics_contribution
FOR EACH ROW
EXECUTE FUNCTION fn_audit_contribution_change();
