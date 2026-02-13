import React from 'react';
import { render, screen } from '@testing-library/react';
import TrendChart from '../TrendChart';
import type { AqiTrend } from '@/types';

// Mock useUnits to avoid needing the full provider tree
jest.mock('@/app/providers', () => ({
  useUnits: () => ({ units: 'metric' as const, toggleUnits: jest.fn() }),
}));

// Mock ResizeObserver (not available in jsdom)
const mockObserve = jest.fn();
const mockDisconnect = jest.fn();
beforeAll(() => {
  global.ResizeObserver = class {
    observe = mockObserve;
    unobserve = jest.fn();
    disconnect = mockDisconnect;
    constructor(cb: ResizeObserverCallback) {
      // Fire immediately with a mock width
      setTimeout(() =>
        cb(
          [{ contentRect: { width: 400 } } as ResizeObserverEntry],
          this as unknown as ResizeObserver,
        ),
        0,
      );
    }
  } as unknown as typeof ResizeObserver;
});

describe('TrendChart', () => {
  describe('empty state', () => {
    it('renders "No trend data available" when aqi array is empty', () => {
      render(<TrendChart aqi={[]} type="aqi" title="AQI Trend (7 days)" />);
      expect(screen.getByText('No trend data available')).toBeInTheDocument();
    });

    it('renders "No trend data available" when aqi is undefined', () => {
      render(<TrendChart type="aqi" title="AQI Trend (7 days)" />);
      expect(screen.getByText('No trend data available')).toBeInTheDocument();
    });
  });

  describe('AQI line chart rendering', () => {
    const sevenDayAqi: AqiTrend[] = [
      { date: '2026-02-12', value: 45 },
      { date: '2026-02-13', value: 50 },
      { date: '2026-02-14', value: 55 },
      { date: '2026-02-15', value: 48 },
      { date: '2026-02-16', value: 52 },
      { date: '2026-02-17', value: 60 },
      { date: '2026-02-18', value: 42 },
    ];

    it('renders the chart title', () => {
      render(<TrendChart aqi={sevenDayAqi} type="aqi" title="AQI Trend (7 days)" />);
      expect(screen.getByText('AQI Trend (7 days)')).toBeInTheDocument();
    });

    it('renders an SVG element for the chart', () => {
      const { container } = render(
        <TrendChart aqi={sevenDayAqi} type="aqi" title="AQI Trend (7 days)" />,
      );
      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('renders 7 data point circles', () => {
      const { container } = render(
        <TrendChart aqi={sevenDayAqi} type="aqi" title="AQI Trend (7 days)" />,
      );
      const circles = container.querySelectorAll('circle');
      expect(circles).toHaveLength(7);
    });

    it('renders a line path connecting data points', () => {
      const { container } = render(
        <TrendChart aqi={sevenDayAqi} type="aqi" title="AQI Trend (7 days)" />,
      );
      const paths = container.querySelectorAll('path');
      // Should have 2 paths: area fill + line
      expect(paths.length).toBeGreaterThanOrEqual(2);

      const linePath = Array.from(paths).find(
        (p) => p.getAttribute('fill') === 'none',
      );
      expect(linePath).toBeTruthy();
      // Line path should start with M and contain L segments
      const d = linePath?.getAttribute('d') || '';
      expect(d).toMatch(/^M /);
      expect(d).toContain(' L ');
    });

    it('displays avg and max values', () => {
      render(<TrendChart aqi={sevenDayAqi} type="aqi" title="AQI Trend (7 days)" />);
      expect(screen.getByText('Avg: 50')).toBeInTheDocument();
      expect(screen.getByText('Max: 60')).toBeInTheDocument();
    });

    it('shows only first, middle, and last date labels for 7 items', () => {
      const { container } = render(
        <TrendChart aqi={sevenDayAqi} type="aqi" title="AQI Trend (7 days)" />,
      );
      // Date labels are spans inside the flex container below the SVG
      const dateContainer = container.querySelector('.flex.justify-between.mt-1');
      expect(dateContainer).toBeTruthy();

      const labels = dateContainer!.querySelectorAll('span');
      expect(labels).toHaveLength(7);

      // First (index 0), middle (index 3), and last (index 6) should be visible
      expect(labels[0]).toHaveStyle('visibility: visible');
      expect(labels[3]).toHaveStyle('visibility: visible');
      expect(labels[6]).toHaveStyle('visibility: visible');

      // Others should be hidden
      expect(labels[1]).toHaveStyle('visibility: hidden');
      expect(labels[2]).toHaveStyle('visibility: hidden');
      expect(labels[4]).toHaveStyle('visibility: hidden');
      expect(labels[5]).toHaveStyle('visibility: hidden');
    });
  });

  describe('null value handling', () => {
    it('maps null AQI values to 0 and still renders all data points', () => {
      const aqiWithNulls: AqiTrend[] = [
        { date: '2026-02-12', value: 45 },
        { date: '2026-02-13', value: null },
        { date: '2026-02-14', value: null },
        { date: '2026-02-15', value: null },
        { date: '2026-02-16', value: null },
        { date: '2026-02-17', value: null },
        { date: '2026-02-18', value: 39 },
      ];

      const { container } = render(
        <TrendChart aqi={aqiWithNulls} type="aqi" title="AQI Trend (7 days)" />,
      );

      // Should render all 7 circles, not just the 2 with non-null values
      const circles = container.querySelectorAll('circle');
      expect(circles).toHaveLength(7);

      // Line path should contain L segments (connecting multiple points)
      const paths = container.querySelectorAll('path');
      const linePath = Array.from(paths).find(
        (p) => p.getAttribute('fill') === 'none',
      );
      const d = linePath?.getAttribute('d') || '';
      expect(d).toContain(' L ');
    });

    it('renders a chart even when all values are null', () => {
      const allNulls: AqiTrend[] = [
        { date: '2026-02-12', value: null },
        { date: '2026-02-13', value: null },
        { date: '2026-02-14', value: null },
      ];

      const { container } = render(
        <TrendChart aqi={allNulls} type="aqi" title="AQI Trend (7 days)" />,
      );

      // Should still render 3 circles (mapped to 0)
      const circles = container.querySelectorAll('circle');
      expect(circles).toHaveLength(3);
    });
  });

  describe('SVG scaling', () => {
    it('does not use preserveAspectRatio="none"', () => {
      const aqi: AqiTrend[] = [
        { date: '2026-02-12', value: 45 },
        { date: '2026-02-13', value: 50 },
      ];

      const { container } = render(
        <TrendChart aqi={aqi} type="aqi" title="AQI Trend" />,
      );

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
      expect(svg?.getAttribute('preserveAspectRatio')).not.toBe('none');
    });
  });

  describe('single data point', () => {
    it('renders a single dot without crashing', () => {
      const singlePoint: AqiTrend[] = [{ date: '2026-02-12', value: 39 }];

      const { container } = render(
        <TrendChart aqi={singlePoint} type="aqi" title="AQI Trend (7 days)" />,
      );

      const circles = container.querySelectorAll('circle');
      expect(circles).toHaveLength(1);
      expect(screen.getByText('Avg: 39')).toBeInTheDocument();
      expect(screen.getByText('Max: 39')).toBeInTheDocument();
    });
  });
});
