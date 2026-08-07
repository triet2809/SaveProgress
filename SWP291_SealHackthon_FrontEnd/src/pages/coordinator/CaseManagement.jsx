import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Form, Modal, Spinner, Table } from 'react-bootstrap';
import { getCases, updateCaseStatus } from '../../api/hackathonApi';

// Ánh xạ trạng thái case -> màu Badge hiển thị.
const STATUS_VARIANT = {
  open: 'secondary',
  in_progress: 'info',
  resolved: 'success',
  rejected: 'danger',
};

export default function CaseManagement() {
  const [cases, setCases] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  // Modal xử lý case (đánh dấu đã giải quyết / từ chối).
  const [resolving, setResolving] = useState(null); // case object đang xử lý
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Tải toàn bộ case (mọi sự kiện) — không cần chọn event nữa.
  const load = () => {
    setLoading(true);
    setError('');
    getCases()
      .then((data) => setCases(data?.content || []))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };
  useEffect(() => { load(); }, []);

  const visibleCases = statusFilter ? cases.filter((c) => c.status === statusFilter) : cases;

  // Mở modal xử lý với trạng thái mục tiêu (resolved / rejected).
  const openResolve = (item) => { setResolving(item); setNote(''); };
  const applyStatus = async (targetStatus) => {
    if (!resolving) return;
    setSubmitting(true);
    setError('');
    try {
      await updateCaseStatus(resolving.id, { status: targetStatus, note: note || undefined });
      setResolving(null);
      load();
    } catch (e) {
      setError(e.message || 'Unable to update case');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card className="mt-3">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h1 className="h4 mb-0">Case Management</h1>
          <div className="d-flex gap-2 align-items-center">
            <Form.Select size="sm" style={{ width: 180 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">All statuses</option>
              <option value="open">Open</option>
              <option value="in_progress">In progress</option>
              <option value="resolved">Resolved</option>
              <option value="rejected">Rejected</option>
            </Form.Select>
            <Button size="sm" variant="outline-secondary" onClick={load}>Refresh</Button>
          </div>
        </div>
        {error && <Alert variant="danger">{error}</Alert>}
        {loading ? <Spinner animation="border" /> : (
          <Table responsive hover className="align-middle">
            <thead>
              <tr>
                <th>ID</th>
                <th>Subject</th>
                <th>Description</th>
                <th>Team</th>
                <th>Event</th>
                <th>Track</th>
                <th>Reporter</th>
                <th>Status</th>
                <th>Created</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {visibleCases.length === 0 && <tr><td colSpan={10}>No cases found.</td></tr>}
              {visibleCases.map((item) => (
                <tr key={item.id}>
                  <td><code>{item.id}</code></td>
                  <td>{item.subject}</td>
                  <td style={{ maxWidth: 320, whiteSpace: 'pre-wrap' }}>{item.description || '—'}</td>
                  <td>{item.teamName || '—'}</td>
                  <td>{item.eventName || '—'}</td>
                  <td>{item.trackName || '—'}</td>
                  <td>{item.reporterName || '—'}</td>
                  <td><Badge bg={STATUS_VARIANT[item.status] || 'secondary'}>{item.status}</Badge></td>
                  <td>{item.createdAt ? new Date(item.createdAt).toLocaleString() : '—'}</td>
                  <td>
                    {item.status === 'resolved' || item.status === 'rejected' ? (
                      <span className="text-muted small">Closed</span>
                    ) : (
                      <Button size="sm" variant="success" onClick={() => openResolve(item)}>Resolve</Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card.Body>

      {/* Modal xử lý case: coordinator đánh dấu đã giải quyết hoặc từ chối, kèm ghi chú. */}
      <Modal show={!!resolving} onHide={() => setResolving(null)}>
        <Modal.Header closeButton>
          <Modal.Title>Resolve case</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {resolving && (
            <>
              <p className="mb-1"><strong>{resolving.subject}</strong></p>
              <p className="text-muted small mb-2">
                {resolving.teamName || '—'} · {resolving.eventName || '—'}
                {resolving.trackName ? ` · ${resolving.trackName}` : ''}
              </p>
              <p className="mb-3" style={{ whiteSpace: 'pre-wrap' }}>{resolving.description || 'No description.'}</p>
              <Form.Group>
                <Form.Label>Resolution note (optional)</Form.Label>
                <Form.Control as="textarea" rows={3} value={note} onChange={(e) => setNote(e.target.value)} placeholder="How was this resolved?" />
              </Form.Group>
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-danger" disabled={submitting} onClick={() => applyStatus('rejected')}>Reject</Button>
          <Button variant="success" disabled={submitting} onClick={() => applyStatus('resolved')}>
            {submitting ? 'Saving…' : 'Mark as resolved'}
          </Button>
        </Modal.Footer>
      </Modal>
    </Card>
  );
}
