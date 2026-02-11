package com.breathego.dto;

import java.util.List;

public record Score(
        int value,
        String recommendation,
        List<String> reasons
) {}
