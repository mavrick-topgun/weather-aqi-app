package com.breathego.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrendsResponse(
        Long locationId,
        String locationName,
        int period,
        List<AqiTrend> aqi,
        List<TemperatureTrend> temperature,
        List<ScoreTrend> scores
) {
    public record AqiTrend(
            LocalDate date,
            Integer value
    ) {}

    public record TemperatureTrend(
            LocalDate date,
            BigDecimal min,
            BigDecimal max
    ) {}

    public record ScoreTrend(
            LocalDate date,
            int score,
            String recommendation
    ) {}
}
