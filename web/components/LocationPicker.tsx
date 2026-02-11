'use client';

import { useState } from 'react';
import type { Location, GeocodingResult } from '@/types';
import LocationSearch from './LocationSearch';

interface LocationPickerProps {
  locations: Location[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  onAdd: (result: GeocodingResult) => Promise<void>;
  onDelete: (id: number) => Promise<void>;
}

export default function LocationPicker({
  locations,
  selectedId,
  onSelect,
  onAdd,
  onDelete,
}: LocationPickerProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAddLocation = async (result: GeocodingResult) => {
    setLoading(true);
    setError(null);
    try {
      await onAdd(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add location');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-md">
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        Locations
      </h3>

      {/* Search box */}
      <div className="mb-4">
        <LocationSearch
          onSelect={handleAddLocation}
          placeholder="Search for a city..."
        />
        {loading && (
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">Adding location...</p>
        )}
        {error && (
          <p className="text-sm text-red-500 mt-2">{error}</p>
        )}
      </div>

      {/* Location list */}
      <div className="space-y-2">
        {locations.length === 0 ? (
          <p className="text-gray-500 dark:text-gray-400 text-sm py-4 text-center">
            No locations saved. Search for a city above to add one.
          </p>
        ) : (
          locations.map((location) => (
            <div
              key={location.id}
              className={`flex items-center justify-between p-3 rounded-lg cursor-pointer transition-colors ${
                selectedId === location.id
                  ? 'bg-blue-100 dark:bg-blue-900/30 border-2 border-blue-500'
                  : 'hover:bg-gray-100 dark:hover:bg-gray-700 border-2 border-transparent'
              }`}
              onClick={() => onSelect(location.id)}
            >
              <div className="flex items-center space-x-3">
                <div className={`w-2 h-2 rounded-full ${
                  selectedId === location.id ? 'bg-blue-500' : 'bg-gray-300 dark:bg-gray-600'
                }`} />
                <div>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {location.name}
                  </span>
                </div>
              </div>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onDelete(location.id);
                }}
                className="text-gray-400 hover:text-red-500 p-1 rounded transition-colors"
                title="Delete location"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
