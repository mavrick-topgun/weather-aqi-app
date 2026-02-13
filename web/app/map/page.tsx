import type { Metadata } from 'next';
import AqiMap from '@/components/map/AqiMap';

export const metadata: Metadata = {
  title: 'AQI Map - Breathe & Go',
  description: 'Interactive global air quality index map',
};

export default function MapPage() {
  return (
    <div className="-mx-4 sm:-mx-6 lg:-mx-8 -mt-8 -mb-8">
      <AqiMap />
    </div>
  );
}
