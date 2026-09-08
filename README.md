# Heurislink

Heurislink is a production-oriented URL shortener built with Spring Boot 4 and Java 21. It creates compact links, redirects requests with low latency, records click events asynchronously, and exposes lifecycle and analytics operations through a small REST API.

## What is included

- PostgreSQL persistence with Flyway migrations
- Redis caching for redirect lookups and distributed create-request rate limiting
- Kafka click events with a dead-letter topic and idempotent event persistence
- Stateless Spring Security configuration with only health and Prometheus actuator endpoints exposed
- OpenTelemetry tracing and Prometheus metrics
- Docker Compose for local development
- Kubernetes manifests with probes, rolling updates, HPA, PDB, resource limits, and a monitoring stack
- CI for tests, container scanning, Kubernetes validation, and GHCR publishing

The high-level system view is documented in [docs/architecture.md](docs/architecture.md). The API contract is available as [docs/openapi.yaml](docs/openapi.yaml).

## Requirements

- Java 21
- Docker Engine with the Compose plugin for the full local stack
- Git

The Gradle wrapper is committed, so Gradle does not need to be installed separately.

## Run locally

The application depends on PostgreSQL, Redis, and Kafka. Start the complete stack with:

```bash
docker compose up --build
```

The public HTTP endpoint is exposed through Nginx at `http://localhost:8080`. The application itself listens on port `8080` inside the Compose network. Stop the stack with:

```bash
docker compose down
```

PostgreSQL data is kept in the `postgres-data` named volume. To remove local database data as well, use `docker compose down -v`.

For an application-only run, start compatible PostgreSQL, Redis, and Kafka services, then set the variables listed below and run:

```bash
./gradlew bootRun
```

## Configuration

The application has safe local defaults for the Compose setup. Override them with environment variables when running outside Compose:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/heurislink` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `local-development-only` | Local development database password |
| `POSTGRES_PASSWORD` | `local-development-only` | Compose PostgreSQL password; change it for local use if needed |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `APP_BASE_URL` | `http://localhost:8080` | Base URL returned in create responses |
| `APP_RATELIMIT_MAXREQUESTS` | `10` | Create requests allowed per client IP |
| `APP_RATELIMIT_WINDOWSECONDS` | `60` | Rate-limit window |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP HTTP trace endpoint |

## API quick start

Create a short URL:

```bash
curl -i -X POST http://localhost:8080/urls \
  -H 'Content-Type: application/json' \
  -d '{"originalUrl":"https://example.com/docs","expiresAt":null}'
```

Use the returned `shortCode` to redirect, inspect analytics, update, activate/deactivate, or delete the link:

```bash
curl -i http://localhost:8080/{shortCode}
curl -s http://localhost:8080/urls/{shortCode}/analytics
curl -i -X PATCH http://localhost:8080/urls/{shortCode}/deactivate
curl -i -X PATCH http://localhost:8080/urls/{shortCode}/activate
curl -i -X DELETE http://localhost:8080/urls/{shortCode}
```

The complete request/response contract, validation rules, and error shapes are in [docs/openapi.yaml](docs/openapi.yaml).

## Observability

The following actuator endpoints are intentionally available:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/prometheus`

Other actuator endpoints are denied by Spring Security. In Kubernetes, Prometheus, Grafana, Loki, Alloy, and Tempo manifests are included under `k8s/`.

## Tests and quality checks

Run the test suite:

```bash
./gradlew test
```

Build the production artifact:

```bash
./gradlew bootJar
```

Build the container image:

```bash
docker build -t heurislink:local .
```

The CI workflow also validates Kubernetes manifests with kubeconform and scans the image and manifests with Trivy. Load-test scenarios are in [load-tests](load-tests).

## Kubernetes deployment

The manifests assume a namespace named `heurislink`, an existing `ghcr-pull-secret`, and the database secret referenced by `k8s/heurislink.yaml`. Before deploying, replace the placeholder `APP_BASE_URL` in `k8s/configmap.yaml` and create the referenced Kubernetes secrets out of band. Apply the manifests in dependency order through the CI/deployment workflow or with:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres.yaml -f k8s/redis.yaml -f k8s/kafka.yaml
kubectl apply -f k8s/heurislink.yaml -f k8s/ingress.yaml -f k8s/hpa.yaml -f k8s/pdb.yaml
```

Monitoring resources can then be applied from the remaining manifests in `k8s/`. The deployment workflow replaces the application image with an immutable digest after CI publishes it.

## Repository layout

```text
src/main/java/.../controller   REST endpoints
src/main/java/.../service      URL lifecycle, analytics, and rate limiting
src/main/java/.../messaging    Kafka producer/consumer and DLT handling
src/main/resources/db          Flyway migrations
docs                            Architecture and OpenAPI contract
k8s                             Application and observability manifests
load-tests                      k6 scenarios
```

## License

No license file is currently published. Treat this repository as private unless a separate license agreement applies.
