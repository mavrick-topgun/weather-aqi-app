package com.breathego.service;

import com.breathego.dto.AqiData;
import com.breathego.dto.Score;
import com.breathego.dto.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ScoringServiceTest {

    private ScoringService scoringService;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService();
        today = LocalDate.now();
    }

    @Nested
    @DisplayName("AQI Score Calculation")
    class AqiScoreTests {

        @Test
        @DisplayName("Good AQI (0-50) should give full 60 points")
        void goodAqi() {
            assertEquals(60, scoringService.calculateAqiScore(0));
            assertEquals(60, scoringService.calculateAqiScore(25));
            assertEquals(60, scoringService.calculateAqiScore(50));
        }

        @Test
        @DisplayName("Moderate AQI (51-100) should give 45-60 points")
        void moderateAqi() {
            int score51 = scoringService.calculateAqiScore(51);
            int score100 = scoringService.calculateAqiScore(100);
            assertTrue(score51 < 60 && score51 >= 45);
            assertTrue(score100 < 60 && score100 >= 45);
        }

        @Test
        @DisplayName("Unhealthy for Sensitive Groups AQI (101-150) should give 30-45 points")
        void unhealthySensitiveAqi() {
            int score101 = scoringService.calculateAqiScore(101);
            int score150 = scoringService.calculateAqiScore(150);
            assertTrue(score101 <= 45 && score101 >= 30);
            assertTrue(score150 <= 45 && score150 >= 30);
        }

        @Test
        @DisplayName("Unhealthy AQI (151-200) should give 15-30 points")
        void unhealthyAqi() {
            int score151 = scoringService.calculateAqiScore(151);
            int score200 = scoringService.calculateAqiScore(200);
            assertTrue(score151 <= 30 && score151 >= 15);
            assertTrue(score200 <= 30 && score200 >= 15);
        }

        @Test
        @DisplayName("Very Unhealthy AQI (201-300) should give 0-15 points")
        void veryUnhealthyAqi() {
            int score201 = scoringService.calculateAqiScore(201);
            int score300 = scoringService.calculateAqiScore(300);
            assertTrue(score201 <= 15 && score201 >= 0);
            assertTrue(score300 <= 15 && score300 >= 0);
        }

        @Test
        @DisplayName("Hazardous AQI (301+) should give 0 points")
        void hazardousAqi() {
            assertEquals(0, scoringService.calculateAqiScore(301));
            assertEquals(0, scoringService.calculateAqiScore(500));
        }

        @Test
        @DisplayName("Null AQI should give neutral score")
        void nullAqi() {
            assertEquals(30, scoringService.calculateAqiScore(null));
        }
    }

    @Nested
    @DisplayName("Precipitation Score Calculation")
    class PrecipScoreTests {

        @Test
        @DisplayName("No precipitation should give full 15 points")
        void noPrecipitation() {
            WeatherData weather = createWeather(BigDecimal.ZERO);
            assertEquals(15, scoringService.calculatePrecipScore(weather));
        }

        @Test
        @DisplayName("Light drizzle (<2mm) should give 12 points")
        void lightDrizzle() {
            WeatherData weather = createWeather(new BigDecimal("1.5"));
            assertEquals(12, scoringService.calculatePrecipScore(weather));
        }

        @Test
        @DisplayName("Light rain (2-5mm) should give 7 points")
        void lightRain() {
            WeatherData weather = createWeather(new BigDecimal("4.0"));
            assertEquals(7, scoringService.calculatePrecipScore(weather));
        }

        @Test
        @DisplayName("Heavy rain (10mm+) should give 0 points")
        void heavyRain() {
            WeatherData weather = createWeather(new BigDecimal("15.0"));
            assertEquals(0, scoringService.calculatePrecipScore(weather));
        }

        private WeatherData createWeather(BigDecimal precip) {
            return new WeatherData(today, new BigDecimal("20"), new BigDecimal("15"),
                    precip, new BigDecimal("10"), null, new BigDecimal("5"));
        }
    }

    @Nested
    @DisplayName("Temperature Score Calculation")
    class TempScoreTests {

        @Test
        @DisplayName("Optimal temperature (15-25C) should give full 15 points")
        void optimalTemp() {
            WeatherData weather = createWeather(new BigDecimal("22"), new BigDecimal("18"));
            assertEquals(15, scoringService.calculateTempScore(weather));
        }

        @Test
        @DisplayName("Slightly cold temperature should reduce score")
        void slightlyCold() {
            WeatherData weather = createWeather(new BigDecimal("12"), new BigDecimal("8"));
            int score = scoringService.calculateTempScore(weather);
            assertTrue(score < 15 && score > 0);
        }

        @Test
        @DisplayName("Very hot temperature should reduce score")
        void veryHot() {
            WeatherData weather = createWeather(new BigDecimal("40"), new BigDecimal("32"));
            int score = scoringService.calculateTempScore(weather);
            assertTrue(score < 15 && score >= 0);
        }

        private WeatherData createWeather(BigDecimal max, BigDecimal min) {
            return new WeatherData(today, max, min, BigDecimal.ZERO,
                    new BigDecimal("10"), null, new BigDecimal("5"));
        }
    }

    @Nested
    @DisplayName("Wind Score Calculation")
    class WindScoreTests {

        @Test
        @DisplayName("Calm wind (<15 km/h) should give full 10 points")
        void calmWind() {
            WeatherData weather = createWeather(new BigDecimal("10"));
            assertEquals(10, scoringService.calculateWindScore(weather));
        }

        @Test
        @DisplayName("Strong wind (40+ km/h) should give 0 points")
        void strongWind() {
            WeatherData weather = createWeather(new BigDecimal("45"));
            assertEquals(0, scoringService.calculateWindScore(weather));
        }

        private WeatherData createWeather(BigDecimal wind) {
            return new WeatherData(today, new BigDecimal("20"), new BigDecimal("15"),
                    BigDecimal.ZERO, wind, null, new BigDecimal("5"));
        }
    }

    @Nested
    @DisplayName("Overall Score Calculation")
    class OverallScoreTests {

        @Test
        @DisplayName("Perfect conditions should give 100 score")
        void perfectConditions() {
            WeatherData weather = new WeatherData(
                    today,
                    new BigDecimal("22"),
                    new BigDecimal("18"),
                    BigDecimal.ZERO,
                    new BigDecimal("10"),
                    null,
                    new BigDecimal("3")
            );
            AqiData aqi = new AqiData(today, 25, new BigDecimal("10"), new BigDecimal("30"));

            Score score = scoringService.calculate(weather, aqi);
            assertEquals(100, score.value());
            assertEquals("Great", score.recommendation());
        }

        @Test
        @DisplayName("Bad conditions should give low score")
        void badConditions() {
            WeatherData weather = new WeatherData(
                    today,
                    new BigDecimal("38"),
                    new BigDecimal("30"),
                    new BigDecimal("20"),
                    new BigDecimal("50"),
                    null,
                    new BigDecimal("10")
            );
            AqiData aqi = new AqiData(today, 180, new BigDecimal("100"), new BigDecimal("100"));

            Score score = scoringService.calculate(weather, aqi);
            assertTrue(score.value() < 40);
            assertEquals("Avoid", score.recommendation());
        }

        @Test
        @DisplayName("Score should include relevant reasons")
        void reasonsIncluded() {
            WeatherData weather = new WeatherData(
                    today,
                    new BigDecimal("22"),
                    new BigDecimal("18"),
                    BigDecimal.ZERO,
                    new BigDecimal("10"),
                    null,
                    new BigDecimal("9")
            );
            AqiData aqi = new AqiData(today, 25, new BigDecimal("10"), new BigDecimal("30"));

            Score score = scoringService.calculate(weather, aqi);
            assertFalse(score.reasons().isEmpty());
            assertTrue(score.reasons().stream().anyMatch(r -> r.contains("Air quality")));
        }
    }

    @Nested
    @DisplayName("Recommendation Bands")
    class RecommendationTests {

        @Test
        void greatRecommendation() {
            assertEquals("Great", scoringService.getRecommendation(100));
            assertEquals("Great", scoringService.getRecommendation(80));
        }

        @Test
        void okayRecommendation() {
            assertEquals("Okay", scoringService.getRecommendation(79));
            assertEquals("Okay", scoringService.getRecommendation(60));
        }

        @Test
        void cautionRecommendation() {
            assertEquals("Caution", scoringService.getRecommendation(59));
            assertEquals("Caution", scoringService.getRecommendation(40));
        }

        @Test
        void avoidRecommendation() {
            assertEquals("Avoid", scoringService.getRecommendation(39));
            assertEquals("Avoid", scoringService.getRecommendation(0));
        }
    }
}
