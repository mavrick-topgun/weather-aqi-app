package com.breathego.client;

import com.breathego.dto.AqiData;
import com.breathego.dto.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private final WebClient webClient;
    private final String weatherUrl;
    private final String airQualityUrl;

    public OpenMeteoClient(
            WebClient webClient,
            @Value("${openmeteo.weather.url}") String weatherUrl,
            @Value("${openmeteo.airquality.url}") String airQualityUrl
    ) {
        this.webClient = webClient;
        this.weatherUrl = weatherUrl;
        this.airQualityUrl = airQualityUrl;
    }

    public List<WeatherData> getWeatherForecast(BigDecimal latitude, BigDecimal longitude, int days) {
        log.debug("Fetching weather forecast for lat={}, lon={}, days={}", latitude, longitude, days);

        String url = String.format(
                "%s?latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max,wind_direction_10m_dominant,uv_index_max&forecast_days=%d&timezone=auto",
                weatherUrl, latitude, longitude, days
        );

        JsonNode response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseWeatherResponse(response);
    }

    public List<AqiData> getAirQuality(BigDecimal latitude, BigDecimal longitude, int days) {
        log.debug("Fetching air quality for lat={}, lon={}, days={}", latitude, longitude, days);

        // Air Quality API uses hourly data, not daily
        String url = String.format(
                "%s?latitude=%s&longitude=%s&hourly=us_aqi,pm2_5,ozone&forecast_days=%d&timezone=auto",
                airQualityUrl, latitude, longitude, days
        );

        JsonNode response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseHourlyAqiResponse(response);
    }

    public List<AqiData> getHistoricalAirQuality(BigDecimal latitude, BigDecimal longitude, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching historical air quality for lat={}, lon={}, start={}, end={}", latitude, longitude, startDate, endDate);

        String url = String.format(
                "%s?latitude=%s&longitude=%s&hourly=us_aqi,pm2_5,ozone&start_date=%s&end_date=%s&timezone=auto",
                airQualityUrl, latitude, longitude, startDate, endDate
        );

        JsonNode response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseHourlyAqiResponse(response);
    }

    public List<WeatherData> getHistoricalWeather(BigDecimal latitude, BigDecimal longitude, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching historical weather for lat={}, lon={}, start={}, end={}", latitude, longitude, startDate, endDate);

        String url = String.format(
                "https://archive-api.open-meteo.com/v1/archive?latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max,wind_direction_10m_dominant&start_date=%s&end_date=%s&timezone=auto",
                latitude, longitude, startDate, endDate
        );

        JsonNode response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseWeatherResponse(response);
    }

    private List<WeatherData> parseWeatherResponse(JsonNode response) {
        List<WeatherData> result = new ArrayList<>();

        if (response == null || !response.has("daily")) {
            return result;
        }

        JsonNode daily = response.get("daily");
        JsonNode dates = daily.get("time");
        JsonNode tempMax = daily.get("temperature_2m_max");
        JsonNode tempMin = daily.get("temperature_2m_min");
        JsonNode precip = daily.get("precipitation_sum");
        JsonNode wind = daily.get("wind_speed_10m_max");
        JsonNode windDir = daily.has("wind_direction_10m_dominant") ? daily.get("wind_direction_10m_dominant") : null;
        JsonNode uv = daily.has("uv_index_max") ? daily.get("uv_index_max") : null;

        for (int i = 0; i < dates.size(); i++) {
            result.add(new WeatherData(
                    LocalDate.parse(dates.get(i).asText()),
                    getBigDecimal(tempMax, i),
                    getBigDecimal(tempMin, i),
                    getBigDecimal(precip, i),
                    getBigDecimal(wind, i),
                    windDir != null ? getInteger(windDir, i) : null,
                    uv != null ? getBigDecimal(uv, i) : null
            ));
        }

        return result;
    }

    /**
     * Parse hourly AQI data and aggregate to daily values.
     * Uses max AQI for each day (worst case for the day).
     */
    private List<AqiData> parseHourlyAqiResponse(JsonNode response) {
        List<AqiData> result = new ArrayList<>();

        if (response == null || !response.has("hourly")) {
            return result;
        }

        JsonNode hourly = response.get("hourly");
        JsonNode times = hourly.get("time");
        JsonNode usAqi = hourly.get("us_aqi");
        JsonNode pm25 = hourly.get("pm2_5");
        JsonNode ozone = hourly.get("ozone");

        // Group hourly data by date and aggregate
        Map<LocalDate, List<Integer>> aqiByDate = new LinkedHashMap<>();
        Map<LocalDate, List<Double>> pm25ByDate = new LinkedHashMap<>();
        Map<LocalDate, List<Double>> ozoneByDate = new LinkedHashMap<>();

        for (int i = 0; i < times.size(); i++) {
            String timeStr = times.get(i).asText();
            LocalDate date = LocalDate.parse(timeStr.substring(0, 10));

            aqiByDate.computeIfAbsent(date, k -> new ArrayList<>());
            pm25ByDate.computeIfAbsent(date, k -> new ArrayList<>());
            ozoneByDate.computeIfAbsent(date, k -> new ArrayList<>());

            if (usAqi != null && !usAqi.get(i).isNull()) {
                aqiByDate.get(date).add(usAqi.get(i).asInt());
            }
            if (pm25 != null && !pm25.get(i).isNull()) {
                pm25ByDate.get(date).add(pm25.get(i).asDouble());
            }
            if (ozone != null && !ozone.get(i).isNull()) {
                ozoneByDate.get(date).add(ozone.get(i).asDouble());
            }
        }

        // Create daily aggregates (using max for AQI, average for pm25/ozone)
        for (LocalDate date : aqiByDate.keySet()) {
            List<Integer> aqiValues = aqiByDate.get(date);
            List<Double> pm25Values = pm25ByDate.get(date);
            List<Double> ozoneValues = ozoneByDate.get(date);

            Integer maxAqi = aqiValues.isEmpty() ? null : Collections.max(aqiValues);
            BigDecimal avgPm25 = pm25Values.isEmpty() ? null :
                    BigDecimal.valueOf(pm25Values.stream().mapToDouble(d -> d).average().orElse(0))
                            .setScale(2, RoundingMode.HALF_UP);
            BigDecimal avgOzone = ozoneValues.isEmpty() ? null :
                    BigDecimal.valueOf(ozoneValues.stream().mapToDouble(d -> d).average().orElse(0))
                            .setScale(2, RoundingMode.HALF_UP);

            result.add(new AqiData(date, maxAqi, avgPm25, avgOzone));
        }

        return result;
    }

    private BigDecimal getBigDecimal(JsonNode node, int index) {
        if (node == null || node.get(index) == null || node.get(index).isNull()) {
            return null;
        }
        return BigDecimal.valueOf(node.get(index).asDouble());
    }

    private Integer getInteger(JsonNode node, int index) {
        if (node == null || node.get(index) == null || node.get(index).isNull()) {
            return null;
        }
        return node.get(index).asInt();
    }
}
