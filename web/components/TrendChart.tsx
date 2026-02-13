'use client';

import { useState, useRef, useCallback } from 'react';
import { useUnits } from '@/app/providers';
import type { AqiTrend, TemperatureTrend } from '@/types';

function toF(c: number): number {
  return (c * 9) / 5 + 32;
}

interface TrendChartProps {
  aqi?: AqiTrend[];
  temperature?: TemperatureTrend[];
  type: 'aqi' | 'temperature';
  title: string;
}

function getAqiColor(value: number): string {
  if (value <= 50) return '#22c55e';
  if (value <= 100) return '#eab308';
  if (value <= 150) return '#f97316';
  return '#ef4444';
}

export default function TrendChart({ aqi, temperature, type, title }: TrendChartProps) {
  const { units } = useUnits();
  const imperial = units === 'imperial';
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [chartWidth, setChartWidth] = useState(500);
  const chartHeight = 160;
  const roRef = useRef<ResizeObserver | null>(null);

  const containerRef = useCallback((node: HTMLDivElement | null) => {
    if (roRef.current) {
      roRef.current.disconnect();
      roRef.current = null;
    }
    if (!node) return;

    const ro = new ResizeObserver((entries) => {
      const width = entries[0].contentRect.width;
      if (width > 0) setChartWidth(Math.round(width));
    });

    ro.observe(node);
    setChartWidth(Math.round(node.clientWidth));
    roRef.current = ro;
  }, []);

  const convertTemp = (v: number) => (imperial ? toF(v) : v);
  const tempUnit = imperial ? '°F' : '°C';

  const data = type === 'aqi' ? aqi : temperature;

  if (!data || data.length === 0) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-200 dark:border-gray-700 shadow-sm h-full">
        <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">{title}</h3>
        <p className="text-gray-500 dark:text-gray-400">No trend data available</p>
      </div>
    );
  }

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr + 'T00:00:00');
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  // Line chart for AQI
  if (type === 'aqi' && aqi) {
    const values = aqi.map((a) => a.value ?? 0);
    const maxValue = Math.max(...values, 1);
    const minValue = Math.min(...values, 0);
    const range = maxValue - minValue || 1;

    const padding = { top: 20, right: 16, bottom: 4, left: 16 };
    const plotWidth = chartWidth - padding.left - padding.right;
    const plotHeight = chartHeight - padding.top - padding.bottom;

    // Add vertical breathing room
    const yMin = Math.max(0, minValue - range * 0.1);
    const yMax = maxValue + range * 0.1;
    const yRange = yMax - yMin || 1;

    const points = values.map((v, i) => ({
      x: padding.left + (i / (values.length - 1 || 1)) * plotWidth,
      y: padding.top + plotHeight - ((v - yMin) / yRange) * plotHeight,
      value: v,
      date: aqi[i].date,
    }));

    const linePath = points
      .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`)
      .join(' ');

    // Gradient fill area under the line
    const areaPath = `${linePath} L ${points[points.length - 1].x.toFixed(1)} ${(padding.top + plotHeight).toFixed(1)} L ${points[0].x.toFixed(1)} ${(padding.top + plotHeight).toFixed(1)} Z`;

    // Determine dominant AQI color for gradient
    const avgAqi = Math.round(values.reduce((a, b) => a + b, 0) / values.length);
    const gradientColor = getAqiColor(avgAqi);

    // Decide which date labels to show (first, last, and optionally middle)
    const labelIndices = new Set<number>();
    labelIndices.add(0);
    labelIndices.add(aqi.length - 1);
    if (aqi.length >= 5) {
      labelIndices.add(Math.floor(aqi.length / 2));
    }

    return (
      <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-200 dark:border-gray-700 shadow-sm h-full">
        <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">{title}</h3>

        <div ref={containerRef} className="relative">
          <svg
            viewBox={`0 0 ${chartWidth} ${chartHeight}`}
            className="w-full"
            style={{ height: `${chartHeight}px` }}
          >
            <defs>
              <linearGradient id="aqiGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={gradientColor} stopOpacity="0.3" />
                <stop offset="100%" stopColor={gradientColor} stopOpacity="0.02" />
              </linearGradient>
            </defs>

            {/* Area fill */}
            <path d={areaPath} fill="url(#aqiGradient)" />

            {/* Line */}
            <path
              d={linePath}
              fill="none"
              stroke={gradientColor}
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />

            {/* Data points and hover areas */}
            {points.map((p, i) => {
              const hitWidth = plotWidth / points.length;
              return (
                <g key={i}>
                  {/* Invisible wider hit area */}
                  <rect
                    x={p.x - hitWidth / 2}
                    y={padding.top}
                    width={hitWidth}
                    height={plotHeight}
                    fill="transparent"
                    onMouseEnter={() => setHoveredIndex(i)}
                    onMouseLeave={() => setHoveredIndex(null)}
                  />
                  {/* Dot */}
                  <circle
                    cx={p.x}
                    cy={p.y}
                    r={hoveredIndex === i ? 5 : 3.5}
                    fill={getAqiColor(p.value)}
                    stroke="white"
                    strokeWidth="2"
                    className="transition-all"
                  />
                </g>
              );
            })}
          </svg>

          {/* Tooltip */}
          {hoveredIndex !== null && points[hoveredIndex] && (
            <div
              className="absolute -top-2 bg-gray-900 text-white text-xs font-semibold px-2 py-1 rounded-lg pointer-events-none whitespace-nowrap z-10"
              style={{
                left: `${(points[hoveredIndex].x / chartWidth) * 100}%`,
                transform: 'translateX(-50%)',
              }}
            >
              AQI: {points[hoveredIndex].value} &middot; {formatDate(points[hoveredIndex].date)}
            </div>
          )}
        </div>

        {/* Date labels — show subset to avoid overlap */}
        <div className="flex justify-between mt-1">
          {aqi.map((item, i) => (
            <span
              key={i}
              className="text-xs font-semibold text-gray-500 dark:text-gray-400"
              style={{ visibility: labelIndices.has(i) ? 'visible' : 'hidden' }}
            >
              {formatDate(item.date)}
            </span>
          ))}
        </div>

        <div className="mt-3 flex justify-between text-sm font-semibold text-gray-500 dark:text-gray-400">
          <span>Avg: {avgAqi}</span>
          <span>Max: {Math.round(maxValue)}</span>
        </div>
      </div>
    );
  }

  // Bar chart for temperature
  const tempValues = temperature
    ? temperature.map((t) => convertTemp(t.max ?? 0))
    : [];
  const maxTempValue = Math.max(...tempValues, 1);
  const minTempValue = Math.min(...tempValues, 0);
  const tempRange = maxTempValue - minTempValue || 1;

  return (
    <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-200 dark:border-gray-700 shadow-sm h-full">
      <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">{title}</h3>

      <div className="flex items-end justify-between h-40 gap-1">
        {data.map((item, index) => {
          const value = temperature
            ? convertTemp(temperature[index].max ?? 0)
            : 0;

          const height = ((value - minTempValue) / tempRange) * 100;

          const dateStr = 'date' in item ? item.date : '';
          const label = `Temperature on ${formatDate(dateStr)}: ${Math.round(value)}${tempUnit}`;

          return (
            <div
              key={index}
              className="flex-1 flex flex-col items-center group"
              role="img"
              aria-label={label}
            >
              <div className="relative w-full flex justify-center mb-1">
                <div
                  className="w-full max-w-[24px] rounded-t-lg bg-green-500 transition-all group-hover:opacity-80"
                  style={{ height: `${Math.max(height, 5)}%`, minHeight: '4px' }}
                />
                <div className="absolute -top-6 opacity-0 group-hover:opacity-100 transition-opacity bg-gray-900 text-white text-xs font-semibold px-2 py-1 rounded-lg">
                  {Math.round(value)}{tempUnit}
                </div>
              </div>
              <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 truncate max-w-full">
                {formatDate('date' in item ? item.date : '')}
              </span>
            </div>
          );
        })}
      </div>

      <div className="mt-4 flex justify-between text-sm font-semibold text-gray-500 dark:text-gray-400">
        <span>
          Avg: {Math.round(tempValues.reduce((a, b) => a + b, 0) / tempValues.length)}{tempUnit}
        </span>
        <span>
          Max: {Math.round(maxTempValue)}{tempUnit}
        </span>
      </div>
    </div>
  );
}
