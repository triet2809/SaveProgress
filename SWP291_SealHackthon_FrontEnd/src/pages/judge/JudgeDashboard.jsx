import React, { useEffect, useMemo, useState } from 'react';
import { Row, Col, Card, Spinner, Alert } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { FileText, CheckCircle, Tag, Star } from 'lucide-react';
import { getJudgeSubmissions, getScores } from '../../api/hackathonApi';
import { getStoredUser, getInitials } from '../../utils/authUser';
import StatCard from '../../components/ui/StatCard';
import StatusBadge from '../../components/ui/StatusBadge';
import Avatar from '../../components/ui/Avatar';
import styles from './JudgeDashboard.module.css';

const asArray = (data) => data?.content || data || [];

const JudgeDashboard = () => {
  const navigate = useNavigate();
  const user = getStoredUser();
  const judgeId = user?.id;

  const [assigned, setAssigned] = useState([]);
  const [myScores, setMyScores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!judgeId) {
      setError('No logged-in judge found.');
      setLoading(false);
      return;
    }
    let active = true;
    (async () => {
      try {
        const subs = asArray(await getJudgeSubmissions(judgeId));
        // Gather this judge's scores across the rounds they judge.
        const roundIds = Array.from(new Set(subs.map((s) => s.roundId).filter(Boolean)));
        const scoreLists = await Promise.all(
          roundIds.map((rid) => getScores({ roundId: rid, size: 200 }).catch(() => null))
        );
        const scores = scoreLists
          .flatMap((r) => asArray(r))
          .filter((sc) => sc.judgeId === judgeId);
        if (!active) return;
        setAssigned(subs);
        setMyScores(scores);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load dashboard');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [judgeId]);

  const stats = useMemo(() => {
    const scoredSubmissionIds = new Set(myScores.map((s) => s.submissionId));
    const total = assigned.length;
    const completed = assigned.filter((s) => scoredSubmissionIds.has(s.submissionId)).length;
    const pending = total - completed;
    const rounds = new Set(assigned.map((s) => s.roundName).filter(Boolean));
    const scoreVals = myScores.map((s) => Number(s.score)).filter((n) => !Number.isNaN(n));
    const avg = scoreVals.length
      ? scoreVals.reduce((a, b) => a + b, 0) / scoreVals.length
      : 0;
    const highest = scoreVals.length ? Math.max(...scoreVals) : 0;
    const lowest = scoreVals.length ? Math.min(...scoreVals) : 0;
    const std = scoreVals.length
      ? Math.sqrt(scoreVals.reduce((a, b) => a + (b - avg) ** 2, 0) / scoreVals.length)
      : 0;
    const pct = total ? (completed / total) * 100 : 0;
    return {
      total,
      completed,
      pending,
      roundCount: rounds.size,
      roundNames: Array.from(rounds).slice(0, 2).join(' · ') || '—',
      avg,
      highest,
      lowest,
      std,
      pct,
    };
  }, [assigned, myScores]);

  const pendingQueue = useMemo(() => {
    const scoredSubmissionIds = new Set(myScores.map((s) => s.submissionId));
    return assigned.filter((s) => !scoredSubmissionIds.has(s.submissionId));
  }, [assigned, myScores]);

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <Spinner animation="border" role="status" />
      </div>
    );
  }

  return (
    <div className={styles.dashboard}>
      {error && <Alert variant="danger">{error}</Alert>}

      <div className="mb-4">
        <h1 className={styles.greeting}>Good day, {user?.fullName || 'Judge'}</h1>
        <p className="text-muted mb-0">
          {stats.pending} submission{stats.pending === 1 ? '' : 's'} pending evaluation
        </p>
      </div>

      <Row className="g-4 mb-4">
        <Col md={3}>
          <StatCard
            icon={FileText} iconColor="#ef4444" iconBg="#fee2e2"
            value={String(stats.pending)} title="Pending Reviews" subtitle={`${stats.total} total assigned`}
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={CheckCircle} iconColor="#10b981" iconBg="#d1fae5"
            value={String(stats.completed)} title="Completed" subtitle={`${stats.pct.toFixed(1)}% evaluated`}
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={Tag} iconColor="#3b82f6" iconBg="#dbeafe"
            value={String(stats.roundCount)} title="Rounds" subtitle={stats.roundNames}
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={Star} iconColor="#f59e0b" iconBg="#fef3c7"
            value={stats.avg.toFixed(1)} title="Avg. Score" subtitle="Out of 100 points"
          />
        </Col>
      </Row>

      <Row className="g-4">
        <Col md={8}>
          <Card className="h-100">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center mb-4">
                <h5 className={styles.cardTitle}>Submission Queue</h5>
                <span className={styles.pendingBadge}>{stats.pending} pending</span>
              </div>

              <div className={styles.submissionList}>
                {pendingQueue.length === 0 && (
                  <div className="text-muted">No pending submissions.</div>
                )}
                {pendingQueue.map((sub) => (
                  <div key={sub.submissionId} className={styles.submissionItem}>
                    <Avatar
                      initials={getInitials(sub.teamName)}
                      bg="var(--cf-bg-main)"
                      color="var(--cf-status-success)"
                    />
                    <div className={styles.submissionInfo}>
                      <div className={styles.teamName}>{sub.teamName}</div>
                      <div className={styles.projectName}>{sub.roundName}</div>
                    </div>
                    <div className={styles.submissionTags}>
                      <StatusBadge type={sub.roundName} />
                    </div>
                    <button
                      className={styles.reviewBtn}
                      onClick={() => navigate(`/judge/score/${sub.submissionId}`)}
                    >
                      Review →
                    </button>
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col md={4}>
          <div className="d-flex flex-column gap-4 h-100">
            <Card>
              <Card.Body>
                <h5 className={styles.cardTitle}>Evaluation Progress</h5>

                <div className={styles.progressHeader}>
                  <div className={styles.progressValue}>{stats.completed}<span className={styles.progressTotal}>/{stats.total}</span></div>
                  <div className={styles.progressSubtitle}>Submissions evaluated</div>
                </div>

                <div className="progress mt-4 mb-4" style={{ height: '6px' }}>
                  <div className="progress-bar bg-success" style={{ width: `${stats.pct}%` }}></div>
                </div>

                <div className={styles.progressStats}>
                  <div className={styles.statRow}>
                    <span className={styles.statLabel}>Completed</span>
                    <span className={styles.statValueSuccess}>{stats.completed}</span>
                  </div>
                  <div className={styles.statRow}>
                    <span className={styles.statLabel}>Pending</span>
                    <span className={styles.statValueDanger}>{stats.pending}</span>
                  </div>
                  <div className={styles.statRow}>
                    <span className={styles.statLabel}>In Progress</span>
                    <span className={styles.statValueInfo}>0</span>
                  </div>
                </div>
              </Card.Body>
            </Card>

            <Card className="flex-grow-1">
              <Card.Body>
                <h5 className={styles.cardTitle}>Scoring Summary</h5>

                <div className={styles.scoringList}>
                  <div className={styles.scoringItem}>
                    <span className={styles.scoringLabel}>Highest Score</span>
                    <span className={styles.scoringValueSuccess}>{stats.highest.toFixed(0)}/100</span>
                  </div>
                  <div className={styles.scoringItem}>
                    <span className={styles.scoringLabel}>Lowest Score</span>
                    <span className={styles.scoringValueDanger}>{stats.lowest.toFixed(0)}/100</span>
                  </div>
                  <div className={styles.scoringItem}>
                    <span className={styles.scoringLabel}>Average</span>
                    <span className={styles.scoringValue}>{stats.avg.toFixed(1)}/100</span>
                  </div>
                  <div className={styles.scoringItem}>
                    <span className={styles.scoringLabel}>Std. Deviation</span>
                    <span className={styles.scoringValue}>±{stats.std.toFixed(1)}</span>
                  </div>
                </div>
              </Card.Body>
            </Card>
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default JudgeDashboard;
