/**
 * App.jsx — Component gốc của ứng dụng.
 * Bọc toàn bộ cây component trong ThemeProvider (quản lý theme) và render hệ thống route.
 */
import React from 'react';
import AppRoutes from './routes/AppRoutes';
import { ThemeProvider } from './context/ThemeContext';

function App() {
  return (
    <ThemeProvider>
      <AppRoutes />
    </ThemeProvider>
  );
}

export default App;
