# Breathe & Go

Weather + AQI Daily Decision App that helps users decide when to go outside based on real-world data.

![Sample](Sample1.png)

## Features

- Real-time weather and air quality data from Open-Meteo API (no API keys required)
- Smart scoring algorithm (0-100) based on AQI, precipitation, temperature, and wind
- Color-coded recommendations: Great, Okay, Caution, Avoid
- 3-day forecast with daily scores
- 7-day trend charts for score and AQI
- Interactive global AQI map with real-time air quality overlay
- Dark / light mode toggle
- Multiple location support with search
- Mobile-responsive design

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.2, Java 17, Maven |
| Frontend | Next.js 14, TypeScript, Tailwind CSS |
| Database | PostgreSQL 16 |
| Map | Leaflet, react-leaflet, AQICN tiles |
| Infrastructure | Docker Compose |

## Prerequisites

Make sure you have the following installed before proceeding:

| Tool | Version | Check command |
|------|---------|---------------|
| **Java** | 17 or higher | `java -version` |
| **Node.js** | 18 or higher | `node -v` |
| **npm** | 9 or higher | `npm -v` |
| **Docker** | 20 or higher | `docker -v` |
| **Docker Compose** | v2 | `docker compose version` |

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/weather-aqi-app.git
cd weather-aqi-app
```

### 2. Install frontend dependencies

```bash
cd web
npm install
cd ..
```

### 3. Set up the AQI Map token (optional)

The interactive AQI map requires a free token from [AQICN](https://aqicn.org/data-platform/token/). The map works without it, but won't show the colored air quality overlay.

```bash
cp web/.env.local.example web/.env.local
```

Then edit `web/.env.local` and replace `your_token_here` with your token:

```
NEXT_PUBLIC_AQICN_TOKEN=your_token_here
```

### 4. Run the app

**Option A: Single command (recommended)**

```bash
./start.sh
```

This starts all three services (PostgreSQL, backend, frontend) and shuts them all down when you press `Ctrl+C`.

**Option B: Run services individually**

Open three separate terminals:

```bash
# Terminal 1 — Database
cd infra && docker compose up -d

# Terminal 2 — Backend (wait for DB to start)
cd api && ./mvnw spring-boot:run

# Terminal 3 — Frontend (wait for backend to start)
cd web && npm run dev
```

### 5. Open the app

Once all services are running:

| Service | URL |
|---------|-----|
| App | [http://localhost:3000](http://localhost:3000) |
| AQI Map | [http://localhost:3000/map](http://localhost:3000/map) |
| API | [http://localhost:8080](http://localhost:8080) |

## Project Structure

```
weather-aqi-app/
├── api/                        # Spring Boot backend
│   ├── src/main/java/com/breathego/
│   │   ├── controller/         # REST endpoints
│   │   ├── service/            # Business logic + scoring
│   │   ├── domain/             # JPA entities
│   │   ├── dto/                # Request/Response records
│   │   ├── repository/         # Data access
│   │   ├── client/             # Open-Meteo API client
│   │   └── config/             # App configuration
│   └── src/test/               # Tests
├── web/                        # Next.js frontend
│   ├── app/                    # App router pages
│   │   ├── map/                # AQI world map page
│   │   └── locations/          # Location management page
│   ├── components/             # React components
│   │   └── map/                # Map-specific components
│   ├── lib/                    # API client
│   └── types/                  # TypeScript types
├── infra/                      # Docker Compose for PostgreSQL
│   └── docker-compose.yml
├── start.sh                    # One-command startup script
└── README.md
```

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

### Geocoding

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/geocoding/search?query=London&limit=5` | Search for locations by name |

## Scoring Algorithm

The app calculates an outdoor activity score (0-100):

| Factor | Weight | Description |
|--------|--------|-------------|
| AQI | 60% | US EPA Air Quality Index |
| Precipitation | 15% | Rain/snow expected |
| Temperature | 15% | Comfort range (15-25°C optimal) |
| Wind | 10% | Wind speed impact |

### Score Bands

| Score | Label | Meaning |
|-------|-------|---------|
| 80-100 | Great (green) | Perfect conditions |
| 60-79 | Okay (yellow) | Minor concerns |
| 40-59 | Caution (orange) | Consider limiting outdoor time |
| 0-39 | Avoid (red) | Not recommended for outdoor activities |

## Running Tests

```bash
# Backend tests
cd api && ./mvnw test

# Frontend tests
cd web && npm test
```

## Troubleshooting

### Port already in use

If port 3000 or 8080 is already taken, the app will fail to start. Free the port or stop the conflicting process:

```bash
# Find what's using a port
lsof -i :3000
lsof -i :8080

# Kill by PID
kill <PID>
```

### Database connection refused

Make sure Docker is running and the PostgreSQL container is healthy:

```bash
docker ps
docker logs breathego-db
```

### Frontend can't reach backend

The frontend proxies `/api/*` requests to `http://localhost:8080`. Make sure the backend is running before starting the frontend.

### AQI map shows no color overlay

Make sure you've created `web/.env.local` with a valid AQICN token. Restart the frontend after adding the token.

## License

MIT
