package com.breathego.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ForecastResponse(
        Long locationId,
        String locationName,
        int score,
        String recommendation,
        List<String> reasons,
        WeatherInfo weather,
        AqiInfo aqi,
        List<DailyForecast> forecast
) {
    public record WeatherInfo(
            BigDecimal temperatureMax,
            BigDecimal temperatureMin,
            BigDecimal precipitation,
            BigDecimal windSpeed,
            Integer windDirection,
            BigDecimal uvIndex
    ) {}

    public record AqiInfo(
            Integer value,
            BigDecimal pm25,
            BigDecimal ozone
    ) {}

    public record DailyForecast(
            LocalDate date,
            int score,
            String recommendation,
            BigDecimal temperatureMax,
            BigDecimal temperatureMin,
            Integer aqi
    ) {}
}
