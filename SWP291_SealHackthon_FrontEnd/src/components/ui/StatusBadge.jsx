/**
 * StatusBadge.jsx — Badge màu sắc theo trạng thái hoặc loại.
 * Ánh xạ chuỗi status/type -> cặp màu (nền + chữ) theo bảng thiết kế.
 * @param {string} status - Trạng thái (On Track, At Risk...).
 * @param {string} type - Loại (AI/ML, High, Low...) dùng khi không có status.
 */
import React from 'react';
import { Badge } from 'react-bootstrap';

const StatusBadge = ({ status, type }) => {
  let bg = 'secondary';
  let textColor = '#fff'; // mặc định
  let customBg = null;

  // Ánh xạ chuỗi trạng thái -> màu theo thiết kế.
  const statusMap = {
    'On Track': { bg: 'var(--cf-status-success-bg)', color: 'var(--cf-status-success)' },
    'Needs Attention': { bg: 'var(--cf-status-warning-bg)', color: 'var(--cf-status-warning)' },
    'At Risk': { bg: 'var(--cf-status-danger-bg)', color: 'var(--cf-status-danger)' },
    'Pending': { bg: 'var(--cf-status-danger-bg)', color: 'var(--cf-status-danger)' },
    'Draft': { bg: 'var(--cf-status-warning-bg)', color: 'var(--cf-status-warning)' },
    'Completed': { bg: 'var(--cf-status-success-bg)', color: 'var(--cf-status-success)' },
  };

  // Ánh xạ loại (type) -> màu; dùng khi status không khớp.
  const typeMap = {
    'AI/ML': { bg: 'var(--cf-brand-blue-light)', color: 'var(--cf-brand-blue)' },
    'Data Science': { bg: 'var(--cf-brand-blue-light)', color: 'var(--cf-brand-blue)' }, // They look similar in screenshots
    'High': { bg: 'var(--cf-status-danger-bg)', color: 'var(--cf-status-danger)' },
    'Medium': { bg: 'var(--cf-status-warning-bg)', color: 'var(--cf-status-warning)' },
    'Low': { bg: 'var(--cf-status-success-bg)', color: 'var(--cf-status-success)' },
  };

  // Chọn màu theo thứ tự ưu tiên: status -> type -> màu xám mặc định.
  const styleObj = statusMap[status] || typeMap[type] || { bg: '#e2e8f0', color: '#64748b' };

  return (
    <Badge 
      bg="light" /* Reset bootstrap bg */
      style={{ 
        backgroundColor: styleObj.bg, 
        color: styleObj.color,
        fontWeight: 600,
        padding: '0.4em 0.8em',
        borderRadius: 'var(--cf-radius-sm)',
        border: 'none'
      }}
    >
      {status || type}
    </Badge>
  );
};

export default StatusBadge;
