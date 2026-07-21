import { useEffect, useState } from 'react';
import { Row, Col, Card, Badge, Spinner, Alert } from 'react-bootstrap';
import { Calendar, Users, Upload, Activity, AlertTriangle } from 'lucide-react';
import { getEvents, getTeams, getSubmissions } from '../../api/hackathonApi';
import {
  coordinatorDashboardMetrics,
  loadCoordinatorDashboardData,
} from './coordinatorDashboardData';

const CoordinatorDashboard = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [teamsError, setTeamsError] = useState('');
  const [submissionsError, setSubmissionsError] = useState('');
  const [events, setEvents] = useState([]);
  const [teams, setTeams] = useState([]);
  const [submissions, setSubmissions] = useState([]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setLoading(true);
        setError('');
        setTeamsError('');
        setSubmissionsError('');
        const data = await loadCoordinatorDashboardData({ getEvents, getTeams, getSubmissions });
        if (!active) return;
        setEvents(data.events);
        setTeams(data.teams);
        setSubmissions(data.submissions);
        setTeamsError(data.teamsError);
        setSubmissionsError(data.submissionsError);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load dashboard');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const metrics = coordinatorDashboardMetrics({ events, teams, submissions, teamsError, submissionsError });
  const activeEventName = events.find((event) => (event.status || '').toLowerCase() === 'ongoing')?.title
    || events[0]?.title || 'the platform';

  const stats = [
    { title: 'Total Events', value: metrics.totalEvents, icon: Calendar, color: 'primary' },
    { title: 'Active Events', value: metrics.activeEvents, icon: Activity, color: 'success' },
    { title: 'Registered Teams', value: metrics.registeredTeams, icon: Users, color: 'info' },
    { title: 'Disqualified Teams', value: metrics.disqualifiedTeams, icon: AlertTriangle, color: 'warning' },
    { title: 'Submissions', value: metrics.submissions, icon: Upload, color: 'primary' },
  ];

  if (loading) {
    return (
      <div className="py-5 d-flex justify-content-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-2">
        <h1 className="h3 fw-bold mb-4" style={{ color: 'var(--cf-text-primary)' }}>Coordinator Dashboard</h1>
        <Alert variant="danger">{error}</Alert>
      </div>
    );
  }

  return (
    <div className="py-2">
      {(teamsError || submissionsError) && (
        <Alert variant="warning">
          {[teamsError, submissionsError].filter(Boolean).join(' ')} Other dashboard data remains available.
        </Alert>
      )}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Coordinator Dashboard</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            System overview and quick metrics for {activeEventName}
          </div>
        </div>
      </div>

      <Row className="g-4 mb-4">
        {stats.map((stat, index) => (
          <Col md={3} sm={6} key={index}>
            <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Card.Body className="d-flex align-items-center p-4">
                <div 
                  className={`d-flex align-items-center justify-content-center me-3`}
                  style={{ 
                    width: '48px', height: '48px', borderRadius: '12px',
                    backgroundColor: `var(--bs-${stat.color}-bg-subtle, rgba(0,0,0,0.05))`,
                    color: `var(--bs-${stat.color})`
                  }}
                >
                  <stat.icon size={24} />
                </div>
                <div>
                  <h3 className="h4 fw-bold mb-0" style={{ color: 'var(--cf-text-primary)' }}>{stat.value}</h3>
                  <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem', fontWeight: '500' }}>{stat.title}</div>
                </div>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      <Row className="g-4">
        <Col lg={8}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4" style={{ color: 'var(--cf-text-primary)' }}>Recent Teams</h5>
              <div className="d-flex flex-column gap-3">
                {teamsError && <Alert variant="danger" className="mb-0">{teamsError}</Alert>}
                {!teamsError && teams.length === 0 && (
                  <div className="text-muted small">No teams registered yet.</div>
                )}
                {teams.slice(0, 5).map((team) => (
                  <div key={team.id} className="d-flex align-items-center justify-content-between p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)' }}>
                    <div className="d-flex align-items-center gap-3">
                      <Activity size={18} className="text-primary" />
                      <div>
                        <div className="fw-medium" style={{ color: 'var(--cf-text-primary)' }}>{team.name}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)' }}>{team.members?.length || 0} members</div>
                      </div>
                    </div>
                    <Badge bg={(team.status || '').toLowerCase() === 'disqualified' ? 'danger' : 'success'}>
                      {team.status || 'active'}
                    </Badge>
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={4}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4" style={{ color: 'var(--cf-text-primary)' }}>Recent Submissions</h5>
              <div className="d-flex flex-column gap-3">
                {submissionsError && <Alert variant="danger" className="mb-0">{submissionsError}</Alert>}
                {!submissionsError && submissions.length === 0 && (
                  <div className="text-muted small">No submissions yet.</div>
                )}
                {submissions.slice(0, 5).map((sub) => (
                  <div key={sub.id} className="d-flex align-items-center justify-content-between p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)' }}>
                    <div className="d-flex align-items-center gap-3">
                      <Upload size={18} className="text-info" />
                      <div>
                        <div className="fw-medium" style={{ color: 'var(--cf-text-primary)' }}>{sub.teamName || 'Team'}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)' }}>
                          {sub.submittedAt ? new Date(sub.submittedAt).toLocaleDateString() : ''}
                        </div>
                      </div>
                    </div>
                    <Badge bg="info">Submission</Badge>
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

export default CoordinatorDashboard;
