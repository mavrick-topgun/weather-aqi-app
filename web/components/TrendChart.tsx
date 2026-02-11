'use client';

import type { ScoreTrend, AqiTrend, TemperatureTrend } from '@/types';

interface TrendChartProps {
  scores?: ScoreTrend[];
  aqi?: AqiTrend[];
  temperature?: TemperatureTrend[];
  type: 'score' | 'aqi' | 'temperature';
  title: string;
}

export default function TrendChart({ scores, aqi, temperature, type, title }: TrendChartProps) {
  const data = type === 'score' ? scores : type === 'aqi' ? aqi : temperature;

  if (!data || data.length === 0) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-md">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">{title}</h3>
        <p className="text-gray-500 dark:text-gray-400">No trend data available</p>
      </div>
    );
  }

  const getValues = () => {
    if (type === 'score' && scores) {
      return scores.map((s) => s.score);
    }
    if (type === 'aqi' && aqi) {
      return aqi.map((a) => a.value ?? 0);
    }
    if (type === 'temperature' && temperature) {
      return temperature.map((t) => t.max ?? 0);
    }
    return [];
  };

  const values = getValues();
  const maxValue = Math.max(...values, 1);
  const minValue = Math.min(...values, 0);
  const range = maxValue - minValue || 1;

  const getBarColor = (value: number, index: number) => {
    if (type === 'score') {
      if (value >= 80) return 'bg-green-500';
      if (value >= 60) return 'bg-yellow-500';
      if (value >= 40) return 'bg-orange-500';
      return 'bg-red-500';
    }
    if (type === 'aqi') {
      if (value <= 50) return 'bg-green-500';
      if (value <= 100) return 'bg-yellow-500';
      if (value <= 150) return 'bg-orange-500';
      return 'bg-red-500';
    }
    return 'bg-blue-500';
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-md">
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">{title}</h3>

      <div className="flex items-end justify-between h-40 gap-1">
        {data.map((item, index) => {
          const value = type === 'score' && scores
            ? scores[index].score
            : type === 'aqi' && aqi
            ? (aqi[index].value ?? 0)
            : temperature
            ? (temperature[index].max ?? 0)
            : 0;

          const height = ((value - minValue) / range) * 100;

          return (
            <div
              key={index}
              className="flex-1 flex flex-col items-center group"
            >
              <div className="relative w-full flex justify-center mb-1">
                <div
                  className={`w-full max-w-[24px] rounded-t ${getBarColor(value, index)} transition-all group-hover:opacity-80`}
                  style={{ height: `${Math.max(height, 5)}%`, minHeight: '4px' }}
                />
                <div className="absolute -top-6 opacity-0 group-hover:opacity-100 transition-opacity bg-gray-900 text-white text-xs px-2 py-1 rounded">
                  {type === 'temperature' ? `${value}°` : value}
                </div>
              </div>
              <span className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-full">
                {formatDate('date' in item ? item.date : '')}
              </span>
            </div>
          );
        })}
      </div>

      <div className="mt-4 flex justify-between text-sm text-gray-500 dark:text-gray-400">
        <span>
          Avg:{' '}
          {type === 'temperature'
            ? `${Math.round(values.reduce((a, b) => a + b, 0) / values.length)}°`
            : Math.round(values.reduce((a, b) => a + b, 0) / values.length)}
        </span>
        <span>
          {type === 'score' ? 'Best' : 'Max'}:{' '}
          {type === 'temperature' ? `${Math.round(maxValue)}°` : Math.round(maxValue)}
        </span>
      </div>
    </div>
  );
}
