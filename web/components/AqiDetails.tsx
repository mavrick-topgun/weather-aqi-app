'use client';

import type { AqiInfo } from '@/types';

interface AqiDetailsProps {
  aqi: AqiInfo;
}

export default function AqiDetails({ aqi }: AqiDetailsProps) {
  const aqiCategory = getAqiCategory(aqi.value);

  return (
    <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-200 dark:border-gray-700 shadow-sm">
      <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">
        Air Quality
      </h3>

      <div className="flex items-center justify-between mb-4">
        <div>
          <span className="text-4xl font-bold text-gray-900 dark:text-white">
            {aqi.value ?? '--'}
          </span>
          <span className="ml-2 text-sm font-semibold text-gray-500 dark:text-gray-400">US AQI</span>
        </div>
        <span
          className={`px-3 py-1 rounded-full text-sm font-semibold ${aqiCategory.bgColor} ${aqiCategory.textColor}`}
        >
          {aqiCategory.label}
        </span>
      </div>

      <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2 mb-4">
        <div
          className={`h-2 rounded-full ${aqiCategory.barColor}`}
          style={{ width: `${Math.min((aqi.value ?? 0) / 3, 100)}%` }}
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col">
          <span className="text-sm font-semibold text-gray-500 dark:text-gray-400">PM2.5</span>
          <span className="text-lg font-bold text-gray-900 dark:text-white">
            {aqi.pm25 !== null ? `${aqi.pm25.toFixed(1)} μg/m³` : '--'}
          </span>
        </div>
        <div className="flex flex-col">
          <span className="text-sm font-semibold text-gray-500 dark:text-gray-400">Ozone</span>
          <span className="text-lg font-bold text-gray-900 dark:text-white">
            {aqi.ozone !== null ? `${aqi.ozone.toFixed(1)} μg/m³` : '--'}
          </span>
        </div>
      </div>
    </div>
  );
}

interface AqiCategory {
  label: string;
  bgColor: string;
  textColor: string;
  barColor: string;
}

function getAqiCategory(value: number | null): AqiCategory {
  if (value === null) {
    return {
      label: 'Unknown',
      bgColor: 'bg-gray-100 dark:bg-gray-700',
      textColor: 'text-gray-600 dark:text-gray-300',
      barColor: 'bg-gray-400',
    };
  }

  if (value <= 50) {
    return {
      label: 'Good',
      bgColor: 'bg-green-100 dark:bg-green-900/30',
      textColor: 'text-green-700 dark:text-green-400',
      barColor: 'bg-green-500',
    };
  }

  if (value <= 100) {
    return {
      label: 'Moderate',
      bgColor: 'bg-yellow-100 dark:bg-yellow-900/30',
      textColor: 'text-yellow-700 dark:text-yellow-400',
      barColor: 'bg-yellow-500',
    };
  }

  if (value <= 150) {
    return {
      label: 'Unhealthy for Sensitive',
      bgColor: 'bg-orange-100 dark:bg-orange-900/30',
      textColor: 'text-orange-700 dark:text-orange-400',
      barColor: 'bg-orange-500',
    };
  }

  if (value <= 200) {
    return {
      label: 'Unhealthy',
      bgColor: 'bg-red-100 dark:bg-red-900/30',
      textColor: 'text-red-700 dark:text-red-400',
      barColor: 'bg-red-500',
    };
  }

  return {
    label: 'Very Unhealthy',
    bgColor: 'bg-purple-100 dark:bg-purple-900/30',
    textColor: 'text-purple-700 dark:text-purple-400',
    barColor: 'bg-purple-500',
  };
}
