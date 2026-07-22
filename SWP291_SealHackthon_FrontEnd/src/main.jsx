/**
 * main.jsx — Điểm khởi động (entry point) của ứng dụng React.
 * Mount <App /> vào thẻ #root và nạp CSS toàn cục. StrictMode giúp phát hiện lỗi khi dev.
 */
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import './styles/global.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
