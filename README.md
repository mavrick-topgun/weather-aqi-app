# Breathe & Go

Weather + AQI Daily Decision App that helps users decide when to go outside based on real-world data.

## Features

- Real-time weather and air quality data from Open-Meteo API (no API keys required)
- Smart scoring algorithm (0-100) based on:
  - Air Quality Index (60% weight)
  - Precipitation (15% weight)
  - Temperature comfort (15% weight)
  - Wind conditions (10% weight)
- Color-coded recommendations: Great, Okay, Caution, Avoid
- 3-day forecast with daily scores
- 7-day trend charts for score and AQI
- Multiple location support
- Mobile-responsive design

## Tech Stack

- **Backend**: Spring Boot 3, Java 17, Maven, PostgreSQL, Flyway
- **Frontend**: Next.js 14, TypeScript, Tailwind CSS
- **Infrastructure**: Docker Compose

## Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- Docker and Docker Compose

### 1. Start PostgreSQL

```bash
cd infra
docker-compose up -d
```

### 2. Start Backend

```bash
cd api
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 3. Start Frontend

```bash
cd web
npm install
npm run dev
```

The app will be available at `http://localhost:3000`.

## API Endpoints

### Locations

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/locations` | List all saved locations |
| POST | `/api/locations` | Add a new location |
| GET | `/api/locations/{id}` | Get a specific location |
| DELETE | `/api/locations/{id}` | Remove a location |

### Forecast & Trends

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/locations/{id}/forecast` | Get current score and 3-day forecast |
| GET | `/api/locations/{id}/trends?period=14` | Get trend data (7-30 days) |

## Project Structure

```
weather-aqi-app/
├── api/                    # Spring Boot backend
│   ├── src/main/java/com/breathego/
│   │   ├── controller/     # REST endpoints
│   │   ├── service/        # Business logic + scoring
│   │   ├── domain/         # JPA entities
│   │   ├── dto/            # Request/Response records
│   │   ├── repository/     # Data access
│   │   ├── client/         # Open-Meteo API client
│   │   └── config/         # App configuration
│   └── src/test/           # Tests
├── web/                    # Next.js frontend
│   ├── app/                # App router pages
│   ├── components/         # React components
│   ├── lib/                # API client
│   └── types/              # TypeScript types
├── infra/                  # Docker Compose
│   └── docker-compose.yml
└── README.md
```

## Scoring Algorithm

The app calculates an outdoor activity score (0-100) using:

| Factor | Weight | Description |
|--------|--------|-------------|
| AQI | 60% | US EPA Air Quality Index |
| Precipitation | 15% | Rain/snow expected |
| Temperature | 15% | Comfort range (15-25°C optimal) |
| Wind | 10% | Wind speed impact |

### Score Bands

- **80-100**: Great (green) - Perfect conditions
- **60-79**: Okay (yellow) - Minor concerns
- **40-59**: Caution (orange) - Consider limiting outdoor time
- **0-39**: Avoid (red) - Not recommended for outdoor activities

## Running Tests

### Backend

```bash
cd api
./mvnw test
```

### Frontend

```bash
cd web
npm test
```

## Development

### Backend Development

The backend uses:
- Spring Boot 3.2 with Java 17
- JPA/Hibernate for data persistence
- Flyway for database migrations
- WebFlux WebClient for API calls

### Frontend Development

The frontend uses:
- Next.js 14 with App Router
- TypeScript for type safety
- Tailwind CSS for styling
- React hooks for state management

## License

MIT
