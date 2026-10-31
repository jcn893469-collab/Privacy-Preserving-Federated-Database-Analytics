-- V13__seed_production_demo_accounts.sql
-- Create demonstration accounts for production evaluation.

INSERT INTO app_user (
    full_name,
    email,
    password_hash,
    role,
    is_active,
    created_at
)
VALUES
    (
        'System Administrator',
        'admin@vit.ac.in',
        '$2a$10$LrKqtR7BdbxtZA3YBF2ph.tbSV3VFFIFoY.J8Y014vdfKrBHZbFqa',
        'ADMIN',
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'Apollo Data Custodian',
        'apollo@hospital.org',
        '$2a$10$y1o0nB5VtzVd805glQDFn.JFvYTbehwOTDGruRdUS9vMae8xm/wJy',
        'DATA_OWNER',
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'Fortis Research Lead',
        'fortis@hospital.org',
        '$2a$10$vMnnItmzj4rqe7XHHS2KxOICN9ljKLkM9A.QedOSw36TNZ8qlflwe',
        'DATA_OWNER',
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'VIT Clinical Researcher',
        'researcher@vit.ac.in',
        '$2a$10$yNCNTJCwSlXd0XG7RvvaheyVxfCeMmviiyWjlJama2nOHW0suRFGu',
        'RESEARCHER',
        TRUE,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (email) DO NOTHING;