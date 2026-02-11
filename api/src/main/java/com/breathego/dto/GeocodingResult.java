package com.breathego.dto;

import java.math.BigDecimal;

public record GeocodingResult(
        Long id,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String country,
        String countryCode,
        String admin1,  // State/Province
        String timezone
) {}
