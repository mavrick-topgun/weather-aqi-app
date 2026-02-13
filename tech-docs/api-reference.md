# Breathe & Go - API Reference

## Base URL

- Development: `http://localhost:8080/api`
- Frontend proxy: `/api` (rewritten by Next.js to backend)

## Authentication

No authentication required. All endpoints are public.

---

## Locations

### GET /api/locations

List all saved locations.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "New York, New York",
    "latitude": 40.712800,
    "longitude": -74.006000,
    "timezone": "America/New_York",
    "createdAt": "2026-02-10T10:00:00Z"
  }
]
```

### GET /api/locations/{id}

Get a single location by ID.

**Response:** `200 OK` | `404 Not Found`
```json
{
  "id": 1,
  "name": "New York, New York",
  "latitude": 40.712800,
  "longitude": -74.006000,
  "timezone": "America/New_York",
  "createdAt": "2026-02-10T10:00:00Z"
}
```

### POST /api/locations

Create a new location.

**Request Body:**
```json
{
  "name": "Los Angeles, California",
  "latitude": 34.052200,
  "longitude": -118.243700,
  "timezone": "America/Los_Angeles"   // optional, defaults to "auto"
}
```

**Validation Rules:**
| Field     | Rule                           |
|-----------|--------------------------------|
| name      | Required, non-blank            |
| latitude  | Required, -90 to 90            |
| longitude | Required, -180 to 180          |
| timezone  | Optional (defaults to "auto")  |

**Response:** `201 Created` | `400 Bad Request`

### DELETE /api/locations/{id}

Delete a location and all its cached metrics.

**Response:** `204 No Content` | `404 Not Found`

---

## Forecast

### GET /api/locations/{id}/forecast

Get current conditions and 3-day forecast for a location.

**Response:** `200 OK` | `404 Not Found` | `503 Service Unavailable`
```json
{
  "locationId": 1,
  "locationName": "New York, New York",
  "score": 75,
  "recommendation": "Okay",
  "reasons": [
    "Air quality is moderate (AQI 65)",
    "No precipitation expected",
    "Temperature is pleasant at 22°C"
  ],
  "weather": {
    "temperatureMax": 22.50,
    "temperatureMin": 15.00,
    "precipitation": 0.00,
    "windSpeed": 12.00,
    "windDirection": 230,
    "uvIndex": 5.50
  },
  "aqi": {
    "value": 65,
    "pm25": 18.50,
    "ozone": 45.00
  },
  "forecast": [
    {
      "date": "2026-02-12",
      "score": 75,
      "recommendation": "Okay",
      "temperatureMax": 22.50,
      "temperatureMin": 15.00,
      "aqi": 65
    },
    {
      "date": "2026-02-13",
      "score": 82,
      "recommendation": "Great",
      "temperatureMax": 20.00,
      "temperatureMin": 14.00,
      "aqi": 42
    },
    {
      "date": "2026-02-14",
      "score": 60,
      "recommendation": "Okay",
      "temperatureMax": 18.00,
      "temperatureMin": 10.00,
      "aqi": 78
    }
  ]
}
```

### GET /api/locations/{id}/trends

Get historical/forecast trend data.

**Query Parameters:**
| Param  | Default | Range | Description           |
|--------|---------|-------|-----------------------|
| period | 14      | 7-30  | Number of days        |

**Response:** `200 OK`
```json
{
  "locationId": 1,
  "locationName": "New York, New York",
  "period": 7,
  "aqi": [
    { "date": "2026-02-06", "value": 55 },
    { "date": "2026-02-07", "value": 62 },
    { "date": "2026-02-08", "value": 48 }
  ],
  "temperature": [
    { "date": "2026-02-06", "min": 12.0, "max": 20.5 },
    { "date": "2026-02-07", "min": 14.0, "max": 22.0 }
  ],
  "scores": [
    { "date": "2026-02-06", "score": 78, "recommendation": "Okay" },
    { "date": "2026-02-07", "score": 65, "recommendation": "Okay" }
  ]
}
```

---

## Geocoding

### GET /api/geocoding/search

Search for locations by name.

**Query Parameters:**
| Param | Required | Default | Description              |
|-------|----------|---------|--------------------------|
| query | Yes      | -       | Search term (min 2 chars) |
| limit | No       | 5       | Max results (1-10)        |

**Response:** `200 OK`
```json
[
  {
    "id": 5128581,
    "name": "New York",
    "latitude": 40.71427,
    "longitude": -74.00597,
    "country": "United States",
    "countryCode": "US",
    "admin1": "New York",
    "timezone": "America/New_York"
  }
]
```

---

## Error Responses

### 400 Bad Request (Validation)
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": {
    "name": "Name is required",
    "latitude": "Latitude must be between -90 and 90"
  }
}
```

### 404 Not Found
```json
{
  "code": "NOT_FOUND",
  "message": "Location not found with id: 99",
  "timestamp": "2026-02-12T14:00:00Z"
}
```

### 503 Service Unavailable
```json
{
  "code": "SERVICE_UNAVAILABLE",
  "message": "Weather data is temporarily unavailable",
  "timestamp": "2026-02-12T14:00:00Z"
}
```

### 500 Internal Server Error
```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "timestamp": "2026-02-12T14:00:00Z"
}
```

---

## External API Dependencies

| API                        | Endpoint                                           | Auth     | Rate Limit      |
|----------------------------|----------------------------------------------------|----------|-----------------|
| Open-Meteo Weather         | `https://api.open-meteo.com/v1/forecast`           | None     | 10,000 req/day  |
| Open-Meteo Air Quality     | `https://air-quality-api.open-meteo.com/v1/air-quality` | None | 10,000 req/day  |
| Open-Meteo Archive         | `https://archive-api.open-meteo.com/v1/archive`    | None     | 10,000 req/day  |
| Open-Meteo Geocoding       | `https://geocoding-api.open-meteo.com/v1/search`   | None     | 10,000 req/day  |
| AQICN Tiles (map overlay)  | `https://tiles.aqicn.org/tiles/...`                | Token    | Varies          |
