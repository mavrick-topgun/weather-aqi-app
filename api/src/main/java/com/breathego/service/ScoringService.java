package com.breathego.service;

import com.breathego.dto.AqiData;
import com.breathego.dto.Score;
import com.breathego.dto.WeatherData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScoringService {

    // Weights (total = 100)
    private static final int AQI_WEIGHT = 60;
    private static final int PRECIP_WEIGHT = 15;
    private static final int TEMP_WEIGHT = 15;
    private static final int WIND_WEIGHT = 10;

    // Thresholds
    private static final int OPTIMAL_TEMP_MIN = 15; // Celsius
    private static final int OPTIMAL_TEMP_MAX = 25;
    private static final double HIGH_WIND_THRESHOLD = 40.0; // km/h
    private static final double HEAVY_RAIN_THRESHOLD = 10.0; // mm

    public Score calculate(WeatherData weather, AqiData aqi) {
        int aqiScore = calculateAqiScore(aqi != null ? aqi.usAqi() : null);
        int precipScore = calculatePrecipScore(weather);
        int tempScore = calculateTempScore(weather);
        int windScore = calculateWindScore(weather);

        int total = aqiScore + precipScore + tempScore + windScore;
        List<String> reasons = generateReasons(weather, aqi, aqiScore, precipScore, tempScore, windScore);

        return new Score(total, getRecommendation(total), reasons);
    }

    public String getRecommendation(int score) {
        if (score >= 80) return "Great";
        if (score >= 60) return "Okay";
        if (score >= 40) return "Caution";
        return "Avoid";
    }

    int calculateAqiScore(Integer aqiValue) {
        if (aqiValue == null) {
            return AQI_WEIGHT / 2; // Unknown AQI, give neutral score
        }

        // US AQI Scale:
        // 0-50: Good
        // 51-100: Moderate
        // 101-150: Unhealthy for Sensitive Groups
        // 151-200: Unhealthy
        // 201-300: Very Unhealthy
        // 301+: Hazardous

        if (aqiValue <= 50) {
            return AQI_WEIGHT; // Full points for good AQI
        } else if (aqiValue <= 100) {
            // Linear decrease from 60 to 45 (75% of max)
            return (int) (AQI_WEIGHT * (1 - (aqiValue - 50) * 0.005));
        } else if (aqiValue <= 150) {
            // Linear decrease from 45 to 30 (50% of max)
            return (int) (AQI_WEIGHT * (0.75 - (aqiValue - 100) * 0.005));
        } else if (aqiValue <= 200) {
            // Linear decrease from 30 to 15 (25% of max)
            return (int) (AQI_WEIGHT * (0.5 - (aqiValue - 150) * 0.005));
        } else if (aqiValue <= 300) {
            // Linear decrease from 15 to 0
            return (int) (AQI_WEIGHT * (0.25 - (aqiValue - 200) * 0.0025));
        } else {
            return 0;
        }
    }

    int calculatePrecipScore(WeatherData weather) {
        if (weather == null || weather.precipitation() == null) {
            return PRECIP_WEIGHT;
        }

        double precip = weather.precipitation().doubleValue();

        if (precip == 0) {
            return PRECIP_WEIGHT; // Full points for no rain
        } else if (precip < 2) {
            return (int) (PRECIP_WEIGHT * 0.8); // Light drizzle
        } else if (precip < 5) {
            return (int) (PRECIP_WEIGHT * 0.5); // Light rain
        } else if (precip < HEAVY_RAIN_THRESHOLD) {
            return (int) (PRECIP_WEIGHT * 0.3); // Moderate rain
        } else {
            return 0; // Heavy rain
        }
    }

    int calculateTempScore(WeatherData weather) {
        if (weather == null || weather.temperatureMax() == null) {
            return TEMP_WEIGHT / 2;
        }

        double tempMax = weather.temperatureMax().doubleValue();
        double tempMin = weather.temperatureMin() != null ? weather.temperatureMin().doubleValue() : tempMax - 10;

        // Use average temperature for scoring
        double avgTemp = (tempMax + tempMin) / 2;

        if (avgTemp >= OPTIMAL_TEMP_MIN && avgTemp <= OPTIMAL_TEMP_MAX) {
            return TEMP_WEIGHT; // Optimal temperature range
        } else if (avgTemp < OPTIMAL_TEMP_MIN) {
            // Cold: reduce score based on how far from optimal
            double diff = OPTIMAL_TEMP_MIN - avgTemp;
            if (diff <= 5) return (int) (TEMP_WEIGHT * 0.8);
            if (diff <= 10) return (int) (TEMP_WEIGHT * 0.5);
            if (diff <= 20) return (int) (TEMP_WEIGHT * 0.3);
            return 0;
        } else {
            // Hot: reduce score based on how far from optimal
            double diff = avgTemp - OPTIMAL_TEMP_MAX;
            if (diff <= 5) return (int) (TEMP_WEIGHT * 0.8);
            if (diff <= 10) return (int) (TEMP_WEIGHT * 0.5);
            if (diff <= 15) return (int) (TEMP_WEIGHT * 0.3);
            return 0;
        }
    }

    int calculateWindScore(WeatherData weather) {
        if (weather == null || weather.windSpeed() == null) {
            return WIND_WEIGHT;
        }

        double wind = weather.windSpeed().doubleValue();

        if (wind < 15) {
            return WIND_WEIGHT; // Calm
        } else if (wind < 25) {
            return (int) (WIND_WEIGHT * 0.8); // Light breeze
        } else if (wind < 35) {
            return (int) (WIND_WEIGHT * 0.5); // Moderate wind
        } else if (wind < HIGH_WIND_THRESHOLD) {
            return (int) (WIND_WEIGHT * 0.3); // Strong wind
        } else {
            return 0; // Very windy
        }
    }

    private List<String> generateReasons(WeatherData weather, AqiData aqi,
                                          int aqiScore, int precipScore, int tempScore, int windScore) {
        List<String> reasons = new ArrayList<>();

        // AQI reasons
        if (aqi != null && aqi.usAqi() != null) {
            int aqiValue = aqi.usAqi();
            if (aqiValue <= 50) {
                reasons.add("Air quality is good");
            } else if (aqiValue <= 100) {
                reasons.add("Air quality is moderate");
            } else if (aqiValue <= 150) {
                reasons.add("Air quality is unhealthy for sensitive groups");
            } else if (aqiValue <= 200) {
                reasons.add("Air quality is unhealthy");
            } else {
                reasons.add("Air quality is very unhealthy - avoid outdoor activities");
            }
        }

        // Precipitation reasons
        if (weather != null && weather.precipitation() != null) {
            double precip = weather.precipitation().doubleValue();
            if (precip == 0) {
                reasons.add("No precipitation expected");
            } else if (precip < 2) {
                reasons.add("Light drizzle possible");
            } else if (precip < 5) {
                reasons.add("Light rain expected");
            } else if (precip < 10) {
                reasons.add("Moderate rain expected");
            } else {
                reasons.add("Heavy rain expected - bring an umbrella");
            }
        }

        // Temperature reasons
        if (weather != null && weather.temperatureMax() != null) {
            double tempMax = weather.temperatureMax().doubleValue();
            if (tempMax >= OPTIMAL_TEMP_MIN && tempMax <= OPTIMAL_TEMP_MAX + 5) {
                reasons.add("Temperature is pleasant");
            } else if (tempMax < OPTIMAL_TEMP_MIN) {
                reasons.add("Temperature is cool - dress warmly");
            } else {
                reasons.add("Temperature is hot - stay hydrated");
            }
        }

        // Wind reasons
        if (weather != null && weather.windSpeed() != null) {
            double wind = weather.windSpeed().doubleValue();
            if (wind >= HIGH_WIND_THRESHOLD) {
                reasons.add("Strong winds - be cautious outdoors");
            } else if (wind >= 25) {
                reasons.add("Moderate winds expected");
            }
        }

        // UV reasons
        if (weather != null && weather.uvIndex() != null) {
            double uv = weather.uvIndex().doubleValue();
            if (uv >= 8) {
                reasons.add("Very high UV - use sunscreen and seek shade");
            } else if (uv >= 6) {
                reasons.add("High UV - sunscreen recommended");
            }
        }

        return reasons;
    }
}
