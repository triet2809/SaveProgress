import React from 'react';

const Avatar = ({ initials, bg = 'var(--cf-brand-blue-light)', color = 'var(--cf-brand-blue)', size = 36 }) => {
  return (
    <div style={{
      width: size,
      height: size,
      backgroundColor: bg,
      color: color,
      borderRadius: '50%',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontWeight: 600,
      fontSize: size * 0.4,
      flexShrink: 0
    }}>
      {initials}
    </div>
  );
};

export default Avatar;
