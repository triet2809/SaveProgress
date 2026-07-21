import React, { useEffect, useState } from 'react';
import { Card, Table, Button, InputGroup, Form, Spinner, Alert } from 'react-bootstrap';
import { Search, Eye, AlertTriangle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getIncidents } from '../../api/hackathonApi';
import IncidentStatusBadge from '../../components/incident/IncidentStatusBadge';

const asArray = (data) => data?.content || data || [];

// BE incident statuses: reported, under_review, resolved, rejected.
const STATUS_LABEL = {
  reported: 'Pending Review',
  under_review: 'Under Review',
  resolved: 'Resolved',
  rejected: 'Rejected',
};

const typeLabel = (t) =>
  (t || 'other')
    .split('_')
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' ');

// BE severity: low/medium/high/critical (lowercase). Display capitalized + colored.
const severityLabel = (s) => {
  if (!s) return '—';
  return s.charAt(0).toUpperCase() + s.slice(1);
};
const severityColor = (s) => {
  const v = (s || '').toLowerCase();
  if (v === 'high' || v === 'critical') return 'var(--cf-status-danger)';
  if (v === 'medium') return 'var(--cf-status-warning)';
  return 'var(--cf-text-secondary)';
};

const teamName = (incident) =>
  incident.teamName || incident.team?.name || incident.teamId || '—';

const shortId = (id) => (id ? String(id).slice(0, 8) : '—');

const IncidentReports = () => {
  const navigate = useNavigate();
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('All Statuses');
  const [severityFilter, setSeverityFilter] = useState('All Severities');

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const data = asArray(await getIncidents({ size: 200 }));
        if (active) setIncidents(data);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load incidents');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const filteredIncidents = incidents.filter((incident) => {
    const q = searchTerm.toLowerCase();
    const matchesSearch =
      (incident.title || '').toLowerCase().includes(q) ||
      teamName(incident).toLowerCase().includes(q) ||
      (incident.reporterEmail || '').toLowerCase().includes(q) ||
      (incident.type || '').toLowerCase().includes(q);
    const matchesStatus =
      statusFilter === 'All Statuses'
        ? true
        : (STATUS_LABEL[incident.status] || incident.status) === statusFilter;
    const matchesSeverity =
      severityFilter === 'All Severities'
        ? true
        : (incident.severity || '').toLowerCase() === severityFilter.toLowerCase();
    return matchesSearch && matchesStatus && matchesSeverity;
  });

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Incident Review</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Review and manage reported violations and incidents</div>
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text className="bg-transparent border-end-0">
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control className="border-start-0" placeholder="Search incidents..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
          </InputGroup>
          <div className="d-flex gap-2">
            <Form.Select style={{ width: '170px' }} value={severityFilter} onChange={(e) => setSeverityFilter(e.target.value)}>
              <option>All Severities</option>
              <option>Low</option>
              <option>Medium</option>
              <option>High</option>
              <option>Critical</option>
            </Form.Select>
            <Form.Select style={{ width: '170px' }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option>All Statuses</option>
              <option>Pending Review</option>
              <option>Under Review</option>
              <option>Resolved</option>
              <option>Rejected</option>
            </Form.Select>
          </div>
        </div>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className="border-top-0 border-bottom">ID</th>
                <th className="border-top-0 border-bottom">Team</th>
                <th className="border-top-0 border-bottom">Title</th>
                <th className="border-top-0 border-bottom">Reporter</th>
                <th className="border-top-0 border-bottom">Type</th>
                <th className="border-top-0 border-bottom">Severity</th>
                <th className="border-top-0 border-bottom">Reported</th>
                <th className="border-top-0 border-bottom">Status</th>
                <th className="border-top-0 border-bottom text-end">Action</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={9} className="text-center py-4"><Spinner animation="border" variant="primary" size="sm" /></td></tr>
              ) : filteredIncidents.length === 0 ? (
                <tr><td colSpan={9} className="text-center py-4 text-muted">No incidents found.</td></tr>
              ) : filteredIncidents.map((incident) => (
                <tr key={incident.id} className="align-middle">
                  <td className="py-3" style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }} title={incident.id}>{shortId(incident.id)}</td>
                  <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{teamName(incident)}</td>
                  <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>
                    <div className="d-flex align-items-center gap-2">
                      <AlertTriangle size={14} className="text-warning" />
                      {incident.title}
                    </div>
                  </td>
                  <td className="py-3">
                    <div style={{ color: 'var(--cf-text-primary)' }}>{incident.reporterEmail || '—'}</div>
                  </td>
                  <td className="py-3">{typeLabel(incident.type)}</td>
                  <td className="py-3">
                    <span className="d-flex align-items-center gap-1" style={{ color: severityColor(incident.severity) }}>
                      {(incident.severity || '').toLowerCase() === 'high' || (incident.severity || '').toLowerCase() === 'critical' ? <AlertTriangle size={14} /> : null}
                      {severityLabel(incident.severity)}
                    </span>
                  </td>
                  <td className="py-3">{incident.createdAt ? new Date(incident.createdAt).toLocaleDateString() : '—'}</td>
                  <td className="py-3">
                    <IncidentStatusBadge status={STATUS_LABEL[incident.status] || incident.status} />
                  </td>
                  <td className="py-3 text-end">
                    <Button
                      variant="primary"
                      size="sm"
                      className="d-inline-flex align-items-center gap-1"
                      onClick={() => navigate(`/coordinator/incidents/${incident.id}`)}
                    >
                      <Eye size={14} /> Review
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  );
};

export default IncidentReports;
