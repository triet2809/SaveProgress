import React, { useState, useEffect } from 'react';
import { Card, Button, Form, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { ArrowLeft, User, Mail, Lock, Briefcase, Tag, FileText, Shield, CheckCircle } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { getUsers, createUser, updateUserProfile, addUserRole, removeUserRole } from '../../api/userApi';

const JUDGE_ROLE = 'judge';

const JudgeForm = () => {
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
  const [judge, setJudge] = useState(null);
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
        setJudge(found || null);
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
          setError('Judge user not found.');
        }
      } catch (err) {
        if (active) setError(err.message || 'Failed to load user');
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

  const hasJudge = (u) => (u?.roles || []).some((r) => r.toLowerCase() === JUDGE_ROLE);

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
        roles: [JUDGE_ROLE],
      });
      navigate('/coordinator/judges');
    } catch (err) {
      setError(err.message || 'Failed to create judge');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveEdit = async () => {
    if (!judge) return;
    try {
      setSaving(true);
      setError('');
      await updateUserProfile(judge.id, {
        fullName: form.fullName.trim(),
        company: form.company || '',
        expertise: form.expertise || '',
        bio: form.bio || '',
        phone: form.phone || '',
      });
      navigate('/coordinator/judges');
    } catch (err) {
      setError(err.message || 'Failed to update judge');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleRole = async () => {
    if (!judge) return;
    try {
      setSaving(true);
      setError('');
      if (hasJudge(judge)) {
        await removeUserRole(judge, JUDGE_ROLE);
      } else {
        await addUserRole(judge, JUDGE_ROLE);
      }
      const res = await getUsers({});
      setJudge((res.value || []).find((u) => u.id === id) || judge);
    } catch (err) {
      setError(err.message || 'Failed to update judge role');
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

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/coordinator/judges')}>
          <ArrowLeft size={24} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>
            {isEditing ? 'Edit Judge Profile' : 'Create New Judge'}
          </h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            {isEditing ? 'Update profile and manage judge role' : 'Create a judge account directly'}
          </div>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Row className="justify-content-center">
        <Col lg={8}>
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
                      <Form.Text className="text-muted">The judge can change this after first login.</Form.Text>
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
                    <Form.Control type="text" name="expertise" placeholder="e.g. AI/ML, System Design" value={form.expertise} onChange={handleChange} />
                  </Form.Group>
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-medium d-flex align-items-center gap-2"><FileText size={16} className="text-primary"/> Professional Bio</Form.Label>
                    <Form.Control as="textarea" rows={4} name="bio" placeholder="Background, achievements, judging experience..." value={form.bio} onChange={handleChange} />
                  </Form.Group>
                </div>

                {isEditing && (
                  <div className="mb-4">
                    <h5 className="fw-bold mb-3 border-bottom pb-2">Roles & Status</h5>
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-medium d-flex align-items-center gap-2"><Shield size={16} className="text-warning"/> Current Roles</Form.Label>
                      <div>{(judge?.roles || []).join(', ') || '\u2014'}</div>
                    </Form.Group>
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-medium d-flex align-items-center gap-2"><CheckCircle size={16} className="text-success"/> Status</Form.Label>
                      <div>{judge?.status || '\u2014'}</div>
                    </Form.Group>
                    <Button variant={hasJudge(judge) ? 'outline-danger' : 'outline-primary'} size="sm" onClick={handleToggleRole} disabled={saving || !judge}>
                      {hasJudge(judge) ? 'Revoke Judge Role' : 'Grant Judge Role'}
                    </Button>
                  </div>
                )}

                <div className="d-flex justify-content-end gap-3 mt-5 pt-3 border-top">
                  <Button variant="secondary" onClick={() => navigate('/coordinator/judges')} disabled={saving}>
                    Cancel
                  </Button>
                  <Button variant="primary" onClick={isEditing ? handleSaveEdit : handleCreate} disabled={saving}>
                    {saving ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Judge'}
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

export default JudgeForm;
