'use client';

import type { WeatherInfo } from '@/types';

interface WeatherDetailsProps {
  weather: WeatherInfo;
}

export default function WeatherDetails({ weather }: WeatherDetailsProps) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-md">
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        Weather
      </h3>
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col">
          <span className="text-sm text-gray-500 dark:text-gray-400">Temperature</span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {weather.temperatureMax !== null
              ? `${Math.round(weather.temperatureMax)}°`
              : '--'}
          </span>
          <span className="text-sm text-gray-500 dark:text-gray-400">
            Low: {weather.temperatureMin !== null ? `${Math.round(weather.temperatureMin)}°` : '--'}
          </span>
        </div>

        <div className="flex flex-col">
          <span className="text-sm text-gray-500 dark:text-gray-400">Precipitation</span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {weather.precipitation !== null
              ? `${weather.precipitation.toFixed(1)} mm`
              : '--'}
          </span>
        </div>

        <div className="flex flex-col">
          <span className="text-sm text-gray-500 dark:text-gray-400">Wind</span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {weather.windSpeed !== null
              ? `${Math.round(weather.windSpeed)} km/h`
              : '--'}
          </span>
        </div>

        <div className="flex flex-col">
          <span className="text-sm text-gray-500 dark:text-gray-400">UV Index</span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {weather.uvIndex !== null ? weather.uvIndex.toFixed(1) : '--'}
          </span>
          <span className="text-sm text-gray-500 dark:text-gray-400">
            {getUvLabel(weather.uvIndex)}
          </span>
        </div>
      </div>
    </div>
  );
}

function getUvLabel(uv: number | null): string {
  if (uv === null) return '';
  if (uv < 3) return 'Low';
  if (uv < 6) return 'Moderate';
  if (uv < 8) return 'High';
  if (uv < 11) return 'Very High';
  return 'Extreme';
}
