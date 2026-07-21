import { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Badge, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Search, Mail, Edit, UserPlus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getTracks, getTrackMentors } from '../../api/hackathonApi';
import EventSelector from '../../components/coordinator/EventSelector';
import { useSearchParams } from 'react-router-dom';

const asArray = (data) => data?.content || data || [];

const MentorManagement = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const trackId = searchParams.get('trackId') || '';
  const [tracks, setTracks] = useState([]);
  const [mentors, setMentors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const load = async () => {
    try {
      setLoading(true);
      setError('');
      const [tracksRes, tmRes] = await Promise.all([
        getTracks({ eventId, size: 200 }),
        getTrackMentors({ eventId, ...(trackId ? { trackId } : {}), size: 500 }),
      ]);
      const scopedTracks = asArray(tracksRes);
      setTracks(scopedTracks);
      const trackName = {};
      scopedTracks.forEach((t) => { trackName[t.id] = t.name; });
      // derive category (track names) per mentor from track-mentors
      const catByUser = {};
      asArray(tmRes).forEach((tm) => {
        const list = catByUser[tm.userId] || (catByUser[tm.userId] = []);
        const name = trackName[tm.trackId] || tm.trackName;
        if (name) list.push(name);
      });
      const byUser = new Map();
      asArray(tmRes).forEach((tm) => byUser.set(tm.userId, {
        id: tm.userId,
        name: tm.fullName || tm.email,
        email: tm.email,
        category: (catByUser[tm.userId] || []).join(', ') || '—',
        assignedTeams: catByUser[tm.userId] || [],
        status: 'Active',
      }));
      setMentors([...byUser.values()]);
    } catch (err) {
      setError(err.message || 'Failed to load mentors');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Synchronize records with the URL-selected scope.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (eventId) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId, trackId]);

  const filteredMentors = useMemo(() => mentors.filter((mentor) =>
    mentor.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    mentor.email.toLowerCase().includes(searchTerm.toLowerCase())
  ), [mentors, searchTerm]);

  return (
    <>
    <EventSelector />
    {eventId && <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Mentor Management</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Assign mentors to categories and teams</div>
        </div>
        <Button
          variant="primary"
          onClick={() => navigate('/coordinator/mentors/new')}
        >
          Invite Mentor
        </Button>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between gap-2">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text className="bg-transparent border-end-0">
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control
              className="border-start-0"
              placeholder="Search mentors..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </InputGroup>
          <Form.Select value={trackId} onChange={(e) => {
            const next = new URLSearchParams(searchParams);
            if (e.target.value) next.set('trackId', e.target.value); else next.delete('trackId');
            setSearchParams(next);
          }} style={{ maxWidth: 240 }}>
            <option value="">All Tracks</option>
            {tracks.map((track) => <option key={track.id} value={track.id}>{track.name}</option>)}
          </Form.Select>
        </div>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className="border-top-0 border-bottom">Name</th>
                <th className="border-top-0 border-bottom">Email</th>
                <th className="border-top-0 border-bottom">Category</th>
                <th className="border-top-0 border-bottom">Assigned Teams</th>
                <th className="border-top-0 border-bottom">Status</th>
                <th className="border-top-0 border-bottom text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={6} className="text-center py-4"><Spinner animation="border" size="sm" /></td></tr>
              )}
              {!loading && filteredMentors.length === 0 && (
                <tr><td colSpan={6} className="text-center py-4 text-muted">No mentors found.</td></tr>
              )}
              {!loading && filteredMentors.map((mentor) => (
                <tr key={mentor.id}>
                  <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{mentor.name}</td>
                  <td className="py-3" style={{ color: 'var(--cf-text-secondary)' }}>{mentor.email}</td>
                  <td className="py-3">{mentor.category}</td>
                  <td className="py-3">
                    <div className="d-flex flex-wrap gap-1">
                      {mentor.assignedTeams && mentor.assignedTeams.map((team, idx) => (
                        <Badge key={idx} bg="info" text="dark">{team}</Badge>
                      ))}
                    </div>
                  </td>
                  <td className="py-3">
                    <Badge bg={mentor.status === 'Active' ? 'success' : 'secondary'}>{mentor.status}</Badge>
                  </td>
                  <td className="py-3 text-end">
                    <Button
                      variant="link"
                      size="sm"
                      className="p-0 text-secondary me-3"
                      onClick={() => window.open(`mailto:${mentor.email}`)}
                    >
                      <Mail size={16} />
                    </Button>
                    <Button
                      variant="link"
                      size="sm"
                      className="p-0 text-success me-3"
                      onClick={() => navigate(`/coordinator/mentors/${mentor.id}/assign?eventId=${eventId}${trackId ? `&trackId=${trackId}` : ''}`)}
                    >
                      <UserPlus size={16} />
                    </Button>
                    <Button
                      variant="link"
                      size="sm"
                      className="p-0 text-primary"
                      onClick={() => navigate(`/coordinator/mentors/${mentor.id}/edit?eventId=${eventId}`)}
                    >
                      <Edit size={16} />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      </Card>

    </div>}
    </>
  );
};

export default MentorManagement;
