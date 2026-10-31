ALTER TABLE app_user
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_app_user_token_version CHECK (token_version >= 0);
