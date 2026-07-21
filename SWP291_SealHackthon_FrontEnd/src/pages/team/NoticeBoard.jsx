import React, { useEffect, useState } from 'react';
import { Card, Spinner, Alert } from 'react-bootstrap';
import { Clock } from 'lucide-react';
import { getNotices } from '../../api/hackathonApi';
import { getInitials } from '../../utils/authUser';
import styles from './NoticeBoard.module.css';

const isHigh = (priority) => (priority || '').toLowerCase() === 'high';

const NoticeBoard = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notices, setNotices] = useState([]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const res = await getNotices({ size: 50 });
        const list = res?.content || res || [];
        if (active) {
          setNotices(
            list
              .slice()
              .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
          );
        }
      } catch (e) {
        if (active) setError(e.message || 'Failed to load notices');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  if (loading) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Notice Board</h1>
        <div className={styles.pageSubtitle}>
          Recent announcements from organizers and mentors
        </div>
      </div>

      {error && <Alert variant="danger" className="mb-3">{error}</Alert>}

      <div className={styles.noticeList}>
        {notices.length === 0 && !error && (
          <div className="text-muted small">No notices yet.</div>
        )}
        {notices.map((notice) => {
          const authorName = notice.authorName || notice.authorEmail || 'Organizer';
          const roleLabel = notice.targetRole || 'Announcement';
          return (
            <Card 
              key={notice.id} 
              className={`${styles.noticeCard} ${isHigh(notice.priority) ? styles.priorityHigh : styles.priorityNormal}`}
            >
              <Card.Body className="p-4">
                <div className={styles.cardHeader}>
                  <div className={styles.authorInfo}>
                    <div className={styles.avatar}>
                      {getInitials(authorName)}
                    </div>
                    <div className={styles.authorDetails}>
                      <span className={styles.authorName}>{authorName}</span>
                      <span className={styles.authorRole}>
                        {roleLabel}
                      </span>
                    </div>
                  </div>
                  <div className={styles.noticeDate}>
                    <Clock size={12} />
                    {notice.createdAt ? new Date(notice.createdAt).toLocaleString() : ''}
                  </div>
                </div>
                
                <h5 className={styles.noticeTitle}>
                  {notice.title}
                  {isHigh(notice.priority) && (
                    <span className={styles.importantIndicator}>Important</span>
                  )}
                </h5>
                <p className={styles.noticeContent}>{notice.content}</p>
              </Card.Body>
            </Card>
          );
        })}
      </div>
    </div>
  );
};

export default NoticeBoard;
