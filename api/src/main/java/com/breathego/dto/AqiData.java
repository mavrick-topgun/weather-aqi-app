package com.breathego.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AqiData(
        LocalDate date,
        Integer usAqi,
        BigDecimal pm25,
        BigDecimal ozone
) {}
