import { useEffect, useState } from 'react';
import { Card, Table, Button, Badge, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Search, Mail, Edit, UserPlus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getRounds, getRoundJudges, getTracks } from '../../api/hackathonApi';
import EventSelector from '../../components/coordinator/EventSelector';
import { useSearchParams } from 'react-router-dom';

const JudgeManagement = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const roundId = searchParams.get('roundId') || '';
  const trackId = searchParams.get('trackId') || '';
  const [rounds, setRounds] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [judges, setJudges] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setLoading(true);
        setError('');
        const [roundData, trackData, assignmentData] = await Promise.all([
          getRounds({ eventId, size: 200 }),
          getTracks({ eventId, size: 200 }),
          getRoundJudges({ eventId, ...(roundId ? { roundId } : {}), ...(trackId ? { trackId } : {}), size: 500 }),
        ]);
        if (active) {
          setRounds(roundData.content || roundData || []);
          setTracks(trackData.content || trackData || []);
          setJudges((assignmentData.content || assignmentData || []).map((assignment) => ({
            id: assignment.userId, fullName: assignment.fullName, email: assignment.email,
            roles: ['judge'], status: 'approved', roundName: assignment.roundName,
          })));
        }
      } catch (err) {
        if (active) setError(err.message || 'Failed to load judges');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [eventId, roundId, trackId]);

  const filteredJudges = (judges || []).filter(
    (judge) =>
      (judge.fullName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (judge.email || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <>
    <EventSelector />
    {eventId && <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Judge Management</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Manage judges and evaluation panels</div>
        </div>
        <Button variant="primary" onClick={() => navigate('/coordinator/judges/new')}>Invite Judge</Button>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between gap-2">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text className="bg-transparent border-end-0">
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control className="border-start-0" placeholder="Search judges..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
          </InputGroup>
          <Form.Select value={trackId} disabled={!roundId} onChange={(e) => {
            const next = new URLSearchParams(searchParams);
            if (e.target.value) next.set('trackId', e.target.value); else next.delete('trackId');
            next.delete('roundId'); setSearchParams(next);
          }} style={{ maxWidth: 220 }}>
            <option value="">All Tracks</option>
            {tracks.filter((track) => !roundId || rounds.find((round) => round.id === roundId)?.trackId === track.id)
              .map((track) => <option key={track.id} value={track.id}>{track.name}</option>)}
          </Form.Select>
          <Form.Select value={roundId} onChange={(e) => {
            const next = new URLSearchParams(searchParams);
            if (e.target.value) {
              next.set('roundId', e.target.value);
              const selected = rounds.find((round) => round.id === e.target.value);
              if (selected?.trackId) next.set('trackId', selected.trackId);
            } else {
              next.delete('roundId'); next.delete('trackId');
            }
            setSearchParams(next);
          }} style={{ maxWidth: 220 }}>
            <option value="">All Rounds</option>
            {rounds.filter((round) => !trackId || round.trackId === trackId).map((round) => (
              <option key={round.id} value={round.id}>{round.name}</option>
            ))}
          </Form.Select>
        </div>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className="border-top-0 border-bottom">Name</th>
                <th className="border-top-0 border-bottom">Email</th>
                <th className="border-top-0 border-bottom">Roles</th>
                <th className="border-top-0 border-bottom">Status</th>
                <th className="border-top-0 border-bottom text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={5} className="text-center py-4"><Spinner animation="border" variant="primary" size="sm" /></td></tr>
              ) : filteredJudges.length === 0 ? (
                <tr><td colSpan={5} className="text-center py-4 text-muted">No judges found.</td></tr>
              ) : filteredJudges.map((judge) => {
                const status = (judge.status || '').toLowerCase();
                return (
                  <tr key={judge.id}>
                    <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{judge.fullName || '-'}</td>
                    <td className="py-3" style={{ color: 'var(--cf-text-secondary)' }}>{judge.email}</td>
                    <td className="py-3">
                      <div className="d-flex flex-wrap gap-1">
                        {(judge.roles || []).map((role, idx) => (
                          <Badge key={idx} bg="secondary">{role}</Badge>
                        ))}
                      </div>
                    </td>
                    <td className="py-3">
                      <Badge bg={status === 'approved' ? 'success' : status === 'pending' ? 'warning' : 'secondary'} text={status === 'pending' ? 'dark' : 'light'}>
                        {judge.status || '-'}
                      </Badge>
                    </td>
                    <td className="py-3 text-end">
                      <Button variant="link" size="sm" className="p-0 text-secondary me-3" onClick={() => window.open(`mailto:${judge.email}`)}>
                        <Mail size={16} />
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        className="p-0 text-success me-3"
                        onClick={() => navigate(`/coordinator/judges/${judge.id}/assign?eventId=${eventId}${roundId ? `&roundId=${roundId}` : ''}${trackId ? `&trackId=${trackId}` : ''}`)}
                      >
                        <UserPlus size={16} />
                      </Button>
                      <Button variant="link" size="sm" className="p-0 text-primary" onClick={() => navigate(`/coordinator/judges/${judge.id}/edit?eventId=${eventId}`)}>
                        <Edit size={16} />
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>}
    </>
  );
};

export default JudgeManagement;
