/**
 * StatCard.jsx — Thẻ hiển thị một chỉ số thống kê (icon + giá trị + tiêu đề).
 * Dùng nhiều ở các trang dashboard.
 */
import React from 'react';
import { Card } from 'react-bootstrap';
import styles from './StatCard.module.css';

/**
 * @param {Component} icon - Component icon (đổi tên thành Icon để render JSX).
 * @param {string} iconColor - Màu icon.
 * @param {string} iconBg - Màu nền ô icon.
 * @param {string|number} value - Giá trị thống kê chính.
 * @param {string} title - Nhãn chỉ số.
 * @param {string} [subtitle] - Chú thích phụ (tùy chọn).
 */
const StatCard = ({ icon: Icon, iconColor, iconBg, value, title, subtitle }) => {
  return (
    <Card className={`${styles.statCard} h-100`}>
      <Card.Body className="d-flex flex-column justify-content-between">
        <div 
          className={styles.iconContainer} 
          style={{ color: iconColor, backgroundColor: iconBg }}
        >
          <Icon size={20} />
        </div>
        <div className="mt-3">
          <h2 className={styles.value}>{value}</h2>
          <div className={styles.title}>{title}</div>
          {subtitle && <div className={styles.subtitle}>{subtitle}</div>}
        </div>
      </Card.Body>
    </Card>
  );
};

export default StatCard;
