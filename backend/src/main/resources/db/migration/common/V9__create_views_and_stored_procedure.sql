-- V9__create_views_and_stored_procedure.sql
-- PostgreSQL Database Views and Stored Function.

-- View 1: Organization Contribution & Privacy Summary View
-- Computes real-time aggregated metrics per organization database across joins.
CREATE OR REPLACE VIEW v_organization_contribution_summary AS
SELECT 
    fd.id AS database_id,
    fd.organization_name,
    fd.db_identifier,
    fd.is_active,
    u.email AS owner_email,
    u.full_name AS owner_name,
    COUNT(ac.id) AS contribution_count,
    COALESCE(SUM(ac.record_count), 0) AS total_records_protected,
    COALESCE(SUM(ac.noisy_sum), 0.0000) AS total_noisy_sum,
    COALESCE(AVG(ac.record_count), 0.0000) AS avg_records_per_contribution,
    COALESCE(AVG(ac.anomaly_rate), 0.0000) AS avg_anomaly_rate,
    COALESCE(SUM(ac.anomaly_count), 0) AS total_anomalies_flagged,
    fd.privacy_epsilon_budget,
    fd.privacy_epsilon_spent,
    (fd.privacy_epsilon_budget - fd.privacy_epsilon_spent) AS privacy_budget_remaining,
    fd.registered_at,
    MAX(ac.submitted_at) AS last_contribution_at
FROM federated_database fd
JOIN app_user u ON fd.owner_user_id = u.id
LEFT JOIN analytics_contribution ac ON fd.id = ac.database_id
GROUP BY 
    fd.id, fd.organization_name, fd.db_identifier, fd.is_active, 
    u.email, u.full_name, fd.privacy_epsilon_budget, fd.privacy_epsilon_spent, fd.registered_at;

-- View 2: Global Federated Analytics Overview View
CREATE OR REPLACE VIEW v_federated_analytics_overview AS
SELECT
    COUNT(DISTINCT fd.id) AS total_registered_organizations,
    COUNT(DISTINCT CASE WHEN ac.id IS NOT NULL THEN fd.id END) AS participating_organizations,
    COUNT(ac.id) AS total_contributions,
    COALESCE(SUM(ac.record_count), 0) AS total_protected_records,
    COALESCE(SUM(ac.noisy_sum), 0.0000) AS global_noisy_sum,
    CASE 
        WHEN COALESCE(SUM(ac.record_count), 0) > 0 
        THEN ROUND(CAST(COALESCE(SUM(ac.noisy_sum), 0.0000) / SUM(ac.record_count) AS NUMERIC), 4)
        ELSE 0.0000 
    END AS global_protected_mean,
    COALESCE(AVG(ac.record_count), 0.0) AS avg_record_count,
    COALESCE(MIN(ac.record_count), 0) AS min_record_count,
    COALESCE(MAX(ac.record_count), 0) AS max_record_count,
    COALESCE(AVG(ac.anomaly_rate), 0.0) AS system_avg_anomaly_rate,
    COALESCE(SUM(fd.privacy_epsilon_spent), 0.0000) AS total_privacy_budget_spent
FROM federated_database fd
LEFT JOIN analytics_contribution ac ON fd.id = ac.database_id;

-- Stored Function / Procedure: fn_calculate_federated_statistics
-- Computes and returns aggregated federated metrics for all or a specific database ID
CREATE OR REPLACE FUNCTION fn_calculate_federated_statistics(p_database_id BIGINT DEFAULT NULL)
RETURNS TABLE (
    p_participating_orgs BIGINT,
    p_contribution_count BIGINT,
    p_total_records BIGINT,
    p_aggregate_noisy_sum NUMERIC,
    p_global_mean NUMERIC,
    p_avg_records NUMERIC,
    p_min_records INTEGER,
    p_max_records INTEGER
) AS $$
BEGIN
    IF p_database_id IS NOT NULL THEN
        RETURN QUERY
        SELECT 
            COUNT(DISTINCT ac.database_id) AS p_participating_orgs,
            COUNT(ac.id) AS p_contribution_count,
            COALESCE(SUM(ac.record_count), 0)::BIGINT AS p_total_records,
            COALESCE(SUM(ac.noisy_sum), 0.0000)::NUMERIC AS p_aggregate_noisy_sum,
            CASE 
                WHEN COALESCE(SUM(ac.record_count), 0) > 0 
                THEN ROUND(CAST(SUM(ac.noisy_sum) / SUM(ac.record_count) AS NUMERIC), 4)
                ELSE 0.0000 
            END AS p_global_mean,
            COALESCE(AVG(ac.record_count), 0.0)::NUMERIC AS p_avg_records,
            COALESCE(MIN(ac.record_count), 0)::INTEGER AS p_min_records,
            COALESCE(MAX(ac.record_count), 0)::INTEGER AS p_max_records
        FROM analytics_contribution ac
        WHERE ac.database_id = p_database_id;
    ELSE
        RETURN QUERY
        SELECT 
            COUNT(DISTINCT ac.database_id) AS p_participating_orgs,
            COUNT(ac.id) AS p_contribution_count,
            COALESCE(SUM(ac.record_count), 0)::BIGINT AS p_total_records,
            COALESCE(SUM(ac.noisy_sum), 0.0000)::NUMERIC AS p_aggregate_noisy_sum,
            CASE 
                WHEN COALESCE(SUM(ac.record_count), 0) > 0 
                THEN ROUND(CAST(SUM(ac.noisy_sum) / SUM(ac.record_count) AS NUMERIC), 4)
                ELSE 0.0000 
            END AS p_global_mean,
            COALESCE(AVG(ac.record_count), 0.0)::NUMERIC AS p_avg_records,
            COALESCE(MIN(ac.record_count), 0)::INTEGER AS p_min_records,
            COALESCE(MAX(ac.record_count), 0)::INTEGER AS p_max_records
        FROM analytics_contribution ac;
    END IF;
END;
$$ LANGUAGE plpgsql;
