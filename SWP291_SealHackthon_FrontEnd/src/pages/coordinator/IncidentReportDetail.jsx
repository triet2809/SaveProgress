import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Badge, Button, Spinner, Alert } from 'react-bootstrap';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Calendar, User, ShieldAlert, History } from 'lucide-react';
import { getIncident, updateIncidentStatus, addIncidentAction } from '../../api/hackathonApi';
import { getInitials } from '../../utils/authUser';
import IncidentStatusBadge from '../../components/incident/IncidentStatusBadge';
import DecisionPanel from '../../components/incident/DecisionPanel';

// BE incident statuses: reported, under_review, resolved, rejected.
const STATUS_LABEL = {
  reported: 'Pending Review',
  under_review: 'Under Review',
  resolved: 'Resolved',
  rejected: 'Rejected',
};

// DecisionPanel emits FE decision strings. Map each to a BE status change
// and/or an incident action (IncidentActionType).
const DECISION_MAP = {
  'Under Review': { status: 'under_review' },
  'Warning Issued': { status: 'resolved', actionType: 'warning' },
  'Require Resubmission': { status: 'under_review', actionType: 'require_resubmission' },
  'Adjust Score': { status: 'resolved', actionType: 'score_adjustment' },
  'Disqualified': { status: 'resolved', actionType: 'disqualify_team' },
  'Rejected': { status: 'rejected', actionType: 'reject_report' },
  'Resolved': { status: 'resolved' },
};

const typeLabel = (t) =>
  (t || 'other')
    .split('_')
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' ');

const IncidentReportDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [incident, setIncident] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await getIncident(id);
      setIncident(data);
    } catch (err) {
      setError(err.message || 'Failed to load incident');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleDecision = async (decision) => {
    const map = DECISION_MAP[decision] || { status: 'under_review' };
    setSaving(true);
    setError('');
    try {
      if (map.actionType) {
        await addIncidentAction(id, {
          actionType: map.actionType,
          oldValue: incident?.status,
          newValue: map.status,
          note: `Decision: ${decision}`,
        });
      }
      if (map.status) {
        await updateIncidentStatus(id, { status: map.status, note: `Decision: ${decision}` });
      }
      await load();
    } catch (err) {
      setError(err.message || 'Failed to record decision');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <Spinner animation="border" role="status" />
      </div>
    );
  }

  if (!incident) {
    return (
      <div className="py-2">
        <Alert variant="danger">{error || 'Incident not found.'}</Alert>
        <Button variant="link" onClick={() => navigate('/coordinator/incidents')}>Back to Incidents</Button>
      </div>
    );
  }

  const statusText = STATUS_LABEL[incident.status] || incident.status;
  const actions = incident.actions || [];

  return (
    <div className="py-2">
      {error && <Alert variant="danger">{error}</Alert>}
      <div className="mb-4">
        <Button
          variant="link"
          className="p-0 text-decoration-none d-inline-flex align-items-center gap-2 mb-3"
          style={{ color: 'var(--cf-text-secondary)' }}
          onClick={() => navigate('/coordinator/incidents')}
        >
          <ArrowLeft size={16} /> Back to Incidents
        </Button>
        <div className="d-flex justify-content-between align-items-start">
          <div>
            <div className="d-flex align-items-center gap-3 mb-2">
              <h1 className="h3 fw-bold mb-0" style={{ color: 'var(--cf-text-primary)' }}>{incident.title}</h1>
              <IncidentStatusBadge status={statusText} />
            </div>
            <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }} className="d-flex align-items-center gap-3">
              <span>Report ID: {incident.id}</span>
              <span>•</span>
              <span className="d-flex align-items-center gap-1"><Calendar size={14} /> {incident.createdAt ? new Date(incident.createdAt).toLocaleString() : '—'}</span>
            </div>
          </div>
        </div>
      </div>

      <Row className="g-4">
        <Col lg={8}>
          <Card className="mb-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
                <ShieldAlert size={20} className="text-warning" />
                Incident Details
              </h5>

              <Row className="mb-4">
                <Col md={6}>
                  <div className="mb-3">
                    <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Type</div>
                    <div className="fw-medium" style={{ color: 'var(--cf-text-primary)' }}>{typeLabel(incident.type)}</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Team</div>
                    <div className="fw-medium text-primary">{incident.teamId || '—'}</div>
                  </div>
                </Col>
                <Col md={6}>
                  <div className="mb-3">
                    <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Status</div>
                    <IncidentStatusBadge status={statusText} />
                  </div>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Submission Involved</div>
                    <div className="fw-medium" style={{ color: 'var(--cf-text-primary)' }}>{incident.submissionId || '—'}</div>
                  </div>
                </Col>
              </Row>

              <hr style={{ borderColor: 'var(--cf-border-color)' }} />

              <div className="mb-4">
                <h6 className="fw-bold mb-2" style={{ color: 'var(--cf-text-primary)' }}>Description</h6>
                <p style={{ color: 'var(--cf-text-secondary)', lineHeight: '1.6', whiteSpace: 'pre-wrap' }}>{incident.description}</p>
              </div>

              {(incident.evidences || []).length > 0 && (
                <div>
                  <h6 className="fw-bold mb-2" style={{ color: 'var(--cf-text-primary)' }}>Evidence</h6>
                  <div className="d-flex flex-column gap-2">
                    {incident.evidences.map((ev, idx) => (
                      <div key={ev.id || idx} className="p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)', border: '1px solid var(--cf-border-color)' }}>
                        {ev.url ? (
                          <a href={ev.url} target="_blank" rel="noreferrer">{ev.description || ev.url}</a>
                        ) : (
                          <span>{ev.description || 'Evidence'}</span>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="mt-4">
                <h6 className="fw-bold mb-3" style={{ color: "var(--cf-text-primary)" }}>
                  Quick Actions
                </h6>
                <div className="d-flex gap-2">
                  <Button variant="warning" onClick={() => handleDecision('Under Review')} disabled={saving} >
                    Mark Under Review
                  </Button>
                  <Button variant="success" onClick={() => handleDecision('Resolved')} disabled={saving} >
                    Resolve
                  </Button>
                  <Button variant="danger" onClick={() => handleDecision('Rejected')} disabled={saving} >
                    Reject Report
                  </Button>
                </div>
              </div>
            </Card.Body>
          </Card>

          <DecisionPanel onDecisionMade={handleDecision} />
        </Col>

        <Col lg={4}>
          <Card className="mb-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
                <User size={20} />
                Reporter Info
              </h5>
              <div className="d-flex align-items-center gap-3 mb-3">
                <div className="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center fw-bold" style={{ width: '40px', height: '40px' }}>
                  {getInitials(incident.reporterEmail)}
                </div>
                <div>
                  <div className="fw-bold" style={{ color: 'var(--cf-text-primary)' }}>{incident.reporterEmail || 'Unknown'}</div>
                  <div style={{ fontSize: '0.875rem', color: 'var(--cf-text-secondary)' }}>Reporter</div>
                </div>
              </div>
            </Card.Body>
          </Card>

          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
                <History size={20} />
                Audit Trail
              </h5>
              <div className="d-flex flex-column gap-3">
                {actions.length === 0 && (
                  <div className="text-muted small">No actions recorded yet.</div>
                )}
                {actions.map((log, index) => (
                  <div key={log.id || index} className="position-relative ps-3" style={{ borderLeft: '2px solid var(--cf-border-color)' }}>
                    <div className="position-absolute rounded-circle bg-primary" style={{ width: '8px', height: '8px', left: '-5px', top: '5px' }}></div>
                    <div className="fw-medium mb-1" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>
                      {typeLabel(log.actionType)}{log.newValue ? `: ${log.newValue}` : ''}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)' }}>
                      {log.createdAt ? new Date(log.createdAt).toLocaleString() : ''} by {log.actionByEmail || 'system'}
                    </div>
                    {log.note && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)' }}>{log.note}</div>
                    )}
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default IncidentReportDetail;
