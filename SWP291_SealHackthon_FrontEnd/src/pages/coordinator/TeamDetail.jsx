import { useEffect, useState } from 'react';
import { Card, Badge, Button, Row, Col, Table, Spinner, Alert } from 'react-bootstrap';
import { ArrowLeft, Users, FolderOpen, Activity, Ban, RotateCcw } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getTeam, getTrack, disqualifyTeam, reactivateTeam,
  getTeamRecognitionEvidence, recalculateTeamRecognitions, revokeTeamRecognition,
  restoreTeamRecognition,
} from '../../api/hackathonApi';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const TeamDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [team, setTeam] = useState(null);
  const [track, setTrack] = useState(null);
  const [evidence, setEvidence] = useState(null);

  const loadTeam = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await getTeam(id);
      setTeam(data);
      if (data?.teamProfileId) {
        try { setEvidence(await getTeamRecognitionEvidence(data.teamProfileId)); } catch { setEvidence(null); }
      }
      if (data?.trackId) {
        try { setTrack(await getTrack(data.trackId)); } catch { setTrack(null); }
      }
    } catch (err) {
      setError(err.message || 'Failed to load team');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // The loader synchronizes this page with the route's team id.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadTeam();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleDisqualify = async () => {
    const reason = window.prompt('Disqualify this team? Enter a reason:');
    if (reason === null) return;
    try {
      setError('');
      await disqualifyTeam(id, reason || 'Disqualified by coordinator');
      await loadTeam();
    } catch (err) {
      setError(err.message || 'Failed to disqualify team');
    }
  };

  const handleReactivate = async () => {
    if (!window.confirm('Reactivate this team?')) return;
    try {
      setError('');
      await reactivateTeam(id);
      await loadTeam();
    } catch (err) {
      setError(err.message || 'Failed to reactivate team');
    }
  };

  const handleRecalculateRecognition = async () => {
    try {
      setError('');
      setEvidence(await recalculateTeamRecognitions(team.teamProfileId));
      await loadTeam();
    } catch (err) {
      setError(err.message || 'Failed to recalculate recognition');
    }
  };

  const handleRevokeRecognition = async () => {
    const reason = window.prompt('Enter the administrative correction reason:');
    if (!reason?.trim()) return;
    try {
      setError('');
      setEvidence(await revokeTeamRecognition(
        team.teamProfileId, evidence.recognitionId, reason.trim(),
      ));
      await loadTeam();
    } catch (err) {
      setError(err.message || 'Failed to revoke recognition');
    }
  };

  const handleRestoreRecognition = async () => {
    if (!window.confirm('Restore this recognition after coordinator review?')) return;
    try {
      setError('');
      setEvidence(await restoreTeamRecognition(team.teamProfileId, evidence.recognitionId));
      await loadTeam();
    } catch (err) {
      setError(err.message || 'Failed to restore recognition');
    }
  };

  if (loading) {
    return (
      <div className="py-5 d-flex justify-content-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (!team) {
    return (
      <div className="py-2">
        <Alert variant="danger">{error || 'Team not found.'}</Alert>
        <Button variant="link" className="p-0" onClick={() => navigate('/coordinator/teams')}>Back to teams</Button>
      </div>
    );
  }

  const status = (team.status || 'active').toLowerCase();
  const members = team.members || [];

  return (
    <div className="py-2">
      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/coordinator/teams')}>
          <ArrowLeft size={24} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>{team.name}</h1>
          <TeamRecognitionBadge recognitions={team.recognitions} variant="detailed" />
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Team ID: #{team.id}</div>
        </div>
        <div className="ms-auto d-flex align-items-center gap-2">
          <Badge bg={status === 'disqualified' ? 'danger' : 'success'} className="px-3 py-2 fs-6">
            {team.status || 'active'}
          </Badge>
          {status === 'disqualified' ? (
            <Button variant="outline-success" size="sm" className="d-flex align-items-center gap-1" onClick={handleReactivate}>
              <RotateCcw size={14} /> Reactivate
            </Button>
          ) : (
            <Button variant="outline-danger" size="sm" className="d-flex align-items-center gap-1" onClick={handleDisqualify}>
              <Ban size={14} /> Disqualify
            </Button>
          )}
        </div>
      </div>

      <Row className="g-4 mb-4">
        <Col md={8}>
          <Card className="h-100" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4 border-bottom pb-3">Team Members</h5>
              <div className="table-responsive">
                <Table className="mb-0 align-middle" hover>
                  <thead>
                    <tr>
                      <th className="border-top-0 border-bottom text-muted py-2">Name</th>
                      <th className="border-top-0 border-bottom text-muted py-2">Email</th>
                      <th className="border-top-0 border-bottom text-muted py-2 text-end">Role</th>
                    </tr>
                  </thead>
                  <tbody>
                    {members.length === 0 ? (
                      <tr><td colSpan={3} className="text-center py-3 text-muted">No members.</td></tr>
                    ) : members.map((m) => (
                      <tr key={m.id || m.userId}>
                        <td className="py-2 fw-medium">{m.fullName || '—'}</td>
                        <td className="py-2 text-muted">{m.email}</td>
                        <td className="py-2 text-end">
                          <Badge bg={m.role === 'leader' ? 'primary' : 'secondary'}>{m.role}</Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>
              {status === 'disqualified' && team.disqualifiedReason && (
                <div className="mt-3">
                  <div className="text-muted small fw-medium mb-1">Disqualification Reason</div>
                  <div className="text-danger">{team.disqualifiedReason}</div>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col md={4}>
          <Card className="h-100" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4 border-bottom pb-3">Team Information</h5>

              <div className="mb-4">
                <div className="text-muted small fw-medium mb-1 d-flex align-items-center gap-2">
                  <FolderOpen size={16} /> Category / Track
                </div>
                <div><Badge bg="secondary" className="px-2 py-1">{track?.name || 'Unassigned'}</Badge></div>
              </div>

              <div className="mb-4">
                <div className="text-muted small fw-medium mb-1 d-flex align-items-center gap-2">
                  <Users size={16} /> Team Size
                </div>
                <div className="fw-bold fs-4">{members.length} <span className="fs-6 text-muted fw-normal">Members</span></div>
              </div>

              <div className="mb-0">
                <div className="text-muted small fw-medium mb-1 d-flex align-items-center gap-2">
                  <Activity size={16} /> Current Status
                </div>
                <div className="fw-medium text-dark">{team.status || 'active'}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Card className="border-0 shadow-sm">
        <Card.Body className="p-4">
          <div className="d-flex justify-content-between align-items-center gap-3 mb-3">
            <div>
              <h5 className="fw-bold mb-1">Recognition evidence</h5>
              <div className="text-muted small">
                {evidence?.distinctQualifyingSeasons || 0} distinct completed qualifying seasons
              </div>
            </div>
            <div className="d-flex gap-2">
              <Button variant="outline-primary" size="sm" onClick={handleRecalculateRecognition}>
                Recalculate
              </Button>
              {evidence?.recognition?.active && (
                <Button variant="outline-danger" size="sm" onClick={handleRevokeRecognition}>
                  Revoke
                </Button>
              )}
              {evidence?.recognition && !evidence.recognition.active && evidence.recognitionId && (
                <Button variant="outline-success" size="sm" onClick={handleRestoreRecognition}>
                  Restore
                </Button>
              )}
            </div>
          </div>
          <Table responsive className="mb-0">
            <thead>
              <tr><th>Event</th><th>Track</th><th>Final placement</th><th>Completed</th></tr>
            </thead>
            <tbody>
              {!evidence?.qualifyingSeasons?.length ? (
                <tr><td colSpan={4} className="text-center text-muted py-3">No qualifying immutable finishes.</td></tr>
              ) : evidence.qualifyingSeasons.map((season) => (
                <tr key={season.finishId}>
                  <td>{season.eventName}</td>
                  <td>{season.trackName}</td>
                  <td>#{season.finalPlacement}</td>
                  <td>{new Date(season.completedAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
    </div>
  );
};

export default TeamDetail;
