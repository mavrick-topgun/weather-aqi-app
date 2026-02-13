# Breathe & Go - Scoring Algorithm

## Overview

The scoring engine produces a **0-100 composite score** indicating how suitable outdoor conditions are. It combines four weighted factors into a single actionable number.

## Score Breakdown

```
Total Score (0-100)
|
+-- AQI Component       (0-60 points)  ████████████████████  60%
+-- Precipitation Comp.  (0-15 points)  █████                15%
+-- Temperature Comp.    (0-15 points)  █████                15%
+-- Wind Component       (0-10 points)  ███                  10%
```

## Component Details

### 1. Air Quality Index (60 points max)

AQI has the highest weight because poor air quality is the biggest health risk for outdoor activity.

| US AQI Range | Points | Category                    |
|--------------|--------|-----------------------------|
| 0 - 50       | 60     | Good                        |
| 51 - 100     | 45-60  | Moderate (linear decrease)  |
| 101 - 150    | 30-45  | Unhealthy for Sensitive     |
| 151 - 200    | 15-30  | Unhealthy                   |
| 201 - 300    | 0-15   | Very Unhealthy              |
| 301+         | 0      | Hazardous                   |
| null         | 30     | Data unavailable (neutral)  |

The 51-300 ranges use **linear interpolation** within each band.

### 2. Precipitation (15 points max)

| Rainfall (mm) | Points | Description    |
|---------------|--------|----------------|
| 0             | 15     | Dry            |
| < 2           | 12     | Light drizzle  |
| 2 - 5         | 7      | Light rain     |
| 5 - 10        | 4      | Moderate rain  |
| 10+           | 0      | Heavy rain     |

### 3. Temperature (15 points max)

Optimal range: **15-25 degC**. Points decrease as temperature moves away from this range.

```
Points
15 |        ___________
   |       /           \
12 |      /             \
   |     /               \
 7 |    /                 \
   |   /                   \
 4 |  /                     \
   | /                       \
 0 +--+--+--+--+--+--+--+--+-->  degC
   -10  0   10  15  25  35  45
```

| Condition          | Points |
|--------------------|--------|
| 15 - 25 degC       | 15     |
| 10-15 or 25-30     | 12     |
| 5-10 or 30-35      | 7      |
| 0-5 or 35-40       | 4      |
| <0 or >40          | 0      |

### 4. Wind Speed (10 points max)

| Wind Speed (km/h) | Points | Description   |
|--------------------|--------|---------------|
| < 15               | 10     | Calm          |
| 15 - 25            | 8      | Light breeze  |
| 25 - 35            | 5      | Moderate wind |
| 35 - 40            | 3      | Strong wind   |
| 40+                | 0      | Very strong   |

## Recommendations

| Score Range | Recommendation | Meaning                        |
|-------------|----------------|--------------------------------|
| 80 - 100    | Great          | Excellent conditions for going outside |
| 60 - 79     | Okay           | Acceptable with minor concerns |
| 40 - 59     | Caution        | Consider limiting time outside |
| 0 - 39      | Avoid          | Stay indoors if possible       |

## Reason Generation

The algorithm generates human-readable explanations:

- AQI-based: "Air quality is good" / "Air quality is moderate" / "Air quality is unhealthy"
- Precipitation: "No precipitation expected" / "Light rain expected" / "Heavy rain expected"
- Temperature: "Temperature is pleasant" / "It's cold" / "It's hot"
- Wind: "Winds are calm" / "Strong winds expected"

## Implementation

**Class:** `com.breathego.service.ScoringService`

**Method:** `Score calculate(WeatherData weather, AqiData aqi)`

**Returns:** `Score(int value, String recommendation, List<String> reasons)`

All data is stored in metric units (Celsius, km/h, mm). Unit conversion to imperial is handled exclusively in the frontend display layer.
