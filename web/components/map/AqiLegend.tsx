'use client';

import { useState } from 'react';

const AQI_LEVELS = [
  { label: 'Good', range: '0–50', color: '#00e400' },
  { label: 'Moderate', range: '51–100', color: '#ffff00' },
  { label: 'Unhealthy for Sensitive', range: '101–150', color: '#ff7e00' },
  { label: 'Unhealthy', range: '151–200', color: '#ff0000' },
  { label: 'Very Unhealthy', range: '201–300', color: '#8f3f97' },
  { label: 'Hazardous', range: '301+', color: '#7e0023' },
];

export default function AqiLegend() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="absolute bottom-4 left-4 z-[1000]">
      <div className="bg-white/95 dark:bg-gray-800/95 backdrop-blur-sm rounded-2xl border border-gray-200 dark:border-gray-700 shadow-lg overflow-hidden">
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="w-full px-4 py-2.5 flex items-center justify-between text-sm font-semibold text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
        >
          <span>AQI Legend</span>
          <svg
            className={`w-4 h-4 transition-transform ${collapsed ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        {!collapsed && (
          <div className="px-4 pb-3 space-y-1.5">
            {AQI_LEVELS.map((level) => (
              <div key={level.label} className="flex items-center gap-2">
                <span
                  className="w-4 h-4 rounded-full flex-shrink-0"
                  style={{ backgroundColor: level.color }}
                />
                <span className="text-xs text-gray-600 dark:text-gray-300">
                  {level.range}
                </span>
                <span className="text-xs font-medium text-gray-800 dark:text-gray-100">
                  {level.label}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
