import React, { useEffect, useState } from 'react';
import { Card, Form, Row, Col, Button, Spinner, Alert } from 'react-bootstrap';
import { getMe, updateMe } from '../../api/userApi';
import { getStoredUser, saveStoredUser, getInitials } from '../../utils/authUser';
import styles from './JudgeProfile.module.css';

// PATCH /users/me accepts { fullName, phone, department, position }. email/roles/status
// and universityId are read-only.
const JudgeProfile = () => {
  const [me, setMe] = useState(getStoredUser());
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [department, setDepartment] = useState('');
  const [position, setPosition] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const res = await getMe();
        if (!res.ok) throw new Error(res.data?.message || 'Failed to load profile');
        if (!active) return;
        setMe(res.value);
        setFullName(res.value?.fullName || '');
        setPhone(res.value?.phone || '');
        setDepartment(res.value?.department || '');
        setPosition(res.value?.position || '');
      } catch (err) {
        if (active) setError(err.message || 'Failed to load profile');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const res = await updateMe({ fullName, phone, department, position });
      if (!res.ok) throw new Error(res.data?.message || 'Failed to update profile');
      const updated = res.value || { ...me, fullName, phone, department, position };
      setMe(updated);
      setPhone(updated.phone || '');
      setDepartment(updated.department || '');
      setPosition(updated.position || '');
      saveStoredUser({ ...getStoredUser(), fullName: updated.fullName });
      setSuccess('Profile updated successfully.');
    } catch (err) {
      setError(err.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const roleText = (me?.roles || []).join(', ') || 'Judge';

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <Spinner animation="border" role="status" />
      </div>
    );
  }

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Profile</h1>
        <div className={styles.pageSubtitle}>
          Manage your account information
        </div>
      </div>

      <Card className={styles.profileCard}>
        <Card.Body className="p-4 p-md-5">

          {error && <Alert variant="danger">{error}</Alert>}
          {success && <Alert variant="success">{success}</Alert>}

          <div className={styles.profileHeaderInfo}>
            <div className={styles.avatarLarge}>
              {getInitials(me?.fullName)}
            </div>
            <div className={styles.userInfo}>
              <div className={styles.userName}>{me?.fullName}</div>
              <div className={styles.userSpecialty}>{me?.email}</div>
              <div className={styles.roleBadge}>{roleText}</div>
            </div>
          </div>

          <Form onSubmit={handleSubmit}>
            <Row className="mb-4">
              <Col md={6} className="mb-3 mb-md-0">
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Full Name</Form.Label>
                  <Form.Control
                    type="text"
                    name="fullName"
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
                    value={me?.email || ''}
                    readOnly
                    disabled
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row className="mb-4">
              <Col md={6} className="mb-3 mb-md-0">
                <Form.Group>
                  <Form.Label className={styles.formLabel}>University ID</Form.Label>
                  <Form.Control
                    type="text"
                    value={me?.universityId || '—'}
                    readOnly
                    disabled
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Department</Form.Label>
                  <Form.Control
                    type="text"
                    name="department"
                    value={department}
                    onChange={(e) => setDepartment(e.target.value)}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row className="mb-4">
              <Col md={6} className="mb-3 mb-md-0">
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Title / Position</Form.Label>
                  <Form.Control
                    type="text"
                    name="position"
                    value={position}
                    onChange={(e) => setPosition(e.target.value)}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Phone</Form.Label>
                  <Form.Control
                    type="tel"
                    name="phone"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row className="mb-4">
              <Col md={6} className="mb-3 mb-md-0">
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Roles</Form.Label>
                  <Form.Control
                    type="text"
                    value={roleText}
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
                    value={me?.status || '—'}
                    readOnly
                    disabled
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Button type="submit" className={styles.saveBtn} disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </Form>

        </Card.Body>
      </Card>
    </div>
  );
};

export default JudgeProfile;
