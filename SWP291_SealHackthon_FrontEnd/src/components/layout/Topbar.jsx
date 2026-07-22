/**
 * Topbar.jsx — Thanh trên của dashboard: breadcrumb tên trang, ô tìm kiếm, nút đổi theme.
 * Tên trang được suy ra từ URL hiện tại.
 * @param {string} role - Vai trò hiện tại (để format tên role).
 */
import React from 'react';
import { useLocation } from 'react-router-dom';
import { Search, Sun, Moon } from 'lucide-react';
import styles from './Topbar.module.css';
import { Form } from 'react-bootstrap';
import { useTheme } from '../../context/ThemeContext';

const Topbar = ({ role }) => {
  const location = useLocation();
  const { theme, toggleTheme } = useTheme();
  
  // Format tên role cho breadcrumb (team -> Team Member).
  const roleName = role === 'team' ? 'Team Member' : role.charAt(0).toUpperCase() + role.slice(1);
  
  // Lấy tên trang từ phần cuối của đường dẫn URL.
  const pathParts = location.pathname.split('/').filter(Boolean);
  let pageNameRaw = pathParts[pathParts.length - 1] || 'Overview';
  
  // Nếu phần cuối là ID (số) thì dùng tên thực thể cha dạng số ít + "-details".
  if (!isNaN(pageNameRaw) && pathParts.length > 1) {
    const parentEntity = pathParts[pathParts.length - 2];
    const singularEntity = parentEntity.endsWith('s') ? parentEntity.slice(0, -1) : parentEntity;
    pageNameRaw = `${singularEntity}-details`;
  }
  
  // Trang dashboard hiển thị là "Overview".
  if (pageNameRaw.toLowerCase() === 'dashboard') {
    pageNameRaw = 'Overview';
  }
  
  // Viết hoa chữ cái đầu và thay '-' bằng khoảng trắng.
  const pageName = pageNameRaw.charAt(0).toUpperCase() + pageNameRaw.slice(1).replace('-', ' ');

  return (
    <header className={styles.topbar}>
      <div className={styles.breadcrumb}>
        <span className={styles.breadcrumbPage} style={{ fontSize: '1.125rem', fontWeight: '600', color: 'var(--cf-text-primary)' }}>{pageName}</span>
      </div>

      <div className={styles.actions}>
        <div className={styles.searchContainer}>
          <Search className={styles.searchIcon} size={16} />
          <Form.Control 
            type="text" 
            placeholder="Search..." 
            className={styles.searchInput} 
          />
        </div>
        
        <button 
          className={styles.notificationBtn} 
          onClick={toggleTheme}
          title={theme === 'dark' ? "Switch to light mode" : "Switch to dark mode"}
        >
          {theme === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
        </button>
      </div>
    </header>
  );
};

export default Topbar;
