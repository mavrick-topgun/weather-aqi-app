import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        score: {
          great: '#22c55e',
          okay: '#eab308',
          caution: '#f97316',
          avoid: '#ef4444',
        },
      },
    },
  },
  plugins: [],
}
export default config
