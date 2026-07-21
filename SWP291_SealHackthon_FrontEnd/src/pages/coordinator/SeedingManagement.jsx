import { useCallback, useEffect, useState } from 'react';
import {
  Accordion, Alert, Badge, Button, Card, Col, Form, Modal, Row, Spinner, Table,
} from 'react-bootstrap';
import { Award, Check, RotateCcw, ShieldCheck, X } from 'lucide-react';
import {
  finalizeEventResults, getEvents, getSeedCandidates, getTracks,
  removeEventSeed, setEventSeed,
} from '../../api/hackathonApi';
import SeedLabel from '../../components/seeding/SeedLabel';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const listOf = (data) => data?.content || data || [];

const SeedingManagement = () => {
  const [events, setEvents] = useState([]);
  const [eventId, setEventId] = useState('');
  const [tracks, setTracks] = useState([]);
  const [trackId, setTrackId] = useState('');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [active, setActive] = useState(null);
  const [decision, setDecision] = useState({
    status: 'confirmed', seedNumber: '', seedTier: '', candidateSourceFinishId: '', rationale: '',
  });

  useEffect(() => {
    let mounted = true;
    getEvents({ size: 100 })
      .then((result) => { if (mounted) setEvents(listOf(result)); })
      .catch((err) => { if (mounted) setError(err.message || 'Failed to load events'); });
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    if (!eventId) return;
    let mounted = true;
    getTracks({ eventId, size: 200 })
      .then((result) => { if (mounted) setTracks(listOf(result)); })
      .catch((err) => { if (mounted) setError(err.message || 'Failed to load tracks'); });
    return () => { mounted = false; };
  }, [eventId]);

  const load = useCallback(async () => {
    if (!eventId) return;
    setLoading(true);
    setError('');
    try {
      setData(await getSeedCandidates(eventId, trackId || undefined));
    } catch (err) {
      setData(null);
      setError(err.message || 'Failed to load seed candidates');
    } finally {
      setLoading(false);
    }
  }, [eventId, trackId]);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const openDecision = (candidate, status) => {
    const current = candidate.currentDecision;
    setActive(candidate);
    setDecision({
      status,
      seedNumber: current?.seedNumber || '',
      seedTier: current?.seedTier || candidate.suggestedSeedTier || '',
      candidateSourceFinishId: current?.candidateSourceFinishId
        || candidate.supportingFinishes.find((finish) => finish.qualifiedByContinuity)?.historicalFinishId
        || '',
      rationale: current?.rationale || '',
    });
  };

  const saveDecision = async () => {
    setBusy(true);
    setError('');
    try {
      await setEventSeed(eventId, active.teamId, {
        status: decision.status,
        seedNumber: decision.seedNumber ? Number(decision.seedNumber) : null,
        seedTier: decision.seedTier || null,
        candidateSourceFinishId: decision.candidateSourceFinishId || null,
        rationale: decision.rationale || null,
      });
      setActive(null);
      setNotice('Seed decision saved. It is event preparation metadata and does not imply bracket separation.');
      await load();
    } catch (err) {
      setError(err.message || 'Failed to save decision');
    } finally {
      setBusy(false);
    }
  };

  const removeDecision = async (candidate) => {
    setBusy(true);
    setError('');
    try {
      await removeEventSeed(eventId, candidate.teamId);
      setNotice('Seed assignment removed; historical finishes were not changed.');
      await load();
    } catch (err) {
      setError(err.message || 'Failed to remove assignment');
    } finally {
      setBusy(false);
    }
  };

  const finalize = async () => {
    setBusy(true);
    setError('');
    try {
      const result = await finalizeEventResults(eventId);
      setNotice(`Finalization complete: ${result.createdCount} finish snapshot(s) created, ${result.existingCount} already existed.`);
    } catch (err) {
      setError(err.message || 'Finalization failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="py-2">
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">Historical Seeding Review</h1>
          <p className="text-muted mb-0">Review immutable top-five history and make event-specific EC decisions.</p>
        </div>
        <Button variant="outline-primary" onClick={finalize} disabled={!eventId || busy}>
          <ShieldCheck size={16} className="me-2" />Finalize historical finishes
        </Button>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}
      {notice && <Alert variant="success" onClose={() => setNotice('')} dismissible>{notice}</Alert>}
      <Alert variant="info">
        Seed metadata prepares future allocation. No groups, brackets, matchups, or seed-separation guarantee exists yet.
      </Alert>

      <Card className="border-0 shadow-sm mb-4">
        <Card.Body className="p-4">
          <Row className="g-3">
            <Col md={6}>
              <Form.Label>Event</Form.Label>
              <Form.Select value={eventId} onChange={(event) => {
                setEventId(event.target.value);
                setTrackId('');
                setTracks([]);
                setData(null);
              }}>
                <option value="">Select event</option>
                {events.map((event) => <option key={event.id} value={event.id}>{event.title}</option>)}
              </Form.Select>
            </Col>
            <Col md={6}>
              <Form.Label>Track</Form.Label>
              <Form.Select value={trackId} onChange={(event) => setTrackId(event.target.value)} disabled={!eventId}>
                <option value="">All tracks</option>
                {tracks.map((track) => <option key={track.id} value={track.id}>{track.name}</option>)}
              </Form.Select>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {data?.summary && (
        <Row className="g-3 mb-4">
          {[
            ['Eligible', data.summary.eligibleCandidates, 'primary'],
            ['Confirmed', data.summary.confirmed, 'success'],
            ['Rejected', data.summary.rejected, 'danger'],
            ['Overridden', data.summary.overridden, 'info'],
            ['Unreviewed', data.summary.unreviewed, 'warning'],
          ].map(([label, value, variant]) => (
            <Col key={label}>
              <Card className="border-0 shadow-sm text-center"><Card.Body>
                <Badge bg={variant} className="mb-2">{label}</Badge>
                <div className="h4 mb-0">{value}</div>
              </Card.Body></Card>
            </Col>
          ))}
        </Row>
      )}
      {data?.summary?.warning && <Alert variant="warning">{data.summary.warning}</Alert>}

      {loading ? <div className="py-5 text-center"><Spinner /></div> : data && (
        <Card className="border-0 shadow-sm">
          <Card.Body className="p-0">
            {data.candidates.length === 0 ? (
              <Alert variant="secondary" className="m-4">No eligible candidates for this selection.</Alert>
            ) : (
              <Table responsive hover className="mb-0 align-middle">
                <thead><tr>
                  <th>Team / Profile</th><th>Track</th><th>History</th><th>Suggested</th><th>EC decision</th><th />
                </tr></thead>
                <tbody>
                  {data.candidates.map((candidate) => (
                    <tr key={candidate.teamId}>
                      <td>
                        <div className="fw-semibold">{candidate.teamName}</div>
                        <TeamRecognitionBadge recognitions={candidate.recognitions} />
                        <small className="text-muted">{candidate.profileName}</small>
                      </td>
                      <td>{candidate.trackName}</td>
                      <td>
                        <div>Best rank #{candidate.bestHistoricalRank}</div>
                        <small className="text-muted">{candidate.topFiveFinishCount} top-five finish(es)</small>
                      </td>
                      <td><SeedLabel suggestedTier={candidate.suggestedSeedTier} /></td>
                      <td><SeedLabel assignment={candidate.currentDecision} /></td>
                      <td className="text-end">
                        <div className="d-flex flex-wrap justify-content-end gap-1">
                          <Button size="sm" variant="success" onClick={() => openDecision(candidate, 'confirmed')}><Check size={14} /></Button>
                          <Button size="sm" variant="outline-danger" onClick={() => openDecision(candidate, 'rejected')}><X size={14} /></Button>
                          <Button size="sm" variant="outline-primary" onClick={() => openDecision(candidate, 'overridden')}><Award size={14} /></Button>
                          {candidate.currentDecision && (
                            <Button size="sm" variant="outline-secondary" onClick={() => removeDecision(candidate)}><RotateCcw size={14} /></Button>
                          )}
                        </div>
                        <Accordion className="mt-2 text-start">
                          <Accordion.Item eventKey="0">
                            <Accordion.Header>History and continuity</Accordion.Header>
                            <Accordion.Body>
                              {candidate.supportingFinishes.map((finish) => (
                                <div className="border-bottom pb-2 mb-2" key={finish.historicalFinishId}>
                                  <strong>{finish.eventName} · {finish.trackName} · Rank #{finish.finalRank}</strong>
                                  <div className="small">
                                    Immutable result {finish.resultVersionId}; {finish.returningMemberCount}/{finish.historicalRosterSize} returning
                                  </div>
                                  <div className="small text-muted">
                                    Matching: {finish.matchingMembers.map((member) => member.fullName).join(', ') || 'none'}
                                  </div>
                                </div>
                              ))}
                              <small className="text-muted">{candidate.recommendationFormula}</small>
                            </Accordion.Body>
                          </Accordion.Item>
                        </Accordion>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            )}
          </Card.Body>
        </Card>
      )}

      <Modal show={Boolean(active)} onHide={() => setActive(null)} centered>
        <Modal.Header closeButton><Modal.Title>EC seed decision</Modal.Title></Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label>Decision</Form.Label>
            <Form.Control value={decision.status} disabled />
          </Form.Group>
          {decision.status !== 'rejected' && (
            <Row className="g-3 mb-3">
              <Col><Form.Label>Seed number</Form.Label><Form.Control type="number" min="1" value={decision.seedNumber} onChange={(event) => setDecision({ ...decision, seedNumber: event.target.value })} /></Col>
              <Col><Form.Label>Seed tier</Form.Label><Form.Control value={decision.seedTier} onChange={(event) => setDecision({ ...decision, seedTier: event.target.value })} /></Col>
            </Row>
          )}
          <Form.Group className="mb-3">
            <Form.Label>Supporting historical finish</Form.Label>
            <Form.Select value={decision.candidateSourceFinishId} onChange={(event) => setDecision({ ...decision, candidateSourceFinishId: event.target.value })}>
              {active?.supportingFinishes.filter((finish) => finish.qualifiedByContinuity).map((finish) => (
                <option key={finish.historicalFinishId} value={finish.historicalFinishId}>
                  {finish.eventName} · Rank #{finish.finalRank} · {finish.returningMemberCount} returning
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          <Form.Group>
            <Form.Label>Rationale {decision.status === 'overridden' && '*'}</Form.Label>
            <Form.Control as="textarea" rows={3} value={decision.rationale} onChange={(event) => setDecision({ ...decision, rationale: event.target.value })} />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setActive(null)}>Cancel</Button>
          <Button onClick={saveDecision} disabled={busy || (decision.status === 'overridden' && !decision.rationale.trim())}>Save decision</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default SeedingManagement;
