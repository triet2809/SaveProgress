import { useEffect, useMemo, useState } from 'react';
import { Card, Table, Badge, Spinner, Alert, Form } from 'react-bootstrap';
import { LifeBuoy, Inbox } from 'lucide-react';
import { getSupportTickets, updateSupportTicketStatus, markAllNotificationsRead } from '../../api/hackathonApi';

const CATEGORY_LABELS = {
  technical: 'Technical / Platform',
  rules: 'Rules clarification',
  team: 'Team member change',
  other: 'Other',
};

const STATUS_OPTIONS = [
  { value: 'open', label: 'Open', variant: 'secondary' },
  { value: 'in_progress', label: 'In progress', variant: 'info' },
  { value: 'resolved', label: 'Resolved', variant: 'success' },
];
const ALLOWED_STATUS_TRANSITIONS = {
  open: ['in_progress'],
  in_progress: ['open', 'resolved'],
  resolved: ['open'],
};
const PRIORITY_VARIANT = { low: 'light', medium: 'warning', high: 'danger' };

const statusOptionsFor = (currentStatus) => {
  const allowed = new Set([currentStatus, ...(ALLOWED_STATUS_TRANSITIONS[currentStatus] || [])]);
  return STATUS_OPTIONS.filter((status) => allowed.has(status.value));
};

const SupportTickets = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [updating, setUpdating] = useState({});

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getSupportTickets();
      const list = Array.isArray(res) ? res : (res?.content || []);
      setTickets(list);
    } catch (err) {
      setError(err.message || 'Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // The initial request intentionally populates component state after mount.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
    markAllNotificationsRead('support').catch(() => {});
  }, []);

  const handleStatusChange = async (ticket, status) => {
    setUpdating((prev) => ({ ...prev, [ticket.id]: true }));
    setError('');
    try {
      const updated = await updateSupportTicketStatus(ticket.id, status);
      setTickets((prev) => prev.map((t) => (t.id === ticket.id ? { ...t, status: updated?.status || status } : t)));
    } catch (err) {
      setError(err.message || 'Failed to update status');
    } finally {
      setUpdating((prev) => {
        const next = { ...prev };
        delete next[ticket.id];
        return next;
      });
    }
  };

  const filtered = useMemo(
    () => (statusFilter ? tickets.filter((t) => t.status === statusFilter) : tickets),
    [tickets, statusFilter]
  );

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
          <h1 className="h3 fw-bold mb-1 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
            <LifeBuoy size={22} className="text-primary" /> Support Tickets
          </h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Support requests from participants. Update the status to track handling.</div>
        </div>
        <div style={{ width: '220px' }}>
          <Form.Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </Form.Select>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5"><Spinner animation="border" variant="primary" /></div>
          ) : filtered.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <Inbox size={48} className="mb-3 opacity-50" />
              <h5>No tickets.</h5>
            </div>
          ) : (
            <div className="table-responsive">
              <Table className="mb-0" hover>
                <thead>
                  <tr>
                    <th className="border-top-0">Sender</th>
                    <th className="border-top-0">Category</th>
                    <th className="border-top-0">Priority</th>
                    <th className="border-top-0">Content</th>
                    <th className="border-top-0" style={{ minWidth: '160px' }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((t) => (
                    <tr key={t.id}>
                      <td className="align-middle">
                        <div className="fw-medium">{t.requesterName || '—'}</div>
                        <div className="text-muted small">{t.requesterEmail}</div>
                      </td>
                      <td className="align-middle">
                        <div className="fw-medium">{t.subject}</div>
                        <Badge bg="light" text="dark" className="border mt-1">{CATEGORY_LABELS[t.category] || t.category}</Badge>
                      </td>
                      <td className="align-middle">
                        <Badge bg={PRIORITY_VARIANT[t.priority] || 'light'} text={t.priority === 'low' ? 'dark' : undefined}>{t.priority}</Badge>
                      </td>
                      <td className="align-middle text-muted small" style={{ maxWidth: '320px', whiteSpace: 'pre-wrap' }}>{t.description}</td>
                      <td className="align-middle">
                        <Form.Select
                          size="sm"
                          value={t.status}
                          disabled={!!updating[t.id]}
                          onChange={(e) => handleStatusChange(t, e.target.value)}
                        >
                          {statusOptionsFor(t.status).map((s) => (
                            <option key={s.value} value={s.value}>{s.label}</option>
                          ))}
                        </Form.Select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default SupportTickets;
