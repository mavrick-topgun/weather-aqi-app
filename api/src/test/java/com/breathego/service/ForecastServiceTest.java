package com.breathego.service;

import com.breathego.client.OpenMeteoClient;
import com.breathego.domain.DailyMetrics;
import com.breathego.domain.Location;
import com.breathego.dto.*;
import com.breathego.repository.DailyMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private LocationService locationService;

    @Mock
    private OpenMeteoClient openMeteoClient;

    @Mock
    private ScoringService scoringService;

    @Mock
    private DailyMetricsRepository dailyMetricsRepository;

    @InjectMocks
    private ForecastService forecastService;

    private Location testLocation;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        testLocation = new Location("Denver, Colorado", new BigDecimal("39.7392"), new BigDecimal("-104.9903"));
        testLocation.setId(1L);
    }

    @Nested
    @DisplayName("getTrends - Date Range Fix")
    class GetTrendsDateRangeTests {

        @Test
        @DisplayName("Should query cache with forward-looking date range [today, today+6]")
        void shouldUseForwardLookingDateRange() {
            when(locationService.getLocationEntity(1L)).thenReturn(testLocation);
            when(dailyMetricsRepository.findByLocationIdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(openMeteoClient.getWeatherForecast(any(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(openMeteoClient.getAirQuality(any(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            forecastService.getTrends(1L, 7);

            ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);

            verify(dailyMetricsRepository).findByLocationIdAndDateBetween(
                    eq(1L), startCaptor.capture(), endCaptor.capture()
            );

            assertEquals(today, startCaptor.getValue(), "Start date should be today");
            assertEquals(today.plusDays(6), endCaptor.getValue(), "End date should be today + 6 days");
        }

        @Test
        @DisplayName("Empty cache should fetch from API and return 7 entries")
        void emptyCache_shouldFetchFromApi() {
            when(locationService.getLocationEntity(1L)).thenReturn(testLocation);
            when(dailyMetricsRepository.findByLocationIdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<WeatherData> weatherList = createWeatherList(7);
            List<AqiData> aqiList = createAqiList(7);

            when(openMeteoClient.getWeatherForecast(any(), any(), eq(7))).thenReturn(weatherList);
            when(openMeteoClient.getAirQuality(any(), any(), eq(7))).thenReturn(aqiList);
            when(scoringService.calculate(any(), any()))
                    .thenReturn(new Score(85, "Great", List.of("Air quality is good")));

            TrendsResponse result = forecastService.getTrends(1L, 7);

            assertEquals(7, result.aqi().size(), "Should return 7 AQI trend entries");
            assertEquals(7, result.temperature().size(), "Should return 7 temperature trend entries");
            assertEquals(7, result.scores().size(), "Should return 7 score trend entries");

            verify(openMeteoClient).getWeatherForecast(any(), any(), eq(7));
            verify(openMeteoClient).getAirQuality(any(), any(), eq(7));
        }

        @Test
        @DisplayName("Complete cache (7 entries) should NOT call API")
        void completeCache_shouldNotCallApi() {
            when(locationService.getLocationEntity(1L)).thenReturn(testLocation);

            List<DailyMetrics> cachedMetrics = createCachedMetrics(7);
            when(dailyMetricsRepository.findByLocationIdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(cachedMetrics);

            TrendsResponse result = forecastService.getTrends(1L, 7);

            assertEquals(7, result.aqi().size(), "Should return 7 cached AQI entries");
            verifyNoInteractions(openMeteoClient);
        }

        @Test
        @DisplayName("Partial cache (< 7 entries) should fetch from API instead of returning partial data")
        void partialCache_shouldFetchFromApi() {
            when(locationService.getLocationEntity(1L)).thenReturn(testLocation);

            // Simulate the bug scenario: only 1 entry in cache (today's data from getForecast)
            List<DailyMetrics> partialCache = createCachedMetrics(1);
            when(dailyMetricsRepository.findByLocationIdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(partialCache);

            List<WeatherData> weatherList = createWeatherList(7);
            List<AqiData> aqiList = createAqiList(7);

            when(openMeteoClient.getWeatherForecast(any(), any(), eq(7))).thenReturn(weatherList);
            when(openMeteoClient.getAirQuality(any(), any(), eq(7))).thenReturn(aqiList);
            when(scoringService.calculate(any(), any()))
                    .thenReturn(new Score(75, "Okay", List.of("Air quality is moderate")));

            TrendsResponse result = forecastService.getTrends(1L, 7);

            assertEquals(7, result.aqi().size(),
                    "Should return 7 entries from API, not 1 from partial cache");
            verify(openMeteoClient).getWeatherForecast(any(), any(), eq(7));
            verify(openMeteoClient).getAirQuality(any(), any(), eq(7));
        }

        @Test
        @DisplayName("API failure should fall back to partial cache")
        void apiFailure_shouldFallbackToPartialCache() {
            when(locationService.getLocationEntity(1L)).thenReturn(testLocation);

            List<DailyMetrics> partialCache = createCachedMetrics(3);
            when(dailyMetricsRepository.findByLocationIdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(partialCache);

            when(openMeteoClient.getWeatherForecast(any(), any(), anyInt()))
                    .thenThrow(new RuntimeException("API unavailable"));

            TrendsResponse result = forecastService.getTrends(1L, 7);

            assertEquals(3, result.aqi().size(),
                    "Should fall back to 3 partial cache entries when API fails");
            assertEquals(3, result.temperature().size());
            assertEquals(3, result.scores().size());
        }

        @Test
        @DisplayName("Days parameter should be capped at 7")
        void daysParamShouldBeCappedAt7() {
            when(locationService.getLocationEntity(1L)).thenReturn(testLocation);
            when(dailyMetricsRepository.findByLocationIdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<WeatherData> weatherList = createWeatherList(7);
            List<AqiData> aqiList = createAqiList(7);

            when(openMeteoClient.getWeatherForecast(any(), any(), eq(7))).thenReturn(weatherList);
            when(openMeteoClient.getAirQuality(any(), any(), eq(7))).thenReturn(aqiList);
            when(scoringService.calculate(any(), any()))
                    .thenReturn(new Score(85, "Great", List.of("Good")));

            forecastService.getTrends(1L, 14);

            // Should cap at 7, not request 14
            verify(openMeteoClient).getWeatherForecast(any(), any(), eq(7));
            verify(openMeteoClient).getAirQuality(any(), any(), eq(7));
        }

        @Test
        @DisplayName("Should cache fetched API data via saveDailyMetrics")
        void shouldCacheFetchedApiData() {
            when(locationService.getLocationEntity(1L)).thenReturn(testLocation);
            when(dailyMetricsRepository.findByLocationIdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<WeatherData> weatherList = createWeatherList(3);
            List<AqiData> aqiList = createAqiList(3);

            when(openMeteoClient.getWeatherForecast(any(), any(), anyInt())).thenReturn(weatherList);
            when(openMeteoClient.getAirQuality(any(), any(), anyInt())).thenReturn(aqiList);
            when(scoringService.calculate(any(), any()))
                    .thenReturn(new Score(85, "Great", List.of("Good")));

            forecastService.getTrends(1L, 3);

            verify(dailyMetricsRepository, times(3)).save(any(DailyMetrics.class));
        }

        @Test
        @DisplayName("AQI trend entries should contain correct values from API data")
        void aqiTrendsShouldContainCorrectValues() {
            when(locationService.getLocationEntity(1L)).thenReturn(testLocation);
            when(dailyMetricsRepository.findByLocationIdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<WeatherData> weatherList = createWeatherList(3);
            List<AqiData> aqiList = List.of(
                    new AqiData(today, 45, new BigDecimal("12.5"), new BigDecimal("30.0")),
                    new AqiData(today.plusDays(1), 60, new BigDecimal("18.0"), new BigDecimal("35.0")),
                    new AqiData(today.plusDays(2), null, null, null)
            );

            when(openMeteoClient.getWeatherForecast(any(), any(), anyInt())).thenReturn(weatherList);
            when(openMeteoClient.getAirQuality(any(), any(), anyInt())).thenReturn(aqiList);
            when(scoringService.calculate(any(), any()))
                    .thenReturn(new Score(80, "Great", List.of("Good")));

            TrendsResponse result = forecastService.getTrends(1L, 3);

            assertEquals(45, result.aqi().get(0).value());
            assertEquals(60, result.aqi().get(1).value());
            assertNull(result.aqi().get(2).value(), "Null AQI should be preserved as null");
        }
    }

    // --- Helper methods ---

    private List<WeatherData> createWeatherList(int days) {
        List<WeatherData> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            list.add(new WeatherData(
                    today.plusDays(i),
                    new BigDecimal("22"),
                    new BigDecimal("15"),
                    BigDecimal.ZERO,
                    new BigDecimal("10"),
                    180,
                    new BigDecimal("5")
            ));
        }
        return list;
    }

    private List<AqiData> createAqiList(int days) {
        List<AqiData> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            list.add(new AqiData(
                    today.plusDays(i),
                    40 + i * 5,
                    new BigDecimal("12.5"),
                    new BigDecimal("30.0")
            ));
        }
        return list;
    }

    private List<DailyMetrics> createCachedMetrics(int days) {
        List<DailyMetrics> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            DailyMetrics dm = new DailyMetrics();
            dm.setLocation(testLocation);
            dm.setDate(today.plusDays(i));
            dm.setScore(80 + i);
            dm.setRecommendation("Great");
            dm.setAqiValue(40 + i * 5);
            dm.setTemperatureMax(new BigDecimal("22"));
            dm.setTemperatureMin(new BigDecimal("15"));
            dm.setPrecipitation(BigDecimal.ZERO);
            dm.setWindSpeed(new BigDecimal("10"));
            dm.setUvIndex(new BigDecimal("5"));
            list.add(dm);
        }
        return list;
    }
}
