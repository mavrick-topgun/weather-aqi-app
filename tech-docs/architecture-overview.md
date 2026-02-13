# Breathe & Go - Architecture Overview

## System Architecture

```
                           +-------------------+
                           |    Browser/Client  |
                           |   (Desktop/Mobile) |
                           +--------+----------+
                                    |
                                    | HTTPS (port 3000)
                                    |
                           +--------v----------+
                           |   Next.js 14 App   |
                           |   (App Router)     |
                           |                    |
                           |  +- Dashboard     |
                           |  +- Map (Leaflet) |
                           |  +- Locations     |
                           +--------+----------+
                                    |
                                    | /api/* proxy rewrite
                                    | (next.config.js)
                                    |
                           +--------v----------+
                           | Spring Boot 3.2.2  |
                           |    (port 8080)     |
                           |                    |
                           | +- Controllers    |
                           | +- Services       |
                           | +- Repositories   |
                           +--+-----+-----+----+
                              |     |     |
               +--------------+     |     +--------------+
               |                    |                     |
      +--------v-------+  +--------v--------+  +---------v--------+
      |  PostgreSQL 16  |  | Open-Meteo APIs |  | AQICN Tile API   |
      |  (port 5432)    |  | (Weather, AQI,  |  | (Map overlay)    |
      |                 |  |  Archive, Geo)  |  |                  |
      |  - locations    |  |  No auth req'd  |  | Requires token   |
      |  - daily_metrics|  |  10K req/day    |  |                  |
      +----------------+  +-----------------+  +------------------+
```

## Technology Stack

| Layer          | Technology            | Version  | Purpose                          |
|----------------|-----------------------|----------|----------------------------------|
| Frontend       | Next.js (App Router)  | 14.1.0   | React SSR/CSR framework          |
| Frontend       | React                 | 18.2.0   | UI component library             |
| Frontend       | TypeScript            | 5.3.0    | Type safety                      |
| Frontend       | Tailwind CSS          | 3.4.1    | Utility-first styling            |
| Frontend       | Leaflet/React-Leaflet | 1.9.4    | Interactive maps                 |
| Backend        | Spring Boot           | 3.2.2    | REST API framework               |
| Backend        | Java                  | 17       | Backend language                 |
| Backend        | Spring Data JPA       | (Boot)   | ORM / data access                |
| Backend        | Spring WebFlux        | (Boot)   | Reactive HTTP client (WebClient) |
| Backend        | Flyway                | (Boot)   | Database migrations              |
| Database       | PostgreSQL            | 16       | Relational database              |
| Infrastructure | Docker Compose        | 3.8      | Container orchestration          |
| Testing        | JUnit 5 / Mockito     | (Boot)   | Backend unit/integration tests   |
| Testing        | Jest / RTL            | 29.7.0   | Frontend testing                 |

## Component Interaction Diagram

