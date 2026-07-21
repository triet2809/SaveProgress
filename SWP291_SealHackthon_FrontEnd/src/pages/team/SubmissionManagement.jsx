import { useEffect, useState } from 'react';
import { Card, Form, Row, Col, Button, Spinner, Alert } from 'react-bootstrap';
import { AlertCircle } from 'lucide-react';
import { getMyTeams, getTrack, getRounds, getSubmissions, upsertSubmission, updateSubmission } from '../../api/hackathonApi';
import styles from './SubmissionManagement.module.css';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const SubmissionManagement = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState('');

  const [team, setTeam] = useState(null);
  const [trackName, setTrackName] = useState('');
  const [round, setRound] = useState(null);
  const [submission, setSubmission] = useState(null);

  const [formData, setFormData] = useState({
    repoUrl: '',
    demoUrl: '',
    slideUrl: '',
    reportUrl: '',
    apiMetadata: '',
  });

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : teams?.content || [];
        const current = list[0] || null;
        if (!active) return;
        setTeam(current);
        if (!current?.trackId || !current?.id) {
          setLoading(false);
          return;
        }
        getTrack(current.trackId).then((t) => active && setTrackName(t?.name || '')).catch(() => {});

        const roundsRes = await getRounds({ trackId: current.trackId });
        const rounds = (roundsRes?.content || roundsRes || [])
          .slice()
          .sort((a, b) => (a.sequenceNumber || 0) - (b.sequenceNumber || 0));
        // Current round = earliest with a future deadline, else last round.
        const now = new Date();
        const upcoming = rounds.find((r) => r.submissionDeadline && new Date(r.submissionDeadline) >= now);
        const currentRound = upcoming || rounds[rounds.length - 1] || null;
        if (active) setRound(currentRound);

        const subsRes = await getSubmissions({ teamId: current.id });
        const subs = subsRes?.content || subsRes || [];
        const sub = currentRound
          ? subs.find((s) => s.roundId === currentRound.id) || null
          : subs[0] || null;
        if (active && sub) {
          setSubmission(sub);
          setFormData({
            repoUrl: sub.repoUrl || '',
            demoUrl: sub.demoUrl || '',
            slideUrl: sub.slideUrl || '',
            reportUrl: sub.reportUrl || '',
            apiMetadata: sub.apiMetadata && sub.apiMetadata !== '{}' ? sub.apiMetadata : '',
          });
        }
      } catch (e) {
        if (active) setError(e.message || 'Failed to load submission');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const save = async (status) => {
    if (!team?.id || !round?.id) {
      setError('No active round to submit to.');
      return;
    }
    setSaving(true);
    setSaved('');
    setError('');
    try {
      const payload = { ...formData, status };
      if (submission?.id) {
        const updated = await updateSubmission(submission.id, payload);
        setSubmission(updated);
      } else {
        const created = await upsertSubmission({ roundId: round.id, teamId: team.id, ...payload });
        setSubmission(created);
      }
      setSaved(status === 'submitted' ? 'Submission sent.' : 'Draft saved.');
    } catch (e) {
      setError(e.message || 'Failed to save submission');
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    save('submitted');
  };

  if (loading) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  const deadlineText = round?.submissionDeadline
    ? new Date(round.submissionDeadline).toLocaleString()
    : 'TBD';

  return (
    <div className="py-2">
      <TeamRecognitionBadge recognitions={team?.recognitions} variant="detailed" className="mb-3" />
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Submission Management</h1>
        <div className={styles.pageSubtitle}>
          Upload and manage your hackathon project
        </div>
      </div>

      {error && <Alert variant="danger" className="mb-3">{error}</Alert>}
      {saved && <Alert variant="success" className="mb-3">{saved}</Alert>}
      {!team && <Alert variant="info" className="mb-3">You are not part of any team yet.</Alert>}

      <Card className={styles.formCard}>
        <Card.Body className="p-4">
          <h5 className={styles.cardTitle}>Current Submission</h5>
          
          <div className={styles.draftAlert}>
            <AlertCircle size={18} className={styles.draftAlertIcon} />
            <span>
              Status: <strong>{submission?.status === 'submitted' ? 'Submitted' : 'Draft'}</strong>
              {round ? <> · Round: {round.name} · Deadline: {deadlineText}</> : ''}
            </span>
          </div>

          <Form onSubmit={handleSubmit}>
            <Row className="mb-4">
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Team</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={team?.name || ''}
                    readOnly
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Track</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={trackName}
                    readOnly
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-4">
              <Form.Label className={styles.formLabel}>Project Notes / Metadata</Form.Label>
              <Form.Control 
                as="textarea" 
                rows={4}
                name="apiMetadata"
                value={formData.apiMetadata}
                onChange={handleChange}
                className={styles.formControl}
              />
            </Form.Group>

            <Row className="mb-4">
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Demo URL</Form.Label>
                  <Form.Control 
                    type="url" 
                    name="demoUrl"
                    value={formData.demoUrl}
                    onChange={handleChange}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label className={styles.formLabel}>Slides URL</Form.Label>
                  <Form.Control 
                    type="url" 
                    name="slideUrl"
                    value={formData.slideUrl}
                    onChange={handleChange}
                    className={styles.formControl}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-4">
              <Form.Label className={styles.formLabel}>Report URL</Form.Label>
              <Form.Control 
                type="url" 
                name="reportUrl"
                value={formData.reportUrl}
                onChange={handleChange}
                className={styles.formControl}
              />
            </Form.Group>

            <Form.Group className="mb-4">
              <Form.Label className={styles.formLabel}>GitHub Repository URL</Form.Label>
              <Form.Control 
                type="url" 
                name="repoUrl"
                value={formData.repoUrl}
                onChange={handleChange}
                className={styles.formControl}
              />
            </Form.Group>

            <div className={styles.buttonContainer}>
              <button type="button" className={`btn ${styles.btnSave}`} onClick={() => save('draft')} disabled={saving || !team || !round}>
                {saving ? 'Saving…' : 'Save Draft'}
              </button>
              <Button variant="primary" type="submit" className={styles.btnSubmit} disabled={saving || !team || !round}>
                {saving ? 'Saving…' : 'Submit'}
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
};

export default SubmissionManagement;
