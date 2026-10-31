# Technology Stack

| Layer | Verified technology |
|---|---|
| Language/runtime | Java 21 |
| Backend framework | Spring Boot 3.2.5 |
| Persistence | Hibernate / Spring Data JPA |
| Database/migrations | PostgreSQL 16 / Flyway |
| Authentication | Spring Security, JJWT 0.12.5, BCrypt |
| Frontend | Static HTML, CSS, vanilla JavaScript |
| Privacy | Gaussian mechanism and RDP accountant |
| CSV/anomaly processing | Browser-side CSV processing and Java utilities |
| Build | Apache Maven |
| Containers | Dockerfile and Docker Compose |

No separate frontend package manager or frontend build exists in the tracked
workspace; the static assets are packaged by Spring Boot.
