package com.breathego.controller;

import com.breathego.client.GeocodingClient;
import com.breathego.dto.GeocodingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private final GeocodingClient geocodingClient;

    public GeocodingController(GeocodingClient geocodingClient) {
        this.geocodingClient = geocodingClient;
    }

    @GetMapping("/search")
    public List<GeocodingResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return geocodingClient.search(query.trim(), Math.min(limit, 10));
    }
}
