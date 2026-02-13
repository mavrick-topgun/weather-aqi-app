# Breathe & Go - Frontend Component Architecture

## Component Tree

```
RootLayout (layout.tsx)
|
+-- ThemeProvider (providers.tsx)
|   +-- UnitProvider (providers.tsx)
|       |
|       +-- Header
|       |   +-- Logo ("Breathe & Go")
|       |   +-- Nav Links (Dashboard, Map, Locations)
|       |   +-- UnitToggle (metric/imperial)
|       |   +-- ThemeToggle (dark/light)
|       |
|       +-- ErrorBoundary
|           |
|           +-- Page Content (route-dependent)
|
|
+-- / (Dashboard - page.tsx)
|   +-- LocationPicker
|   |   +-- LocationSearch (debounced, keyboard nav)
|   |   +-- Location list (selectable, deletable)
|   |
|   +-- ScoreCard (ring + recommendation + reasons)
|   +-- WeatherDetails (temp, rain, wind, UV)
|   +-- AqiDetails (AQI value, PM2.5, ozone, bar)
|   +-- ForecastCards (3-day rows)
|   +-- TrendChart[aqi] (SVG line chart)
|
+-- /map (Map - map/page.tsx)
|   +-- AqiMap (dynamic import, SSR disabled)
|       +-- AqiMapInner
|           +-- TileLayer (theme-aware, Carto)
|           +-- AQICN overlay tiles
|           +-- Marker[] (per location)
|           |   +-- Popup
|           |       +-- LocationPopup (loads forecast)
|           +-- AqiLegend (collapsible)
|
+-- /locations (Locations - locations/page.tsx)
    +-- LocationSearch
    +-- Location table (name, coords, actions)
```

## Context Providers

### ThemeProvider
- **State:** `theme: 'light' | 'dark'`
- **Storage:** `localStorage.theme`
- **Default:** System preference via `prefers-color-scheme`
- **Effect:** Toggles `dark` class on `<html>`
- **Hook:** `useTheme()` returns `{ theme, toggleTheme }`

### UnitProvider
- **State:** `units: 'metric' | 'imperial'`
- **Storage:** `localStorage.units`
- **Default:** `'metric'`
- **Hook:** `useUnits()` returns `{ units, toggleUnits }`
- **Consumers:** WeatherDetails, ForecastCards, TrendChart

## Component Details

### ScoreCard
| Prop           | Type             | Description                    |
|----------------|------------------|--------------------------------|
| score          | number           | 0-100 composite score          |
| recommendation | Recommendation   | Great / Okay / Caution / Avoid |
| reasons        | string[]         | Explanation bullet points      |
| locationName   | string           | Display name                   |

**Visual:** SVG circular progress ring (w-28 h-28) with score number centered. Ring and badge are laid out side-by-side. Color changes per recommendation level.

### WeatherDetails
| Prop    | Type        | Description        |
|---------|-------------|--------------------|
| weather | WeatherInfo | Today's weather    |

**Metrics displayed:** Temperature (high/low), Precipitation (mm), Wind speed + direction arrow, UV Index with severity label.

**Unit conversion:** Temperature (C/F), Wind speed (km/h / mph). Conversion uses `useUnits()` context.

### AqiDetails
| Prop | Type    | Description          |
|------|---------|----------------------|
| aqi  | AqiInfo | Today's air quality  |

**Metrics displayed:** AQI value with category badge, progress bar, PM2.5, Ozone.

**AQI categories:** Good (green), Moderate (yellow), Unhealthy for Sensitive (orange), Unhealthy (red), Very Unhealthy (purple).

### ForecastCards
| Prop     | Type            | Description          |
|----------|-----------------|----------------------|
| forecast | DailyForecast[] | 3-day forecast array |

**Layout:** Stacked horizontal rows. Each row shows: day label, recommendation badge, score, temperature range, AQI.

### TrendChart
| Prop        | Type              | Description                |
|-------------|-------------------|----------------------------|
| aqi         | AqiTrend[]        | AQI data points (optional) |
| temperature | TemperatureTrend[]| Temp data points (optional)|
| type        | 'aqi' or 'temperature' | Chart type            |
| title       | string            | Chart heading              |

**AQI mode:** SVG line chart with gradient fill, color-coded dots (green/yellow/orange/red by AQI level), hover tooltips.

**Temperature mode:** Vertical bar chart with green bars, hover tooltips showing converted values.

### LocationSearch
| Prop        | Type                               | Description        |
|-------------|------------------------------------|--------------------|
| onSelect    | (result: GeocodingResult) => void  | Selection callback |
| placeholder | string (optional)                  | Input placeholder  |

**Behavior:** 300ms debounce, min 2 chars, keyboard navigation (arrows/enter/escape), click outside closes dropdown.

### LocationPopup (Map)
| Prop     | Type     | Description    |
|----------|----------|----------------|
| location | Location | Map location   |

**Behavior:** Loads forecast on mount via `api.getForecast()`. Shows AQI value and recommendation badge. Links to dashboard.

## API Client

**File:** `lib/api.ts`
**Class:** `ApiClient` (exported as singleton `api`)
**Base URL:** `/api` (proxied to `localhost:8080`)

| Method                 | HTTP           | Endpoint                                  |
|------------------------|----------------|-------------------------------------------|
| getLocations()         | GET            | /api/locations                            |
| getLocation(id)        | GET            | /api/locations/{id}                       |
| createLocation(data)   | POST           | /api/locations                            |
| deleteLocation(id)     | DELETE         | /api/locations/{id}                       |
| getForecast(id)        | GET            | /api/locations/{id}/forecast              |
| getTrends(id, period)  | GET            | /api/locations/{id}/trends?period={n}     |
| searchLocations(q, n)  | GET            | /api/geocoding/search?query={q}&limit={n} |

## Styling

- **Framework:** Tailwind CSS 3.4.1 with `darkMode: 'class'`
- **Font:** Nunito Sans (400, 600, 700, 800)
- **Colors:** Default Tailwind palette + custom `score` colors (great/okay/caution/avoid)
- **Dark mode:** `dark:` prefix classes throughout, CSS variables for base colors
- **Responsive:** Mobile-first with `md:` and `lg:` breakpoints

## State Management

No external state library. All state is managed via:
- `useState` for local component state
- `useContext` for global theme and units (via providers)
- `useCallback` for memoized callbacks
- `useEffect` for data fetching on mount/dependency change
