# Breathe & Go - Backend Component Architecture

## Package Structure

```
com.breathego
|
+-- BreatheGoApplication.java          (Entry point)
|
+-- config/
|   +-- WebConfig.java                 (CORS configuration)
|   +-- WebClientConfig.java           (HTTP client bean)
|
+-- controller/
|   +-- LocationController.java        (CRUD endpoints)
|   +-- ForecastController.java        (Forecast + trends)
|   +-- GeocodingController.java       (Location search)
|   +-- GlobalExceptionHandler.java    (Error handling)
|
+-- service/
|   +-- LocationService.java           (Location CRUD logic)
|   +-- ForecastService.java           (Forecast orchestration)
|   +-- ScoringService.java            (Score calculation)
|
+-- client/
|   +-- OpenMeteoClient.java           (Weather + AQI API)
|   +-- GeocodingClient.java           (Geocoding API)
|
+-- domain/
|   +-- Location.java                  (JPA entity)
|   +-- DailyMetrics.java              (JPA entity)
|
+-- repository/
|   +-- LocationRepository.java        (Spring Data JPA)
|   +-- DailyMetricsRepository.java    (Spring Data JPA)
|
+-- dto/
    +-- LocationRequest.java           (Input validation)
    +-- LocationResponse.java          (Output mapping)
    +-- ForecastResponse.java          (Nested: WeatherInfo, AqiInfo, DailyForecast)
    +-- TrendsResponse.java            (Nested: AqiTrend, TemperatureTrend, ScoreTrend)
    +-- WeatherData.java               (Internal weather DTO)
    +-- AqiData.java                   (Internal AQI DTO)
    +-- Score.java                     (Score result)
    +-- GeocodingResult.java           (Search result)
```

## Controllers

### LocationController (`/api/locations`)

| Method | Endpoint            | Service Call                    | Response Code |
|--------|---------------------|---------------------------------|---------------|
| GET    | /                   | locationService.getAllLocations()| 200           |
| GET    | /{id}               | locationService.getLocation(id) | 200 / 404     |
| POST   | /                   | locationService.createLocation()| 201 / 400     |
| DELETE | /{id}               | locationService.deleteLocation()| 204 / 404     |

### ForecastController (`/api/locations/{id}`)

| Method | Endpoint            | Service Call                    | Response Code |
|--------|---------------------|---------------------------------|---------------|
| GET    | /{id}/forecast      | forecastService.getForecast(id) | 200 / 404 / 503 |
| GET    | /{id}/trends        | forecastService.getTrends(id, period) | 200 / 404 |

### GeocodingController (`/api/geocoding`)

| Method | Endpoint            | Service Call                    | Response Code |
|--------|---------------------|---------------------------------|---------------|
| GET    | /search?query=&limit= | geocodingClient.search(q, limit) | 200         |

### GlobalExceptionHandler

| Exception                        | HTTP Status | Error Code          |
|----------------------------------|-------------|---------------------|
| LocationNotFoundException        | 404         | NOT_FOUND           |
| ForecastUnavailableException     | 503         | SERVICE_UNAVAILABLE |
| MethodArgumentNotValidException  | 400         | VALIDATION_ERROR    |
| Exception (generic)              | 500         | INTERNAL_ERROR      |

## Services

### LocationService

**Annotations:** `@Service`, `@Transactional`

**Dependencies:** `LocationRepository`

| Method              | Description                              | Exceptions             |
|---------------------|------------------------------------------|------------------------|
| getAllLocations()    | Returns all locations as DTOs            | -                      |
| getLocation(id)     | Returns single location DTO              | LocationNotFoundException |
| getLocationEntity(id)| Returns JPA entity (internal use)       | LocationNotFoundException |
| createLocation(req) | Creates and saves new location           | -                      |
| deleteLocation(id)  | Deletes location + cascaded metrics      | LocationNotFoundException |

### ForecastService

**Annotations:** `@Service`, `@Transactional`

**Dependencies:** `LocationService`, `OpenMeteoClient`, `ScoringService`, `DailyMetricsRepository`

| Method                 | Description                                         |
|------------------------|-----------------------------------------------------|
| getForecast(locationId)| Fetches weather + AQI, calculates scores for 3 days |
| getTrends(id, days)    | Checks cache, fills gaps from API, returns trends   |
| saveDailyMetrics(...)  | Caches a day's data (ignores duplicates)            |

