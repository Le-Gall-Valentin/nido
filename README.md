# synapsys-api

Spring Boot (Java 21) backend with an embedded React/TypeScript frontend, built with Maven.

## Stack

- Java 21, Spring Boot 4
- Spring Data JPA + PostgreSQL, Liquibase migrations
- Spring Security + JWT, optional TOTP/2FA
- Redis (rate limiting, ephemeral stores, pub/sub)
- React/TypeScript frontend (`src/main/frontend`), bundled at build time
- OpenAPI / Swagger UI

## Prerequisites

- Java 21+
- PostgreSQL 15+
- Redis 7+
- Node.js 22+ (provided automatically at build time by the Maven frontend plugin)

Maven itself is not required globally — use the bundled wrapper (`./mvnw`).

## Configuration

Copy the example environment file and fill in the values:

```bash
cp .env.example .env
```

Generate the required secrets:

```bash
openssl rand -base64 32   # SYNAPSYS_JWT_SECRET
openssl rand -base64 32   # SYNAPSYS_ENCRYPTION_SECRET
```

See `.env.example` for the full list of variables (database, Redis, JWT, CORS, seed admin account…).

## Run (development)

Start PostgreSQL and Redis, then:

```bash
./mvnw spring-boot:run
```

The application serves on `http://localhost:8080`.

To run the frontend dev server separately (hot reload):

```bash
cd src/main/frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` (configure `SYNAPSYS_CORS_ALLOWED_ORIGINS` accordingly).

## Build

```bash
./mvnw clean package
```

This compiles the backend, builds the frontend, and produces an executable JAR in `target/`:

```bash
java -jar target/api-0.0.1-SNAPSHOT.jar
```

## Tests

```bash
./mvnw test
```

Integration tests use Testcontainers, so a running Docker daemon is required.

## API documentation

When `SWAGGER_ENABLED=true`, Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## License

See [LICENSE](LICENSE).