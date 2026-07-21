import React, { useEffect, useState } from 'react';
import { Row, Col, Card, Spinner, Alert } from 'react-bootstrap';
import { Users, Tag, MessageSquare, Calendar } from 'lucide-react';
import { getMentorTeams, getMentorFeedbacks } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';
import StatCard from '../../components/ui/StatCard';
import StatusBadge from '../../components/ui/StatusBadge';
import Avatar from '../../components/ui/Avatar';
import styles from './MentorDashboard.module.css';

const statusMeta = (teamStatus) => {
  switch ((teamStatus || '').toLowerCase()) {
    case 'active':
      return { status: 'On Track', color: 'success', complete: 80 };
    case 'disqualified':
      return { status: 'At Risk', color: 'danger', complete: 30 };
    default:
      return { status: 'Needs Attention', color: 'warning', complete: 60 };
  }
};

const MentorDashboard = () => {
  const user = getStoredUser();
  const mentorId = user?.id;
  const [teams, setTeams] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    async function load() {
      if (!mentorId) {
        setError('No mentor session found.');
        setLoading(false);
        return;
      }
      try {
        const [teamsRes, fbRes] = await Promise.all([
          getMentorTeams(mentorId),
          getMentorFeedbacks({ mentorId, size: 100 }),
        ]);
        if (!active) return;
        setTeams(Array.isArray(teamsRes) ? teamsRes : teamsRes?.content || []);
        setFeedbacks(fbRes?.content || fbRes || []);
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
  }, [mentorId]);

  const categoryNames = [...new Set(teams.map((t) => t.trackName).filter(Boolean))];
  const recentFeedbacks = [...feedbacks]
    .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
    .slice(0, 5);

  if (loading) {
    return (
      <div className={styles.dashboard}>
        <div className="text-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      </div>
    );
  }

  return (
    <div className={styles.dashboard}>
      <div className="mb-4">
        <h1 className={styles.greeting}>Good morning, {user?.fullName || 'Mentor'}</h1>
        <p className="text-muted mb-0">
          {teams.length} team{teams.length === 1 ? '' : 's'} under your mentorship
        </p>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Row className="g-4 mb-4">
        <Col md={3}>
          <StatCard
            icon={Users} iconColor="#a855f7" iconBg="#f3e8ff"
            value={String(teams.length)} title="Assigned Teams"
            subtitle={teams.map((t) => t.teamName).slice(0, 3).join(', ') || 'None yet'}
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={Tag} iconColor="#3b82f6" iconBg="#dbeafe"
            value={String(categoryNames.length)} title="Active Categories"
            subtitle={categoryNames.join(' · ') || 'None yet'}
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={MessageSquare} iconColor="#10b981" iconBg="#d1fae5"
            value={String(feedbacks.length)} title="Feedback Given"
            subtitle="Across your teams"
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={Calendar} iconColor="#f59e0b" iconBg="#fef3c7"
            value={String(new Set(teams.map((t) => t.roundName).filter(Boolean)).size)}
            title="Active Rounds" subtitle="In your tracks"
          />
        </Col>
      </Row>

      <Row className="g-4">
        <Col md={8}>
          <Card className="h-100">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center mb-4">
                <h5 className={styles.cardTitle}>Team Progress</h5>
                <span className={styles.teamsCountBadge}>{teams.length} teams</span>
              </div>

              <div className={styles.teamProgressList}>
                {teams.length === 0 && (
                  <div className="text-muted">No teams assigned yet.</div>
                )}
                {teams.map((team) => {
                  const meta = statusMeta(team.teamStatus);
                  return (
                    <div key={team.teamId} className={styles.teamProgressItem}>
                      <Avatar
                        initials={(team.teamName || '?').charAt(0)}
                        bg={meta.color === 'success' ? '#d1fae5' : meta.color === 'warning' ? '#fef3c7' : '#fee2e2'}
                        color={meta.color === 'success' ? '#10b981' : meta.color === 'warning' ? '#f59e0b' : '#ef4444'}
                      />
                      <div className={styles.teamInfo}>
                        <div className="d-flex justify-content-between align-items-center mb-1">
                          <div>
                            <div className={styles.teamName}>{team.teamName}</div>
                            <div className={styles.projectName}>{team.trackName}</div>
                          </div>
                          <StatusBadge status={meta.status} />
                        </div>

                        <div className={styles.progressTrack}>
                          <div
                            className={`${styles.progressBar} bg-${meta.color}`}
                            style={{ width: `${meta.complete}%` }}
                          ></div>
                        </div>
                        <div className={styles.progressText}>{meta.complete}% complete</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col md={4}>
          <div className="d-flex flex-column gap-4 h-100">
            <Card>
              <Card.Body>
                <h5 className={styles.cardTitle}>Your Tracks</h5>

                <div className={styles.sessionList}>
                  {categoryNames.length === 0 && (
                    <div className="text-muted">No tracks yet.</div>
                  )}
                  {categoryNames.map((name) => {
                    const roundName = teams.find((t) => t.trackName === name)?.roundName;
                    return (
                      <div key={name} className={styles.sessionItem}>
                        <div className={styles.sessionTeam}>{name}</div>
                        <div className={styles.sessionType}>{roundName || 'Active'}</div>
                      </div>
                    );
                  })}
                </div>
              </Card.Body>
            </Card>

            <Card className="flex-grow-1">
              <Card.Body>
                <h5 className={styles.cardTitle}>Recent Feedback</h5>

                <div className={styles.feedbackList}>
                  {recentFeedbacks.length === 0 && (
                    <div className="text-muted">No feedback given yet.</div>
                  )}
                  {recentFeedbacks.map((item) => (
                    <div key={item.id} className={styles.feedbackItem}>
                      <div className="d-flex justify-content-between">
                        <div className={styles.feedbackTeam}>{item.teamName}</div>
                        <div className={styles.feedbackTime}>
                          {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : ''}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </Card.Body>
            </Card>
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default MentorDashboard;
