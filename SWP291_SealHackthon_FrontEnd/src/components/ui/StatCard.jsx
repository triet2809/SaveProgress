import React from 'react';
import { Card } from 'react-bootstrap';
import styles from './StatCard.module.css';

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