**Forecast flow:**
1. Load location entity from DB
2. Parallel-ish calls to Open-Meteo (weather + AQI)
3. Score each day via ScoringService
4. Cache in daily_metrics
5. Build ForecastResponse

**Trends flow:**
1. Load location entity
2. Query daily_metrics for cached data
3. If gaps exist, fetch from historical APIs
4. Score and cache new data
5. Aggregate into trend arrays

### ScoringService

**Annotations:** `@Service`

**Dependencies:** None (pure computation)

See [scoring-algorithm.md](./scoring-algorithm.md) for full details.

## API Clients

### OpenMeteoClient

**Annotations:** `@Component`

**Dependencies:** `WebClient`

| Method                      | External API                           | Returns         |
|-----------------------------|----------------------------------------|-----------------|
| getWeatherForecast(lat,lon,days) | api.open-meteo.com/v1/forecast    | List<WeatherData> |
| getAirQuality(lat,lon,days) | air-quality-api.open-meteo.com/v1/air-quality | List<AqiData> |
| getHistoricalWeather(...)   | archive-api.open-meteo.com/v1/archive  | List<WeatherData> |
| getHistoricalAirQuality(...)| air-quality-api.open-meteo.com/v1/air-quality | List<AqiData> |

**Key implementation detail:** The Air Quality API returns **hourly** data. The client aggregates to daily values using:
- AQI: **max** value per day (worst case)
- PM2.5: **average** per day
- Ozone: **average** per day

### GeocodingClient

**Annotations:** `@Component`

**Dependencies:** `WebClient`

| Method              | External API                              | Returns              |
|---------------------|-------------------------------------------|----------------------|
| search(query, limit)| geocoding-api.open-meteo.com/v1/search   | List<GeocodingResult>|

## Data Transfer Objects

All DTOs are Java **records** (immutable).

### Input DTOs (with validation)

```
LocationRequest
  name:      @NotBlank String
  latitude:  @NotNull @DecimalMin(-90) @DecimalMax(90) BigDecimal
  longitude: @NotNull @DecimalMin(-180) @DecimalMax(180) BigDecimal
  timezone:  String (optional)
```

### Output DTOs

```
LocationResponse     { id, name, latitude, longitude, timezone, createdAt }
ForecastResponse     { locationId, locationName, score, recommendation, reasons[], weather, aqi, forecast[] }
  WeatherInfo        { temperatureMax, temperatureMin, precipitation, windSpeed, windDirection, uvIndex }
  AqiInfo            { value, pm25, ozone }
  DailyForecast      { date, score, recommendation, temperatureMax, temperatureMin, aqi }
TrendsResponse       { locationId, locationName, period, aqi[], temperature[], scores[] }
  AqiTrend           { date, value }
  TemperatureTrend   { date, min, max }
  ScoreTrend         { date, score, recommendation }
GeocodingResult      { id, name, latitude, longitude, country, countryCode, admin1, timezone }
Score                { value, recommendation, reasons[] }
```

### Internal DTOs

```
WeatherData  { date, temperatureMax, temperatureMin, precipitation, windSpeed, windDirection, uvIndex }
AqiData      { date, usAqi, pm25, ozone }
```

## Configuration

### WebConfig
- CORS: Allows `http://localhost:3000`
- Methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials: Enabled
- Max age: 3600s

### WebClientConfig
- WebClient bean with 2 MB buffer limit
- Used by OpenMeteoClient and GeocodingClient

### application.properties
- Server port: 8080
- DB: PostgreSQL on localhost:5432/breathego
- JPA: Validate mode (schema managed by Flyway)
- Flyway: Enabled with baseline-on-migrate
- Logging: DEBUG for com.breathego

## Testing

| Test Class              | Type        | Scope                     | Tests |
|-------------------------|-------------|---------------------------|-------|
| BreatheGoApplicationTests| Integration| Context loading           | 1     |
| ScoringServiceTest      | Unit        | All scoring components    | 15+   |
| LocationControllerTest  | Web MVC     | Controller + validation   | 7     |
| OpenMeteoClientTest     | Unit        | HTTP client + parsing     | 5+    |

**Test Infrastructure:**
- H2 in-memory database (test profile)
- MockWebServer (OkHttp) for HTTP mocking
- Mockito for service mocking
- Flyway disabled in tests
