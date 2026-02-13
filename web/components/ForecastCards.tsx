'use client';

import { useUnits } from '@/app/providers';
import type { DailyForecast, Recommendation } from '@/types';

function toF(c: number): number {
  return (c * 9) / 5 + 32;
}

interface ForecastCardsProps {
  forecast: DailyForecast[];
}

const recommendationColors: Record<Recommendation, string> = {
  Great: 'bg-green-500',
  Okay: 'bg-yellow-500',
  Caution: 'bg-orange-500',
  Avoid: 'bg-red-500',
};

export default function ForecastCards({ forecast }: ForecastCardsProps) {
  const { units } = useUnits();
  const imperial = units === 'imperial';

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    if (date.toDateString() === today.toDateString()) {
      return 'Today';
    }
    if (date.toDateString() === tomorrow.toDateString()) {
      return 'Tomorrow';
    }
    return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-2xl p-4 border border-gray-200 dark:border-gray-700 shadow-sm h-full">
      <h3 className="text-sm font-bold text-gray-900 dark:text-white mb-3">
        3-Day Forecast
      </h3>

      <div className="space-y-2">
        {forecast.map((day, index) => (
          <div
            key={day.date}
            className={`px-3 py-2 rounded-xl border ${
              index === 0
                ? 'bg-gray-50 dark:bg-gray-700 border-gray-200 dark:border-gray-600'
                : 'bg-gray-50/50 dark:bg-gray-700/50 border-gray-100 dark:border-gray-700'
            }`}
          >
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold text-gray-900 dark:text-white min-w-[60px]">
                {formatDate(day.date)}
              </span>
              <span
                className={`px-2 py-0.5 text-[11px] font-semibold text-white rounded-full ${recommendationColors[day.recommendation]}`}
              >
                {day.recommendation}
              </span>
              <div className="flex items-baseline gap-0.5">
                <span className="text-lg font-bold text-gray-900 dark:text-white">
                  {day.score}
                </span>
                <span className="text-[11px] text-gray-400">/100</span>
              </div>
              <div className="text-right text-xs">
                <span className="text-gray-600 dark:text-gray-300 font-semibold">
                  {day.temperatureMax !== null ? `${Math.round(imperial ? toF(day.temperatureMax) : day.temperatureMax)}°` : '--'}
                  <span className="text-gray-400 font-normal">/</span>
                  {day.temperatureMin !== null ? `${Math.round(imperial ? toF(day.temperatureMin) : day.temperatureMin)}°` : '--'}
                </span>
                <span className="text-gray-400 ml-1">AQI {day.aqi ?? '--'}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
