# Deployment

## Local container deployment

The repository includes a multi-stage backend Dockerfile and a Compose
PostgreSQL service with a persistent volume. Set `DB_USERNAME`,
`DB_PASSWORD`, and `JWT_SECRET` in `.env` before running:

```powershell
Copy-Item .env.example .env
# Fill the required values in .env.
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

The backend health check targets `/api/health`. Docker is unavailable in the
environment where this work was performed, so image build and container
startup are not verified here.

## PostgreSQL integration tests

The backend test suite includes Testcontainers-based tests for Flyway,
PostgreSQL views, the stored procedure, contribution triggers, HAVING, and
transaction rollback. Run them on a machine with Docker available:

```powershell
mvn -f backend\pom.xml -Dtest=PostgresMigrationIntegrationTest,PostgresDatasetJpaIntegrationTest test
```

In this checkout the four integration tests were skipped because Docker was
not available. A passing Maven build without those tests executing is not
evidence of PostgreSQL verification.

## Docker Hub and cloud

No Docker Hub image or live cloud deployment was found. To publish, first
build and test the image, authenticate to Docker Hub, tag it with the actual
account/repository, and push it. For cloud deployment, configure managed
PostgreSQL, environment secrets, network access, migrations, persistent
backups, TLS, monitoring, and a real health check. Record the resulting image
and URL only after successful publication/deployment.

Example commands (replace the placeholders only after creating the real
Docker Hub repository):

```powershell
docker build -t <dockerhub-user>/privacy-federated-analytics:<tag> .
docker login
docker push <dockerhub-user>/privacy-federated-analytics:<tag>
```

For a cloud deployment, configure `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
and `JWT_EXPIRATION_MS` in the provider's secret manager; point
`SPRING_DATASOURCE_URL` at managed PostgreSQL; then deploy the published image.
The deployment URL, image name, and successful health check must be recorded
from the real provider after deployment.
