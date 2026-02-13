# Breathe & Go - Database Schema

## Overview

- **Engine:** PostgreSQL 16 (Alpine)
- **Schema management:** Flyway migrations
- **ORM:** Hibernate via Spring Data JPA (validate mode)
- **Connection:** `jdbc:postgresql://localhost:5432/breathego`

## Entity Relationship Diagram

```
+---------------------+          +---------------------------+
|     locations        |          |      daily_metrics         |
+---------------------+          +---------------------------+
| PK  id BIGSERIAL     |<---------| PK  id BIGSERIAL          |
|     name VARCHAR(255) |    1:N  | FK  location_id BIGINT    |
|     latitude DEC(9,6) |         |     date DATE              |
|     longitude DEC(9,6)|         |     score INTEGER (0-100)  |
|     timezone VARCHAR  |         |     recommendation VARCHAR |
|     created_at TSTZ   |         |     aqi_value INTEGER      |
|     updated_at TSTZ   |         |     pm25 DEC(6,2)          |
+---------------------+          |     ozone DEC(6,2)          |
                                  |     temperature_max DEC(5,2)|
                                  |     temperature_min DEC(5,2)|
                                  |     precipitation DEC(6,2)  |
                                  |     wind_speed DEC(5,2)     |
                                  |     uv_index DEC(4,2)       |
                                  |     created_at TSTZ          |
                                  +---------------------------+
```

## Table: locations

Stores user-saved geographic locations.

```sql
CREATE TABLE locations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    latitude    DECIMAL(9, 6) NOT NULL,
    longitude   DECIMAL(9, 6) NOT NULL,
    timezone    VARCHAR(100) DEFAULT 'auto',
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_locations_lat_lon UNIQUE (latitude, longitude)
);
```

| Column     | Type            | Nullable | Default           | Notes                    |
|------------|-----------------|----------|-------------------|--------------------------|
| id         | BIGSERIAL       | No       | Auto-increment    | Primary key              |
| name       | VARCHAR(255)    | No       | -                 | Display name             |
| latitude   | DECIMAL(9,6)    | No       | -                 | -90.000000 to 90.000000  |
| longitude  | DECIMAL(9,6)    | No       | -                 | -180.000000 to 180.000000|
| timezone   | VARCHAR(100)    | Yes      | 'auto'            | IANA timezone identifier |
| created_at | TIMESTAMPTZ     | Yes      | CURRENT_TIMESTAMP | Auto-set on insert       |
| updated_at | TIMESTAMPTZ     | Yes      | CURRENT_TIMESTAMP | Auto-set on update       |

**Constraints:**
- `uk_locations_lat_lon` - UNIQUE on (latitude, longitude) prevents duplicate locations

**JPA Lifecycle:**
- `@PrePersist` sets `createdAt` and `updatedAt`
- `@PreUpdate` sets `updatedAt`

## Table: daily_metrics

Caches daily weather/AQI data and computed scores to reduce external API calls.

```sql
CREATE TABLE daily_metrics (
    id              BIGSERIAL PRIMARY KEY,
    location_id     BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    date            DATE NOT NULL,
    score           INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    recommendation  VARCHAR(50) NOT NULL,
    aqi_value       INTEGER,
    pm25            DECIMAL(6, 2),
    ozone           DECIMAL(6, 2),
    temperature_max DECIMAL(5, 2),
    temperature_min DECIMAL(5, 2),
    precipitation   DECIMAL(6, 2),
    wind_speed      DECIMAL(5, 2),
    uv_index        DECIMAL(4, 2),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_daily_metrics_location_date UNIQUE (location_id, date)
);
```

| Column          | Type         | Nullable | Notes                          |
|-----------------|--------------|----------|--------------------------------|
| id              | BIGSERIAL    | No       | Primary key                    |
| location_id     | BIGINT       | No       | FK to locations (cascade del)  |
| date            | DATE         | No       | Metrics date                   |
| score           | INTEGER      | No       | 0-100 composite score          |
| recommendation  | VARCHAR(50)  | No       | Great / Okay / Caution / Avoid |
| aqi_value       | INTEGER      | Yes      | US EPA AQI value               |
| pm25            | DECIMAL(6,2) | Yes      | PM2.5 in ug/m3                 |
| ozone           | DECIMAL(6,2) | Yes      | Ozone in ug/m3                 |
| temperature_max | DECIMAL(5,2) | Yes      | Max temp in Celsius            |
| temperature_min | DECIMAL(5,2) | Yes      | Min temp in Celsius            |
| precipitation   | DECIMAL(6,2) | Yes      | Rainfall in mm                 |
| wind_speed      | DECIMAL(5,2) | Yes      | Max wind in km/h               |
| uv_index        | DECIMAL(4,2) | Yes      | UV index max                   |
| created_at      | TIMESTAMPTZ  | Yes      | Auto-set on insert             |

**Constraints:**
- `uk_daily_metrics_location_date` - UNIQUE on (location_id, date) ensures one record per location per day
- `CHECK (score >= 0 AND score <= 100)` - Score range validation
- `ON DELETE CASCADE` - Deleting a location removes all its metrics

## Indexes

```sql
CREATE INDEX idx_daily_metrics_location_id
    ON daily_metrics (location_id);

CREATE INDEX idx_daily_metrics_date
    ON daily_metrics (date);

CREATE INDEX idx_daily_metrics_location_date
    ON daily_metrics (location_id, date DESC);
```

| Index                               | Columns              | Purpose                        |
|-------------------------------------|----------------------|--------------------------------|
| idx_daily_metrics_location_id       | location_id          | Filter metrics by location     |
| idx_daily_metrics_date              | date                 | Filter metrics by date range   |
| idx_daily_metrics_location_date     | location_id, date DESC | Trends query (location + date) |

## Storage Estimates

| Table          | Row Size (approx) | Growth Rate              |
|----------------|--------------------|--------------------------|
| locations      | ~200 bytes         | Slow (user-driven)       |
| daily_metrics  | ~150 bytes         | ~1 row/location/day      |

For 100 locations over 1 year: ~100 x 365 = 36,500 rows (~5.5 MB)

## Migration

Migration file: `V1__create_schema.sql`

Location: `api/src/main/resources/db/migration/`

Flyway is configured to run on startup with `spring.flyway.baseline-on-migrate=true`.
