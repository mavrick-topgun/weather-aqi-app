# Breathe & Go - Developer Guide

## Project Overview

This is a Weather + AQI Daily Decision App that helps users decide when to go outside based on weather and air quality data from Open-Meteo APIs.

## Key Commands

### Development

```bash
# Start PostgreSQL
cd infra && docker-compose up -d

# Start backend (runs on port 8080)
cd api && ./mvnw spring-boot:run

# Start frontend (runs on port 3000)
cd web && npm run dev

# Run backend tests
cd api && ./mvnw test

# Run frontend tests
cd web && npm test
```

### Build

```bash
# Build backend JAR
cd api && ./mvnw clean package

# Build frontend
cd web && npm run build
```

## Architecture

### Backend (Spring Boot)

- **Controllers**: Handle HTTP requests, validation
- **Services**: Business logic, scoring algorithm
- **Repositories**: Data access via Spring Data JPA
- **Client**: Open-Meteo API integration using WebClient
- **DTOs**: Immutable records for API contracts

### Frontend (Next.js)

- **App Router**: Pages in `app/` directory
- **Components**: Reusable UI components
- **API Client**: Typed fetch wrapper in `lib/api.ts`
- **Types**: Shared TypeScript interfaces

## Scoring Logic

The `ScoringService` calculates scores (0-100) based on:

1. **AQI (60 points)**: US EPA scale
   - 0-50: Full points
   - 51-100: 75% of max
   - 101-150: 50% of max
   - 151-200: 25% of max
   - 201+: 0 points

2. **Precipitation (15 points)**
   - 0mm: Full points
   - <2mm: 80%
   - <5mm: 50%
   - <10mm: 30%
   - 10mm+: 0 points

3. **Temperature (15 points)**
   - 15-25°C: Full points
   - Gradually decreases outside range

4. **Wind (10 points)**
   - <15 km/h: Full points
   - Decreases up to 40 km/h

## Database Schema

```sql
-- locations: User-saved locations
CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    timezone VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- daily_metrics: Cached historical data
CREATE TABLE daily_metrics (
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT REFERENCES locations(id),
    date DATE NOT NULL,
    score INTEGER NOT NULL,
    recommendation VARCHAR(50) NOT NULL,
    aqi_value INTEGER,
    pm25 DECIMAL(6, 2),
    ...
);
```

## API Response Examples

### GET /api/locations/{id}/forecast

```json
{
  "locationId": 1,
  "locationName": "New York",
  "score": 75,
  "recommendation": "Okay",
  "reasons": [
    "Air quality is moderate",
    "No precipitation expected",
    "Temperature is pleasant"
  ],
  "weather": {
    "temperatureMax": 22.5,
    "temperatureMin": 15.0,
    "precipitation": 0.0,
    "windSpeed": 12.0,
    "uvIndex": 5.5
  },
  "aqi": {
    "value": 65,
    "pm25": 18.5,
    "ozone": 45.0
  },
  "forecast": [...]
}
```

## Common Issues

### CORS Errors
- Frontend proxies `/api/*` to backend via `next.config.js`
- Backend CORS configured in `WebConfig.java`

### Database Connection
- Ensure Postgres is running: `docker ps`
- Check connection in `application.properties`

### Open-Meteo Rate Limits
- Free tier: 10,000 requests/day
- Caching implemented in `daily_metrics` table

## Testing Strategy

### Backend Tests
- `ScoringServiceTest`: Unit tests for scoring logic
- `LocationControllerTest`: Integration tests with MockMvc

### Frontend Tests
- Component tests with React Testing Library
- API mocking for isolated tests
