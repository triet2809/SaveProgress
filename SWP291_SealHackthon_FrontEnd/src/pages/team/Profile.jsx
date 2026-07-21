import React, { useEffect, useState } from 'react';
import { Card, Form, Row, Col, Button, Spinner, Alert } from 'react-bootstrap';
import { getMe, updateMe } from '../../api/userApi';
import { getInitials, saveStoredUser } from '../../utils/authUser';
import styles from './Profile.module.css';

const Profile = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState('');
  const [user, setUser] = useState(null);
  const [fullName, setFullName] = useState('');

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const res = await getMe();
        const u = res?.value || null;
        if (active) {
          setUser(u);
          setFullName(u?.fullName || '');
        }
      } catch (e) {
        if (active) setError(e.message || 'Failed to load profile');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!fullName.trim()) {
      setError('Full name cannot be empty.');
      return;
    }
    setSaving(true);
    setSaved('');
    setError('');
    try {
      const res = await updateMe({ fullName: fullName.trim() });
      const updated = res?.value || null;
      if (updated) {
        setUser(updated);
        saveStoredUser(updated);
      }
      setSaved('Profile updated.');
    } catch (err) {
      setError(err.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  const roleLabel = (user?.roles || []).join(', ') || 'Participant';

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Profile</h1>
        <div className={styles.pageSubtitle}>
          Manage your account information
        </div>
      </div>

      {error && <Alert variant="danger" className="mb-3">{error}</Alert>}
      {saved && <Alert variant="success" className="mb-3">{saved}</Alert>}

      <Card className={styles.profileCard}>
        <div className={styles.profileHeader}>
          <div className={styles.avatar}>
            {getInitials(user?.fullName || user?.email || 'U')}
          </div>
          <div className={styles.headerInfo}>
            <div className={styles.userName}>{user?.fullName || user?.email}</div>
            <div className={styles.teamRole}>
              {user?.campusName || user?.universityName || 'SEAL Hackathon'}
            </div>
            <div className={styles.roleBadge}>{roleLabel}</div>
          </div>
        </div>

        <Card.Body className="p-4">
          <Form onSubmit={handleSave}>
            <Row className="mb-4">
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Full Name</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Email Address</Form.Label>
                  <Form.Control 
                    type="email" 
                    value={user?.email || ''}
                    readOnly
                    disabled
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row className="mb-4">
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Student ID</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={user?.studentId || '—'}
                    readOnly
                    disabled
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>University</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={user?.universityName || '—'}
                    readOnly
                    disabled
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row className="mb-4">
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Campus</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={user?.campusName || '—'}
                    readOnly
                    disabled
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Status</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={user?.status || '—'}
                    readOnly
                    disabled
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Button variant="primary" type="submit" className={styles.saveBtn} disabled={saving}>
              {saving ? 'Saving…' : 'Save Changes'}
            </Button>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
};

export default Profile;
