'use client';

import { useUnits } from '@/app/providers';
import type { WeatherInfo } from '@/types';

interface WeatherDetailsProps {
  weather: WeatherInfo;
}

function toF(c: number): number {
  return (c * 9) / 5 + 32;
}

function toMph(kmh: number): number {
  return kmh * 0.621371;
}

export default function WeatherDetails({ weather }: WeatherDetailsProps) {
  const { units } = useUnits();
  const imperial = units === 'imperial';
  return (
    <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-200 dark:border-gray-700 shadow-sm h-full">
      <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">
        Weather
      </h3>
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col">
          <span className="text-sm font-semibold text-gray-500 dark:text-gray-400">Temperature</span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {weather.temperatureMax !== null
              ? `${Math.round(imperial ? toF(weather.temperatureMax) : weather.temperatureMax)}°${imperial ? 'F' : 'C'}`
              : '--'}
          </span>
          <span className="text-sm text-gray-500 dark:text-gray-400">
            Low: {weather.temperatureMin !== null ? `${Math.round(imperial ? toF(weather.temperatureMin) : weather.temperatureMin)}°${imperial ? 'F' : 'C'}` : '--'}
          </span>
        </div>

        <div className="flex flex-col">
          <span className="text-sm font-semibold text-gray-500 dark:text-gray-400">Precipitation</span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {weather.precipitation !== null
              ? `${weather.precipitation.toFixed(1)} mm`
              : '--'}
          </span>
        </div>

        <div className="flex flex-col">
          <span className="text-sm font-semibold text-gray-500 dark:text-gray-400">Wind</span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {weather.windSpeed !== null
              ? `${Math.round(imperial ? toMph(weather.windSpeed) : weather.windSpeed)} ${imperial ? 'mph' : 'km/h'}`
              : '--'}
          </span>
          {weather.windDirection !== null && (
            <span className="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400">
              <svg
                width="14"
                height="14"
                viewBox="0 0 14 14"
                className="text-gray-600 dark:text-gray-300"
                style={{ transform: `rotate(${weather.windDirection}deg)` }}
              >
                <path
                  d="M7 1L3 12L7 9L11 12L7 1Z"
                  fill="currentColor"
                />
              </svg>
              {getWindDirectionLabel(weather.windDirection)}
            </span>
          )}
        </div>

        <div className="flex flex-col">
          <span className="text-sm font-semibold text-gray-500 dark:text-gray-400">UV Index</span>
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

function getWindDirectionLabel(degrees: number): string {
  const directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
  const index = Math.round(degrees / 45) % 8;
  return directions[index];
}

function getUvLabel(uv: number | null): string {
  if (uv === null) return '';
  if (uv < 3) return 'Low';
  if (uv < 6) return 'Moderate';
  if (uv < 8) return 'High';
  if (uv < 11) return 'Very High';
  return 'Extreme';
}
