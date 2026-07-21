import React, { useEffect, useState } from 'react';
import { Card, Form, Button, Spinner, Alert } from 'react-bootstrap';
import { Clock } from 'lucide-react';
import { getNotices, createNotice } from '../../api/hackathonApi';
import { getStoredUser, getInitials } from '../../utils/authUser';
import styles from './JudgeNoticeBoard.module.css';

const asArray = (data) => data?.content || data || [];

const roleLabel = (role) => {
  if (!role) return 'All';
  return role
    .split('_')
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' ');
};

const JudgeNoticeBoard = () => {
  const user = getStoredUser();

  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  // Form State
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isImportant, setIsImportant] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const loadNotices = async () => {
    try {
      setLoading(true);
      setError('');
      const data = asArray(await getNotices({ size: 100 }));
      setNotices(data);
    } catch (err) {
      setError(err.message || 'Failed to load notices');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotices();
  }, []);

  const getAvatarClass = (role) => {
    switch (role) {
      case 'judge': return styles.avatarJudge;
      case 'mentor': return styles.avatarMentor;
      case 'coordinator': return styles.avatarAdmin;
      default: return '';
    }
  };

  const getRoleClass = (role) => {
    switch (role) {
      case 'judge': return styles.roleJudge;
      case 'mentor': return styles.roleMentor;
      case 'coordinator': return styles.roleAdmin;
      default: return '';
    }
  };

  const handlePostNotice = async (e) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    try {
      setSaving(true);
      setError('');
      await createNotice({
        title: title.trim(),
        content: content.trim(),
        priority: isImportant ? 'high' : 'normal',
      });
      setTitle('');
      setContent('');
      setIsImportant(false);
      setShowForm(false);
      await loadNotices();
    } catch (err) {
      setError(err.message || 'Failed to post notice');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <div className="d-flex justify-content-between align-items-end">
          <div>
            <h1 className={styles.pageTitle}>Notice Board</h1>
            <div className={styles.pageSubtitle}>
              Post global announcements and view event updates
            </div>
          </div>
          {!showForm && (
            <Button
              className={styles.postBtn}
              onClick={() => setShowForm(true)}
            >
              + Create Announcement
            </Button>
          )}
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {/* Post Notice Form */}
      {showForm && (
        <Card className={styles.postCard}>
          <Card.Body className="p-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
              <h5 style={{ fontWeight: 600, margin: 0 }}>Create Global Announcement</h5>
              <Button
                variant="link"
                className="text-muted p-0 text-decoration-none"
                onClick={() => setShowForm(false)}
              >
                Cancel
              </Button>
            </div>
            <Form onSubmit={handlePostNotice}>
              <Form.Group className="mb-3">
                <Form.Label className={styles.formLabel}>Title</Form.Label>
                <Form.Control
                  type="text"
                  className={styles.formControl}
                  placeholder="Brief, descriptive title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label className={styles.formLabel}>Message Content</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  className={styles.formControl}
                  placeholder="What do you want to announce globally?"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  required
                />
              </Form.Group>

              <div className="d-flex justify-content-between align-items-center">
                <Form.Check
                  type="checkbox"
                  id="important-flag"
                  label="Flag as Important"
                  checked={isImportant}
                  onChange={(e) => setIsImportant(e.target.checked)}
                  style={{ fontSize: '0.875rem', fontWeight: 500 }}
                />
                <Button type="submit" className={styles.submitBtn} disabled={saving}>
                  {saving ? 'Posting...' : 'Post Global Notice'}
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
      )}

      {/* Notices Feed */}
      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      ) : (
        <div>
          {notices.length === 0 && (
            <div className="text-muted">No notices yet.</div>
          )}
          {notices.map((notice) => {
            const isHigh = (notice.priority || '').toLowerCase() === 'high';
            const authorName = notice.authorName || notice.authorEmail || 'Unknown';
            return (
              <Card
                key={notice.id}
                className={`${styles.noticeCard} ${isHigh ? styles.noticeImportant : ''}`}
              >
                <Card.Body className="p-4">
                  <div className={styles.noticeHeader}>
                    <div>
                      <h4 className={styles.noticeTitle}>
                        {notice.title}
                        {isHigh && (
                          <span className={styles.priorityBadge}>IMPORTANT</span>
                        )}
                      </h4>
                      <div className={styles.noticeMeta}>
                        <div className={styles.authorInfo}>
                          <div className={`${styles.avatar}`}>
                            {getInitials(authorName)}
                          </div>
                          <span className={styles.authorName}>{authorName}</span>
                        </div>
                        {notice.targetRole && (
                          <span className={styles.targetBadge}>To: {roleLabel(notice.targetRole)}</span>
                        )}
                      </div>
                    </div>
                    <div className={styles.noticeDate}>
                      <Clock size={14} />
                      {notice.createdAt ? new Date(notice.createdAt).toLocaleString() : ''}
                    </div>
                  </div>

                  <p className={styles.noticeContent}>
                    {notice.content}
                  </p>
                </Card.Body>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default JudgeNoticeBoard;
