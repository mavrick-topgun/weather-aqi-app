package com.breathego.service;

import com.breathego.client.OpenMeteoClient;
import com.breathego.domain.DailyMetrics;
import com.breathego.domain.Location;
import com.breathego.dto.*;
import com.breathego.repository.DailyMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ForecastService {

    private static final Logger log = LoggerFactory.getLogger(ForecastService.class);

    private final LocationService locationService;
    private final OpenMeteoClient openMeteoClient;
    private final ScoringService scoringService;
    private final DailyMetricsRepository dailyMetricsRepository;

    public ForecastService(
            LocationService locationService,
            OpenMeteoClient openMeteoClient,
            ScoringService scoringService,
            DailyMetricsRepository dailyMetricsRepository
    ) {
        this.locationService = locationService;
        this.openMeteoClient = openMeteoClient;
        this.scoringService = scoringService;
        this.dailyMetricsRepository = dailyMetricsRepository;
    }

    public ForecastResponse getForecast(Long locationId) {
        Location location = locationService.getLocationEntity(locationId);

        // Fetch weather and AQI data for next 3 days
        List<WeatherData> weatherList = openMeteoClient.getWeatherForecast(
                location.getLatitude(), location.getLongitude(), 3
        );
        List<AqiData> aqiList = openMeteoClient.getAirQuality(
                location.getLatitude(), location.getLongitude(), 3
        );

        if (weatherList.isEmpty()) {
            throw new ForecastUnavailableException("Unable to fetch weather data");
        }

        // Today's data
        WeatherData todayWeather = weatherList.get(0);
        AqiData todayAqi = !aqiList.isEmpty() ? aqiList.get(0) : null;
        Score todayScore = scoringService.calculate(todayWeather, todayAqi);

        // Build forecast for next 3 days
        List<ForecastResponse.DailyForecast> forecast = new ArrayList<>();
        for (int i = 0; i < weatherList.size(); i++) {
            WeatherData wd = weatherList.get(i);
            AqiData ad = i < aqiList.size() ? aqiList.get(i) : null;
            Score score = scoringService.calculate(wd, ad);

            forecast.add(new ForecastResponse.DailyForecast(
                    wd.date(),
                    score.value(),
                    score.recommendation(),
                    wd.temperatureMax(),
                    wd.temperatureMin(),
                    ad != null ? ad.usAqi() : null
            ));
        }

        return new ForecastResponse(
                location.getId(),
                location.getName(),
                todayScore.value(),
                todayScore.recommendation(),
                todayScore.reasons(),
                new ForecastResponse.WeatherInfo(
                        todayWeather.temperatureMax(),
                        todayWeather.temperatureMin(),
                        todayWeather.precipitation(),
                        todayWeather.windSpeed(),
                        todayWeather.uvIndex()
                ),
                new ForecastResponse.AqiInfo(
                        todayAqi != null ? todayAqi.usAqi() : null,
                        todayAqi != null ? todayAqi.pm25() : null,
                        todayAqi != null ? todayAqi.ozone() : null
                ),
                forecast
        );
    }

    public TrendsResponse getTrends(Long locationId, int days) {
        Location location = locationService.getLocationEntity(locationId);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // First check if we have cached data
        List<DailyMetrics> cachedMetrics = dailyMetricsRepository.findByLocationIdAndDateBetween(
                locationId, startDate, endDate
        );

        List<TrendsResponse.AqiTrend> aqiTrends = new ArrayList<>();
        List<TrendsResponse.TemperatureTrend> tempTrends = new ArrayList<>();
        List<TrendsResponse.ScoreTrend> scoreTrends = new ArrayList<>();

        if (!cachedMetrics.isEmpty()) {
            // Use cached data
            for (DailyMetrics dm : cachedMetrics) {
                aqiTrends.add(new TrendsResponse.AqiTrend(dm.getDate(), dm.getAqiValue()));
                tempTrends.add(new TrendsResponse.TemperatureTrend(
                        dm.getDate(), dm.getTemperatureMin(), dm.getTemperatureMax()
                ));
                scoreTrends.add(new TrendsResponse.ScoreTrend(
                        dm.getDate(), dm.getScore(), dm.getRecommendation()
                ));
            }
        } else {
            // Fetch from API - for forecast data only (Open-Meteo free tier)
            try {
                List<AqiData> aqiList = openMeteoClient.getAirQuality(
                        location.getLatitude(), location.getLongitude(), Math.min(days, 7)
                );
                List<WeatherData> weatherList = openMeteoClient.getWeatherForecast(
                        location.getLatitude(), location.getLongitude(), Math.min(days, 7)
                );

                for (int i = 0; i < weatherList.size(); i++) {
                    WeatherData wd = weatherList.get(i);
                    AqiData ad = i < aqiList.size() ? aqiList.get(i) : null;
                    Score score = scoringService.calculate(wd, ad);

                    aqiTrends.add(new TrendsResponse.AqiTrend(wd.date(), ad != null ? ad.usAqi() : null));
                    tempTrends.add(new TrendsResponse.TemperatureTrend(
                            wd.date(), wd.temperatureMin(), wd.temperatureMax()
                    ));
                    scoreTrends.add(new TrendsResponse.ScoreTrend(
                            wd.date(), score.value(), score.recommendation()
                    ));

                    // Cache the metrics
                    saveDailyMetrics(location, wd, ad, score);
                }
            } catch (Exception e) {
                log.warn("Unable to fetch trend data from API: {}", e.getMessage());
            }
        }

        return new TrendsResponse(
                location.getId(),
                location.getName(),
                days,
                aqiTrends,
                tempTrends,
                scoreTrends
        );
    }

    private void saveDailyMetrics(Location location, WeatherData weather, AqiData aqi, Score score) {
        DailyMetrics metrics = new DailyMetrics();
        metrics.setLocation(location);
        metrics.setDate(weather.date());
        metrics.setScore(score.value());
        metrics.setRecommendation(score.recommendation());
        metrics.setTemperatureMax(weather.temperatureMax());
        metrics.setTemperatureMin(weather.temperatureMin());
        metrics.setPrecipitation(weather.precipitation());
        metrics.setWindSpeed(weather.windSpeed());
        metrics.setUvIndex(weather.uvIndex());

        if (aqi != null) {
            metrics.setAqiValue(aqi.usAqi());
            metrics.setPm25(aqi.pm25());
            metrics.setOzone(aqi.ozone());
        }

        try {
            dailyMetricsRepository.save(metrics);
        } catch (Exception e) {
            log.debug("Metrics already exist for this date, skipping save");
        }
    }

    public static class ForecastUnavailableException extends RuntimeException {
        public ForecastUnavailableException(String message) {
            super(message);
        }
    }
}
