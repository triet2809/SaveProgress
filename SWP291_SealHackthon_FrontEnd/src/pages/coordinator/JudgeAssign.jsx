import React, { useEffect, useState } from 'react';
import { Card, Button, Form, Badge, Row, Col, Spinner, Alert, Tabs, Tab } from 'react-bootstrap';
import { ArrowLeft, UserCheck, Mail, Bookmark, Layers, Grid } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { getRounds, getRoundJudges, assignRoundJudge, deleteRoundJudge, getTracks, getTrackJudges, assignTrackJudge, deleteTrackJudge } from '../../api/hackathonApi';
import { getUsers } from '../../api/userApi';

const listOf = (data) => data?.content || data || [];

const JudgeAssign = () => {
  const navigate = useNavigate();
  const { id } = useParams(); // judge userId

  const [judge, setJudge] = useState(null);
  const [availableRounds, setAvailableRounds] = useState([]);
  // map roundId -> round-judge assignment id (existing BE assignments for this judge)
  const [assignments, setAssignments] = useState({});
  const [selectedRounds, setSelectedRounds] = useState([]);
  // tracks
  const [availableTracks, setAvailableTracks] = useState([]);
  const [trackAssignments, setTrackAssignments] = useState({}); // trackId -> track-judge id
  const [selectedTracks, setSelectedTracks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');
      const [usersRes, roundsData, rjData, tracksData, tjData] = await Promise.all([
        getUsers({}),
        getRounds({ size: 100 }),
        getRoundJudges({ size: 100 }),
        getTracks({ size: 100 }),
        getTrackJudges({ userId: id, size: 100 }),
      ]);
      const users = usersRes.value || [];
      setJudge(users.find((u) => u.id === id) || null);

      setAvailableRounds(listOf(roundsData));

      const rjs = listOf(rjData).filter((rj) => rj.userId === id);
      const map = {};
      rjs.forEach((rj) => { map[rj.roundId] = rj.id; });
      setAssignments(map);
      setSelectedRounds(Object.keys(map));

      setAvailableTracks(listOf(tracksData));
      const tjs = listOf(tjData).filter((tj) => tj.userId === id);
      const tmap = {};
      tjs.forEach((tj) => { tmap[tj.trackId] = tj.id; });
      setTrackAssignments(tmap);
      setSelectedTracks(Object.keys(tmap));
    } catch (err) {
      setError(err.message || 'Failed to load assignment data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleToggleRound = (roundId) => {
    if (selectedRounds.includes(roundId)) {
      setSelectedRounds(selectedRounds.filter((rid) => rid !== roundId));
    } else {
      setSelectedRounds([...selectedRounds, roundId]);
    }
  };

  const handleToggleTrack = (trackId) => {
    if (selectedTracks.includes(trackId)) {
      setSelectedTracks(selectedTracks.filter((tid) => tid !== trackId));
    } else {
      setSelectedTracks([...selectedTracks, trackId]);
    }
  };

  const handleSave = async () => {
    if (!judge) return;
    try {
      setSaving(true);
      setError('');
      // rounds diff
      const existing = Object.keys(assignments);
      const toAdd = selectedRounds.filter((rid) => !existing.includes(rid));
      const toRemove = existing.filter((rid) => !selectedRounds.includes(rid));
      for (const roundId of toAdd) {
        await assignRoundJudge({ roundId, userId: id });
      }
      for (const roundId of toRemove) {
        await deleteRoundJudge(assignments[roundId]);
      }
      // tracks diff
      const existingTracks = Object.keys(trackAssignments);
      const tAdd = selectedTracks.filter((tid) => !existingTracks.includes(tid));
      const tRemove = existingTracks.filter((tid) => !selectedTracks.includes(tid));
      for (const trackId of tAdd) {
        await assignTrackJudge({ trackId, userId: id });
      }
      for (const trackId of tRemove) {
        await deleteTrackJudge(trackAssignments[trackId]);
      }
      navigate('/coordinator/judges');
    } catch (err) {
      setError(err.message || 'Failed to save assignments');
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
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Assign Rounds to Judge</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Select which rounds this judge will evaluate</div>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', marginBottom: '1.5rem' }}>
        <Card.Body className="p-4 d-flex justify-content-between align-items-center">
          <div className="d-flex gap-4 align-items-center">
            <div className="d-flex align-items-center justify-content-center rounded-circle bg-info bg-opacity-10 text-info" style={{ width: '64px', height: '64px' }}>
              <UserCheck size={32} />
            </div>
            <div>
              <h4 className="fw-bold mb-1">{judge?.fullName || 'Unknown Judge'}</h4>
              <div className="text-muted d-flex gap-3 align-items-center">
                <span><Mail size={14} className="me-1" /> {judge?.email || '\u2014'}</span>
                <span><Bookmark size={14} className="me-1" /> {(judge?.roles || []).join(', ') || '\u2014'}</span>
                <Badge bg={(judge?.status || '').toLowerCase() === 'approved' ? 'success' : 'warning'} text={(judge?.status || '').toLowerCase() === 'approved' ? 'light' : 'dark'}>
                  {judge?.status || '\u2014'}
                </Badge>
              </div>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Row className="g-4">
        <Col md={12}>
          <Card className="h-100" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <Tabs defaultActiveKey="rounds" className="mb-4">
                <Tab eventKey="rounds" title={<span className="d-flex align-items-center gap-2"><Layers size={16} /> Rounds</span>}>
                  <div className="text-muted small mb-3">Which rounds will this judge evaluate?</div>
                  {availableRounds.length === 0 ? (
                    <div className="text-muted">No rounds available.</div>
                  ) : (
                    <Form className="d-flex flex-column gap-3">
                      {availableRounds.map((round) => {
                        const isSelected = selectedRounds.includes(round.id);
                        return (
                          <div
                            key={round.id}
                            className={`p-3 rounded border ${isSelected ? 'border-primary bg-primary bg-opacity-10' : 'border-light'}`}
                            style={{ cursor: 'pointer', transition: 'all 0.2s' }}
                            onClick={() => handleToggleRound(round.id)}
                          >
                            <Form.Check
                              type="checkbox"
                              id={`round-${round.id}`}
                              label={<span className="fw-medium ms-2">{round.name}{round.sequenceNumber ? ` (#${round.sequenceNumber})` : ''}</span>}
                              checked={isSelected}
                              onChange={() => {}}
                              className="mb-0"
                            />
                          </div>
                        );
                      })}
                    </Form>
                  )}
                </Tab>
                <Tab eventKey="tracks" title={<span className="d-flex align-items-center gap-2"><Grid size={16} /> Tracks</span>}>
                  <div className="text-muted small mb-3">Which thematic tracks will this judge cover?</div>
                  {availableTracks.length === 0 ? (
                    <div className="text-muted">No tracks available.</div>
                  ) : (
                    <Form className="d-flex flex-column gap-3">
                      {availableTracks.map((track) => {
                        const isSelected = selectedTracks.includes(track.id);
                        return (
                          <div
                            key={track.id}
                            className={`p-3 rounded border ${isSelected ? 'border-primary bg-primary bg-opacity-10' : 'border-light'}`}
                            style={{ cursor: 'pointer', transition: 'all 0.2s' }}
                            onClick={() => handleToggleTrack(track.id)}
                          >
                            <Form.Check
                              type="checkbox"
                              id={`track-${track.id}`}
                              label={<span className="fw-medium ms-2">{track.name}</span>}
                              checked={isSelected}
                              onChange={() => {}}
                              className="mb-0"
                            />
                          </div>
                        );
                      })}
                    </Form>
                  )}
                </Tab>
              </Tabs>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <div className="mt-4 d-flex justify-content-end gap-2">
        <Button variant="secondary" onClick={() => navigate('/coordinator/judges')} disabled={saving}>Cancel</Button>
        <Button variant="primary" onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Save Judge Assignments'}</Button>
      </div>
    </div>
  );
};

export default JudgeAssign;
