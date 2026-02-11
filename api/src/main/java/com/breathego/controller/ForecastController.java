package com.breathego.controller;

import com.breathego.dto.ForecastResponse;
import com.breathego.dto.TrendsResponse;
import com.breathego.service.ForecastService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations/{id}")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/forecast")
    public ForecastResponse getForecast(@PathVariable Long id) {
        return forecastService.getForecast(id);
    }

    @GetMapping("/trends")
    public TrendsResponse getTrends(
            @PathVariable Long id,
            @RequestParam(defaultValue = "14") int period
    ) {
        // Limit period to max 30 days
        int days = Math.min(Math.max(period, 7), 30);
        return forecastService.getTrends(id, days);
    }
}
