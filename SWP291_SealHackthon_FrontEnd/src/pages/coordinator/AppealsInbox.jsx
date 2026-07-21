import { useEffect, useState, useCallback } from 'react';
import { Card, Table, Badge, Button, Form, Spinner, Alert, Modal } from 'react-bootstrap';
import { MessageSquareWarning, Check, X, Reply } from 'lucide-react';
import { getEvents, getAppeals, respondToAppeal, resolveAppeal, resumeRound, advanceRound } from '../../api/hackathonApi';

const listOf = (data) => (Array.isArray(data) ? data : data?.content || []);

const statusVariant = (s) => (s === 'PENDING' ? 'warning' : s === 'ACCEPTED' ? 'success' : 'danger');

// Trang EC duyệt khiếu nại: lọc theo sự kiện + trạng thái, ghi phản hồi, chốt ACCEPTED/REJECTED.
const AppealsInbox = () => {
  const [events, setEvents] = useState([]);
  const [eventId, setEventId] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [appeals, setAppeals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  // Modal ghi phản hồi trung gian
  const [showRespond, setShowRespond] = useState(false);
  // Modal chốt kết luận
  const [showResolve, setShowResolve] = useState(false);
  const [active, setActive] = useState(null); // đơn khiếu nại đang thao tác
  const [text, setText] = useState('');
  const [resolveStatus, setResolveStatus] = useState('ACCEPTED');
  const [recalculationRequired, setRecalculationRequired] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const evList = listOf(await getEvents({ size: 100 }));
        setEvents(evList);
        if (evList.length) setEventId(evList[0].id);
      } catch (e) {
        setError(e.message || 'Failed to load events');
      }
    })();
  }, []);

  const load = useCallback(async () => {
    if (!eventId) { setLoading(false); return; }
    setLoading(true);
    setError('');
    try {
      const params = { eventId, size: 100 };
      if (statusFilter) params.status = statusFilter;
      const data = await getAppeals(params);
      setAppeals(listOf(data));
    } catch (e) {
      setError(e.message || 'Failed to load appeals');
    } finally {
      setLoading(false);
    }
  }, [eventId, statusFilter]);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const openRespond = (a) => { setActive(a); setText(a.response || ''); setShowRespond(true); };
  const openResolve = (a) => { setActive(a); setText(''); setResolveStatus('ACCEPTED'); setShowResolve(true); };

  const submitRespond = async () => {
    if (!text.trim()) { alert('Please enter a response.'); return; }
    try {
      setBusy(true);
      await respondToAppeal(active.id, text.trim());
      setShowRespond(false);
      await load();
    } catch (e) {
      setError(e.message || 'Failed to respond');
    } finally {
      setBusy(false);
    }
  };

  const submitResolve = async () => {
    try {
      setBusy(true);
      // response tùy chọn; status bắt buộc ACCEPTED/REJECTED.
      await resolveAppeal(active.id, { status: resolveStatus, response: text.trim() || undefined, recalculationRequired });
      setShowResolve(false);
      await load();
    } catch (e) {
      setError(e.message || 'Failed to resolve');
    } finally {
      setBusy(false);
    }
  };

  const continueRound = async (appeal, advance) => {
    try {
      setBusy(true);
      if (advance) await advanceRound(appeal.eventId, appeal.roundId);
      else await resumeRound(appeal.eventId, appeal.roundId);
      await load();
    } catch (e) {
      setError(e.message || 'Failed to continue round');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-2 mb-4">
        <MessageSquareWarning size={24} className="text-warning" />
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Appeals</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            Review and resolve team appeals for round results
          </div>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex gap-2 flex-wrap">
          <Form.Select style={{ maxWidth: 320 }} value={eventId} onChange={(e) => setEventId(e.target.value)}>
            {events.length === 0 && <option value="">No events</option>}
            {events.map((ev) => <option key={ev.id} value={ev.id}>{ev.title}</option>)}
          </Form.Select>
          <Form.Select style={{ maxWidth: 200 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="ACCEPTED">Accepted</option>
            <option value="REJECTED">Rejected</option>
          </Form.Select>
        </div>

        <div className="table-responsive">
          {loading ? (
            <div className="text-center py-5"><Spinner animation="border" /></div>
          ) : (
            <Table className="mb-0" hover>
              <thead>
                <tr>
                  <th className="border-top-0 border-bottom">Team</th>
                  <th className="border-top-0 border-bottom">Round</th>
                  <th className="border-top-0 border-bottom">Reason</th>
                  <th className="border-top-0 border-bottom">Status</th>
                  <th className="border-top-0 border-bottom">Submitted</th>
                  <th className="border-top-0 border-bottom text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {appeals.length === 0 && (
                  <tr><td colSpan={6} className="text-center text-muted py-4">No appeals.</td></tr>
                )}
                {appeals.map((a) => (
                  <tr key={a.id}>
                    <td className="fw-medium py-3">{a.teamName}</td>
                    <td className="py-3">{a.roundName}<div className="small text-muted">Version {a.resultVersion} · {a.lifecycleState}</div></td>
                    <td className="py-3" style={{ maxWidth: 320, whiteSpace: 'pre-wrap' }}>
                      {a.reason}
                      {a.response && <div className="text-muted small mt-1">Response: {a.response}</div>}
                    </td>
                    <td className="py-3"><Badge bg={statusVariant(a.status)}>{a.status}</Badge></td>
                    <td className="py-3 text-muted small">{a.createdAt ? new Date(a.createdAt).toLocaleString() : ''}</td>
                    <td className="py-3 text-end">
                      {a.status === 'PENDING' ? (
                        <div className="d-flex gap-2 justify-content-end">
                          <Button size="sm" variant="outline-secondary" onClick={() => openRespond(a)} title="Add response">
                            <Reply size={14} />
                          </Button>
                          <Button size="sm" variant="outline-success" onClick={() => openResolve(a)} title="Resolve">
                            <Check size={14} />
                          </Button>
                        </div>
                      ) : (
                        <div className="d-flex gap-2 justify-content-end align-items-center">
                          <span className="text-muted small">{a.resolvedByName ? `by ${a.resolvedByName}` : 'Resolved'}</span>
                          {a.lifecycleState === 'PAUSED_FOR_APPEAL' && (
                            <Button size="sm" variant="outline-primary" disabled={busy} onClick={() => continueRound(a, false)}>Resume</Button>
                          )}
                          {a.lifecycleState === 'READY_TO_ADVANCE' && (
                            <>
                              <Button size="sm" variant="primary" disabled={busy} onClick={() => continueRound(a, true)}>Advance</Button>
                            </>
                          )}
                          {a.lifecycleState === 'READY_FOR_AWARDS' && (
                            <Button size="sm" variant="outline-primary" disabled={busy} onClick={() => continueRound(a, false)}>Confirm ready</Button>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </div>
      </Card>

      {/* Modal ghi phản hồi trung gian (đơn vẫn PENDING) */}
      <Modal show={showRespond} onHide={() => setShowRespond(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="d-flex align-items-center gap-2"><Reply size={20} /> Add Response</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="text-muted small">Team: <strong>{active?.teamName}</strong> · {active?.roundName}</p>
          <Form.Control as="textarea" rows={4} value={text} onChange={(e) => setText(e.target.value)}
            placeholder="Write a response to the team" />
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowRespond(false)} disabled={busy}>Cancel</Button>
          <Button variant="primary" onClick={submitRespond} disabled={busy}>{busy ? 'Saving...' : 'Save Response'}</Button>
        </Modal.Footer>
      </Modal>

      {/* Modal chốt kết luận ACCEPTED / REJECTED */}
      <Modal show={showResolve} onHide={() => setShowResolve(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Resolve Appeal</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="text-muted small">Team: <strong>{active?.teamName}</strong> · {active?.roundName}</p>
          <Form.Group className="mb-3">
            <Form.Label className="fw-medium">Decision</Form.Label>
            <Form.Select value={resolveStatus} onChange={(e) => setResolveStatus(e.target.value)}>
              <option value="ACCEPTED">Accept appeal</option>
              <option value="REJECTED">Reject appeal</option>
            </Form.Select>
          </Form.Group>
          {resolveStatus === 'ACCEPTED' && <Form.Check className="mb-3" label="Result change requires recalculation and republication" checked={recalculationRequired} onChange={(e) => setRecalculationRequired(e.target.checked)} />}
          <Form.Group>
            <Form.Label className="fw-medium">Response (optional)</Form.Label>
            <Form.Control as="textarea" rows={3} value={text} onChange={(e) => setText(e.target.value)}
              placeholder="Explain the decision" />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowResolve(false)} disabled={busy}>Cancel</Button>
          <Button variant={resolveStatus === 'ACCEPTED' ? 'success' : 'danger'} onClick={submitResolve} disabled={busy}>
            {busy ? 'Submitting...' : resolveStatus === 'ACCEPTED' ? <><Check size={16} className="me-1" />Accept</> : <><X size={16} className="me-1" />Reject</>}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default AppealsInbox;
