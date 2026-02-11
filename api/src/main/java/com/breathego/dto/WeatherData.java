package com.breathego.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeatherData(
        LocalDate date,
        BigDecimal temperatureMax,
        BigDecimal temperatureMin,
        BigDecimal precipitation,
        BigDecimal windSpeed,
        BigDecimal uvIndex
) {}
