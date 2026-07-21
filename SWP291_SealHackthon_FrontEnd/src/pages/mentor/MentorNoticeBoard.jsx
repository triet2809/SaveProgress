import React, { useEffect, useState } from 'react';
import { Card, Form, Row, Col, Button, Spinner, Alert } from 'react-bootstrap';
import { Clock } from 'lucide-react';
import { getNotices, createNotice } from '../../api/hackathonApi';
import { getStoredUser, getInitials } from '../../utils/authUser';
import styles from './MentorNoticeBoard.module.css';

const roleLabel = (targetRole) => {
  switch ((targetRole || '').toLowerCase()) {
    case 'judge': return 'Judge';
    case 'mentor': return 'Mentor';
    case 'coordinator': return 'Admin';
    case 'team_member': return 'Team';
    default: return 'All';
  }
};

const MentorNoticeBoard = () => {
  const user = getStoredUser();
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  // Form State
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isImportant, setIsImportant] = useState(false);
  const [targetRole, setTargetRole] = useState('team_member');
  const [showForm, setShowForm] = useState(false);

  const loadNotices = async () => {
    const res = await getNotices({ size: 100 });
    const list = res?.content || res || [];
    list.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
    setNotices(list);
  };

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        await loadNotices();
      } catch (err) {
        if (active) setError(err.message || 'Failed to load notices');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  const getAvatarClass = (role) => {
    switch (role) {
      case 'Judge': return styles.avatarJudge;
      case 'Mentor': return styles.avatarMentor;
      case 'Admin': return styles.avatarAdmin;
      default: return '';
    }
  };

  const getRoleClass = (role) => {
    switch (role) {
      case 'Judge': return styles.roleJudge;
      case 'Mentor': return styles.roleMentor;
      case 'Admin': return styles.roleAdmin;
      default: return '';
    }
  };

  const handlePostNotice = async (e) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    setSaving(true);
    setError('');
    try {
      await createNotice({
        title: title.trim(),
        content: content.trim(),
        priority: isImportant ? 'high' : 'normal',
        targetRole,
      });
      setTitle('');
      setContent('');
      setIsImportant(false);
      setTargetRole('team_member');
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
              Post updates to your teams and view global announcements
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
              <h5 style={{ fontWeight: 600, margin: 0 }}>Create Announcement</h5>
              <Button
                variant="link"
                className="text-muted p-0 text-decoration-none"
                onClick={() => setShowForm(false)}
              >
                Cancel
              </Button>
            </div>
            <Form onSubmit={handlePostNotice}>
              <Row className="mb-3">
                <Col md={8}>
                  <Form.Group>
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
                </Col>
                <Col md={4}>
                  <Form.Group>
                    <Form.Label className={styles.formLabel}>To:</Form.Label>
                    <Form.Select
                      className={styles.formControl}
                      value={targetRole}
                      onChange={(e) => setTargetRole(e.target.value)}
                    >
                      <option value="team_member">Teams</option>
                      <option value="mentor">Mentors</option>
                      <option value="judge">Judges</option>
                      <option value="coordinator">Coordinators</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
              </Row>

              <Form.Group className="mb-3">
                <Form.Label className={styles.formLabel}>Message Content</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  className={styles.formControl}
                  placeholder="What do you want to announce?"
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
                  className={styles.formLabel}
                  style={{ marginBottom: 0 }}
                />
                <Button type="submit" className={styles.postBtn} disabled={saving}>
                  {saving ? 'Posting...' : 'Post Notice'}
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
      )}

      {/* Notice Feed */}
      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      ) : (
        <div className={styles.noticeList}>
          {notices.length === 0 && !error && (
            <div className="text-muted">No notices yet.</div>
          )}
          {notices.map((notice) => {
            const displayRole = roleLabel(notice.targetRole);
            const isHigh = (notice.priority || '').toLowerCase() === 'high';
            return (
              <Card
                key={notice.id}
                className={`${styles.noticeCard} ${isHigh ? styles.priorityHigh : styles.priorityNormal}`}
              >
                <Card.Body className="p-4">
                  <div className={styles.cardHeader}>
                    <div className={styles.authorInfo}>
                      <div className={`${styles.avatar} ${getAvatarClass(displayRole)}`}>
                        {getInitials(notice.authorName || notice.authorEmail)}
                      </div>
                      <div className={styles.authorDetails}>
                        <span className={styles.authorName}>{notice.authorName || notice.authorEmail}</span>
                        <span className={`${styles.authorRole} ${getRoleClass(displayRole)}`}>
                          {displayRole}
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
                    {isHigh && (
                      <span className={styles.importantIndicator}>Important</span>
                    )}
                    {notice.targetRole && notice.targetRole !== 'team_member' && (
                      <span className={styles.targetBadge}>Targeted: {displayRole}</span>
                    )}
                  </h5>
                  <p className={styles.noticeContent}>{notice.content}</p>
                </Card.Body>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default MentorNoticeBoard;
