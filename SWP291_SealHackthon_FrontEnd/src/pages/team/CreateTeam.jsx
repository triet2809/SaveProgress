import React, { useEffect, useState } from 'react';
import { Card, Button, Form, Row, Col, Alert, Badge, Spinner } from 'react-bootstrap';
import { Plus, Trash2, Users, Save, AlertTriangle, Key, Copy, Check } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getEvents, getTracks, createTeam, getEventRules, acceptEventRules } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';

const MAX_MEMBERS = 5; // leader + up to 4 teammates
const MAX_EMAIL_SLOTS = MAX_MEMBERS - 1; // 4 email slots

const trackIsFull = (t) => t.maxTeams != null && (t.teamCount ?? 0) >= t.maxTeams;
const trackLabel = (t) => {
  const cap = t.maxTeams != null ? `${t.teamCount ?? 0}/${t.maxTeams}` : `${t.teamCount ?? 0}/∞`;
  return `${t.name} (${cap}${trackIsFull(t) ? ' – full' : ''})`;
};

const CreateTeam = () => {
  const navigate = useNavigate();
  const [success, setSuccess] = useState(false);
  const [createdTeam, setCreatedTeam] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  // Event-first: only events with registration open (status=published) are selectable.
  const [events, setEvents] = useState([]);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [tracks, setTracks] = useState([]);
  const [tracksLoading, setTracksLoading] = useState(false);

  // Thể lệ PUBLIC của sự kiện + trạng thái checkbox chấp thuận.
  // Thí sinh phải tích "I agree" trước khi tạo team; BE cũng ghi nhận việc chấp thuận.
  const [rules, setRules] = useState([]);
  const [acceptedRules, setAcceptedRules] = useState(false);

  const currentUser = getStoredUser() || {};

  const [teamData, setTeamData] = useState({ name: '', eventId: '', trackId: '' });
  // One email slot shown by default (person #2). "+" adds up to 3 more (person #3/#4/#5).
  const [memberEmails, setMemberEmails] = useState(['']);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const res = await getEvents({ status: 'published', size: 100 });
        const list = res?.content || res || [];
        if (!active) return;
        setEvents(list);
      } catch (e) {
        if (active) setError(e.message || 'Failed to load events');
      } finally {
        if (active) setEventsLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  // When an event is picked, load its tracks (with capacity info).
  useEffect(() => {
    if (!teamData.eventId) { setTracks([]); setRules([]); setAcceptedRules(false); return; }
    let active = true;
    setTracksLoading(true);
    (async () => {
      try {
        const res = await getTracks({ eventId: teamData.eventId, size: 100 });
        const list = res?.content || res || [];
        if (!active) return;
        setTracks(list);
      } catch (e) {
        if (active) setError(e.message || 'Failed to load tracks');
      } finally {
        if (active) setTracksLoading(false);
      }
    })();
    return () => { active = false; };
  }, [teamData.eventId]);

  // Tải thể lệ PUBLIC của sự kiện đang chọn; reset checkbox mỗi khi đổi sự kiện.
  useEffect(() => {
    if (!teamData.eventId) return;
    let active = true;
    setAcceptedRules(false);
    (async () => {
      try {
        const list = await getEventRules(teamData.eventId);
        if (active) setRules(Array.isArray(list) ? list : list?.content || []);
      } catch {
        if (active) setRules([]); // Không có thể lệ / lỗi nhẹ không chặn tạo team
      }
    })();
    return () => { active = false; };
  }, [teamData.eventId]);

  const handleTeamChange = (e) => {
    const { name, value } = e.target;
    setTeamData((prev) => {
      // Changing the event clears the previously selected track.
      if (name === 'eventId') return { ...prev, eventId: value, trackId: '' };
      return { ...prev, [name]: value };
    });
  };

  const handleEmailChange = (idx, value) => {
    setMemberEmails((prev) => prev.map((v, i) => (i === idx ? value : v)));
  };

  const addEmailSlot = () => {
    if (memberEmails.length < MAX_EMAIL_SLOTS) setMemberEmails((prev) => [...prev, '']);
  };

  const removeEmailSlot = (idx) => {
    setMemberEmails((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!teamData.name.trim()) { setError('Please enter a team name.'); return; }
    if (!teamData.eventId) { setError('Please select an event that is open for registration.'); return; }
    if (!teamData.trackId) { setError('Please select a track.'); return; }
    const chosenTrack = tracks.find((t) => t.id === teamData.trackId);
    if (chosenTrack && trackIsFull(chosenTrack)) { setError('This track is full, please choose another track.'); return; }
    // Bắt buộc chấp thuận thể lệ (chỉ khi sự kiện có công bố thể lệ PUBLIC).
    if (rules.length > 0 && !acceptedRules) { setError('Please read and agree to the event rules before creating a team.'); return; }

    const emails = memberEmails.map((s) => s.trim()).filter(Boolean);
    // Basic email format check for filled slots.
    const bad = emails.find((em) => !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(em));
    if (bad) { setError(`Invalid email: ${bad}`); return; }

    setSubmitting(true);
    try {
      // Ghi nhận việc chấp thuận thể lệ trước khi tạo team (idempotent ở BE).
      if (rules.length > 0 && acceptedRules) {
        try {
          await acceptEventRules({ eventId: teamData.eventId, accepted: true });
        } catch { /* việc ghi nhận không nên chặn tạo team nếu BE tạm lỗi */ }
      }
      const created = await createTeam({
        trackId: teamData.trackId,
        name: teamData.name.trim(),
        leaderUserId: currentUser.id || undefined,
        memberEmails: emails, // BE resolves to approved users; blank list = solo team
      });
      setCreatedTeam(created);
      setSuccess(true);
    } catch (err) {
      setError(err.message || 'Failed to create team');
    } finally {
      setSubmitting(false);
    }
  };

  const copyCode = async () => {
    const code = createdTeam?.inviteCode;
    if (!code) return;
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch { /* clipboard blocked; user can copy manually */ }
  };

  // Total headcount = leader (1) + filled email slots.
  const filledCount = memberEmails.filter((s) => s.trim()).length;
  const totalMembers = 1 + filledCount;

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Create a Team</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Create a new team. You are the leader; add members now or invite them later with the code.</div>
        </div>
      </div>

      {success ? (
        <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <Card.Body className="p-5 text-center">
            <div className="d-inline-flex align-items-center justify-content-center bg-success-subtle text-success rounded-circle mb-4" style={{ width: '80px', height: '80px' }}>
              <Save size={40} />
            </div>
            <h3 className="fw-bold mb-3" style={{ color: 'var(--cf-text-primary)' }}>Team created successfully!</h3>
            <p className="mb-4 text-muted mx-auto" style={{ maxWidth: '500px' }}>
              Team <strong>{createdTeam?.name || teamData.name}</strong> has been created. You are the Team Leader.
            </p>

            {createdTeam?.inviteCode && (
              <div className="p-4 rounded mb-4 mx-auto" style={{ maxWidth: '420px', backgroundColor: 'var(--cf-bg-main)', border: '1px solid var(--cf-border-color)' }}>
                <div className="text-muted mb-2 text-uppercase fw-bold" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Invite Code</div>
                <div className="d-flex align-items-center justify-content-center gap-2">
                  <Key size={20} className="text-primary" />
                  <span className="fw-bold text-primary" style={{ letterSpacing: '3px', fontSize: '1.5rem' }}>{createdTeam.inviteCode}</span>
                  <Button variant="link" className="p-0 text-secondary" onClick={copyCode} title="Copy">
                    {copied ? <Check size={18} className="text-success" /> : <Copy size={18} />}
                  </Button>
                </div>
              </div>
            )}

            <p className="text-muted mb-4 small">
              Share this code with friends so they can enter it on the Join Team page.
              <br />A team needs 3 to 5 members before the event starts.
            </p>

            <Button variant="primary" className="px-4 py-2" onClick={() => navigate('/team/dashboard')}>
              Go to Team Dashboard
            </Button>
          </Card.Body>
        </Card>
      ) : (
        <Form onSubmit={handleSubmit}>
          {error && <Alert variant="danger" className="mb-4">{error}</Alert>}
          <Row className="g-4">
            <Col lg={4}>
              <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <Card.Body className="p-4">
                  <h5 className="fw-bold mb-4 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
                    <Users size={20} className="text-primary" /> Team Information
                  </h5>

                  <Form.Group className="mb-3">
                    <Form.Label style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--cf-text-secondary)' }}>Team name *</Form.Label>
                    <Form.Control type="text" name="name" value={teamData.name} onChange={handleTeamChange} required placeholder="Enter team name" />
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--cf-text-secondary)' }}>Event *</Form.Label>
                    <Form.Select name="eventId" value={teamData.eventId} onChange={handleTeamChange} disabled={eventsLoading} required>
                      <option value="">{eventsLoading ? 'Loading events...' : 'Select an event open for registration...'}</option>
                      {events.map((ev) => (
                        <option key={ev.id} value={ev.id}>{ev.title}{ev.term ? ` · ${ev.term}` : ''}</option>
                      ))}
                    </Form.Select>
                    {!eventsLoading && events.length === 0 && (
                      <Form.Text className="text-danger">There are no events open for registration right now.</Form.Text>
                    )}
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--cf-text-secondary)' }}>Track *</Form.Label>
                    <Form.Select name="trackId" value={teamData.trackId} onChange={handleTeamChange} disabled={!teamData.eventId || tracksLoading} required>
                      <option value="">
                        {!teamData.eventId ? 'Select an event first' : (tracksLoading ? 'Loading tracks...' : (tracks.length === 0 ? 'This event has no tracks' : 'Select a track...'))}
                      </option>
                      {tracks.map((t) => (
                        <option key={t.id} value={t.id} disabled={trackIsFull(t)}>{trackLabel(t)}</option>
                      ))}
                    </Form.Select>
                  </Form.Group>
                </Card.Body>
              </Card>
            </Col>

            <Col lg={8}>
              <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <Card.Body className="p-4">
                  <div className="d-flex justify-content-between align-items-center mb-4">
                    <h5 className="fw-bold mb-0" style={{ color: 'var(--cf-text-primary)' }}>
                      Members ({totalMembers}/{MAX_MEMBERS})
                    </h5>
                    {memberEmails.length < MAX_EMAIL_SLOTS && (
                      <Button variant="outline-primary" size="sm" className="d-flex align-items-center gap-1" onClick={addEmailSlot}>
                        <Plus size={16} /> Add member
                      </Button>
                    )}
                  </div>

                  <Alert variant="warning" className="py-2 mb-4 d-flex align-items-center gap-2" style={{ fontSize: '0.875rem' }}>
                    <AlertTriangle size={16} />
                    <span><strong>Note:</strong> Enter the emails of members who have registered (and been approved). You can leave these blank and invite later with the code. A team can have up to 5 members.</span>
                  </Alert>

                  <div className="d-flex flex-column gap-3">
                    {/* Leader row (you) */}
                    <div className="p-3 rounded" style={{ border: '1px solid var(--cf-status-info)', backgroundColor: 'var(--cf-bg-main)' }}>
                      <div className="d-flex justify-content-between align-items-center">
                        <span className="fw-bold" style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
                          Member 1 <Badge bg="info" className="ms-2">Team Leader (You)</Badge>
                        </span>
                        <span className="text-muted small">{currentUser.email || currentUser.fullName || 'You'}</span>
                      </div>
                    </div>

                    {/* Email slots (person #2..#5) */}
                    {memberEmails.map((email, idx) => (
                      <div key={idx} className="p-3 rounded position-relative" style={{ border: '1px solid var(--cf-border-color)', backgroundColor: 'var(--cf-bg-main)' }}>
                        <div className="d-flex justify-content-between align-items-center mb-3">
                          <span className="fw-bold" style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
                            Member {idx + 2}
                          </span>
                          <Button variant="link" className="text-danger p-0" onClick={() => removeEmailSlot(idx)} title="Remove this slot">
                            <Trash2 size={16} />
                          </Button>
                        </div>
                        <Form.Group>
                          <Form.Label style={{ fontSize: '0.875rem', color: 'var(--cf-text-secondary)' }}>Member email</Form.Label>
                          <Form.Control
                            type="email"
                            placeholder="teammate@example.com"
                            value={email}
                            onChange={(e) => handleEmailChange(idx, e.target.value)}
                          />
                        </Form.Group>
                      </div>
                    ))}
                  </div>

                  {/* Thể lệ sự kiện (chỉ hiện khi sự kiện có rule PUBLIC) + checkbox đồng ý */}
                  {rules.length > 0 && (
                    <div className="mt-4 p-3 rounded" style={{ border: '1px solid var(--cf-border-color)', backgroundColor: 'var(--cf-bg-main)' }}>
                      <h6 className="fw-bold mb-2" style={{ color: 'var(--cf-text-primary)' }}>Event Rules</h6>
                      <div className="mb-3" style={{ maxHeight: '160px', overflowY: 'auto' }}>
                        {rules.map((r) => (
                          <div key={r.id} className="mb-2">
                            <div className="fw-medium small">{r.title}</div>
                            <div className="text-muted small" style={{ whiteSpace: 'pre-wrap' }}>{r.content}</div>
                          </div>
                        ))}
                      </div>
                      <Form.Check
                        type="checkbox"
                        id="accept-event-rules"
                        checked={acceptedRules}
                        onChange={(e) => setAcceptedRules(e.target.checked)}
                        label="I have read and agree to the event rules"
                      />
                    </div>
                  )}

                  <div className="d-flex justify-content-end mt-4 pt-3" style={{ borderTop: '1px solid var(--cf-border-color)' }}>
                    <Button variant="primary" type="submit" className="px-4 py-2 d-flex align-items-center gap-2" disabled={submitting || (rules.length > 0 && !acceptedRules)}>
                      {submitting ? <Spinner animation="border" size="sm" /> : <Save size={18} />}
                      {submitting ? 'Creating...' : 'Create team'}
                    </Button>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </Form>
      )}
    </div>
  );
};

export default CreateTeam;
