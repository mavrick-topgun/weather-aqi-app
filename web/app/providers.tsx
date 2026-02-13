'use client';

import { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark';
export type UnitSystem = 'metric' | 'imperial';

interface ThemeContextValue {
  theme: Theme;
  toggleTheme: () => void;
}

interface UnitContextValue {
  units: UnitSystem;
  toggleUnits: () => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);
const UnitContext = createContext<UnitContextValue | undefined>(undefined);

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}

export function useUnits() {
  const context = useContext(UnitContext);
  if (!context) {
    throw new Error('useUnits must be used within a UnitProvider');
  }
  return context;
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = useState<Theme>('light');
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const stored = localStorage.getItem('theme') as Theme | null;
    const initial =
      stored ?? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    setTheme(initial);
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!mounted) return;
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
    localStorage.setItem('theme', theme);
  }, [theme, mounted]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      <UnitProvider>{children}</UnitProvider>
    </ThemeContext.Provider>
  );
}

function UnitProvider({ children }: { children: React.ReactNode }) {
  const [units, setUnits] = useState<UnitSystem>('metric');
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const stored = localStorage.getItem('units') as UnitSystem | null;
    if (stored === 'imperial') {
      setUnits('imperial');
    }
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!mounted) return;
    localStorage.setItem('units', units);
  }, [units, mounted]);

  const toggleUnits = () => {
    setUnits((prev) => (prev === 'metric' ? 'imperial' : 'metric'));
  };

  return (
    <UnitContext.Provider value={{ units, toggleUnits }}>
      {children}
    </UnitContext.Provider>
  );
}
