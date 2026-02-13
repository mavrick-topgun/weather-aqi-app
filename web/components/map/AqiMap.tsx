'use client';

import dynamic from 'next/dynamic';

const AqiMapInner = dynamic(() => import('./AqiMapInner'), {
  ssr: false,
  loading: () => (
    <div className="h-[calc(100vh-73px)] w-full flex items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="flex flex-col items-center gap-3">
        <div className="w-8 h-8 border-3 border-green-500 border-t-transparent rounded-full animate-spin" />
        <span className="text-sm text-gray-500 dark:text-gray-400">Loading map…</span>
      </div>
    </div>
  ),
});

export default function AqiMap() {
  return <AqiMapInner />;
}
