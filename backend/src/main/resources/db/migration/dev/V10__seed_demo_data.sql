-- V10__seed_demo_data.sql
-- Seeds default demonstration accounts and initial registered databases.

-- Seed Users:
-- 1. System Administrator (admin@vit.ac.in / Admin@123)
-- 2. Data Owner 1 - Apollo Hospitals (apollo@hospital.org / Apollo@123)
-- 3. Data Owner 2 - Fortis Healthcare (fortis@hospital.org / Fortis@123)
-- 4. Academic Researcher (researcher@vit.ac.in / Research@123)

INSERT INTO app_user (full_name, email, password_hash, role, is_active, created_at)
VALUES 
    ('System Administrator', 'admin@vit.ac.in', '$2a$10$LrKqtR7BdbxtZA3YBF2ph.tbSV3VFFIFoY.J8Y014vdfKrBHZbFqa', 'ADMIN', TRUE, CURRENT_TIMESTAMP),
    ('Apollo Data Custodian', 'apollo@hospital.org', '$2a$10$y1o0nB5VtzVd805glQDFn.JFvYTbehwOTDGruRdUS9vMae8xm/wJy', 'DATA_OWNER', TRUE, CURRENT_TIMESTAMP),
    ('Fortis Research Lead', 'fortis@hospital.org', '$2a$10$vMnnItmzj4rqe7XHHS2KxOICN9ljKLkM9A.QedOSw36TNZ8qlflwe', 'DATA_OWNER', TRUE, CURRENT_TIMESTAMP),
    ('VIT Clinical Researcher', 'researcher@vit.ac.in', '$2a$10$yNCNTJCwSlXd0XG7RvvaheyVxfCeMmviiyWjlJama2nOHW0suRFGu', 'RESEARCHER', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Seed Federated Databases for the Data Owners:
INSERT INTO federated_database (
    organization_name,
    db_identifier,
    description,
    owner_user_id,
    is_active,
    registered_at,
    privacy_epsilon_budget,
    privacy_delta_budget,
    privacy_epsilon_spent,
    rdp_accountant_state
)
SELECT 
    'Apollo Hospitals Network',
    'apollo-cardio-db',
    'Cardiology clinical data repository with privacy-preserving analytics integration.',
    u.id,
    TRUE,
    CURRENT_TIMESTAMP,
    10.0000,
    0.000010000000000,
    0.0000,
    NULL
FROM app_user u WHERE u.email = 'apollo@hospital.org'
ON CONFLICT (db_identifier) DO NOTHING;

INSERT INTO federated_database (
    organization_name,
    db_identifier,
    description,
    owner_user_id,
    is_active,
    registered_at,
    privacy_epsilon_budget,
    privacy_delta_budget,
    privacy_epsilon_spent,
    rdp_accountant_state
)
SELECT 
    'Fortis Healthcare Clinical',
    'fortis-oncology-db',
    'Regional oncology and vital statistics research node.',
    u.id,
    TRUE,
    CURRENT_TIMESTAMP,
    10.0000,
    0.000010000000000,
    0.0000,
    NULL
FROM app_user u WHERE u.email = 'fortis@hospital.org'
ON CONFLICT (db_identifier) DO NOTHING;

-- Seed Sample Dataset Metadata for Apollo
INSERT INTO dataset_metadata (database_id, dataset_name, description, schema_version, total_records, created_at, updated_at)
SELECT fd.id, 'Patient Vital Statistics 2026', 'Anonymized cardiovascular indicators and patient blood pressure indices.', '1.0', 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM federated_database fd WHERE fd.db_identifier = 'apollo-cardio-db'
ON CONFLICT (database_id, dataset_name) DO NOTHING;

-- Seed Dataset Column Schema
INSERT INTO dataset_column_schema (dataset_id, column_name, data_type, clipping_lower, clipping_upper, sensitivity, is_target_metric)
SELECT dm.id, 'systolic_bp', 'NUMERIC', 70.0000, 200.0000, 130.0000, TRUE
FROM dataset_metadata dm 
JOIN federated_database fd ON dm.database_id = fd.id
WHERE fd.db_identifier = 'apollo-cardio-db'
ON CONFLICT (dataset_id, column_name) DO NOTHING;

-- Seed Audit Log for initial bootstrap
INSERT INTO audit_log (user_id, user_email, action, entity_type, entity_id, details, status, created_at)
SELECT u.id, u.email, 'SYSTEM_BOOTSTRAP', 'SYSTEM', 1, 'Initial system bootstrap and demo data seeded successfully.', 'SUCCESS', CURRENT_TIMESTAMP
FROM app_user u WHERE u.email = 'admin@vit.ac.in';
