/**
 * ThemeContext.jsx — Context quản lý giao diện sáng/tối (light/dark).
 * Lưu lựa chọn vào localStorage và gắn attribute data-theme lên <html>.
 * Hỗ trợ forceTheme để ép theme tạm thời (ví dụ trên một số trang cụ thể).
 */
import React, { createContext, useContext, useEffect, useState } from 'react';

const ThemeContext = createContext();

// Hook truy cập theme; ném lỗi nếu dùng ngoài ThemeProvider.
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

// Provider bọc cây component, cung cấp { theme, toggleTheme, setForceTheme }.
export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(() => {
    // Ưu tiên theme đã lưu trong localStorage, mặc định 'light'.
    const savedTheme = localStorage.getItem('codeforge-theme');
    if (savedTheme) {
      return savedTheme;
    }
    return 'light';
  });

  // forceTheme: theme bị ép tạm thời (null = dùng theme bình thường).
  const [forceTheme, setForceTheme] = useState(null);

  // Đồng bộ theme vào localStorage mỗi khi thay đổi.
  useEffect(() => {
    localStorage.setItem('codeforge-theme', theme);
  }, [theme]);

  // Áp theme (ưu tiên forceTheme) lên thuộc tính data-theme của <html>.
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', forceTheme || theme);
  }, [theme, forceTheme]);

  // Chuyển đổi qua lại giữa light và dark.
  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, setForceTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};
