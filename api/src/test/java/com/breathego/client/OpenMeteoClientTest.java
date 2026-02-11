package com.breathego.client;

import com.breathego.dto.AqiData;
import com.breathego.dto.WeatherData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OpenMeteoClient to verify:
 * 1. Correct URL construction with proper parameters
 * 2. Proper parsing of API responses
 * 3. Handling of edge cases and errors
 */
class OpenMeteoClientTest {

    private MockWebServer mockWebServer;
    private OpenMeteoClient openMeteoClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient webClient = WebClient.builder().build();

        openMeteoClient = new OpenMeteoClient(
                webClient,
                baseUrl + "v1/forecast",
                baseUrl + "v1/air-quality"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("Weather Forecast API Tests")
    class WeatherForecastTests {

        @Test
        @DisplayName("Should construct correct URL with daily parameters")
        void shouldConstructCorrectWeatherUrl() throws InterruptedException {
            // Given - Real API response structure from Open-Meteo
            String response = """
                {
                    "latitude": 40.7128,
                    "longitude": -74.006,
                    "daily": {
                        "time": ["2026-02-10", "2026-02-11"],
                        "temperature_2m_max": [5.2, 8.1],
                        "temperature_2m_min": [-2.1, 0.5],
                        "precipitation_sum": [0.0, 2.5],
                        "wind_speed_10m_max": [15.5, 22.0],
                        "uv_index_max": [3.5, 4.2]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            openMeteoClient.getWeatherForecast(new BigDecimal("40.7128"), new BigDecimal("-74.006"), 3);

            // Then - Verify the request URL
            RecordedRequest request = mockWebServer.takeRequest();
            String requestUrl = request.getPath();

            assertNotNull(requestUrl);
            assertTrue(requestUrl.contains("daily="), "URL should use 'daily' parameter");
            assertTrue(requestUrl.contains("temperature_2m_max"), "Should request max temperature");
            assertTrue(requestUrl.contains("temperature_2m_min"), "Should request min temperature");
            assertTrue(requestUrl.contains("precipitation_sum"), "Should request precipitation");
            assertTrue(requestUrl.contains("wind_speed_10m_max"), "Should request wind speed");
            assertTrue(requestUrl.contains("uv_index_max"), "Should request UV index");
            assertTrue(requestUrl.contains("forecast_days=3"), "Should specify forecast days");
            assertTrue(requestUrl.contains("timezone=auto"), "Should use auto timezone");
        }

        @Test
        @DisplayName("Should parse weather response correctly")
        void shouldParseWeatherResponse() {
            // Given
            String response = """
                {
                    "daily": {
                        "time": ["2026-02-10", "2026-02-11", "2026-02-12"],
                        "temperature_2m_max": [5.2, 8.1, 12.0],
                        "temperature_2m_min": [-2.1, 0.5, 3.0],
                        "precipitation_sum": [0.0, 2.5, 0.0],
                        "wind_speed_10m_max": [15.5, 22.0, 10.0],
                        "uv_index_max": [3.5, 4.2, 5.0]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            List<WeatherData> result = openMeteoClient.getWeatherForecast(
                    new BigDecimal("40.7128"), new BigDecimal("-74.006"), 3);

            // Then
            assertEquals(3, result.size());

            WeatherData day1 = result.get(0);
            assertEquals(LocalDate.of(2026, 2, 10), day1.date());
            assertEquals(0, new BigDecimal("5.2").compareTo(day1.temperatureMax()));
            assertEquals(0, new BigDecimal("-2.1").compareTo(day1.temperatureMin()));
            assertEquals(0, new BigDecimal("0.0").compareTo(day1.precipitation()));
            assertEquals(0, new BigDecimal("15.5").compareTo(day1.windSpeed()));
            assertEquals(0, new BigDecimal("3.5").compareTo(day1.uvIndex()));
        }

        @Test
        @DisplayName("Should handle null values in weather response")
        void shouldHandleNullValuesInWeatherResponse() {
            // Given
            String response = """
                {
                    "daily": {
                        "time": ["2026-02-10"],
                        "temperature_2m_max": [null],
                        "temperature_2m_min": [5.0],
                        "precipitation_sum": [null],
                        "wind_speed_10m_max": [10.0],
                        "uv_index_max": [null]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            List<WeatherData> result = openMeteoClient.getWeatherForecast(
                    new BigDecimal("40.7128"), new BigDecimal("-74.006"), 1);

            // Then
            assertEquals(1, result.size());
            WeatherData day1 = result.get(0);
            assertNull(day1.temperatureMax());
            assertNotNull(day1.temperatureMin());
            assertNull(day1.precipitation());
            assertNotNull(day1.windSpeed());
            assertNull(day1.uvIndex());
        }

        @Test
        @DisplayName("Should return empty list when no daily data")
        void shouldReturnEmptyListWhenNoDailyData() {
            // Given
            String response = """
                {
                    "latitude": 40.7128,
                    "longitude": -74.006
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            List<WeatherData> result = openMeteoClient.getWeatherForecast(
                    new BigDecimal("40.7128"), new BigDecimal("-74.006"), 3);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Air Quality API Tests")
    class AirQualityTests {

        @Test
        @DisplayName("Should construct correct URL with HOURLY parameters (not daily)")
        void shouldConstructCorrectAqiUrlWithHourlyParams() throws InterruptedException {
            // Given - Real API response structure from Open-Meteo Air Quality API
            String response = """
                {
                    "latitude": 40.7128,
                    "longitude": -74.006,
                    "hourly": {
                        "time": ["2026-02-10T00:00", "2026-02-10T01:00"],
                        "us_aqi": [45, 50],
                        "pm2_5": [12.5, 15.0],
                        "ozone": [30.0, 35.0]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            openMeteoClient.getAirQuality(new BigDecimal("40.7128"), new BigDecimal("-74.006"), 3);

            // Then - Verify the request URL uses 'hourly' NOT 'daily'
            RecordedRequest request = mockWebServer.takeRequest();
            String requestUrl = request.getPath();

            assertNotNull(requestUrl);
            assertTrue(requestUrl.contains("hourly="),
                    "URL MUST use 'hourly' parameter - Air Quality API does not support 'daily'");
            assertFalse(requestUrl.contains("daily="),
                    "URL must NOT use 'daily' parameter - this would cause 400 Bad Request");
            assertTrue(requestUrl.contains("us_aqi"), "Should request US AQI");
            assertTrue(requestUrl.contains("pm2_5"), "Should request PM2.5");
            assertTrue(requestUrl.contains("ozone"), "Should request ozone");
            assertTrue(requestUrl.contains("forecast_days=3"), "Should specify forecast days");
        }

        @Test
        @DisplayName("Should aggregate hourly AQI data to daily values")
        void shouldAggregateHourlyToDailyAqi() {
            // Given - Hourly data for one day (simplified)
            String response = """
                {
                    "hourly": {
                        "time": [
                            "2026-02-10T00:00", "2026-02-10T06:00",
                            "2026-02-10T12:00", "2026-02-10T18:00",
                            "2026-02-11T00:00", "2026-02-11T06:00"
                        ],
                        "us_aqi": [40, 50, 80, 60, 45, 55],
                        "pm2_5": [10.0, 15.0, 25.0, 20.0, 12.0, 18.0],
                        "ozone": [20.0, 25.0, 35.0, 30.0, 22.0, 28.0]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            List<AqiData> result = openMeteoClient.getAirQuality(
                    new BigDecimal("40.7128"), new BigDecimal("-74.006"), 2);

            // Then - Should have 2 days
            assertEquals(2, result.size());

            // Day 1: Max AQI should be 80 (worst hour)
            AqiData day1 = result.get(0);
            assertEquals(LocalDate.of(2026, 2, 10), day1.date());
            assertEquals(80, day1.usAqi(), "Should use MAX AQI for the day (worst case)");
            assertNotNull(day1.pm25());
            assertNotNull(day1.ozone());

            // Day 2: Max AQI should be 55
            AqiData day2 = result.get(1);
            assertEquals(LocalDate.of(2026, 2, 11), day2.date());
            assertEquals(55, day2.usAqi());
        }

        @Test
        @DisplayName("Should handle null values in hourly AQI response")
        void shouldHandleNullValuesInAqiResponse() {
            // Given
            String response = """
                {
                    "hourly": {
                        "time": ["2026-02-10T00:00", "2026-02-10T12:00"],
                        "us_aqi": [null, 50],
                        "pm2_5": [null, 15.0],
                        "ozone": [30.0, null]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            List<AqiData> result = openMeteoClient.getAirQuality(
                    new BigDecimal("40.7128"), new BigDecimal("-74.006"), 1);

            // Then - Should handle nulls gracefully
            assertEquals(1, result.size());
            AqiData day1 = result.get(0);
            assertEquals(50, day1.usAqi()); // Only non-null value
            assertNotNull(day1.pm25());
            assertNotNull(day1.ozone());
        }

        @Test
        @DisplayName("Should return empty list when no hourly data")
        void shouldReturnEmptyListWhenNoHourlyData() {
            // Given
            String response = """
                {
                    "latitude": 40.7128,
                    "longitude": -74.006
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            List<AqiData> result = openMeteoClient.getAirQuality(
                    new BigDecimal("40.7128"), new BigDecimal("-74.006"), 3);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Historical Data API Tests")
    class HistoricalDataTests {

        @Test
        @DisplayName("Should construct correct URL for historical AQI with hourly params")
        void shouldConstructCorrectHistoricalAqiUrl() throws InterruptedException {
            // Given
            String response = """
                {
                    "hourly": {
                        "time": ["2026-02-01T00:00"],
                        "us_aqi": [45],
                        "pm2_5": [12.5],
                        "ozone": [30.0]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Content-Type", "application/json"));

            // When
            openMeteoClient.getHistoricalAirQuality(
                    new BigDecimal("40.7128"),
                    new BigDecimal("-74.006"),
                    LocalDate.of(2026, 2, 1),
                    LocalDate.of(2026, 2, 7)
            );

            // Then
            RecordedRequest request = mockWebServer.takeRequest();
            String requestUrl = request.getPath();

            assertTrue(requestUrl.contains("hourly="), "Historical AQI must use 'hourly' parameter");
            assertTrue(requestUrl.contains("start_date=2026-02-01"), "Should specify start date");
            assertTrue(requestUrl.contains("end_date=2026-02-07"), "Should specify end date");
        }
    }

    @Nested
    @DisplayName("Contract Tests - Real API Response Samples")
    class ContractTests {

        @Test
        @DisplayName("Should parse real Open-Meteo weather API response")
        void shouldParseRealWeatherApiResponse() {
            // Given - Actual response sample from Open-Meteo API
            String realResponse = """
                {
                    "latitude": 40.7128,
                    "longitude": -74.006,
                    "generationtime_ms": 0.5,
                    "utc_offset_seconds": -18000,
                    "timezone": "America/New_York",
                    "timezone_abbreviation": "EST",
                    "elevation": 10.0,
                    "daily_units": {
                        "time": "iso8601",
                        "temperature_2m_max": "°C",
                        "temperature_2m_min": "°C",
                        "precipitation_sum": "mm",
                        "wind_speed_10m_max": "km/h",
                        "uv_index_max": ""
                    },
                    "daily": {
                        "time": ["2026-02-10", "2026-02-11", "2026-02-12"],
                        "temperature_2m_max": [0.8, 3.1, 0.9],
                        "temperature_2m_min": [-5.7, -2.4, -4.8],
                        "precipitation_sum": [0.00, 0.00, 0.10],
                        "wind_speed_10m_max": [12.6, 18.4, 15.2],
                        "uv_index_max": [4.05, 3.80, 4.15]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(realResponse)
                    .addHeader("Content-Type", "application/json"));

            // When
            List<WeatherData> result = openMeteoClient.getWeatherForecast(
                    new BigDecimal("40.7128"), new BigDecimal("-74.006"), 3);

            // Then
            assertEquals(3, result.size());
            assertEquals(LocalDate.of(2026, 2, 10), result.get(0).date());
            assertEquals(0, new BigDecimal("0.8").compareTo(result.get(0).temperatureMax()));
        }

        @Test
        @DisplayName("Should parse real Open-Meteo air quality API response")
        void shouldParseRealAirQualityApiResponse() {
            // Given - Actual response sample from Open-Meteo Air Quality API
            String realResponse = """
                {
                    "latitude": 40.699997,
                    "longitude": -74.0,
                    "generationtime_ms": 0.16,
                    "utc_offset_seconds": -18000,
                    "timezone": "America/New_York",
                    "timezone_abbreviation": "GMT-5",
                    "elevation": 32.0,
                    "hourly_units": {
                        "time": "iso8601",
                        "us_aqi": "USAQI",
                        "pm2_5": "μg/m³",
                        "ozone": "μg/m³"
                    },
                    "hourly": {
                        "time": [
                            "2026-02-10T00:00", "2026-02-10T01:00", "2026-02-10T02:00",
                            "2026-02-10T03:00", "2026-02-10T04:00", "2026-02-10T05:00"
                        ],
                        "us_aqi": [54, 56, 58, 60, 63, 65],
                        "pm2_5": [29.9, 30.0, 30.7, 31.5, 32.7, 33.8],
                        "ozone": [6.0, 6.0, 6.0, 6.0, 6.0, 4.0]
                    }
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(realResponse)
                    .addHeader("Content-Type", "application/json"));

            // When
            List<AqiData> result = openMeteoClient.getAirQuality(
                    new BigDecimal("40.7128"), new BigDecimal("-74.006"), 1);

            // Then
            assertEquals(1, result.size());
            AqiData day1 = result.get(0);
            assertEquals(LocalDate.of(2026, 2, 10), day1.date());
            assertEquals(65, day1.usAqi()); // Max AQI of the day
            assertNotNull(day1.pm25());
            assertNotNull(day1.ozone());
        }
    }
}
