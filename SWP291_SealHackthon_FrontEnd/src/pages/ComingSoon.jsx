import React from 'react';

const ComingSoon = ({ title }) => {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center" style={{ height: '60vh' }}>
      <h2 className="fw-bold mb-3" style={{ color: 'var(--cf-text-primary)' }}>{title}</h2>
      <p className="text-muted">This page is currently under construction.</p>
    </div>
  );
};

export default ComingSoon;
