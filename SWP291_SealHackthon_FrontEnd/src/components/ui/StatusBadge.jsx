import React from 'react';
import { Badge } from 'react-bootstrap';

const StatusBadge = ({ status, type }) => {
  let bg = 'secondary';
  let textColor = '#fff'; // default
  let customBg = null;

  // Map status strings to colors matching design
  const statusMap = {
    'On Track': { bg: 'var(--cf-status-success-bg)', color: 'var(--cf-status-success)' },
    'Needs Attention': { bg: 'var(--cf-status-warning-bg)', color: 'var(--cf-status-warning)' },
    'At Risk': { bg: 'var(--cf-status-danger-bg)', color: 'var(--cf-status-danger)' },
    'Pending': { bg: 'var(--cf-status-danger-bg)', color: 'var(--cf-status-danger)' },
    'Draft': { bg: 'var(--cf-status-warning-bg)', color: 'var(--cf-status-warning)' },
    'Completed': { bg: 'var(--cf-status-success-bg)', color: 'var(--cf-status-success)' },
  };

  // Map types like AI/ML or Data Science to specific colors
  const typeMap = {
    'AI/ML': { bg: 'var(--cf-brand-blue-light)', color: 'var(--cf-brand-blue)' },
    'Data Science': { bg: 'var(--cf-brand-blue-light)', color: 'var(--cf-brand-blue)' }, // They look similar in screenshots
    'High': { bg: 'var(--cf-status-danger-bg)', color: 'var(--cf-status-danger)' },
    'Medium': { bg: 'var(--cf-status-warning-bg)', color: 'var(--cf-status-warning)' },
    'Low': { bg: 'var(--cf-status-success-bg)', color: 'var(--cf-status-success)' },
  };

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
