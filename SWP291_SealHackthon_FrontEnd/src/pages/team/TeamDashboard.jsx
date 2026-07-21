import { useEffect, useState } from 'react';
import { Row, Col, Card, Spinner, Alert } from 'react-bootstrap';
import { Clock, Users, FileText, Award, CheckCircle, Key, Copy, Check } from 'lucide-react';
import { getMyTeams, getTrack, getRounds, getSubmissions, getNotices } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';
import StatCard from '../../components/ui/StatCard';
import StatusBadge from '../../components/ui/StatusBadge';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';
import styles from './TeamDashboard.module.css';

const TeamDashboard = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [team, setTeam] = useState(null);
  const [trackName, setTrackName] = useState('');
  const [rounds, setRounds] = useState([]);
  const [submission, setSubmission] = useState(null);
  const [notices, setNotices] = useState([]);
  const [copied, setCopied] = useState(false);

  const user = getStoredUser();
  const firstName = (user?.fullName || 'there').split(' ')[0];

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : teams?.content || [];
        const current = list[0] || null;
        if (!active) return;
        setTeam(current);

        const jobs = [];
        if (current?.trackId) {
          jobs.push(getTrack(current.trackId).then((t) => active && setTrackName(t?.name || '')).catch(() => {}));
          jobs.push(getRounds({ trackId: current.trackId }).then((r) => {
            if (active) setRounds(r?.content || r || []);
          }).catch(() => {}));
        }
        if (current?.id) {
          jobs.push(getSubmissions({ teamId: current.id }).then((s) => {
            const subList = s?.content || s || [];
            if (active) setSubmission(subList[0] || null);
          }).catch(() => {}));
        }
        jobs.push(getNotices({ size: 5 }).then((n) => {
          if (active) setNotices(n?.content || n || []);
        }).catch(() => {}));
        await Promise.all(jobs);
      } catch (e) {
        if (active) setError(e.message || 'Failed to load dashboard');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  if (loading) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  const memberCount = team?.members?.length || 0;
  const filledLinks = [submission?.repoUrl, submission?.demoUrl, submission?.slideUrl, submission?.reportUrl].filter(Boolean).length;
  const progress = submission ? Math.round((filledLinks / 4) * 100) : 0;
  const nextRound = rounds
    .filter((r) => r.submissionDeadline)
    .sort((a, b) => new Date(a.submissionDeadline) - new Date(b.submissionDeadline))[0];
  const deadlineText = nextRound?.submissionDeadline
    ? new Date(nextRound.submissionDeadline).toLocaleString()
    : 'TBD';
  const daysRemaining = nextRound?.submissionDeadline
    ? Math.max(0, Math.ceil((new Date(nextRound.submissionDeadline) - new Date()) / (1000 * 60 * 60 * 24)))
    : '—';
  const submissionStatus = submission ? 'Submitted' : 'Draft';

  const copyCode = async () => {
    if (!team?.inviteCode) return;
    try {
      await navigator.clipboard.writeText(team.inviteCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch { /* clipboard blocked */ }
  };

  return (
    <div className={styles.dashboard}>
      <div className="mb-4">
        <h1 className={styles.greeting}>Welcome, {firstName}</h1>
        <p className="text-muted mb-0">
          {team ? team.name : 'No team'} • <span className="text-primary fw-medium">{daysRemaining} days to next deadline</span>
        </p>
        <TeamRecognitionBadge recognitions={team?.recognitions} variant="detailed" className="mt-2" />
      </div>

      {error && <Alert variant="danger" className="mb-4">{error}</Alert>}

      {team?.inviteCode && (
        <Card className="mb-4" style={{ border: '1px solid var(--cf-border-color)', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)' }}>
          <Card.Body className="p-3 d-flex align-items-center justify-content-between flex-wrap gap-2">
            <div className="d-flex align-items-center gap-2">
              <Key size={18} className="text-primary" />
              <span className="text-muted">Team invite code:</span>
              <span className="fw-bold text-primary" style={{ letterSpacing: '2px', fontSize: '1.1rem' }}>{team.inviteCode}</span>
              <button type="button" className="btn btn-link p-0 ms-1 text-secondary" onClick={copyCode} title="Copy">
                {copied ? <Check size={16} className="text-success" /> : <Copy size={16} />}
              </button>
            </div>
            <span className="text-muted small">Share this code so friends can join your team.</span>
          </Card.Body>
        </Card>
      )}


      <Row className="g-4 mb-4">
        <Col md={3}>
          <StatCard
            icon={Clock} iconColor="#3b82f6" iconBg="#dbeafe"
            value={daysRemaining} title="Days to Deadline" subtitle={nextRound ? nextRound.name : 'No upcoming round'}
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={Users} iconColor="#a855f7" iconBg="#f3e8ff"
            value={memberCount} title="Team Size" subtitle={team ? team.name : '—'}
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={FileText} iconColor="#f59e0b" iconBg="#fef3c7"
            value={submissionStatus} title="Submission" subtitle={`Deadline ${deadlineText}`}
          />
        </Col>
        <Col md={3}>
          <StatCard
            icon={Award} iconColor="#10b981" iconBg="#d1fae5"
            value={trackName || '—'} title="Track" subtitle={`${rounds.length} round(s)`}
          />
        </Col>
      </Row>

      <Row className="g-4 mb-4">
        <Col md={4}>
          <Card className="h-100">
            <Card.Body>
              <h5 className={styles.cardTitle}>Upcoming Deadlines</h5>
              <div className={styles.timeline}>
                {rounds.length === 0 && (
                  <div className="text-muted small">No rounds scheduled.</div>
                )}
                {[...rounds]
                  .filter((r) => r.submissionDeadline)
                  .sort((a, b) => new Date(a.submissionDeadline) - new Date(b.submissionDeadline))
                  .map((round, idx) => {
                    const isDanger = idx === 0;
                    return (
                      <div key={round.id} className={`${styles.timelineItem} ${isDanger ? styles.timelineDanger : ''}`}>
                        <div className={styles.timelineContent}>
                          <div className={styles.timelineTitle}>{round.name}</div>
                          <div className={isDanger ? styles.timelineTimeDanger : styles.timelineTime}>
                            {new Date(round.submissionDeadline).toLocaleString()}
                          </div>
                        </div>
                      </div>
                    );
                  })}
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col md={4}>
          <Card className="h-100">
            <Card.Body>
              <h5 className={styles.cardTitle}>Team {team ? team.name : ''}</h5>

              <div className="mb-4">
                <div className="text-muted small mb-1">Project</div>
                <div className="fw-semibold text-dark">{submission?.teamName || team?.name || '—'}</div>
                <div className="text-muted small">{trackName || 'No track assigned'}</div>
              </div>

              <div className="mb-4">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="text-muted small">Submission progress</span>
                  <span className="fw-semibold small">{progress}%</span>
                </div>
                <div className="progress" style={{ height: '8px' }}>
                  <div
                    className="progress-bar bg-primary"
                    role="progressbar"
                    style={{ width: `${progress}%` }}
                    aria-valuenow={progress}
                    aria-valuemin="0"
                    aria-valuemax="100"
                  ></div>
                </div>
              </div>

              <div className="d-flex gap-2 mt-auto">
                {trackName && <StatusBadge type={trackName} />}
                <StatusBadge type={`${memberCount} Members`} />
                <StatusBadge status={team?.status || submissionStatus} />
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col md={4}>
          <Card className="h-100">
            <Card.Body>
              <h5 className={styles.cardTitle}>Notifications</h5>
              <div className={styles.notificationsList}>
                {notices.length === 0 && (
                  <div className="text-muted small">No notices.</div>
                )}
                {notices.map((notif) => (
                  <div key={notif.id} className={styles.notificationItem}>
                    <div className={`${styles.notificationIcon} ${styles['bg-info']}`}>
                      <FileText size={14} />
                    </div>
                    <div>
                      <div className={styles.notificationMessage}>{notif.title}</div>
                      <div className={styles.notificationTime}>
                        {notif.createdAt ? new Date(notif.createdAt).toLocaleString() : ''}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Card>
        <Card.Body>
          <h5 className={styles.cardTitle}>Competition Progress</h5>
          <div className={styles.stepperContainer}>
            <div className={styles.stepperLinesContainer}>
              {[
                { completed: true },
                { completed: true },
                { completed: !!submission },
                { completed: false },
                { completed: false }
              ].map((s, i) => (
                <div key={i} className={`${styles.stepperLine} ${s.completed ? styles.lineCompleted : ''}`}></div>
              ))}
            </div>

            <div className={styles.stepperSteps}>
              {[
                { step: 1, label: 'Registration', completed: true },
                { step: 2, label: 'Team Formation', completed: memberCount > 0 },
                { step: 3, label: 'Submission', active: !submission, completed: !!submission },
                { step: 4, label: 'Evaluation', completed: false },
                { step: 5, label: 'Awards', completed: false }
              ].map((s) => (
                <div key={s.step} className={styles.stepItem}>
                  <div className={`${styles.stepCircle} ${s.completed ? styles.completed : s.active ? styles.active : ''}`}>
                    {s.completed ? <CheckCircle size={16} /> : s.step}
                  </div>
                  <div className={`${styles.stepLabel} ${s.active || s.completed ? styles.labelActive : ''}`}>{s.label}</div>
                </div>
              ))}
            </div>
          </div>
        </Card.Body>
      </Card>
    </div>
  );
};

export default TeamDashboard;
