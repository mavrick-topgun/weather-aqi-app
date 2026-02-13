'use client';

import { useEffect, useState } from 'react';
import L from 'leaflet';
import { MapContainer, TileLayer, Marker, Popup, ZoomControl } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { useTheme } from '@/app/providers';
import { api } from '@/lib/api';
import type { Location } from '@/types';
import AqiLegend from './AqiLegend';
import LocationPopup from './LocationPopup';

// Fix Leaflet default icon issue with webpack bundlers
delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)._getIconUrl;

const greenMarkerSvg = encodeURIComponent(
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 36" width="24" height="36">
    <path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 24 12 24s12-15 12-24C24 5.4 18.6 0 12 0z" fill="#16a34a"/>
    <circle cx="12" cy="12" r="5" fill="white"/>
  </svg>`
);

const locationIcon = new L.Icon({
  iconUrl: `data:image/svg+xml,${greenMarkerSvg}`,
  iconSize: [24, 36],
  iconAnchor: [12, 36],
  popupAnchor: [0, -36],
});

const CARTO_LIGHT = 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png';
const CARTO_DARK = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';
const CARTO_ATTR =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/">CARTO</a>';

const AQICN_TOKEN = process.env.NEXT_PUBLIC_AQICN_TOKEN ?? '';
const AQICN_TILES = `https://tiles.aqicn.org/tiles/usepa-aqi/{z}/{x}/{y}.png?token=${AQICN_TOKEN}`;

const DEFAULT_CENTER: [number, number] = [20, 0];
const DEFAULT_ZOOM = 3;

export default function AqiMapInner() {
  const { theme } = useTheme();
  const [locations, setLocations] = useState<Location[]>([]);
  const [center, setCenter] = useState<[number, number]>(DEFAULT_CENTER);
  const [zoom, setZoom] = useState(DEFAULT_ZOOM);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    api
      .getLocations()
      .then((locs) => {
        setLocations(locs);
        if (locs.length > 0) {
          setCenter([locs[0].latitude, locs[0].longitude]);
          setZoom(6);
        }
      })
      .catch(() => {})
      .finally(() => setReady(true));
  }, []);

  if (!ready) return null;

  const tileUrl = theme === 'dark' ? CARTO_DARK : CARTO_LIGHT;

  return (
    <div className="relative h-[calc(100vh-73px)] w-full">
      <MapContainer
        center={center}
        zoom={zoom}
        zoomControl={false}
        className="h-full w-full"
      >
        <TileLayer key={theme} url={tileUrl} attribution={CARTO_ATTR} />
        {AQICN_TOKEN && AQICN_TOKEN !== 'your_token_here' && (
          <TileLayer url={AQICN_TILES} opacity={0.45} />
        )}
        <ZoomControl position="topright" />

        {locations.map((loc) => (
          <Marker
            key={loc.id}
            position={[loc.latitude, loc.longitude]}
            icon={locationIcon}
          >
            <Popup>
              <LocationPopup location={loc} />
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      <AqiLegend />
    </div>
  );
}
