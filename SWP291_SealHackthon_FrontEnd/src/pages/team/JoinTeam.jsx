import { useEffect, useState } from 'react';
import { Card, Button, Form, Row, Col, Badge, InputGroup, Nav, Tab, Alert, Spinner } from 'react-bootstrap';
import { Key, Search, Users, ArrowRight, UserPlus, Clock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { joinTeamByInviteCode, getTeams, getEvents, createJoinRequest, getMyJoinRequests } from '../../api/hackathonApi';

const MAX_TEAM_SIZE = 5;

const JoinTeam = () => {
  const navigate = useNavigate();
  const [inviteCode, setInviteCode] = useState('');
  const [activeTab, setActiveTab] = useState('code');
  const [searchQuery, setSearchQuery] = useState('');
  const [joining, setJoining] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [teams, setTeams] = useState([]);
  const [teamsLoading, setTeamsLoading] = useState(false);
  const [browseError, setBrowseError] = useState('');
  const [requestState, setRequestState] = useState({});
  const [events, setEvents] = useState([]);
  const [eventId, setEventId] = useState('');

  const loadBrowse = async () => {
    setTeamsLoading(true);
    setBrowseError('');
    try {
      const [teamsRes, myReqs] = await Promise.all([
        getTeams({ eventId, size: 200 }),
        getMyJoinRequests().catch(() => []),
      ]);
      const list = teamsRes?.content || teamsRes || [];
      setTeams(Array.isArray(list) ? list : []);
      const reqList = Array.isArray(myReqs) ? myReqs : (myReqs?.content || []);
      const pending = {};
      reqList.forEach((r) => {
        if (String(r.status).toLowerCase() === 'pending') pending[r.teamId] = 'pending';
      });
      setRequestState(pending);
    } catch (err) {
      setBrowseError(err.message || 'Failed to load teams');
    } finally {
      setTeamsLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'browse' && events.length === 0) {
      getEvents({ size: 100 }).then((data) => setEvents(data?.content || data || [])).catch(() => {});
    }
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (activeTab === 'browse' && eventId && !teamsLoading) loadBrowse();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, eventId]);

  const handleJoinViaCode = async (e) => {
    e.preventDefault();
    const code = inviteCode.trim();
    if (!code) return;
    setError('');
    setSuccess('');
    setJoining(true);
    try {
      const team = await joinTeamByInviteCode(code);
      setSuccess(`Joined ${team?.name || 'team'} successfully! Redirecting...`);
      setTimeout(() => navigate('/team/dashboard'), 1500);
    } catch (err) {
      setError(err.message || 'Failed to join. Please check the invite code.');
    } finally {
      setJoining(false);
    }
  };

  const handleSendRequest = async (team) => {
    setRequestState((prev) => ({ ...prev, [team.id]: 'sending' }));
    try {
      await createJoinRequest(team.id);
      setRequestState((prev) => ({ ...prev, [team.id]: 'pending' }));
    } catch (err) {
      setRequestState((prev) => {
        const next = { ...prev };
        delete next[team.id];
        return next;
      });
      setBrowseError(err.message || 'Failed to send request');
    }
  };

  const memberCount = (team) => (Array.isArray(team.members) ? team.members.length : 0);

  const filteredTeams = teams.filter((t) => {
    const q = searchQuery.toLowerCase();
    return (t.name || '').toLowerCase().includes(q);
  });

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Join a Team</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Enter an invite code from a team leader, or request to join a public team.</div>
        </div>
      </div>

      <Tab.Container activeKey={activeTab} onSelect={(k) => setActiveTab(k)}>
        <Nav variant="pills" className="mb-4 d-flex gap-2">
          <Nav.Item>
            <Nav.Link
              eventKey="code"
              className={`px-4 py-2 fw-medium ${activeTab === 'code' ? 'bg-primary text-white' : 'bg-transparent text-muted border'}`}
              style={{ borderRadius: 'var(--cf-radius-md)', cursor: 'pointer' }}
            >
              Have an invite code?
            </Nav.Link>
          </Nav.Item>
          <Nav.Item>
            <Nav.Link
              eventKey="browse"
              className={`px-4 py-2 fw-medium ${activeTab === 'browse' ? 'bg-primary text-white' : 'bg-transparent text-muted border'}`}
              style={{ borderRadius: 'var(--cf-radius-md)', cursor: 'pointer' }}
            >
              Browse public teams
            </Nav.Link>
          </Nav.Item>
        </Nav>

        <Tab.Content>
          <Tab.Pane eventKey="code">
            <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', maxWidth: '600px' }}>
              <Card.Body className="p-5 text-center">
                {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}
                {success && <Alert variant="success">{success}</Alert>}
                <div className="d-inline-flex align-items-center justify-content-center bg-primary-subtle text-primary rounded-circle mb-4" style={{ width: '80px', height: '80px' }}>
                  <Key size={40} />
                </div>
                <h4 className="fw-bold mb-2" style={{ color: 'var(--cf-text-primary)' }}>Enter invite code</h4>
                <p className="text-muted mb-4">
                  Already has a team? Ask the leader for the 6-character invite code and enter it here to join instantly.
                </p>

                <Form onSubmit={handleJoinViaCode} className="mx-auto" style={{ maxWidth: '350px' }}>
                  <InputGroup className="mb-4 shadow-sm" size="lg">
                    <InputGroup.Text className="bg-white border-end-0">
                      <Key size={20} className="text-muted" />
                    </InputGroup.Text>
                    <Form.Control
                      type="text"
                      placeholder="VD: A1B2C3"
                      maxLength={12}
                      value={inviteCode}
                      onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                      className="border-start-0 text-center fw-bold"
                      style={{ letterSpacing: '3px' }}
                      required
                    />
                  </InputGroup>
                  <Button variant="primary" type="submit" size="lg" className="w-100 d-flex align-items-center justify-content-center gap-2" disabled={joining || !!success}>
                    {joining ? <Spinner animation="border" size="sm" /> : <>Tham gia <ArrowRight size={20} /></>}
                  </Button>
                </Form>
              </Card.Body>
            </Card>
          </Tab.Pane>

          <Tab.Pane eventKey="browse">
            <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Card.Body className="p-4">
                <Form.Select className="mb-3" value={eventId} onChange={(e) => { setEventId(e.target.value); setTeams([]); }}>
                  <option value="">Select an event to browse teams</option>
                  {events.map((event) => <option key={event.id} value={event.id}>{event.title}</option>)}
                </Form.Select>
                <div className="d-flex justify-content-between align-items-center mb-4">
                  <h5 className="fw-bold mb-0" style={{ color: 'var(--cf-text-primary)' }}>
                    Teams looking for members
                  </h5>
                  <div style={{ width: '300px' }}>
                    <InputGroup>
                      <InputGroup.Text className="bg-transparent border-end-0">
                        <Search size={16} className="text-muted" />
                      </InputGroup.Text>
                      <Form.Control
                        placeholder="Search by team name..."
                        className="border-start-0 bg-transparent"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                      />
                    </InputGroup>
                  </div>
                </div>

                {browseError && <Alert variant="danger" onClose={() => setBrowseError('')} dismissible>{browseError}</Alert>}

                {teamsLoading ? (
                  <div className="text-center py-5"><Spinner animation="border" /></div>
                ) : (
                  <Row className="g-4">
                    {filteredTeams.map((team) => {
                      const count = memberCount(team);
                      const full = count >= MAX_TEAM_SIZE;
                      const state = requestState[team.id];
                      return (
                        <Col md={6} lg={4} key={team.id}>
                          <Card className="h-100 border transition-hover" style={{ backgroundColor: 'var(--cf-bg-main)' }}>
                            <Card.Body className="p-4 d-flex flex-column">
                              <div className="d-flex justify-content-between align-items-start mb-3">
                                <Badge bg="light" text="dark" className="border">{team.status || 'active'}</Badge>
                                <Badge bg={full ? 'danger' : 'success'}>
                                  {count}/{MAX_TEAM_SIZE} members
                                </Badge>
                              </div>

                              <h5 className="fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>{team.name}</h5>
                              <p className="text-muted small mb-4">Invite code: {team.inviteCode || '—'}</p>

                              <div className="mt-auto pt-3" style={{ borderTop: '1px solid var(--cf-border-color)' }}>
                                {state === 'pending' ? (
                                  <Button variant="outline-secondary" className="w-100 d-flex align-items-center justify-content-center gap-2" disabled>
                                    <Clock size={18} /> Request sent
                                  </Button>
                                ) : (
                                  <Button
                                    variant={full ? 'secondary' : 'outline-primary'}
                                    className="w-100 d-flex align-items-center justify-content-center gap-2"
                                    disabled={full || state === 'sending'}
                                    onClick={() => handleSendRequest(team)}
                                  >
                                    {state === 'sending' ? <Spinner animation="border" size="sm" /> : <UserPlus size={18} />}
                                    {full ? 'Team is full' : 'Request to join'}
                                  </Button>
                                )}
                              </div>
                            </Card.Body>
                          </Card>
                        </Col>
                      );
                    })}

                    {filteredTeams.length === 0 && (
                      <Col xs={12}>
                        <div className="text-center py-5 text-muted">
                          <Users size={48} className="mb-3 opacity-50" />
                          <h5>{searchQuery ? `No teams match "${searchQuery}"` : 'No teams yet'}</h5>
                        </div>
                      </Col>
                    )}
                  </Row>
                )}
              </Card.Body>
            </Card>
          </Tab.Pane>
        </Tab.Content>
      </Tab.Container>
    </div>
  );
};

export default JoinTeam;
