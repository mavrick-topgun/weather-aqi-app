'use client';

import type { Recommendation } from '@/types';

interface ScoreCardProps {
  score: number;
  recommendation: Recommendation;
  reasons: string[];
  locationName: string;
}

const recommendationColors: Record<Recommendation, string> = {
  Great: 'bg-green-500',
  Okay: 'bg-yellow-500',
  Caution: 'bg-orange-500',
  Avoid: 'bg-red-500',
};

const recommendationBgColors: Record<Recommendation, string> = {
  Great: 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800',
  Okay: 'bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800',
  Caution: 'bg-orange-50 dark:bg-orange-900/20 border-orange-200 dark:border-orange-800',
  Avoid: 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800',
};

const recommendationTextColors: Record<Recommendation, string> = {
  Great: 'text-green-700 dark:text-green-400',
  Okay: 'text-yellow-700 dark:text-yellow-400',
  Caution: 'text-orange-700 dark:text-orange-400',
  Avoid: 'text-red-700 dark:text-red-400',
};

export default function ScoreCard({
  score,
  recommendation,
  reasons,
  locationName,
}: ScoreCardProps) {
  const ringColor = recommendationColors[recommendation];
  const bgColor = recommendationBgColors[recommendation];
  const textColor = recommendationTextColors[recommendation];

  return (
    <div className={`rounded-2xl p-6 border ${bgColor} shadow-sm`}>
      <div className="text-center mb-6">
        <h2 className="text-lg font-bold text-gray-600 dark:text-gray-300 mb-2">
          {locationName}
        </h2>
        <div className="relative inline-flex items-center justify-center">
          <svg className="w-40 h-40 transform -rotate-90">
            <circle
              cx="80"
              cy="80"
              r="70"
              stroke="currentColor"
              strokeWidth="8"
              fill="none"
              className="text-gray-200 dark:text-gray-700"
            />
            <circle
              cx="80"
              cy="80"
              r="70"
              stroke="currentColor"
              strokeWidth="8"
              fill="none"
              strokeDasharray={`${(score / 100) * 440} 440`}
              strokeLinecap="round"
              className={textColor}
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className={`text-5xl font-extrabold ${textColor}`}>{score}</span>
            <span className="text-sm font-semibold text-gray-500 dark:text-gray-400">out of 100</span>
          </div>
        </div>
      </div>

      <div className="text-center mb-6">
        <span
          className={`inline-block px-4 py-2 rounded-full text-white font-bold ${ringColor}`}
        >
          {recommendation}
        </span>
      </div>

      <div className="space-y-2">
        <h3 className="font-bold text-gray-700 dark:text-gray-300">Why this score?</h3>
        <ul className="space-y-1">
          {reasons.map((reason, index) => (
            <li
              key={index}
              className="flex items-start text-sm text-gray-600 dark:text-gray-400"
            >
              <span className="mr-2">•</span>
              {reason}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
