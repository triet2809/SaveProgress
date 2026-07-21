import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Badge, Modal, Form, InputGroup, Button, Spinner, Alert } from 'react-bootstrap';
import { History, Search, Eye, Download } from 'lucide-react';
import { getAuditLogs } from '../../api/hackathonApi';

const asArray = (data) => data?.content || data || [];
const fmtDate = (iso) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
};

const AuditLogs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('All');
  const [showModal, setShowModal] = useState(false);
  const [selectedLog, setSelectedLog] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setError('');
        const data = await getAuditLogs({ size: 200 });
        const rows = asArray(data).map((l) => ({
          id: l.id,
          action: l.action,
          user: l.userEmail || l.userId || 'Unknown',
          role: l.targetType || '—',
          timestamp: fmtDate(l.occurredAt),
          ip: l.details || (l.oldValue || l.newValue ? `${l.oldValue ?? ''} → ${l.newValue ?? ''}` : '—'),
          targetId: l.targetId,
          oldValue: l.oldValue,
          newValue: l.newValue,
        }));
        setLogs(rows);
      } catch (err) {
        setError(err.message || 'Failed to load audit logs');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const roleOptions = useMemo(() => {
    const set = new Set(logs.map((l) => l.role).filter(Boolean));
    return ['All', ...Array.from(set)];
  }, [logs]);

  const filteredLogs = useMemo(() => logs.filter((log) => {
    const matchesSearch =
      log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.user.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesRole = typeFilter === 'All' || log.role === typeFilter;
    return matchesSearch && matchesRole;
  }), [logs, searchTerm, typeFilter]);

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Audit Logs</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Track system activity and administrative actions</div>
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text>
              <Search size={16} />
            </InputGroup.Text>

            <Form.Control
              placeholder="Search logs..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </InputGroup>

          <div className="d-flex gap-2">
            <Form.Select
              style={{ width: '180px' }}
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
            >
              {roleOptions.map((opt) => (
                <option key={opt}>{opt}</option>
              ))}
            </Form.Select>

            <Button
              variant="outline-primary"
              onClick={() => alert('Exporting logs...')}
              className="d-flex align-items-center gap-1"
            >
              <Download size={16} />
              Export
            </Button>
          </div>
        </div>
        
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className="border-top-0 border-bottom">Timestamp</th>
                <th className="border-top-0 border-bottom">Action</th>
                <th className="border-top-0 border-bottom">User</th>
                <th className="border-top-0 border-bottom">Target</th>
                <th className="border-top-0 border-bottom">Details</th>
                <th className="border-top-0 border-bottom text-end"> Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={6} className="text-center py-4"><Spinner animation="border" size="sm" /></td></tr>
              )}
              {!loading && filteredLogs.length === 0 && (
                <tr><td colSpan={6} className="text-center py-4 text-muted">No audit logs found.</td></tr>
              )}
              {!loading && filteredLogs.map((log) => (
                <tr key={log.id}>
                  <td className="py-3" style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
                    <div className="d-flex align-items-center gap-2">
                      <History size={14} /> {log.timestamp}
                    </div>
                  </td>
                  <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{log.action}</td>
                  <td className="py-3">{log.user}</td>
                  <td className="py-3">
                    <Badge bg="secondary" className="bg-opacity-25 text-secondary border">{log.role}</Badge>
                  </td>
                  <td className="py-3" style={{ fontFamily: 'monospace', color: 'var(--cf-text-secondary)' }}>{log.ip}</td>
                  <td className="py-3 text-end">
                    <Button variant="link" size="sm" className="p-0 text-primary" onClick={() => { setSelectedLog(log); setShowModal(true); }}>
                      <Eye size={16} />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      </Card>
      <Modal
        show={showModal}
        onHide={() => setShowModal(false)}
      >
        <Modal.Header closeButton>
          <Modal.Title>
            Audit Log Details
          </Modal.Title>
        </Modal.Header>

        <Modal.Body>
          {selectedLog && (
            <>
              <p>
                <strong>Action:</strong>{' '}
                {selectedLog.action}
              </p>

              <p>
                <strong>User:</strong>{' '}
                {selectedLog.user}
              </p>

              <p>
                <strong>Target:</strong>{' '}
                {selectedLog.role}{selectedLog.targetId ? ` (${selectedLog.targetId})` : ''}
              </p>

              <p>
                <strong>Timestamp:</strong>{' '}
                {selectedLog.timestamp}
              </p>

              <p>
                <strong>Details:</strong>{' '}
                {selectedLog.ip}
              </p>
            </>
          )}
        </Modal.Body>

        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => setShowModal(false)}
          >
            Close
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default AuditLogs;
