# Breathe & Go - Infrastructure & Deployment

## Development Environment

### Prerequisites

| Tool             | Version  | Purpose                     |
|------------------|----------|-----------------------------|
| Java JDK         | 17+      | Backend runtime             |
| Node.js          | 18+      | Frontend runtime            |
| Docker + Compose | Latest   | PostgreSQL container        |
| Maven Wrapper    | Included | Backend build (./mvnw)      |

### Service Ports

| Service        | Port | Protocol |
|----------------|------|----------|
| Next.js        | 3000 | HTTP     |
| Spring Boot    | 8080 | HTTP     |
| PostgreSQL     | 5432 | TCP      |

### Quick Start

```bash
# One-command startup
./start.sh

# Or manually:
cd infra && docker-compose up -d          # Start PostgreSQL
cd api && ./mvnw spring-boot:run          # Start backend
cd web && npm run dev                     # Start frontend
```

The `start.sh` script:
1. Starts PostgreSQL via Docker Compose
2. Waits for DB readiness (`pg_isready`)
3. Starts Spring Boot backend
4. Waits for backend health check
5. Starts Next.js dev server
6. Traps SIGINT/SIGTERM for clean shutdown

## Docker Compose

**File:** `infra/docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: breathego-db
    environment:
      POSTGRES_DB: breathego
      POSTGRES_USER: breathego
      POSTGRES_PASSWORD: breathego123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: pg_isready -U breathego -d breathego
      interval: 10s
      timeout: 5s
      retries: 5
```

**Volume:** `postgres_data` persists database files across container restarts.

## Network Topology

```
Browser
  |
  | :3000
  v
Next.js ----rewrite /api/*----> Spring Boot
  |                                  |
  |                                  | JDBC :5432
  |                                  v
  |                             PostgreSQL
  |                                  ^
  |                                  | Flyway migrations
  |                                  | on startup
  |
  | :3000 (static assets, SSR)
  v
Browser

Spring Boot ---HTTPS---> api.open-meteo.com (Weather)
             ---HTTPS---> air-quality-api.open-meteo.com (AQI)
             ---HTTPS---> archive-api.open-meteo.com (Historical)
             ---HTTPS---> geocoding-api.open-meteo.com (Search)

Browser ------HTTPS---> tiles.aqicn.org (Map AQI overlay)
```

## Environment Variables

### Backend (application.properties)

| Variable                     | Default                              | Description               |
|------------------------------|--------------------------------------|---------------------------|
| server.port                  | 8080                                 | API server port           |
| spring.datasource.url        | jdbc:postgresql://localhost:5432/breathego | DB connection       |
| spring.datasource.username   | breathego                            | DB username               |
| spring.datasource.password   | breathego123                         | DB password               |
| cors.allowed-origins         | http://localhost:3000                 | Frontend origin           |
| openmeteo.weather.url        | https://api.open-meteo.com/v1/forecast | Weather API URL        |
| openmeteo.airquality.url     | https://air-quality-api.open-meteo.com/v1/air-quality | AQI API URL |

### Frontend

| Variable                | Required | Description                    |
|-------------------------|----------|--------------------------------|
| NEXT_PUBLIC_AQICN_TOKEN | No       | AQICN tile layer token for map |

**Note:** An `.env.local.example` file is provided as a template.

## Database Migrations

Managed by **Flyway**.

- Migration files: `api/src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- Current: `V1__create_schema.sql`
- Mode: `baseline-on-migrate=true` (auto-baseline for existing DBs)
- JPA mode: `validate` (Hibernate validates schema, doesn't modify it)

## Build & Production

### Backend

```bash
cd api && ./mvnw clean package        # Build JAR
java -jar target/breathego-api-1.0.0-SNAPSHOT.jar  # Run production
```

### Frontend

```bash
cd web && npm run build               # Production build
cd web && npm start                   # Start production server
```

## Production Deployment Checklist

- [ ] Change database password from default
- [ ] Enable HTTPS (TLS certificates)
- [ ] Set `cors.allowed-origins` to production domain
- [ ] Use production Next.js build (`npm run build && npm start`)
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Set logging level to WARN/ERROR for production
- [ ] Add health check endpoint (Spring Actuator)
- [ ] Set up database backups
- [ ] Configure proper connection pool sizing
- [ ] Add rate limiting to API endpoints
- [ ] Set `NEXT_PUBLIC_AQICN_TOKEN` for map overlay
- [ ] Consider reverse proxy (nginx) for TLS termination
