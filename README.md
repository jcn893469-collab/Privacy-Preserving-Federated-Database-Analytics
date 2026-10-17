# Privacy-Preserving Federated Database Analytics

> **VIT Academic Project** — A full-stack system demonstrating Gaussian Differential Privacy (DP) with Rényi Differential Privacy (RDP) accounting for federated analytics across organisations. Raw patient/organizational data **never** leaves the client.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Quick Start — Docker](#quick-start--docker)
4. [Quick Start — Local Development](#quick-start--local-development)
5. [Demo Credentials](#demo-credentials)
6. [API Overview](#api-overview)
7. [Privacy Pipeline](#privacy-pipeline)
8. [Running Tests](#running-tests)
9. [Database](#database)
10. [Project Structure](#project-structure)

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────┐
│                     BROWSER (Client-Side)                          │
│  ┌─────────────────────┐    ┌──────────────────────────────────┐  │
│  │   org-dashboard.html│    │      admin-dashboard.html        │  │
│  │   org-portal.js     │    │      (7-tab admin console)       │  │
│  │                     │    │                                  │  │
│  │  CSV UPLOAD ─►  Z-Score Anomaly Detection                   │  │
│  │             ─►  Data Clipping (Lᵖ sensitivity)              │  │
│  │             ─►  Gaussian Noise Addition  ε,δ-DP             │  │
│  │             ─►  RDP Accounting (Rényi-DP)                   │  │
│  │             ─►  Protected aggregate sent to API  ────────── │  │
│  └─────────────────────┘                                        │  │
└─────────────────────────────────────────────────────────────────── ┘
                          │  Only noisy aggregates sent
                          ▼
┌─────────────────── Spring Boot Backend ────────────────────────────┐
│  JWT Auth • Role-Based Access Control (ADMIN, DATA_OWNER,          │
│            RESEARCHER)                                             │
│  REST API Controllers ──► Service Layer ──► JPA Repositories       │
│  Gaussian DP re-validation • Privacy Budget Ledger                 │
│  Federated Aggregation Engine (weighted averaging)                 │
└────────────────────────────┬───────────────────────────────────────┘
                             │
                             ▼
┌─────────────────── PostgreSQL 16 ──────────────────────────────────┐
│  10 Flyway migrations │ 9 tables │ 2 views │ 1 stored function     │
│  1 audit trigger      │ seed data with BCrypt passwords            │
└────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Build | Apache Maven 3.9 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ORM | Spring Data JPA (Hibernate 6) |
| Security | Spring Security 6 + JWT (JJWT 0.11) |
| DP Math | Custom Gaussian mechanism + RDP accounting |
| Frontend | Vanilla JS + Bootstrap 5 |
| Container | Docker + Docker Compose |

---

## Quick Start — Docker

### Prerequisites
- Docker Desktop ≥ 4.x
- (Optional) `.env` file for custom secrets

```bash
# Clone and enter the project
git clone <repo-url>
cd "Privacy Federated Analytics"

# Start PostgreSQL + backend
docker compose up -d --build

# Watch startup logs
docker compose logs -f backend
```

The backend will be available at **http://localhost:8080**.  
The frontend static files are served from `backend/src/main/resources/static/`.

### Environment Variables (`.env`)

Create a `.env` file in the project root to override defaults:

```env
POSTGRES_DB=fpa_db
POSTGRES_USER=fpa_user
POSTGRES_PASSWORD=change_me_in_prod
POSTGRES_PORT=5432
BACKEND_PORT=8080
JWT_SECRET=your-very-long-random-base64-secret-at-least-64-chars
JWT_EXPIRATION_MS=86400000
```

---

## Quick Start — Local Development

### Prerequisites
- Java 21 (Eclipse Temurin recommended)
- Maven 3.9+
- PostgreSQL 16 running locally

### 1. Create PostgreSQL database

```sql
CREATE USER fpa_user WITH PASSWORD 'fpa_secret';
CREATE DATABASE fpa_db OWNER fpa_user;
GRANT ALL PRIVILEGES ON DATABASE fpa_db TO fpa_user;
```

### 2. Configure application

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fpa_db
    username: fpa_user
    password: fpa_secret
```

### 3. Run backend

```bash
cd backend
mvn spring-boot:run
```

Flyway migrations run automatically — all 10 migrations (V1–V10) apply in order, creating tables, views, trigger, and seed data.

### 4. Open frontend

Navigate to: `http://localhost:8080/org-login.html`

---

## Demo Credentials

| Role | Email | Password |
|---|---|---|
| **Admin** | `admin@vit.ac.in` | `Admin@123` |
| **Data Owner 1** | `apollo@hospital.org` | `Apollo@123` |
| **Data Owner 2** | `fortis@hospital.org` | `Fortis@123` |
| **Researcher** | `researcher@vit.ac.in` | `Research@123` |

> Passwords are stored as BCrypt hashes in the database (migration V10).

---

## API Overview

All protected endpoints require `Authorization: Bearer <JWT>` header.

### Auth
| Method | Path | Access |
|---|---|---|
| `POST` | `/api/auth/login` | Public |
| `POST` | `/api/auth/register` | Public |
| `GET` | `/api/auth/me` | Authenticated |

### Contributions
| Method | Path | Access |
|---|---|---|
| `POST` | `/api/analytics/contribute` | DATA_OWNER |
| `GET` | `/api/analytics/contributions` | DATA_OWNER (own) |
| `GET` | `/api/analytics/contributions/search` | ADMIN |
| `GET` | `/api/analytics/federated-result/{dbId}` | RESEARCHER, ADMIN |
| `POST` | `/api/analytics/run-federated/{dbId}` | ADMIN |

### Databases
| Method | Path | Access |
|---|---|---|
| `POST` | `/api/databases` | DATA_OWNER |
| `GET` | `/api/databases` | Authenticated |
| `PUT` | `/api/databases/{id}` | ADMIN |
| `DELETE` | `/api/databases/{id}` | ADMIN |

### Users (Admin)
| Method | Path | Access |
|---|---|---|
| `GET` | `/api/users` | ADMIN |
| `POST` | `/api/users` | ADMIN |
| `PUT` | `/api/users/{id}` | ADMIN |
| `DELETE` | `/api/users/{id}` | ADMIN |

### Admin
| Method | Path | Access |
|---|---|---|
| `GET` | `/api/admin/status` | ADMIN |
| `GET` | `/api/admin/views/contribution-summary` | ADMIN |
| `GET` | `/api/admin/audit-logs` | ADMIN |

---

## Privacy Pipeline

Raw data **never** leaves the browser. The client-side pipeline in `org-portal.js`:

1. **Parse CSV** — browser File API, all processing in memory
2. **Z-Score Anomaly Detection** — flags rows where `|z| > threshold` (default 3.0)
3. **Data Clipping** — clips each value to `[lower, upper]` to bound global sensitivity
4. **Gaussian Noise** — adds `𝒩(0, σ²)` noise where `σ = sensitivity × √(2 ln(1.25/δ)) / ε`
5. **RDP Accounting** — tracks per-query Rényi DP budget across multiple queries
6. **Submit aggregate** — only the noisy count/mean/variance sent to `/api/analytics/contribute`

The server re-validates the Gaussian noise parameters and records a `PrivacyBudgetLedger` entry, preventing budget exhaustion attacks.

### Privacy Parameters

| Parameter | Default | Meaning |
|---|---|---|
| ε (epsilon) | 1.0 | Privacy budget per query |
| δ (delta) | 1e-5 | Failure probability |
| σ (sigma) | auto-computed | Gaussian noise scale |
| Clipping bound | [0, 100] | Per-column L∞ sensitivity |

---

## Running Tests

```bash
cd backend
mvn test
```

**27 unit tests** in 7 test classes:

| Class | Tests | What it covers |
|---|---|---|
| `ZScoreAnomalyDetectorTest` | 3 | Statistical anomaly flagging |
| `ClippingUtilTest` | 6 | L∞ data clipping correctness |
| `GaussianMechanismTest` | 4 | Noise calibration formula |
| `LocalPrivacyPipelineTest` | 2 | End-to-end local pipeline |
| `PrivacyAccountantTest` | 5 | RDP budget composition |
| `AnalyticsContributionServiceTest` | 3 | Service layer (Mockito) |
| `AuthServiceTest` | 4 | JWT auth (Mockito) |

---

## Database

See [`docs/relational-schema.md`](docs/relational-schema.md) for full table definitions.

### Migrations
| Version | Description |
|---|---|
| V1 | `app_user` table |
| V2 | `federated_database` table |
| V3 | `analytics_contribution` table |
| V4 | Privacy budget columns on `federated_database` |
| V5 | Gaussian metadata columns on `analytics_contribution` |
| V6 | `dataset_metadata` + `dataset_column_schema` tables |
| V7 | `privacy_budget_ledger` + `federated_result` + `federated_run_participant` |
| V8 | `audit_log` table + trigger + audit function |
| V9 | `v_organization_contribution_summary` + `v_federated_analytics_overview` views + stored function |
| V10 | Seed data (4 users, 2 databases, demo data) |

### Backup & Restore

```powershell
# Backup (Windows)
.\scripts\backup-db.ps1

# Restore (Windows)
.\scripts\restore-db.ps1 -BackupFile .\backups\fpa_db_20241015_120000.dump
```

```bash
# Backup (Linux/Mac)
./scripts/backup-db.sh

# Restore (Linux/Mac)
./scripts/restore-db.sh ./backups/fpa_db_20241015_120000.dump
```

---

## Project Structure

```
Privacy Federated Analytics/
├── Dockerfile                          # Multi-stage build (JDK 21 → JRE runtime)
├── docker-compose.yml                  # PostgreSQL 16 + backend services
├── README.md                           # This file
├── scripts/
│   ├── backup-db.ps1                   # PostgreSQL pg_dump (Windows)
│   ├── backup-db.sh                    # PostgreSQL pg_dump (Linux/Mac)
│   ├── restore-db.ps1                  # pg_restore (Windows)
│   └── restore-db.sh                   # pg_restore (Linux/Mac)
├── docs/
│   ├── ER-Diagram.md                   # Entity-Relationship diagram
│   ├── relational-schema.md            # Full table definitions
│   ├── data-dictionary.md              # Column descriptions
│   └── normalization.md                # 3NF compliance evidence
└── backend/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/fpa/
        │   │   ├── api/
        │   │   │   ├── controller/     # REST controllers
        │   │   │   ├── dto/            # Request/Response DTOs
        │   │   │   └── exception/      # GlobalExceptionHandler
        │   │   ├── config/             # SecurityConfig, JwtConfig
        │   │   ├── domain/
        │   │   │   ├── entity/         # JPA entities (10 entities)
        │   │   │   └── repository/     # Spring Data repositories
        │   │   ├── service/            # Business logic services
        │   │   ├── privacy/            # DP math: Gaussian, RDP accounting
        │   │   ├── localprocessing/    # Z-score, clipping utilities
        │   │   ├── security/           # JWT filter, UserDetails
        │   │   └── simulator/          # Federated simulation
        │   └── resources/
        │       ├── application.yml
        │       ├── db/migration/       # Flyway V1–V10
        │       └── static/             # Frontend HTML/JS
        └── test/                       # 27 unit tests
```