```
+------------------------------------------------------------------+
|                         FRONTEND (Next.js)                        |
|                                                                   |
|  Providers (Context)                                              |
|  +--------------------+  +--------------------+                   |
|  | ThemeProvider       |  | UnitProvider        |                  |
|  | (dark/light mode)  |  | (metric/imperial)  |                  |
|  | localStorage:theme |  | localStorage:units |                  |
|  +--------------------+  +--------------------+                   |
|                                                                   |
|  Pages                                                            |
|  +------------------+ +----------------+ +--------------------+   |
|  | / (Dashboard)    | | /map           | | /locations         |   |
|  |                  | |                | |                    |   |
|  | LocationPicker   | | AqiMap         | | LocationSearch     |   |
|  |   LocationSearch | |   AqiMapInner  | | LocationTable      |   |
|  | ScoreCard        | |   AqiLegend    | |                    |   |
|  | WeatherDetails   | |   LocationPopup| |                    |   |
|  | AqiDetails       | |                | |                    |   |
|  | ForecastCards    | |                | |                    |   |
|  | TrendChart       | |                | |                    |   |
|  +--------+---------+ +-------+--------+ +---------+----------+   |
|           |                   |                     |             |
|           +-------------------+---------------------+             |
|                               |                                   |
|                    +----------v-----------+                       |
|                    | API Client (lib/api) |                       |
|                    | Base: /api           |                       |
|                    +----------+-----------+                       |
+-------------------------------|-----------------------------------+
                                |
                    +-----------v-----------+
                    |  next.config.js       |
                    |  rewrite: /api/* -->  |
                    |  localhost:8080/api/* |
                    +-----------+-----------+
                                |
+-------------------------------|-----------------------------------+
|                         BACKEND (Spring Boot)                     |
|                                                                   |
|  Controllers                                                      |
|  +-------------------+ +------------------+ +------------------+  |
|  | LocationController| | ForecastController| | GeocodingController|
|  | /api/locations     | | /api/locations/  | | /api/geocoding/  |  |
|  |                   | |   {id}/forecast  | |   search         |  |
|  | GET    /          | |   {id}/trends    | |                  |  |
|  | GET    /{id}      | |                  | | GET ?query=&     |  |
|  | POST   /          | | GET /forecast    | |     limit=       |  |
|  | DELETE /{id}      | | GET /trends      | |                  |  |
|  +--------+----------+ +--------+---------+ +--------+---------+  |
|           |                     |                     |           |
|  +--------v---------------------v---------------------v--------+  |
|  |                    GlobalExceptionHandler                   |  |
|  |  404: LocationNotFoundException                             |  |
|  |  503: ForecastUnavailableException                          |  |
|  |  400: MethodArgumentNotValidException                       |  |
|  |  500: Generic Exception                                     |  |
|  +-------------------------------------------------------------+  |
|                                                                   |
|  Services                                                         |
|  +------------------+ +------------------+ +------------------+   |
|  | LocationService  | | ForecastService  | | ScoringService   |   |
|  |                  | |                  | |                  |   |
|  | CRUD operations  | | getForecast()    | | calculate()      |   |
|  | Entity <-> DTO   | | getTrends()      | | AQI: 60 pts     |   |
|  |                  | | saveDailyMetrics | | Precip: 15 pts   |   |
|  |                  | |   (cache layer)  | | Temp: 15 pts     |   |
|  +--------+---------+ +---+----+----+----+ | Wind: 10 pts     |   |
|           |                |    |    |      +------------------+   |
|           |                |    |    |                             |
|  +--------v--------+      |    |    +----------+                  |
|  | LocationRepo    |      |    |               |                  |
|  | (JPA/Hibernate) |      |    |    +----------v----------+       |
|  +-----------------+      |    |    | DailyMetricsRepo    |       |
|                           |    |    | (JPA/Hibernate)     |       |
|  +------------------------v----v--+ +---------------------+       |
|  | OpenMeteoClient               |                               |
|  | - getWeatherForecast()        |                               |
|  | - getAirQuality()             |                               |
|  | - getHistoricalWeather()      |                               |
|  | - getHistoricalAirQuality()   |                               |
|  +-------------------------------+                               |
|  | GeocodingClient               |                               |
|  | - search()                    |                               |
|  +-------------------------------+                               |
+------------------------------------------------------------------+
                    |                         |
         +----------v----------+   +----------v----------+
         |    PostgreSQL 16     |   |   Open-Meteo APIs    |
         |                     |   |                      |
         |  locations          |   | Weather Forecast     |
         |  daily_metrics      |   | Air Quality          |
         |                     |   | Archive (Historical) |
         |  Flyway migrations  |   | Geocoding Search     |
         +---------------------+   +----------------------+
```

## Data Flow

### Forecast Request Flow
```
1. User selects location in Dashboard
2. Frontend calls: GET /api/locations/{id}/forecast
3. Next.js proxies to Spring Boot
4. ForecastService:
   a. Fetches location entity from DB
   b. Calls OpenMeteoClient.getWeatherForecast() (3 days)
   c. Calls OpenMeteoClient.getAirQuality() (3 days)
   d. Calculates score for each day via ScoringService
   e. Caches results in daily_metrics table
   f. Returns ForecastResponse
5. Frontend renders ScoreCard + WeatherDetails + AqiDetails + ForecastCards
```

### Trends Request Flow
```
1. Dashboard loads trends alongside forecast (Promise.all)
2. Frontend calls: GET /api/locations/{id}/trends?period=7
3. ForecastService.getTrends():
   a. Checks daily_metrics cache for existing data
   b. If gaps exist, fetches from Open-Meteo APIs
   c. Aggregates into AqiTrend[], TemperatureTrend[], ScoreTrend[]
   d. Caches new data points
   e. Returns TrendsResponse
4. Frontend renders TrendChart (AQI line chart)
```

### Location Search Flow
```
1. User types in search box (min 2 chars)
2. 300ms debounce on frontend
3. Frontend calls: GET /api/geocoding/search?query=...&limit=5
4. GeocodingClient calls Open-Meteo Geocoding API
5. Results displayed in dropdown
6. User selects -> POST /api/locations creates entry
7. Dashboard auto-selects new location
```
