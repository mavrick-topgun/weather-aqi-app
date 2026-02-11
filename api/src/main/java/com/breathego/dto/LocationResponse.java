package com.breathego.dto;

import com.breathego.domain.Location;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LocationResponse(
        Long id,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String timezone,
        OffsetDateTime createdAt
) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getLatitude(),
                location.getLongitude(),
                location.getTimezone(),
                location.getCreatedAt()
        );
    }
}
