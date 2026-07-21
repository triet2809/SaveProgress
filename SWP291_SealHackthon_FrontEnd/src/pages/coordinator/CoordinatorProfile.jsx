import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Form, Button, Spinner, Alert } from 'react-bootstrap';
import { Mail, Shield, UserCheck, Phone, Building, Briefcase } from 'lucide-react';
import { getMe, updateMe } from '../../api/userApi';
import { getStoredUser, saveStoredUser, getInitials } from '../../utils/authUser';

// PATCH /users/me accepts { fullName, phone, department, position }. email/roles/status
// and universityId are read-only. System Preferences toggles are display-only (no BE field).
const CoordinatorProfile = () => {
  const [me, setMe] = useState(getStoredUser());
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [department, setDepartment] = useState('');
  const [position, setPosition] = useState('');
  const [emailNotifications, setEmailNotifications] = useState(true); // display-only, no BE field
  const [weeklyReports, setWeeklyReports] = useState(true); // display-only, no BE field
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

  const handleSave = async (e) => {
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

  const roleText = (me?.roles || []).join(', ') || 'Coordinator';

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <Spinner animation="border" role="status" />
      </div>
    );
  }

  return (
    <div className="py-2">
      <div className="mb-4">
        <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Coordinator Profile</h1>
        <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Manage your account settings and preferences</div>
      </div>

      <Row className="g-4">
        <Col lg={4}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="text-center p-4">
              <div
                className="mx-auto mb-3 d-flex align-items-center justify-content-center text-white fw-bold"
                style={{ width: '96px', height: '96px', borderRadius: '50%', backgroundColor: 'var(--cf-brand-blue)', fontSize: '2rem' }}
              >
                {getInitials(me?.fullName)}
              </div>
              <h4 className="fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>{me?.fullName}</h4>
              <p style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }} className="mb-3">{roleText}</p>

              <div className="d-flex align-items-center justify-content-center gap-2 mb-4">
                <span className="badge bg-danger text-capitalize">{me?.status || 'active'}</span>
              </div>

              <div className="text-start">
                <hr style={{ borderColor: 'var(--cf-border-color)' }} />
                <div className="d-flex align-items-center gap-3 mb-3">
                  <Mail size={18} className="text-muted" />
                  <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>{me?.email}</div>
                </div>
                <div className="d-flex align-items-center gap-3 mb-3">
                  <Phone size={18} className="text-muted" />
                  <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>{phone || '—'}</div>
                </div>
                <div className="d-flex align-items-center gap-3 mb-3">
                  <Building size={18} className="text-muted" />
                  <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>{department || '—'}</div>
                </div>
                <div className="d-flex align-items-center gap-3 mb-3">
                  <Briefcase size={18} className="text-muted" />
                  <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>{me?.universityId || '—'}</div>
                </div>
                <div className="d-flex align-items-center gap-3 mb-3">
                  <Shield size={18} className="text-muted" />
                  <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>{roleText}</div>
                </div>
                <div className="d-flex align-items-center gap-3">
                  <UserCheck size={18} className="text-muted" />
                  <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }} className="text-capitalize">{me?.status || '—'}</div>
                </div>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={8}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4" style={{ color: 'var(--cf-text-primary)' }}>Personal Information</h5>

              {error && <Alert variant="danger">{error}</Alert>}
              {success && <Alert variant="success">{success}</Alert>}

              <Form onSubmit={handleSave}>
                <Row className="g-3 mb-4">
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Full Name</Form.Label>
                      <Form.Control type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Email Address</Form.Label>
                      <Form.Control type="email" value={me?.email || ''} readOnly disabled />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Phone Number</Form.Label>
                      <Form.Control type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Department</Form.Label>
                      <Form.Control type="text" value={department} onChange={(e) => setDepartment(e.target.value)} />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Position / Title</Form.Label>
                      <Form.Control type="text" value={position} onChange={(e) => setPosition(e.target.value)} />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Roles</Form.Label>
                      <Form.Control type="text" value={roleText} readOnly disabled />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Status</Form.Label>
                      <Form.Control type="text" value={me?.status || '—'} readOnly disabled className="text-capitalize" />
                    </Form.Group>
                  </Col>
                </Row>

                <h5 className="fw-bold mb-3 mt-5" style={{ color: 'var(--cf-text-primary)' }}>System Preferences</h5>
                <div className="mb-4">
                  <Form.Check
                    type="switch"
                    id="email-notifications"
                    label="Receive email notifications for critical system events"
                    checked={emailNotifications}
                    onChange={(e) => setEmailNotifications(e.target.checked)}
                    className="mb-2"
                  />
                  <Form.Check
                    type="switch"
                    id="weekly-reports"
                    label="Generate weekly summary reports automatically"
                    checked={weeklyReports}
                    onChange={(e) => setWeeklyReports(e.target.checked)}
                  />
                </div>

                <div className="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
                  <Button variant="light" type="button" onClick={() => { setFullName(me?.fullName || ''); setPhone(me?.phone || ''); setDepartment(me?.department || ''); setPosition(me?.position || ''); }} disabled={saving}>Cancel</Button>
                  <Button variant="primary" type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save Changes'}</Button>
                </div>
              </Form>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default CoordinatorProfile;
