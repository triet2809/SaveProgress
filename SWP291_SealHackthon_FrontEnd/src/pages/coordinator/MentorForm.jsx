import React, { useState, useEffect } from 'react';
import { Card, Button, Form, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { ArrowLeft, User, Mail, Lock, Briefcase, Tag, FileText, Shield, CheckCircle } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { getUsers, createUser, updateUserProfile, addUserRole, removeUserRole } from '../../api/userApi';

const MENTOR_ROLE = 'mentor';

const MentorForm = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditing = !!id;

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    company: '',
    expertise: '',
    bio: '',
    phone: '',
  });
  const [mentor, setMentor] = useState(null);
  const [loading, setLoading] = useState(isEditing);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isEditing) return;
    let active = true;
    (async () => {
      try {
        setLoading(true);
        setError('');
        const res = await getUsers({});
        const users = res.value || [];
        if (!active) return;
        const found = users.find((u) => u.id === id);
        setMentor(found || null);
        if (found) {
          setForm({
            fullName: found.fullName || '',
            email: found.email || '',
            password: '',
            company: found.company || '',
            expertise: found.expertise || '',
            bio: found.bio || '',
            phone: found.phone || '',
          });
        } else {
          setError('Mentor not found.');
        }
      } catch (err) {
        if (active) setError(err.message || 'Failed to load mentor');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [id, isEditing]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const hasMentor = (u) => (u?.roles || []).some((r) => r.toLowerCase() === MENTOR_ROLE);

  const handleCreate = async () => {
    if (!form.fullName || !form.email || !form.password) {
      setError('Full name, email and password are required.');
      return;
    }
    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    try {
      setSaving(true);
      setError('');
      await createUser({
        email: form.email.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        company: form.company || null,
        expertise: form.expertise || null,
        bio: form.bio || null,
        phone: form.phone || null,
        status: 'approved',
        roles: [MENTOR_ROLE],
      });
      navigate('/coordinator/mentors');
    } catch (err) {
      setError(err.message || 'Failed to create mentor');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveEdit = async () => {
    if (!mentor) return;
    try {
      setSaving(true);
      setError('');
      await updateUserProfile(mentor.id, {
        fullName: form.fullName.trim(),
        company: form.company || '',
        expertise: form.expertise || '',
        bio: form.bio || '',
        phone: form.phone || '',
      });
      navigate('/coordinator/mentors');
    } catch (err) {
      setError(err.message || 'Failed to update mentor');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleRole = async () => {
    if (!mentor) return;
    try {
      setSaving(true);
      setError('');
      if (hasMentor(mentor)) {
        await removeUserRole(mentor, MENTOR_ROLE);
      } else {
        await addUserRole(mentor, MENTOR_ROLE);
      }
      const res = await getUsers({});
      setMentor((res.value || []).find((u) => u.id === id) || mentor);
    } catch (err) {
      setError(err.message || 'Failed to update mentor role');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <Spinner animation="border" role="status" />
      </div>
    );
  }

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/coordinator/mentors')}>
          <ArrowLeft size={24} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>
            {isEditing ? 'Edit Mentor Profile' : 'Create New Mentor'}
          </h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            {isEditing ? 'Update mentor details and manage role' : 'Create a mentor account directly'}
          </div>
        </div>
      </div>

      <Row className="justify-content-center">
        <Col lg={8}>
          {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4 p-md-5">
              <Form>
                <div className="mb-4">
                  <h5 className="fw-bold mb-3 border-bottom pb-2">Basic Information</h5>
                  <Row>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-medium d-flex align-items-center gap-2"><User size={16} className="text-primary"/> Full Name</Form.Label>
                        <Form.Control type="text" name="fullName" placeholder="e.g. Dr. Jane Smith" value={form.fullName} onChange={handleChange} />
                      </Form.Group>
                    </Col>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-medium d-flex align-items-center gap-2"><Mail size={16} className="text-danger"/> Email Address</Form.Label>
                        <Form.Control type="email" name="email" placeholder="name@example.com" value={form.email} onChange={handleChange} readOnly={isEditing} disabled={isEditing} />
                      </Form.Group>
                    </Col>
                  </Row>

                  {!isEditing && (
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-medium d-flex align-items-center gap-2"><Lock size={16} className="text-secondary"/> Password</Form.Label>
                      <Form.Control type="password" name="password" placeholder="Min 8 characters" value={form.password} onChange={handleChange} />
                      <Form.Text className="text-muted">The mentor can change this after first login.</Form.Text>
                    </Form.Group>
                  )}

                  <Form.Group className="mb-3">
                    <Form.Label className="fw-medium d-flex align-items-center gap-2"><Briefcase size={16} className="text-info"/> Company / Organization</Form.Label>
                    <Form.Control type="text" name="company" placeholder="e.g. Google, Tech Startup Inc." value={form.company} onChange={handleChange} />
                  </Form.Group>
                </div>

                <div className="mb-4">
                  <h5 className="fw-bold mb-3 border-bottom pb-2">Expertise</h5>
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-medium d-flex align-items-center gap-2"><Tag size={16} className="text-warning"/> Expertise</Form.Label>
                    <Form.Control type="text" name="expertise" placeholder="e.g. React, Node.js, System Design" value={form.expertise} onChange={handleChange} />
                  </Form.Group>
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-medium d-flex align-items-center gap-2"><FileText size={16} className="text-primary"/> Professional Bio</Form.Label>
                    <Form.Control as="textarea" rows={4} name="bio" placeholder="Background, achievements, and mentoring style..." value={form.bio} onChange={handleChange} />
                  </Form.Group>
                </div>

                {isEditing && (
                  <div className="mb-4">
                    <h5 className="fw-bold mb-3 border-bottom pb-2">Roles & Status</h5>
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-medium d-flex align-items-center gap-2"><Shield size={16} className="text-warning"/> Current Roles</Form.Label>
                      <div>{(mentor?.roles || []).join(', ') || '\u2014'}</div>
                    </Form.Group>
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-medium d-flex align-items-center gap-2"><CheckCircle size={16} className="text-success"/> Status</Form.Label>
                      <div>{mentor?.status || '\u2014'}</div>
                    </Form.Group>
                    <Button variant={hasMentor(mentor) ? 'outline-danger' : 'outline-primary'} size="sm" onClick={handleToggleRole} disabled={saving || !mentor}>
                      {hasMentor(mentor) ? 'Revoke Mentor Role' : 'Grant Mentor Role'}
                    </Button>
                  </div>
                )}

                <div className="d-flex justify-content-end gap-3 mt-5 pt-3 border-top">
                  <Button variant="secondary" onClick={() => navigate('/coordinator/mentors')} disabled={saving}>
                    Cancel
                  </Button>
                  <Button variant="primary" onClick={isEditing ? handleSaveEdit : handleCreate} disabled={saving}>
                    {saving ? <Spinner animation="border" size="sm" /> : (isEditing ? 'Save Changes' : 'Create Mentor')}
                  </Button>
                </div>
              </Form>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default MentorForm;
