import { useEffect, useState } from 'react';
import { Card, Table, Button, Badge, Modal, Form, Spinner, Alert } from 'react-bootstrap';
import { Plus, Edit, Settings, Trash2, Eye, Play, Lock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getEvents, createEvent, updateEvent, changeEventStatus, deleteEvent, openEventRegistration, closeEventRegistration, setupCompetition } from '../../api/hackathonApi';

const listOf = (data) => data?.content || data || [];

// date input holds yyyy-mm-dd; BE wants full datetime
const toDateTime = (v) => (v ? (v.includes('T') ? v : `${v}T00:00:00`) : undefined);
const toDateInput = (v) => (v ? String(v).slice(0, 10) : '');

const EventManagement = () => {
  const navigate = useNavigate();
  const [showModal, setShowModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const emptyEvent = {
    name: '',
    description: '',
    term: '',
    prizePool: '',
    registrationStart: '',
    registrationEnd: '',
    eventStart: '',
    eventEnd: '',
    status: 'draft',
  };

  const [newEvent, setNewEvent] = useState(emptyEvent);
  const [editingEvent, setEditingEvent] = useState(null);
  const [events, setEvents] = useState([]);

  // competition setup wizard state
  const [setupEvent, setSetupEvent] = useState(null);
  const [setupBusy, setSetupBusy] = useState(false);
  const [setupResult, setSetupResult] = useState(null);
  const [tracksText, setTracksText] = useState('');

  const loadEvents = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await getEvents({ size: 100 });
      setEvents(listOf(data));
    } catch (err) {
      setError(err.message || 'Failed to load events');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEvents();
  }, []);

  const handleSaveEvent = async () => {
    if (!newEvent.name) {
      alert('Event name is required');
      return;
    }

    // Date-ordering validation: regStart < regEnd <= eventStart < eventEnd
    const { registrationStart, registrationEnd, eventStart, eventEnd } = newEvent;
    if (registrationStart && registrationEnd && new Date(registrationStart) >= new Date(registrationEnd)) {
      alert('Registration Start must be before Registration End');
      return;
    }
    if (registrationEnd && eventStart && new Date(registrationEnd) > new Date(eventStart)) {
      alert('Registration End must be before or equal to Event Start');
      return;
    }
    if (eventStart && eventEnd && new Date(eventStart) >= new Date(eventEnd)) {
      alert('Event Start must be before Event End');
      return;
    }

    try {
      setSaving(true);
      setError('');
      const payload = {
        title: newEvent.name,
        description: newEvent.description || '',
        term: newEvent.term || '',
        prizePool: newEvent.prizePool || '',
        registrationStart: toDateTime(newEvent.registrationStart),
        registrationEnd: toDateTime(newEvent.registrationEnd),
        eventStart: toDateTime(newEvent.eventStart),
        eventEnd: toDateTime(newEvent.eventEnd),
      };
      if (editingEvent) {
        await updateEvent(editingEvent.id, payload);
        const desiredStatus = (newEvent.status || '').toLowerCase();
        if (desiredStatus && desiredStatus !== (editingEvent.status || '').toLowerCase()) {
          await changeEventStatus(editingEvent.id, desiredStatus);
        }
      } else {
        await createEvent(payload);
      }
      setEditingEvent(null);
      setNewEvent(emptyEvent);
      setShowModal(false);
      await loadEvents();
    } catch (err) {
      setError(err.message || 'Failed to save event');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteEvent = async (id) => {
    if (!window.confirm('Delete this event?')) return;
    try {
      setError('');
      await deleteEvent(id);
      await loadEvents();
    } catch (err) {
      setError(err.message || 'Failed to delete event');
    }
  };

  // --- competition lifecycle ---
  const handleOpenRegistration = async (event) => {
    if (!window.confirm(`Open registration for "${event.title}"? Teams will be able to register.`)) return;
    try {
      setError('');
      await openEventRegistration(event.id);
      await loadEvents();
    } catch (err) {
      setError(err.message || 'Failed to open registration');
    }
  };

  const handleCloseRegistration = async (event) => {
    if (!window.confirm(`Close registration for "${event.title}"? No new teams can join after this.`)) return;
    try {
      setError('');
      await closeEventRegistration(event.id);
      await loadEvents();
    } catch (err) {
      setError(err.message || 'Failed to close registration');
    }
  };

  const openSetup = (event) => {
    setSetupEvent(event);
    setSetupResult(null);
    setTracksText('Qualifier | General, Track B | 3\nFinal | Final Stage | 1');
  };

  const handleSetupCompetition = async () => {
    try {
      setSetupBusy(true);
      setError('');
      const lines = tracksText
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean);
      if (lines.length === 0) throw new Error('Add at least one logical round');
      const roundPlan = lines.map((line, index) => {
        const [namePart, tracksPart, promotePart] = line.split('|').map((part) => part?.trim());
        const trackNames = (tracksPart || '').split(',').map((name) => name.trim()).filter(Boolean);
        const promotionCount = Number(promotePart);
        if (!namePart || trackNames.length === 0 || !Number.isInteger(promotionCount) || promotionCount < 1) {
          throw new Error(`Invalid round plan line ${index + 1}; use: Round name | Track A, Track B | 3`);
        }
        const finalRound = index === lines.length - 1;
        if (finalRound && trackNames.length !== 1) {
          throw new Error('The last (final) logical round must contain exactly one track');
        }
        return {
          name: namePart,
          sequenceNumber: index + 1,
          finalRound,
          defaultTopNToPromote: promotionCount,
          tracks: trackNames.map((name) => ({
            name,
            description: '',
            topNToPromote: promotionCount,
          })),
        };
      });
      const payload = { roundPlan };
      const result = await setupCompetition(setupEvent.id, payload);
      setSetupResult(result);
      await loadEvents();
    } catch (err) {
      setError(err.message || 'Failed to set up competition');
    } finally {
      setSetupBusy(false);
    }
  };

  const openEdit = (event) => {
    setEditingEvent(event);
    setNewEvent({
      ...emptyEvent,
      name: event.title || '',
      description: event.description || '',
      term: event.term || '',
      prizePool: event.prizePool || '',
      registrationStart: toDateInput(event.registrationStart),
      registrationEnd: toDateInput(event.registrationEnd),
      eventStart: toDateInput(event.eventStart),
      eventEnd: toDateInput(event.eventEnd),
      status: event.status || 'draft',
    });
    setShowModal(true);
  };

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Event Management</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Create and configure hackathon events</div>
        </div>
        <Button variant="primary" className="d-flex align-items-center gap-2" onClick={() => {
          setEditingEvent(null);
          setNewEvent(emptyEvent);
          setShowModal(true);
        }}>
          <Plus size={18} /> New Event
        </Button>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className="border-top-0 border-bottom">Event Name</th>
                <th className="border-top-0 border-bottom">Start Date</th>
                <th className="border-top-0 border-bottom">End Date</th>
                <th className="border-top-0 border-bottom">Participants</th>
                <th className="border-top-0 border-bottom">Status</th>
                <th className="border-top-0 border-bottom text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="text-center py-4"><Spinner animation="border" variant="primary" size="sm" /></td></tr>
              ) : events.length === 0 ? (
                <tr><td colSpan={6} className="text-center py-4 text-muted">No events found.</td></tr>
              ) : events.map((event) => {
                const status = (event.status || 'draft').toLowerCase();
                return (
                  <tr key={event.id}>
                    <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{event.title}</td>
                    <td className="py-3">{toDateInput(event.eventStart) || '—'}</td>
                    <td className="py-3">{toDateInput(event.eventEnd) || '—'}</td>
                    <td className="py-3">{event.participantsCount ?? 0}</td>
                    <td className="py-3">
                      <Badge bg={
                        status === 'ongoing' ? 'success' :
                          status === 'published' ? 'info' :
                            status === 'completed' ? 'secondary' :
                              status === 'cancelled' ? 'danger' :
                                'warning'
                      } text={status === 'draft' ? 'dark' : 'light'}>
                        {status === 'published' ? 'Registration open' :
                          status === 'ongoing' ? 'In progress' :
                            (event.status || 'draft')}
                      </Badge>
                    </td>
                    <td className="py-3 text-end">
                      {status === 'draft' && (
                        <Button variant="link" size="sm" className="p-0 text-success" title="Open registration" onClick={() => handleOpenRegistration(event)}>
                          <Play size={16} />
                        </Button>
                      )}
                      {status === 'published' && (
                        <Button variant="link" size="sm" className="p-0 text-warning" title="Close registration" onClick={() => handleCloseRegistration(event)}>
                          <Lock size={16} />
                        </Button>
                      )}
                      {status === 'ongoing' && (event.roundsCount ?? 0) === 0 && (
                        <Button variant="link" size="sm" className="p-0 text-primary" title="Set up competition" onClick={() => openSetup(event)}>
                          <Settings size={16} />
                        </Button>
                      )}
                      <Button variant="link" size="sm" className="p-0 text-primary ms-3" onClick={() => navigate(`/coordinator/events/${event.id}`)}>
                        <Eye size={16} />
                      </Button>
                      <Button variant="link" size="sm" className="p-0 text-secondary ms-3" onClick={() => openEdit(event)}>
                        <Edit size={16} />
                      </Button>
                      <Button variant="link" size="sm" className="p-0 text-danger ms-3" onClick={() => handleDeleteEvent(event.id)}>
                        <Trash2 size={16} />
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        </div>
      </Card>
      <Modal show={showModal} onHide={() => { setShowModal(false); setEditingEvent(null); }}>
        <Modal.Header closeButton>
          <Modal.Title>{editingEvent ? 'Edit Event' : 'Create Event'}</Modal.Title>
        </Modal.Header>

        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Event Name</Form.Label>
              <Form.Control
                type="text"
                value={newEvent.name}
                onChange={(e) => setNewEvent({ ...newEvent, name: e.target.value })}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control
                as="textarea"
                rows={2}
                placeholder="Event description"
                value={newEvent.description}
                onChange={(e) => setNewEvent({ ...newEvent, description: e.target.value })}
              />
            </Form.Group>

            <div className="row">
              <div className="col-md-6">
                <Form.Group className="mb-3">
                  <Form.Label>Term / Semester</Form.Label>
                  <Form.Select
                    value={newEvent.term}
                    onChange={(e) => setNewEvent({ ...newEvent, term: e.target.value })}
                  >
                    <option value="">Select Term</option>
                    <option value="Spring">Spring</option>
                    <option value="Summer">Summer</option>
                    <option value="Fall">Fall</option>
                  </Form.Select>
                </Form.Group>
              </div>
              <div className="col-md-6">
                <Form.Group className="mb-3">
                  <Form.Label>Prize Pool</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="e.g. $5000 or 50,000,000 VND"
                    value={newEvent.prizePool}
                    onChange={(e) => setNewEvent({ ...newEvent, prizePool: e.target.value })}
                  />
                </Form.Group>
              </div>
            </div>

            <div className="row">
              <div className="col-md-6">
                <Form.Group className="mb-3">
                  <Form.Label>Registration Start</Form.Label>
                  <Form.Control
                    type="date"
                    value={newEvent.registrationStart}
                    onChange={(e) => setNewEvent({ ...newEvent, registrationStart: e.target.value })}
                  />
                </Form.Group>
              </div>
              <div className="col-md-6">
                <Form.Group className="mb-3">
                  <Form.Label>Registration End</Form.Label>
                  <Form.Control
                    type="date"
                    value={newEvent.registrationEnd}
                    onChange={(e) => setNewEvent({ ...newEvent, registrationEnd: e.target.value })}
                  />
                </Form.Group>
              </div>
            </div>

            <div className="row">
              <div className="col-md-6">
                <Form.Group className="mb-3">
                  <Form.Label>Event Start</Form.Label>
                  <Form.Control
                    type="date"
                    value={newEvent.eventStart}
                    onChange={(e) => setNewEvent({ ...newEvent, eventStart: e.target.value })}
                  />
                </Form.Group>
              </div>
              <div className="col-md-6">
                <Form.Group className="mb-3">
                  <Form.Label>Event End</Form.Label>
                  <Form.Control
                    type="date"
                    value={newEvent.eventEnd}
                    onChange={(e) => setNewEvent({ ...newEvent, eventEnd: e.target.value })}
                  />
                </Form.Group>
              </div>
            </div>

            {editingEvent && (
              <div className="row mb-3">
                <div className="col-md-4">
                  <Form.Label>Rounds</Form.Label>
                  <Form.Control type="text" readOnly disabled value={editingEvent.roundsCount ?? 0} />
                </div>
                <div className="col-md-4">
                  <Form.Label>Tracks</Form.Label>
                  <Form.Control type="text" readOnly disabled value={editingEvent.tracksCount ?? 0} />
                </div>
                <div className="col-md-4">
                  <Form.Label>Participants</Form.Label>
                  <Form.Control type="text" readOnly disabled value={editingEvent.participantsCount ?? 0} />
                </div>
              </div>
            )}

            {editingEvent && (
              <Form.Group>
                <Form.Label>Status</Form.Label>
                <Form.Select
                  value={newEvent.status}
                  onChange={(e) => setNewEvent({ ...newEvent, status: e.target.value })}
                >
                  <option value="draft">Draft</option>
                  <option value="published">Published (registration open)</option>
                  <option value="ongoing">Ongoing (in progress)</option>
                  <option value="completed">Completed</option>
                  <option value="cancelled">Cancelled</option>
                </Form.Select>
                <Form.Text muted>
                  Prefer the lifecycle buttons (open/close registration) on the list; changing status here is a manual override.
                </Form.Text>
              </Form.Group>
            )}
          </Form>
        </Modal.Body>

        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => { setShowModal(false); setEditingEvent(null); }}
            disabled={saving}
          >
            Cancel
          </Button>

          <Button variant="primary" onClick={handleSaveEvent} disabled={saving}>
            {saving ? 'Saving...' : editingEvent ? 'Update Event' : 'Create Event'}
          </Button>
        </Modal.Footer>
      </Modal>

      <Modal show={!!setupEvent} onHide={() => setSetupEvent(null)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Set up competition{setupEvent ? ` — ${setupEvent.title}` : ''}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {!setupResult ? (
            <>
              <p className="text-muted" style={{ fontSize: '0.9rem' }}>
                Registration is closed with <strong>{setupEvent?.participantsCount ?? 0}</strong> team(s).
                Define the event&apos;s ordered logical rounds and each round&apos;s independent
                track structure. Teams are seeded only into Round 1; later assignments happen
                after promotion. This can only be run once.
              </p>
              <Form.Group className="mb-3">
                <Form.Label>Logical round plan (one round per line)</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={5}
                  placeholder={'Qualifier | AI, Web, Fintech | 3\nSemifinal | Group A, Group B | 2\nFinal | Final Stage | 1'}
                  value={tracksText}
                  onChange={(e) => setTracksText(e.target.value)}
                />
                <Form.Text muted>
                  Format: round name | comma-separated round-specific tracks | promotion count.
                  The last line is the final and must have one track. Track names must be unique across lines.
                </Form.Text>
              </Form.Group>
            </>
          ) : (
            <div>
              <Alert variant="success">
                Built {setupResult.trackCount} track(s) for {setupResult.totalTeams} team(s).
              </Alert>
              {setupResult.tracks?.map((t) => (
                <Card key={t.trackId} className="mb-2">
                  <Card.Body className="py-2">
                    <div className="fw-medium mb-1">{t.name} <span className="text-muted">({t.teamCount} teams)</span></div>
                    <Table size="sm" className="mb-0">
                      <thead>
                        <tr><th>Round</th><th>Promote (topN)</th><th>Seeded</th></tr>
                      </thead>
                      <tbody>
                        {t.rounds?.map((r) => (
                          <tr key={r.roundId}>
                            <td>{r.sequenceNumber}. {r.name}</td>
                            <td>{r.topNToPromote}</td>
                            <td>{r.seededParticipants}</td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </Card.Body>
                </Card>
              ))}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          {!setupResult ? (
            <>
              <Button variant="secondary" onClick={() => setSetupEvent(null)} disabled={setupBusy}>Cancel</Button>
              <Button variant="primary" onClick={handleSetupCompetition} disabled={setupBusy}>
                {setupBusy ? 'Building...' : 'Build competition'}
              </Button>
            </>
          ) : (
            <Button variant="primary" onClick={() => setSetupEvent(null)}>Done</Button>
          )}
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default EventManagement;
