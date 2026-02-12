'use client';

import { useUnits } from '@/app/providers';

export default function UnitToggle() {
  const { units, toggleUnits } = useUnits();

  return (
    <button
      onClick={toggleUnits}
      aria-label={units === 'metric' ? 'Switch to imperial units' : 'Switch to metric units'}
      className="flex items-center rounded-full border border-gray-200 dark:border-gray-600 text-sm font-semibold overflow-hidden"
    >
      <span
        className={`px-2 py-1 transition-colors ${
          units === 'metric'
            ? 'bg-green-600 text-white'
            : 'text-gray-500 dark:text-gray-400'
        }`}
      >
        °C
      </span>
      <span
        className={`px-2 py-1 transition-colors ${
          units === 'imperial'
            ? 'bg-green-600 text-white'
            : 'text-gray-500 dark:text-gray-400'
        }`}
      >
        °F
      </span>
    </button>
  );
}
