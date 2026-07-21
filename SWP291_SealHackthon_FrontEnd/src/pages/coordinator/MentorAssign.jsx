import React, { useEffect, useMemo, useState } from 'react';
import { Card, Button, Form, Badge, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { ArrowLeft, Users, Mail, Bookmark } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { getTracks, getTrackMentors, assignTrackMentor, deleteTrackMentor } from '../../api/hackathonApi';
import { getUsers } from '../../api/userApi';

const asArray = (data) => data?.content || data || [];

// BE model links a mentor (userId) to a Track (== FE "Category") via track-mentors,
// not to individual teams. So this page assigns the mentor to tracks.
const MentorAssign = () => {
  const navigate = useNavigate();
  const { id } = useParams(); // mentor userId

  const [mentor, setMentor] = useState(null);
  const [tracks, setTracks] = useState([]);
  const [assignments, setAssignments] = useState([]); // track-mentor rows for this user
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = async () => {
    try {
      setLoading(true);
      setError('');
      const [usersRes, tracksRes, tmRes] = await Promise.all([
        getUsers({ role: 'mentor' }).catch(() => ({ value: [] })),
        getTracks({ size: 200 }).catch(() => null),
        getTrackMentors({ size: 500 }).catch(() => null),
      ]);
      const users = usersRes.value || [];
      setMentor(users.find((u) => u.id === id) || { id, fullName: id, email: '', roles: ['mentor'] });
      setTracks(asArray(tracksRes));
      setAssignments(asArray(tmRes).filter((tm) => tm.userId === id));
    } catch (err) {
      setError(err.message || 'Failed to load mentor assignment data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const assignmentByTrack = useMemo(() => {
    const map = {};
    assignments.forEach((tm) => { map[tm.trackId] = tm; });
    return map;
  }, [assignments]);

  const handleToggleTrack = async (track) => {
    setSaving(true);
    setError('');
    try {
      const existing = assignmentByTrack[track.id];
      if (existing) {
        await deleteTrackMentor(existing.id);
      } else {
        await assignTrackMentor({ trackId: track.id, userId: id });
      }
      await load();
    } catch (err) {
      setError(err.message || 'Failed to update assignment');
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

  const selectedCount = assignments.length;

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/coordinator/mentors')}>
          <ArrowLeft size={24} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Assign Categories to Mentor</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Select which categories (tracks) this mentor will guide</div>
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', marginBottom: '1.5rem' }}>
        <Card.Body className="p-4 d-flex justify-content-between align-items-center">
          <div className="d-flex gap-4 align-items-center">
            <div className="d-flex align-items-center justify-content-center rounded-circle bg-primary bg-opacity-10 text-primary" style={{ width: '64px', height: '64px' }}>
              <Users size={32} />
            </div>
            <div>
              <h4 className="fw-bold mb-1">{mentor?.fullName || mentor?.email || 'Mentor'}</h4>
              <div className="text-muted d-flex gap-3 align-items-center">
                {mentor?.email && <span><Mail size={14} className="me-1" /> {mentor.email}</span>}
                <span><Bookmark size={14} className="me-1" /> {selectedCount} categories</span>
                <Badge bg="success">Mentor</Badge>
              </div>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Card.Header className="bg-transparent border-bottom p-4">
          <h5 className="fw-bold mb-0">Available Categories</h5>
          <div className="text-muted small mt-1">Toggle a category to assign or unassign this mentor.</div>
        </Card.Header>
        <Card.Body className="p-4">
          <Row className="g-3">
            {tracks.length === 0 && (
              <Col xs={12}><div className="text-muted">No categories available.</div></Col>
            )}
            {tracks.map((track) => {
              const isSelected = Boolean(assignmentByTrack[track.id]);
              return (
                <Col md={6} lg={4} key={track.id}>
                  <div
                    className={`p-3 rounded border ${isSelected ? 'border-primary bg-primary bg-opacity-10' : 'border-light'}`}
                    style={{ cursor: saving ? 'wait' : 'pointer', transition: 'all 0.2s' }}
                    onClick={() => !saving && handleToggleTrack(track)}
                  >
                    <Form.Check
                      type="checkbox"
                      id={`track-${track.id}`}
                      label={
                        <div className="ms-2">
                          <div className="fw-bold text-dark">{track.name}</div>
                          <div className="text-muted small">{track.description || '—'}</div>
                        </div>
                      }
                      checked={isSelected}
                      onChange={() => {}}
                    />
                  </div>
                </Col>
              );
            })}
          </Row>
        </Card.Body>
        <Card.Footer className="bg-transparent border-top p-4 d-flex justify-content-end gap-2">
          <Button variant="secondary" onClick={() => navigate('/coordinator/mentors')} disabled={saving}>Done</Button>
          <span className="align-self-center text-muted small">{selectedCount} assigned</span>
        </Card.Footer>
      </Card>
    </div>
  );
};

export default MentorAssign;
