CREATE OR REPLACE FUNCTION fn_audit_contribution_change()
RETURNS TRIGGER AS $$
DECLARE
    v_db_name VARCHAR(150);
    v_owner_id BIGINT;
    v_owner_email VARCHAR(255);
    v_action VARCHAR(100);
    v_details TEXT;
    v_entity_id BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_action := 'CONTRIBUTION_DELETED';
        v_details := CONCAT('Deleted contribution from database ID: ', OLD.database_id);
        v_entity_id := OLD.id;
        INSERT INTO audit_log (
            user_id, user_email, action, entity_type, entity_id, details, status
        ) VALUES (
            NULL, 'SYSTEM', v_action, 'ANALYTICS_CONTRIBUTION', v_entity_id,
            v_details, 'SUCCESS'
        );
        RETURN OLD;
    END IF;

    SELECT fd.organization_name, u.id, u.email
    INTO v_db_name, v_owner_id, v_owner_email
    FROM federated_database fd
    LEFT JOIN app_user u ON fd.owner_user_id = u.id
    WHERE fd.id = NEW.database_id;

    v_entity_id := NEW.id;
    IF TG_OP = 'INSERT' THEN
        v_action := 'CONTRIBUTION_SUBMITTED';
        v_details := CONCAT(
            'Org: ', v_db_name, ', Records: ', NEW.record_count,
            ', DP Epsilon: ', NEW.epsilon, ', Noise Sigma: ', NEW.noise_sigma
        );
    ELSE
        v_action := 'CONTRIBUTION_UPDATED';
        v_details := CONCAT(
            'Org: ', v_db_name, ', Records: ', OLD.record_count, ' -> ', NEW.record_count,
            ', DP Epsilon: ', OLD.epsilon, ' -> ', NEW.epsilon
        );
    END IF;

    INSERT INTO audit_log (
        user_id, user_email, action, entity_type, entity_id, details, status
    ) VALUES (
        v_owner_id, v_owner_email, v_action, 'ANALYTICS_CONTRIBUTION',
        v_entity_id, v_details, 'SUCCESS'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_analytics_contribution_audit ON analytics_contribution;
CREATE TRIGGER trg_analytics_contribution_audit
AFTER INSERT OR UPDATE OR DELETE ON analytics_contribution
FOR EACH ROW
EXECUTE FUNCTION fn_audit_contribution_change();

CREATE OR REPLACE PROCEDURE sp_generate_organization_analytics_summary(
    IN p_database_id BIGINT,
    INOUT p_summary JSONB DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT jsonb_build_object(
        'databaseId', fd.id,
        'organizationName', fd.organization_name,
        'dbIdentifier', fd.db_identifier,
        'active', fd.is_active,
        'ownerEmail', u.email,
        'contributionCount', COUNT(ac.id),
        'totalRecordsProtected', COALESCE(SUM(ac.record_count), 0),
        'totalNoisySum', COALESCE(SUM(ac.noisy_sum), 0.0000),
        'avgRecordsPerContribution', COALESCE(AVG(ac.record_count), 0.0000),
        'avgAnomalyRate', COALESCE(AVG(ac.anomaly_rate), 0.0000),
        'privacyEpsilonBudget', fd.privacy_epsilon_budget,
        'privacyEpsilonSpent', fd.privacy_epsilon_spent,
        'privacyBudgetRemaining', fd.privacy_epsilon_budget - fd.privacy_epsilon_spent,
        'lastContributionAt', MAX(ac.submitted_at)
    )
    INTO p_summary
    FROM federated_database fd
    JOIN app_user u ON u.id = fd.owner_user_id
    LEFT JOIN analytics_contribution ac ON ac.database_id = fd.id
    WHERE fd.id = p_database_id
    GROUP BY fd.id, u.email;

    IF NOT FOUND THEN
        p_summary := NULL;
    END IF;
END;
$$;
