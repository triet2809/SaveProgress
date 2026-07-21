import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Col, Form, Row, Spinner } from 'react-bootstrap';
import { ArrowLeft, RotateCcw, Search } from 'lucide-react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  getEvents,
  getMyTeamProfiles,
  getTracks,
  previewTeamReactivation,
  reactivateTeamProfile,
} from '../../api/hackathonApi';
import HistoricalRosterReview from '../../components/team/HistoricalRosterReview';
import ReactivationEligibilitySummary from '../../components/team/ReactivationEligibilitySummary';

const listOf = (value) => value?.content || value || [];
const historicalStatuses = new Set(['ongoing', 'completed', 'cancelled']);

const TeamReactivationForm = () => {
  const { profileId } = useParams();
  const { pathname } = useLocation();
  const routeBase = pathname.startsWith('/team/') ? '/team' : '/student';
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [events, setEvents] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [form, setForm] = useState({
    sourceTeamId: '', targetEventId: '', targetTrackId: '', returningMemberIds: [], leaderId: '',
  });
  const [preview, setPreview] = useState(null);
  const [confirmed, setConfirmed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');

  const historicalRegistrations = useMemo(
    () => (profile?.previousRegistrations || []).filter((item) =>
      historicalStatuses.has(item.eventStatus) && item.currentUserHistoricalRole === 'leader'),
    [profile],
  );
  const source = historicalRegistrations.find((item) => item.historicalTeamId === form.sourceTeamId);

  useEffect(() => {
    let active = true;
    Promise.all([getMyTeamProfiles(), getEvents({ status: 'published', size: 100 })])
      .then(([profileData, eventData]) => {
        if (!active) return;
        const selectedProfile = listOf(profileData).find((item) => item.teamProfileId === profileId);
        setProfile(selectedProfile || null);
        setEvents(listOf(eventData));
        const firstSource = selectedProfile?.previousRegistrations?.find((item) =>
          historicalStatuses.has(item.eventStatus) && item.currentUserHistoricalRole === 'leader');
        if (firstSource) {
          setForm((current) => ({
            ...current,
            sourceTeamId: firstSource.historicalTeamId,
            returningMemberIds: [],
            leaderId: '',
          }));
        }
      })
      .catch((err) => setError(err.message || 'Failed to load reactivation data'))
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [profileId]);

  useEffect(() => {
    if (!form.targetEventId) return;
    let active = true;
    getTracks({ eventId: form.targetEventId, size: 100 })
      .then((data) => { if (active) setTracks(listOf(data)); })
      .catch((err) => { if (active) setError(err.message || 'Failed to load tracks'); });
    return () => { active = false; };
  }, [form.targetEventId]);

  const resetPreview = (next) => {
    setForm(next);
    setPreview(null);
    setConfirmed(false);
  };

  const chooseSource = (sourceTeamId) => {
    resetPreview({ ...form, sourceTeamId, returningMemberIds: [], leaderId: '' });
  };

  const toggleMember = (userId) => {
    const selected = form.returningMemberIds.includes(userId);
    const returningMemberIds = selected
      ? form.returningMemberIds.filter((id) => id !== userId)
      : [...form.returningMemberIds, userId];
    const leaderId = selected && form.leaderId === userId ? '' : form.leaderId;
    resetPreview({ ...form, returningMemberIds, leaderId });
  };

  const payload = () => ({
    sourceTeamId: form.sourceTeamId,
    targetEventId: form.targetEventId,
    targetTrackId: form.targetTrackId,
    returningMemberIds: form.returningMemberIds,
    leaderId: form.leaderId,
  });

  const runPreview = async () => {
    setError('');
    setWorking(true);
    try {
      setPreview(await previewTeamReactivation(profileId, payload()));
    } catch (err) {
      setError(err.message || 'Failed to preview reactivation');
    } finally {
      setWorking(false);
    }
  };

  const execute = async () => {
    setError('');
    setWorking(true);
    try {
      await reactivateTeamProfile(profileId, payload());
      navigate('/team/my-team', { replace: true });
    } catch (err) {
      setError(err.message || 'Failed to reactivate team');
      setPreview(null);
      setConfirmed(false);
    } finally {
      setWorking(false);
    }
  };

  if (loading) return <div className="py-5 text-center"><Spinner /></div>;
  if (!profile) return <Alert variant="danger">Team profile not found or not available to this account.</Alert>;

  return (
    <div className="py-2">
      <Button as={Link} to={`${routeBase}/previous-teams`} variant="link" className="px-0 mb-3">
        <ArrowLeft size={16} className="me-1" />Previous teams
      </Button>
      <h1 className="h3 fw-bold mb-1">Reactivate {profile.canonicalName}</h1>
      <p className="text-muted mb-4">This creates a new event registration. Historical rosters and competition records remain unchanged.</p>
      {error && <Alert variant="danger">{error}</Alert>}

      <Row className="g-4">
        <Col lg={5}>
          <Card className="border-0 shadow-sm">
            <Card.Body className="p-4">
              <h2 className="h5 fw-bold mb-3">Registration</h2>
              <Form.Group className="mb-3">
                <Form.Label>Historical source registration</Form.Label>
                <Form.Select value={form.sourceTeamId} onChange={(event) => chooseSource(event.target.value)}>
                  {historicalRegistrations.map((item) => (
                    <option key={item.historicalTeamId} value={item.historicalTeamId}>
                      {item.eventName} · {item.trackName}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Target event</Form.Label>
                <Form.Select
                  value={form.targetEventId}
                  onChange={(event) => {
                    setTracks([]);
                    resetPreview({ ...form, targetEventId: event.target.value, targetTrackId: '' });
                  }}
                >
                  <option value="">Select an open event</option>
                  {events.filter((item) => item.id !== source?.eventId).map((item) => (
                    <option key={item.id} value={item.id}>{item.title || item.name}</option>
                  ))}
                </Form.Select>
              </Form.Group>
              <Form.Group>
                <Form.Label>Target track</Form.Label>
                <Form.Select
                  value={form.targetTrackId}
                  onChange={(event) => resetPreview({ ...form, targetTrackId: event.target.value })}
                  disabled={!form.targetEventId}
                >
                  <option value="">Select a track</option>
                  {tracks.map((track) => (
                    <option key={track.id} value={track.id}>
                      {track.name}{track.maxTeams != null ? ` (${track.teamCount || 0}/${track.maxTeams})` : ''}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={7}>
          <Card className="border-0 shadow-sm">
            <Card.Body className="p-4">
              <h2 className="h5 fw-bold mb-2">Returning roster</h2>
              <p className="text-muted small">Select 3–5 historical members and designate exactly one returning member as leader.</p>
              <HistoricalRosterReview
                roster={source?.historicalRoster || []}
                selectedIds={form.returningMemberIds}
                leaderId={form.leaderId}
                onMemberToggle={toggleMember}
                onLeaderChange={(leaderId) => resetPreview({ ...form, leaderId })}
                disabled={working}
              />
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <ReactivationEligibilitySummary preview={preview} />

      <div className="d-flex flex-wrap align-items-center gap-3 mt-4">
        <Button
          variant="outline-primary"
          onClick={runPreview}
          disabled={working || !form.sourceTeamId || !form.targetEventId || !form.targetTrackId || !form.leaderId}
        >
          <Search size={16} className="me-2" />Preview eligibility
        </Button>
        {preview?.eligible && (
          <>
            <Form.Check
              checked={confirmed}
              onChange={(event) => setConfirmed(event.target.checked)}
              label="I confirm this new event roster"
            />
            <Button onClick={execute} disabled={working || !confirmed}>
              <RotateCcw size={16} className="me-2" />Create new registration
            </Button>
          </>
        )}
      </div>
    </div>
  );
};

export default TeamReactivationForm;
