import React, { useEffect, useState } from 'react';
import { Card, Table, Badge, Button, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { Trophy, Calendar, ExternalLink, Award } from 'lucide-react';
import { getMyTeams, getTrack, getSubmissions } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';

const StudentDashboard = () => {
  const user = getStoredUser();
  const userId = user?.id;
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const teamsRes = await getMyTeams();
        const teams = Array.isArray(teamsRes) ? teamsRes : teamsRes?.content || [];

        const built = await Promise.all(
          teams.map(async (team) => {
            const [track, subRes] = await Promise.all([
              team.trackId ? getTrack(team.trackId).catch(() => null) : Promise.resolve(null),
              getSubmissions({ teamId: team.id, size: 5 }).catch(() => null),
            ]);
            const subs = subRes?.content || subRes || [];
            const myMember = (team.members || []).find((m) => m.userId === userId);
            return {
              id: team.id,
              name: track?.name || team.name,
              date: team.createdAt ? new Date(team.createdAt).toLocaleDateString() : '',
              role: myMember?.role === 'leader' ? 'Team Leader' : 'Member',
              team: team.name,
              achievement: subs.length ? 'Submitted' : 'In Progress',
              status: team.status === 'active' ? 'Active' : team.status,
              link: subs[0]?.repoUrl || subs[0]?.demoUrl || null,
            };
          }),
        );

        if (active) setHistory(built);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load dashboard');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, [userId]);

  const totalTeams = history.length;
  const submittedCount = history.filter((h) => h.achievement === 'Submitted').length;

  return (
    <div className="py-2">
      <div className="mb-4">
        <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Student Dashboard</h1>
        <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Welcome back{user?.fullName ? `, ${user.fullName}` : ''}! Here is an overview of your hackathon journey.</div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Row className="g-4 mb-4">
        {/* Quick Stats */}
        <Col md={4}>
          <Card className="h-100" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="d-flex align-items-center gap-3 p-4">
              <div className="d-flex align-items-center justify-content-center rounded-circle bg-primary bg-opacity-10 text-primary" style={{ width: '48px', height: '48px' }}>
                <Trophy size={24} />
              </div>
              <div>
                <div className="text-muted small fw-medium text-uppercase mb-1" style={{ letterSpacing: '0.5px' }}>Total Teams</div>
                <div className="h3 fw-bold mb-0">{totalTeams}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col md={4}>
          <Card className="h-100" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="d-flex align-items-center gap-3 p-4">
              <div className="d-flex align-items-center justify-content-center rounded-circle bg-warning bg-opacity-10 text-warning" style={{ width: '48px', height: '48px' }}>
                <Award size={24} />
              </div>
              <div>
                <div className="text-muted small fw-medium text-uppercase mb-1" style={{ letterSpacing: '0.5px' }}>Submissions</div>
                <div className="h3 fw-bold mb-0">{submittedCount}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Card.Body className="p-4">
          <div className="d-flex align-items-center gap-2 mb-4">
            <Calendar size={20} className="text-primary" />
            <h5 className="fw-bold mb-0">Participation History</h5>
          </div>

          {loading ? (
            <div className="text-center py-4">
              <Spinner animation="border" role="status" />
            </div>
          ) : (
            <div className="table-responsive">
              <Table className="mb-0 align-middle" hover>
                <thead>
                  <tr>
                    <th className="border-top-0 border-bottom text-muted fw-medium py-3">Track / Event</th>
                    <th className="border-top-0 border-bottom text-muted fw-medium py-3">Date</th>
                    <th className="border-top-0 border-bottom text-muted fw-medium py-3">Team / Role</th>
                    <th className="border-top-0 border-bottom text-muted fw-medium py-3">Status</th>
                    <th className="border-top-0 border-bottom text-muted fw-medium py-3 text-end">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {history.length === 0 && !error && (
                    <tr>
                      <td colSpan={5} className="text-muted text-center py-4">
                        You have not joined any teams yet.
                      </td>
                    </tr>
                  )}
                  {history.map((item) => (
                    <tr key={item.id}>
                      <td className="py-3">
                        <div className="fw-bold" style={{ color: 'var(--cf-text-primary)' }}>{item.name}</div>
                        <Badge bg="success" className="mt-1">{item.status}</Badge>
                      </td>
                      <td className="py-3 text-muted">{item.date}</td>
                      <td className="py-3">
                        <div className="fw-medium text-dark">{item.team}</div>
                        <div className="text-muted small">{item.role}</div>
                      </td>
                      <td className="py-3">
                        {item.achievement === 'Submitted' ? (
                          <div className="d-flex align-items-center gap-2 text-warning fw-bold">
                            <Trophy size={16} /> {item.achievement}
                          </div>
                        ) : (
                          <span className="text-muted">{item.achievement}</span>
                        )}
                      </td>
                      <td className="py-3 text-end">
                        <Button
                          variant="outline-primary"
                          size="sm"
                          className="d-flex align-items-center gap-1 ms-auto"
                          as="a"
                          href={item.link || '#'}
                          target={item.link ? '_blank' : undefined}
                          rel="noopener noreferrer"
                          disabled={!item.link}
                        >
                          View Details <ExternalLink size={14} />
                        </Button>
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

export default StudentDashboard;
