'use client';

import { useState, useEffect, useCallback } from 'react';
import { api } from '@/lib/api';
import type { Location, ForecastResponse, TrendsResponse, GeocodingResult } from '@/types';
import ScoreCard from '@/components/ScoreCard';
import WeatherDetails from '@/components/WeatherDetails';
import AqiDetails from '@/components/AqiDetails';
import ForecastCards from '@/components/ForecastCards';
import TrendChart from '@/components/TrendChart';
import LocationPicker from '@/components/LocationPicker';

export default function Dashboard() {
  const [locations, setLocations] = useState<Location[]>([]);
  const [selectedLocationId, setSelectedLocationId] = useState<number | null>(null);
  const [forecast, setForecast] = useState<ForecastResponse | null>(null);
  const [trends, setTrends] = useState<TrendsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadLocations = useCallback(async () => {
    try {
      const data = await api.getLocations();
      setLocations(data);
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load locations');
      return [];
    }
  }, []);

  const loadForecast = useCallback(async (locationId: number) => {
    setLoading(true);
    setError(null);
    try {
      const [forecastData, trendsData] = await Promise.all([
        api.getForecast(locationId),
        api.getTrends(locationId, 7),
      ]);
      setForecast(forecastData);
      setTrends(trendsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load forecast');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadLocations().then((data) => {
      if (data.length > 0) {
        setSelectedLocationId(data[0].id);
      } else {
        setLoading(false);
      }
    });
  }, [loadLocations]);

  useEffect(() => {
    if (selectedLocationId) {
      loadForecast(selectedLocationId);
    }
  }, [selectedLocationId, loadForecast]);

  const handleAddLocation = async (result: GeocodingResult) => {
    // Convert GeocodingResult to LocationRequest
    const newLocation = await api.createLocation({
      name: result.admin1
        ? `${result.name}, ${result.admin1}`
        : result.name,
      latitude: result.latitude,
      longitude: result.longitude,
      timezone: result.timezone || undefined,
    });
    setLocations((prev) => [...prev, newLocation]);
    setSelectedLocationId(newLocation.id);
  };

  const handleDeleteLocation = async (id: number) => {
    await api.deleteLocation(id);
    setLocations((prev) => prev.filter((l) => l.id !== id));
    if (selectedLocationId === id) {
      const remaining = locations.filter((l) => l.id !== id);
      setSelectedLocationId(remaining.length > 0 ? remaining[0].id : null);
      if (remaining.length === 0) {
        setForecast(null);
        setTrends(null);
      }
    }
  };

  if (loading && locations.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        {/* Sidebar with locations */}
        <div className="lg:col-span-1">
          <LocationPicker
            locations={locations}
            selectedId={selectedLocationId}
            onSelect={setSelectedLocationId}
            onAdd={handleAddLocation}
            onDelete={handleDeleteLocation}
          />
        </div>

        {/* Main content */}
        <div className="lg:col-span-3 space-y-4">
          {error && (
            <div role="alert" className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 text-red-700 dark:text-red-400">
              {error}
            </div>
          )}

          {!selectedLocationId && !loading && (
            <div className="bg-white dark:bg-gray-800 rounded-2xl p-12 text-center border border-gray-200 dark:border-gray-700 shadow-sm">
              <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-2">
                Welcome to Breathe & Go
              </h2>
              <p className="text-gray-500 dark:text-gray-400">
                Add a location to get started with weather and air quality monitoring.
              </p>
            </div>
          )}

          {loading && selectedLocationId && (
            <div className="flex items-center justify-center min-h-[300px]">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500" />
            </div>
          )}

          {forecast && !loading && (
            <>
              {/* Score and details row */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="md:col-span-1">
                  <ScoreCard
                    score={forecast.score}
                    recommendation={forecast.recommendation}
                    reasons={forecast.reasons}
                    locationName={forecast.locationName}
                  />
                </div>
                <div className="md:col-span-1">
                  <WeatherDetails weather={forecast.weather} />
                </div>
                <div className="md:col-span-1">
                  <AqiDetails aqi={forecast.aqi} />
                </div>
              </div>

              {/* Forecast + Trends side by side */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <ForecastCards forecast={forecast.forecast} />
                {trends && (
                  <TrendChart
                    aqi={trends.aqi}
                    type="aqi"
                    title="AQI Trend (7 days)"
                  />
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
