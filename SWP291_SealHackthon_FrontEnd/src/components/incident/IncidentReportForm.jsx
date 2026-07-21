import { useEffect, useState } from 'react';
import { Form, Button, Row, Col, Card, Spinner, Alert } from 'react-bootstrap';
import { Send, AlertTriangle } from 'lucide-react';
import {
  getEvents,
  getTracks,
  getTeams,
  getRounds,
  getSubmissions,
  createIncident,
} from '../../api/hackathonApi';

// Các value phải khớp CHÍNH XÁC với enum IncidentType ở backend
// (cheating, plagiarism, invalid_submission, rule_violation, technical_issue, other).
// Trước đây FE gửi 'conduct'/'rules'/'content' nên backend từ chối vì không thuộc enum.
const INCIDENT_TYPES = [
  { value: 'plagiarism', label: 'Code Plagiarism' },
  { value: 'cheating', label: 'Cheating' },
  { value: 'invalid_submission', label: 'Invalid Submission' },
  { value: 'rule_violation', label: 'Rules Violation (Team Size, Late Submission, etc.)' },
  { value: 'technical_issue', label: 'Technical Issue' },
  { value: 'other', label: 'Other' },
];

const IncidentReportForm = ({ isJudge = false }) => {
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const [events, setEvents] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [teams, setTeams] = useState([]);
  const [rounds, setRounds] = useState([]);
  const [submissions, setSubmissions] = useState([]);

  const [form, setForm] = useState({
    eventId: '',
    roundId: '',
    trackId: '',
    teamId: '',
    submissionId: '',
    type: '',
    title: '',
    description: '',
    evidenceUrl: '',
    severity: '',
  });

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const evRes = await getEvents({ size: 100 }).catch(() => null);
        if (!active) return;
        setEvents(evRes?.content || evRes || []);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load form data');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!form.eventId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setTracks([]); setTeams([]); setRounds([]);
      return;
    }
    let active = true;
    Promise.all([
      getTracks({ eventId: form.eventId, size: 100 }),
      getTeams({ eventId: form.eventId, size: 200 }),
      getRounds({ eventId: form.eventId, size: 200 }),
    ]).then(([trRes, tmRes, rdRes]) => {
      if (!active) return;
      setTracks(trRes?.content || trRes || []);
      setTeams(tmRes?.content || tmRes || []);
      setRounds(rdRes?.content || rdRes || []);
    }).catch((err) => active && setError(err.message || 'Failed to load event scope'));
    return () => { active = false; };
  }, [form.eventId]);

  // load submissions for judge when team selected
  useEffect(() => {
    if (!isJudge || !form.teamId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSubmissions([]);
      return;
    }
    let active = true;
    getSubmissions({ teamId: form.teamId, size: 50 })
      .then((res) => {
        if (active) setSubmissions(res?.content || res || []);
      })
      .catch(() => {
        if (active) setSubmissions([]);
      });
    return () => {
      active = false;
    };
  }, [isJudge, form.teamId]);

  const setField = (name) => (e) => setForm((prev) => ({ ...prev, [name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const description = form.evidenceUrl
        ? `${form.description}\n\nSeverity: ${form.severity || 'n/a'}\nEvidence: ${form.evidenceUrl}`
        : `${form.description}\n\nSeverity: ${form.severity || 'n/a'}`;
      const payload = {
        eventId: form.eventId,
        type: form.type || 'other',
        title: form.title,
        description,
      };
      if (form.teamId) payload.teamId = form.teamId;
      if (form.trackId) payload.trackId = form.trackId;
      if (form.roundId) payload.roundId = form.roundId;
      if (isJudge && form.submissionId) payload.submissionId = form.submissionId;
      await createIncident(payload);
      setSubmitted(true);
    } catch (err) {
      setError(err.message || 'Failed to submit report');
    } finally {
      setSaving(false);
    }
  };

  const resetForm = () => {
    setForm({
      eventId: '',
      roundId: '',
      trackId: '',
      teamId: '',
      submissionId: '',
      type: '',
      title: '',
      description: '',
      evidenceUrl: '',
      severity: '',
    });
    setSubmitted(false);
  };

  if (submitted) {
    return (
      <Card className="text-center p-5 border-0 shadow-sm" style={{ backgroundColor: 'var(--cf-bg-surface)', borderRadius: 'var(--cf-radius-lg)' }}>
        <div className="mx-auto mb-4 bg-success bg-opacity-10 rounded-circle d-flex align-items-center justify-content-center" style={{ width: '80px', height: '80px' }}>
          <Send size={40} className="text-success" />
        </div>
        <h3 className="h4 fw-bold" style={{ color: 'var(--cf-text-primary)' }}>Report Submitted</h3>
        <p style={{ color: 'var(--cf-text-secondary)' }}>
          Your incident report has been securely submitted to the Event Coordinators.
          You will be notified once a decision is made.
        </p>
        <Button variant="primary" className="mt-3" onClick={resetForm}>
          Submit Another Report
        </Button>
      </Card>
    );
  }

  if (loading) {
    return (
      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Card.Body className="p-5 text-center">
          <Spinner animation="border" role="status" />
        </Card.Body>
      </Card>
    );
  }

  return (
    <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
      <Card.Body className="p-4">
        <Form onSubmit={handleSubmit}>
          <div className="mb-4">
            <h5 className="fw-bold mb-3 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
              <AlertTriangle size={20} className="text-warning" />
              Report an Incident or Violation
            </h5>
            <p style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
              Please provide detailed information regarding the rule violation. False reporting may result in penalties.
            </p>
          </div>

          {error && <Alert variant="danger">{error}</Alert>}

          <Row className="g-3 mb-4">
            <Col md={6}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Event</Form.Label>
                <Form.Select required value={form.eventId} onChange={setField('eventId')}>
                  <option value="">Select Event...</option>
                  {events.map((ev) => (
                    <option key={ev.id} value={ev.id}>{ev.title}</option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col md={6}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Round</Form.Label>
                <Form.Select value={form.roundId} onChange={setField('roundId')}>
                  <option value="">Select Round...</option>
                  {rounds
                    .filter((r) => !form.eventId || r.eventId === form.eventId)
                    .map((r) => (
                      <option key={r.id} value={r.id}>{r.name}</option>
                    ))}
                </Form.Select>
              </Form.Group>
            </Col>

            <Col md={isJudge ? 4 : 6}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Category</Form.Label>
                <Form.Select value={form.trackId} onChange={setField('trackId')}>
                  <option value="">Select Category...</option>
                  {tracks
                    .filter((t) => !form.eventId || t.eventId === form.eventId)
                    .map((t) => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col md={isJudge ? 4 : 6}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Team</Form.Label>
                <Form.Select value={form.teamId} onChange={setField('teamId')}>
                  <option value="">Select Team...</option>
                  {teams
                    .filter((t) => !form.trackId || t.trackId === form.trackId)
                    .map((t) => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                </Form.Select>
              </Form.Group>
            </Col>

            {isJudge && (
              <Col md={4}>
                <Form.Group>
                  <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Submission</Form.Label>
                  <Form.Select value={form.submissionId} onChange={setField('submissionId')}>
                    <option value="">Select Submission...</option>
                    {submissions.map((s) => (
                      <option key={s.id} value={s.id}>{s.teamName || s.id}</option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
            )}

            <Col md={12}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Incident Type</Form.Label>
                <Form.Select required value={form.type} onChange={setField('type')}>
                  <option value="">Select Type...</option>
                  {INCIDENT_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>

            <Col md={12}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Title</Form.Label>
                <Form.Control required type="text" placeholder="Brief summary of the incident" value={form.title} onChange={setField('title')} />
              </Form.Group>
            </Col>

            <Col md={12}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Description</Form.Label>
                <Form.Control required as="textarea" rows={5} placeholder="Provide a detailed description of the incident..." value={form.description} onChange={setField('description')} />
              </Form.Group>
            </Col>

            <Col md={8}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Evidence URL</Form.Label>
                <Form.Control type="url" placeholder="Link to repository, screenshot, or other evidence" value={form.evidenceUrl} onChange={setField('evidenceUrl')} />
              </Form.Group>
            </Col>

            <Col md={4}>
              <Form.Group>
                <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Severity</Form.Label>
                <Form.Select value={form.severity} onChange={setField('severity')}>
                  <option value="">Select Severity...</option>
                  <option value="low">Low (Minor Infraction)</option>
                  <option value="medium">Medium (Rule Violation)</option>
                  <option value="high">High (Disqualifiable Offense)</option>
                </Form.Select>
              </Form.Group>
            </Col>
          </Row>

          <div className="d-flex justify-content-end gap-3 mt-4 pt-3 border-top">
            <Button variant="light" type="button" onClick={resetForm}>Cancel</Button>
            <Button variant="danger" type="submit" className="d-flex align-items-center gap-2" disabled={saving}>
              <Send size={18} /> {saving ? 'Submitting...' : 'Submit Report'}
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
};

export default IncidentReportForm;
