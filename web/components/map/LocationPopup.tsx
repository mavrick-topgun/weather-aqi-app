'use client';

import { useEffect, useState } from 'react';
import type { Location, ForecastResponse } from '@/types';
import { api } from '@/lib/api';

interface LocationPopupProps {
  location: Location;
}

export default function LocationPopup({ location }: LocationPopupProps) {
  const [forecast, setForecast] = useState<ForecastResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    api
      .getForecast(location.id)
      .then((data) => {
        if (!cancelled) setForecast(data);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [location.id]);

  const badgeColor = forecast
    ? {
        Great: 'bg-green-100 text-green-700',
        Okay: 'bg-yellow-100 text-yellow-700',
        Caution: 'bg-orange-100 text-orange-700',
        Avoid: 'bg-red-100 text-red-700',
      }[forecast.recommendation] ?? 'bg-gray-100 text-gray-700'
    : '';

  return (
    <div className="min-w-[180px]">
      <h3 className="font-bold text-sm text-gray-900 dark:text-gray-100 mb-1">
        {location.name}
      </h3>
      <p className="text-xs text-gray-500 dark:text-gray-400 mb-2">
        {location.latitude.toFixed(2)}°, {location.longitude.toFixed(2)}°
      </p>

      {loading ? (
        <p className="text-xs text-gray-400">Loading forecast…</p>
      ) : error ? (
        <p className="text-xs text-red-500">Failed to load forecast</p>
      ) : forecast ? (
        <div className="flex items-center gap-2 mb-2">
          <span className="text-xs text-gray-600 dark:text-gray-300">
            AQI: <strong>{forecast.aqi.value ?? '--'}</strong>
          </span>
          <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${badgeColor}`}>
            {forecast.recommendation}
          </span>
        </div>
      ) : null}

      <a
        href={`/?location=${location.id}`}
        className="text-xs font-semibold text-green-600 dark:text-green-400 hover:underline"
      >
        View Forecast →
      </a>
    </div>
  );
}
