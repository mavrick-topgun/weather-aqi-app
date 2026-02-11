package com.breathego.client;

import com.breathego.dto.GeocodingResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class GeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(GeocodingClient.class);
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";

    private final WebClient webClient;

    public GeocodingClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<GeocodingResult> search(String query, int limit) {
        log.debug("Searching for location: {}", query);

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s?name=%s&count=%d&language=en&format=json",
                GEOCODING_URL, encodedQuery, limit);

        JsonNode response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseResponse(response);
    }

    private List<GeocodingResult> parseResponse(JsonNode response) {
        List<GeocodingResult> results = new ArrayList<>();

        if (response == null || !response.has("results")) {
            return results;
        }

        JsonNode resultsNode = response.get("results");
        for (JsonNode node : resultsNode) {
            results.add(new GeocodingResult(
                    node.has("id") ? node.get("id").asLong() : null,
                    node.has("name") ? node.get("name").asText() : null,
                    node.has("latitude") ? BigDecimal.valueOf(node.get("latitude").asDouble()) : null,
                    node.has("longitude") ? BigDecimal.valueOf(node.get("longitude").asDouble()) : null,
                    node.has("country") ? node.get("country").asText() : null,
                    node.has("country_code") ? node.get("country_code").asText() : null,
                    node.has("admin1") ? node.get("admin1").asText() : null,
                    node.has("timezone") ? node.get("timezone").asText() : null
            ));
        }

        return results;
    }
}
