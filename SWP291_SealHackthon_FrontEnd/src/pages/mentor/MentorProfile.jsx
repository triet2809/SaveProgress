import React, { useEffect, useState } from 'react';
import { Card, Form, Row, Col, Button, Spinner, Alert } from 'react-bootstrap';
import { getMe, updateMe } from '../../api/userApi';
import { getStoredUser, saveStoredUser, getInitials } from '../../utils/authUser';
import styles from './MentorProfile.module.css';

// PATCH /users/me accepts { fullName, phone, department, position }. email/roles/status,
// studentType and universityId are read-only.
const MentorProfile = () => {
  const [profile, setProfile] = useState(null);
  const [formData, setFormData] = useState({ fullName: '', email: '', phone: '', department: '', position: '' });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const res = await getMe();
        const me = res.value || getStoredUser();
        if (!active) return;
        setProfile(me);
        setFormData({
          fullName: me?.fullName || '',
          email: me?.email || '',
          phone: me?.phone || '',
          department: me?.department || '',
          position: me?.position || '',
        });
      } catch (err) {
        if (active) setError(err.message || 'Failed to load profile');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const res = await updateMe({
        fullName: formData.fullName,
        phone: formData.phone,
        department: formData.department,
        position: formData.position,
      });
      const updated = res.value;
      if (updated) {
        setProfile(updated);
        setFormData((prev) => ({
          ...prev,
          fullName: updated.fullName || '',
          phone: updated.phone || '',
          department: updated.department || '',
          position: updated.position || '',
        }));
        const stored = getStoredUser() || {};
        saveStoredUser({ ...stored, fullName: updated.fullName });
      }
      setSuccess('Profile updated successfully.');
    } catch (err) {
      setError(err.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="py-2">
        <div className="text-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      </div>
    );
  }

  const roles = (profile?.roles || []).join(', ');

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Profile</h1>
        <div className={styles.pageSubtitle}>
          Manage your account information
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}
      {success && <Alert variant="success">{success}</Alert>}

      <Card className={styles.profileCard}>
        <Card.Body className="p-4 p-md-5">

          <div className={styles.profileHeaderInfo}>
            <div className={styles.avatarLarge}>
              {getInitials(profile?.fullName || profile?.email)}
            </div>
            <div className={styles.userInfo}>
              <div className={styles.userName}>{profile?.fullName || profile?.email}</div>
              <div className={styles.userSpecialty}>{profile?.email}</div>
              <div className={styles.roleBadge}>{roles || 'Mentor'}</div>
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
                    value={formData.fullName}
                    onChange={handleChange}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Email Address</Form.Label>
                  <Form.Control
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    className={styles.formControl}
                    disabled
                    readOnly
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
                    value={profile?.universityId || '—'}
                    className={styles.formControl}
                    disabled
                    readOnly
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Department</Form.Label>
                  <Form.Control
                    type="text"
                    name="department"
                    value={formData.department}
                    onChange={handleChange}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row className="mb-4">
              <Col md={6} className="mb-3 mb-md-0">
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Position</Form.Label>
                  <Form.Control
                    type="text"
                    name="position"
                    value={formData.position}
                    onChange={handleChange}
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
                    value={formData.phone}
                    onChange={handleChange}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row className="mb-4">
              <Col md={6} className="mb-3 mb-md-0">
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Student Type</Form.Label>
                  <Form.Control
                    type="text"
                    value={profile?.studentType || 'none'}
                    className={styles.formControl}
                    disabled
                    readOnly
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Status</Form.Label>
                  <Form.Control
                    type="text"
                    value={profile?.status || ''}
                    className={styles.formControl}
                    disabled
                    readOnly
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row className="mb-4">
              <Col md={12}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Roles</Form.Label>
                  <Form.Control
                    type="text"
                    value={roles}
                    className={styles.formControl}
                    disabled
                    readOnly
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

export default MentorProfile;
