'use client';

import type { DailyForecast, Recommendation } from '@/types';

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
    <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-200 dark:border-gray-700 shadow-sm">
      <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">
        3-Day Forecast
      </h3>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {forecast.map((day, index) => (
          <div
            key={day.date}
            className={`p-4 rounded-xl border ${
              index === 0
                ? 'bg-gray-50 dark:bg-gray-700 border-gray-200 dark:border-gray-600'
                : 'bg-gray-50/50 dark:bg-gray-700/50 border-gray-100 dark:border-gray-700'
            }`}
          >
            <div className="flex justify-between items-center mb-2">
              <span className="font-bold text-gray-900 dark:text-white">
                {formatDate(day.date)}
              </span>
              <span
                className={`px-2 py-1 text-xs font-semibold text-white rounded-full ${recommendationColors[day.recommendation]}`}
              >
                {day.recommendation}
              </span>
            </div>

            <div className="flex items-center justify-between">
              <div>
                <span className="text-3xl font-bold text-gray-900 dark:text-white">
                  {day.score}
                </span>
                <span className="text-sm text-gray-500 dark:text-gray-400">/100</span>
              </div>

              <div className="text-right text-sm">
                <div className="text-gray-600 dark:text-gray-300 font-semibold">
                  {day.temperatureMax !== null ? `${Math.round(day.temperatureMax)}°` : '--'}
                  <span className="text-gray-400 font-normal"> / </span>
                  {day.temperatureMin !== null ? `${Math.round(day.temperatureMin)}°` : '--'}
                </div>
                <div className="text-gray-500 dark:text-gray-400">
                  AQI: {day.aqi ?? '--'}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
