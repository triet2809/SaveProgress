import React, { useEffect, useState } from 'react';
import { Card, Button, Form, Row, Col, Alert, Badge, Spinner } from 'react-bootstrap';
import { Plus, Trash2, Users, Save, ArrowLeft, AlertTriangle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getTracks, createTeam, addTeamMember } from '../../api/hackathonApi';

const RecordTeam = () => {
  const navigate = useNavigate();
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [memberWarning, setMemberWarning] = useState('');

  const [tracks, setTracks] = useState([]);
  const [tracksLoading, setTracksLoading] = useState(true);

  const [teamData, setTeamData] = useState({
    name: '',
    project: '',
    trackId: '',
    description: ''
  });

  const [members, setMembers] = useState([
    { id: 1, name: '', studentId: '', email: '', major: '' }
  ]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const res = await getTracks({ size: 100 });
        const list = res?.content || res || [];
        if (!active) return;
        setTracks(list);
        if (list.length) setTeamData((prev) => ({ ...prev, trackId: prev.trackId || list[0].id }));
      } catch (e) {
        if (active) setError(e.message || 'Failed to load tracks');
      } finally {
        if (active) setTracksLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const handleTeamChange = (e) => {
    const { name, value } = e.target;
    setTeamData(prev => ({ ...prev, [name]: value }));
  };

  const handleMemberChange = (id, field, value) => {
    setMembers(members.map(member =>
      member.id === id ? { ...member, [field]: value } : member
    ));
  };

  const addMember = () => {
    if (members.length < 4) {
      setMembers([...members, {
        id: Date.now(),
        name: '',
        studentId: '',
        email: '',
        major: ''
      }]);
    }
  };

  const removeMember = (id) => {
    if (members.length > 1) {
      setMembers(members.filter(m => m.id !== id));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMemberWarning('');
    if (!teamData.trackId) {
      setError('Please select a track.');
      return;
    }
    setSubmitting(true);
    try {
      const created = await createTeam({
        trackId: teamData.trackId,
        name: teamData.name,
      });
      const teamId = created?.id;

      // Add members (each identified by email); collect any failures without aborting.
      const failed = [];
      if (teamId) {
        for (const m of members) {
          if (!m.email) continue;
          try {
            await addTeamMember(teamId, { email: m.email, fullName: m.name, studentId: m.studentId, major: m.major });
          } catch (memErr) {
            failed.push(`${m.email}: ${memErr.message}`);
          }
        }
      }
      if (failed.length) {
        setMemberWarning(`Team created, but some members could not be added: ${failed.join('; ')}`);
      }
      setSuccess(true);
      setTimeout(() => {
        navigate('/coordinator/teams');
      }, 1800);
    } catch (err) {
      setError(err.message || 'Failed to record team');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="py-2">
      <div className="d-flex align-items-center mb-4 gap-3">
        <Button 
          variant="light" 
          className="d-flex align-items-center justify-content-center rounded-circle p-0" 
          style={{ width: '40px', height: '40px', border: '1px solid var(--cf-border-color)' }}
          onClick={() => navigate('/coordinator/teams')}
        >
          <ArrowLeft size={20} style={{ color: 'var(--cf-text-secondary)' }} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Add New Team</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Manually enter registration details for a new team</div>
        </div>
      </div>

      {success && (
        <Alert variant="success" className="d-flex align-items-center gap-2">
          <Save size={18} />
          Team successfully recorded! Redirecting to team list...
        </Alert>
      )}
      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}
      {memberWarning && <Alert variant="warning" onClose={() => setMemberWarning('')} dismissible>{memberWarning}</Alert>}

      <Form onSubmit={handleSubmit}>
        <Row className="g-4">
          <Col lg={4}>
            <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Card.Body className="p-4">
                <h5 className="fw-bold mb-4 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
                  <Users size={20} className="text-primary" /> Team Information
                </h5>
                
                <Form.Group className="mb-3">
                  <Form.Label style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--cf-text-secondary)' }}>Team Name *</Form.Label>
                  <Form.Control 
                    type="text" 
                    name="name"
                    value={teamData.name}
                    onChange={handleTeamChange}
                    required
                    placeholder="Enter team name"
                  />
                </Form.Group>

                <Form.Group className="mb-3">
                  <Form.Label style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--cf-text-secondary)' }}>Project Name</Form.Label>
                  <Form.Control 
                    type="text" 
                    name="project"
                    value={teamData.project}
                    onChange={handleTeamChange}
                    placeholder="Enter project name"
                  />
                </Form.Group>

                <Form.Group className="mb-3">
                  <Form.Label style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--cf-text-secondary)' }}>Track *</Form.Label>
                  <Form.Select 
                    name="trackId"
                    value={teamData.trackId}
                    onChange={handleTeamChange}
                    disabled={tracksLoading}
                    required
                  >
                    {tracksLoading && <option>Loading tracks...</option>}
                    {!tracksLoading && tracks.length === 0 && <option value="">No tracks available</option>}
                    {tracks.map((t) => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                  </Form.Select>
                </Form.Group>

                <Form.Group className="mb-3">
                  <Form.Label style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--cf-text-secondary)' }}>Project Description</Form.Label>
                  <Form.Control 
                    as="textarea" 
                    rows={4}
                    name="description"
                    value={teamData.description}
                    onChange={handleTeamChange}
                    placeholder="Brief overview of the project..."
                  />
                </Form.Group>
              </Card.Body>
            </Card>
          </Col>

          <Col lg={8}>
            <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Card.Body className="p-4">
                <div className="d-flex justify-content-between align-items-center mb-4">
                  <h5 className="fw-bold mb-0" style={{ color: 'var(--cf-text-primary)' }}>
                    Team Members ({members.length}/4)
                  </h5>
                  {members.length < 4 && (
                    <Button 
                      variant="outline-primary" 
                      size="sm" 
                      className="d-flex align-items-center gap-1"
                      onClick={addMember}
                    >
                      <Plus size={16} /> Add Member
                    </Button>
                  )}
                </div>

                {members.length < 4 && (
                  <Alert variant="warning" className="py-2 mb-4 d-flex align-items-center gap-2" style={{ fontSize: '0.875rem' }}>
                    <AlertTriangle size={16} />
                    <strong>Warning:</strong> Teams with fewer than 4 contestants will be flagged for potential disqualification.
                  </Alert>
                )}

                <div className="d-flex flex-column gap-3">
                  {members.map((member, index) => (
                    <div 
                      key={member.id} 
                      className="p-3 rounded" 
                      style={{ border: '1px solid var(--cf-border-color)', backgroundColor: 'var(--cf-bg-main)' }}
                    >
                      <div className="d-flex justify-content-between align-items-center mb-3">
                        <span className="fw-bold" style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
                          Member {index + 1} {index === 0 && <Badge bg="primary" className="ms-2">Team Lead</Badge>}
                        </span>
                        {members.length > 1 && (
                          <Button 
                            variant="link" 
                            className="text-danger p-0" 
                            onClick={() => removeMember(member.id)}
                            title="Remove Member"
                          >
                            <Trash2 size={16} />
                          </Button>
                        )}
                      </div>

                      <Row className="g-3">
                        <Col md={6}>
                          <Form.Group>
                            <Form.Label style={{ fontSize: '0.875rem', color: 'var(--cf-text-secondary)' }}>Full Name *</Form.Label>
                            <Form.Control 
                              type="text" 
                              required
                              placeholder="John Doe"
                              value={member.name}
                              onChange={(e) => handleMemberChange(member.id, 'name', e.target.value)}
                            />
                          </Form.Group>
                        </Col>
                        <Col md={6}>
                          <Form.Group>
                            <Form.Label style={{ fontSize: '0.875rem', color: 'var(--cf-text-secondary)' }}>Student ID *</Form.Label>
                            <Form.Control 
                              type="text" 
                              required
                              placeholder="FPT-2026-1234"
                              value={member.studentId}
                              onChange={(e) => handleMemberChange(member.id, 'studentId', e.target.value)}
                            />
                          </Form.Group>
                        </Col>
                        <Col md={6}>
                          <Form.Group>
                            <Form.Label style={{ fontSize: '0.875rem', color: 'var(--cf-text-secondary)' }}>Email Address *</Form.Label>
                            <Form.Control 
                              type="email" 
                              required
                              placeholder="john.doe@fpt.edu.vn"
                              value={member.email}
                              onChange={(e) => handleMemberChange(member.id, 'email', e.target.value)}
                            />
                          </Form.Group>
                        </Col>
                        <Col md={6}>
                          <Form.Group>
                            <Form.Label style={{ fontSize: '0.875rem', color: 'var(--cf-text-secondary)' }}>Major / Department</Form.Label>
                            <Form.Control 
                              type="text" 
                              placeholder="Computer Science"
                              value={member.major}
                              onChange={(e) => handleMemberChange(member.id, 'major', e.target.value)}
                            />
                          </Form.Group>
                        </Col>
                      </Row>
                    </div>
                  ))}
                </div>

                <div className="d-flex justify-content-end mt-4 pt-3" style={{ borderTop: '1px solid var(--cf-border-color)' }}>
                  <Button 
                    variant="primary" 
                    type="submit" 
                    className="px-4 py-2 d-flex align-items-center gap-2"
                    disabled={submitting || success}
                  >
                    {submitting ? <Spinner animation="border" size="sm" /> : <Save size={18} />}
                    {submitting ? 'Saving...' : 'Add Team'}
                  </Button>
                </div>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Form>
    </div>
  );
};

export default RecordTeam;
