# Authentication and Security

## Implemented

- A single `app_user` relation stores accounts and one of three roles:
  `ADMIN`, `DATA_OWNER`, or `RESEARCHER`.
- Passwords are hashed with BCrypt.
- JWTs are signed with an environment-provided secret. Startup rejects a
  missing or shorter-than-32-byte secret.
- `POST /api/auth/login` issues a JWT; `GET /api/auth/me` returns the current
  account.
- `POST /api/auth/logout` increments `app_user.token_version`. The JWT filter
  rejects tokens carrying an older version and rejects inactive users.
- Role restrictions exist in `SecurityConfig`; services additionally check
  ownership on several record-specific operations.
- Database/JWT credentials are required through environment variables;
  `.env.example` contains blank placeholders.

## Remaining security work

The browser stores JWTs in `localStorage`, which is exposed to same-origin
script execution. Consider an HttpOnly Secure SameSite cookie or another
reviewed token-storage approach before production. Run a complete API
authorization matrix and PostgreSQL-backed logout/migration tests. No
production security certification is claimed.
