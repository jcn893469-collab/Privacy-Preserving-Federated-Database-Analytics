# System Architecture

```text
Organization browser
  ├─ read CSV locally
  ├─ validate / detect anomalies / clip values
  ├─ add Gaussian noise and account privacy spending
  └─ submit protected aggregate
          │ REST + JWT
          ▼
Spring Boot
  ├─ Spring Security role checks and current-user lookup
  ├─ REST controllers → transactional services
  ├─ JPA repositories for normal persistence
  └─ JDBC call for the PostgreSQL summary procedure
          │
          ▼
PostgreSQL
  ├─ Flyway-managed relational tables
  ├─ summary views and aggregate function
  ├─ organization summary procedure
  └─ contribution audit trigger
```

The static frontend is served by Spring Boot. The browser uploads no raw CSV
rows to the backend; the API stores protected sums, counts, mechanism
parameters, and related metadata.

Deployment evidence is not available in this repository. The Docker and cloud
deployment status is recorded in [DEPLOYMENT.md](./DEPLOYMENT.md).
