import React, { createContext, useContext, useEffect, useState } from 'react';

const ThemeContext = createContext();

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(() => {
    // Check localStorage first
    const savedTheme = localStorage.getItem('codeforge-theme');
    if (savedTheme) {
      return savedTheme;
    }
    // Default to light theme
    return 'light';
  });

  const [forceTheme, setForceTheme] = useState(null);

  useEffect(() => {
    // Save to localStorage
    localStorage.setItem('codeforge-theme', theme);
  }, [theme]);

  useEffect(() => {
    // Apply forceTheme if present, otherwise use normal theme
    document.documentElement.setAttribute('data-theme', forceTheme || theme);
  }, [theme, forceTheme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, setForceTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};
