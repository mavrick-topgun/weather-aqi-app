import type { Metadata } from 'next';
import { Nunito_Sans } from 'next/font/google';
import './globals.css';
import { ThemeProvider } from './providers';
import ThemeToggle from '@/components/ThemeToggle';

const nunitoSans = Nunito_Sans({ subsets: ['latin'], weight: ['400', '600', '700', '800'] });

export const metadata: Metadata = {
  title: 'Breathe & Go',
  description: 'Weather + AQI Daily Decision App',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function() {
                try {
                  var theme = localStorage.getItem('theme');
                  if (theme === 'dark' || (!theme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
                    document.documentElement.classList.add('dark');
                  }
                } catch (e) {}
              })();
            `,
          }}
        />
      </head>
      <body className={nunitoSans.className}>
        <ThemeProvider>
          <div className="min-h-screen">
            <header className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
              <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
                <div className="flex justify-between items-center">
                  <a href="/" className="text-2xl font-extrabold text-green-700 dark:text-green-400">
                    Breathe & Go
                  </a>
                  <nav className="flex items-center space-x-1">
                    <a
                      href="/"
                      className="px-4 py-2 rounded-full text-sm font-semibold text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                    >
                      Dashboard
                    </a>
                    <a
                      href="/locations"
                      className="px-4 py-2 rounded-full text-sm font-semibold text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                    >
                      Locations
                    </a>
                    <ThemeToggle />
                  </nav>
                </div>
              </div>
            </header>
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
              {children}
            </main>
          </div>
        </ThemeProvider>
      </body>
    </html>
  );
}
