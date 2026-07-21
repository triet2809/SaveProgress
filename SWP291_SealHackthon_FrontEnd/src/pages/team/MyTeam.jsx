import { useEffect, useState } from 'react';
import { Row, Col, Card, ProgressBar, Spinner, Alert, Button, Modal } from 'react-bootstrap';
import { Code, Globe, FileText, ExternalLink, Key, Copy, Check, LogOut, MessageSquare } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getMyTeams, getTrack, getSubmissions, leaveTeam, getMentorFeedbacks, markAllNotificationsRead } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';
import styles from './MyTeam.module.css';

const MyTeam = () => {
  const navigate = useNavigate();
  const currentUser = getStoredUser() || {};
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [team, setTeam] = useState(null);
  const [trackName, setTrackName] = useState('');
  const [submission, setSubmission] = useState(null);
  const [feedbacks, setFeedbacks] = useState([]);
  const [copied, setCopied] = useState(false);
  const [showLeave, setShowLeave] = useState(false);
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    // Opening My Team clears the mentor-feedback red dot.
    markAllNotificationsRead('mentor_feedback').catch(() => {});
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : teams?.content || [];
        const current = list[0] || null;
        if (!active) return;
        setTeam(current);
        if (current?.trackId) {
          try {
            const track = await getTrack(current.trackId);
            if (active) setTrackName(track?.name || '');
          } catch { /* track optional */ }
        }
        if (current?.id) {
          try {
            const subs = await getSubmissions({ teamId: current.id });
            const subList = subs?.content || subs || [];
            if (active) setSubmission(subList[0] || null);
          } catch { /* submission optional */ }
          try {
            const fbRes = await getMentorFeedbacks({ teamId: current.id, size: 100 });
            const fbList = fbRes?.content || fbRes || [];
            if (active) setFeedbacks(fbList.slice().sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)));
          } catch { /* feedback optional */ }
        }
      } catch (e) {
        if (active) setError(e.message || 'Failed to load team');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const getResourceIcon = (type) => {
    switch (type) {
      case 'github': return <Code size={16} />;
      case 'globe': return <Globe size={16} />;
      case 'file': return <FileText size={16} />;
      default: return <ExternalLink size={16} />;
    }
  };

  const copyCode = async () => {
    if (!team?.inviteCode) return;
    try {
      await navigator.clipboard.writeText(team.inviteCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch { /* clipboard blocked */ }
  };

  const handleLeave = async () => {
    setLeaving(true);
    setError('');
    try {
      await leaveTeam(team.id);
      // No longer on a team -> back to the student area to create/join another.
      navigate('/student/dashboard', { replace: true });
    } catch (e) {
      setError(e.message || 'Failed to leave team');
      setShowLeave(false);
    } finally {
      setLeaving(false);
    }
  };

  if (loading) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (error) {
    return <Alert variant="danger" className="my-3">{error}</Alert>;
  }

  if (!team) {
    return <Alert variant="info" className="my-3">You are not part of any team yet.</Alert>;
  }

  // Derive resources from submission links (BE has no dedicated resources list).
  const resources = [
    submission?.repoUrl && { id: 1, name: 'GitHub Repository', type: 'github', url: submission.repoUrl },
    submission?.demoUrl && { id: 2, name: 'Live Demo', type: 'globe', url: submission.demoUrl },
    submission?.reportUrl && { id: 3, name: 'Project Docs', type: 'file', url: submission.reportUrl },
    submission?.slideUrl && { id: 4, name: 'Design Files', type: 'external', url: submission.slideUrl },
  ].filter(Boolean);

  const filledLinks = [submission?.repoUrl, submission?.demoUrl, submission?.slideUrl, submission?.reportUrl].filter(Boolean).length;
  const progress = submission ? Math.round((filledLinks / 4) * 100) : 0;
  const project = submission?.teamName || team.name;

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>My Team</h1>
        <div className={styles.pageSubtitle}>
          {team.name}{trackName ? ` · ${trackName}` : ''}
        </div>
        <TeamRecognitionBadge recognitions={team.recognitions} variant="detailed" className="mt-2" />
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible className="mb-3">{error}</Alert>}

      {/* Invite code + leave controls */}
      <Card className="mb-4 border-0 shadow-sm">
        <Card.Body className="p-3 d-flex align-items-center justify-content-between flex-wrap gap-2">
          <div className="d-flex align-items-center gap-2">
            <Key size={18} className="text-primary" />
            <span className="text-muted">Invite code:</span>
            {team.inviteCode ? (
              <>
                <span className="fw-bold text-primary" style={{ letterSpacing: '2px', fontSize: '1.1rem' }}>{team.inviteCode}</span>
                <Button variant="link" className="p-0 ms-1 text-secondary" onClick={copyCode} title="Copy">
                  {copied ? <Check size={16} className="text-success" /> : <Copy size={16} />}
                </Button>
              </>
            ) : <span className="text-muted">—</span>}
          </div>
          <Button variant="outline-danger" size="sm" className="d-flex align-items-center gap-1" onClick={() => setShowLeave(true)}>
            <LogOut size={16} /> Leave team
          </Button>
        </Card.Body>
      </Card>

      <Row className="g-4">
        {/* Left Column: Project Overview */}
        <Col lg={8}>
          <Card className="mb-4 border-0 shadow-sm">
            <Card.Body className="p-4">
              <h5 className={styles.cardTitle}>Project Overview</h5>

              <div className={styles.projectTitle}>{project}</div>
              <div className={styles.projectSubtitle}>{trackName || 'Hackathon Project'}</div>

              <p className={styles.projectDescription}>
                {submission?.apiMetadata && submission.apiMetadata !== '{}'
                  ? submission.apiMetadata
                  : 'Submission details and project summary will appear here once your team submits.'}
              </p>

              <div className={styles.techStack}>
                {(trackName ? [trackName] : []).map((tech, index) => (
                  <span
                    key={index}
                    className={`${styles.techBadge} ${index === 0 ? styles.primary : ''}`}
                  >
                    {tech}
                  </span>
                ))}
              </div>

              <div className="mt-4 pt-2">
                <div className={styles.progressLabel}>
                  <span>Submission completeness</span>
                  <span className={styles.progressValue}>{progress}%</span>
                </div>
                <ProgressBar
                  now={progress}
                  variant="primary"
                  style={{ height: '6px' }}
                />
              </div>
            </Card.Body>
          </Card>

          {/* Mentor feedback — read-only for the team */}
          <Card className="border-0 shadow-sm">
            <Card.Body className="p-4">
              <div className="d-flex align-items-center gap-2 mb-3">
                <MessageSquare size={20} className="text-primary" />
                <h5 className={styles.cardTitle} style={{ marginBottom: 0 }}>Mentor Feedback</h5>
              </div>
              {feedbacks.length === 0 ? (
                <div className="text-muted small">No mentor feedback yet.</div>
              ) : (
                <div className="d-flex flex-column gap-3">
                  {feedbacks.map((fb) => (
                    <div key={fb.id} className="p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)', border: '1px solid var(--cf-border-color)' }}>
                      <div className="d-flex justify-content-between align-items-center mb-2">
                        <span className="fw-bold small text-primary">{fb.mentorEmail || 'Mentor'}</span>
                        <span className="text-muted" style={{ fontSize: '0.75rem' }}>
                          {fb.createdAt ? new Date(fb.createdAt).toLocaleString() : ''}{fb.roundName ? ` · ${fb.roundName}` : ''}
                        </span>
                      </div>
                      <p className="mb-0 text-muted small" style={{ lineHeight: '1.5', whiteSpace: 'pre-wrap' }}>{fb.content}</p>
                    </div>
                  ))}
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>

        {/* Right Column: Resources & Members */}
        <Col lg={4}>
          <Card className="h-100 border-0 shadow-sm">
            <Card.Body className="p-4">
              <h5 className={styles.cardTitle}>Resources</h5>

              <div className={styles.resourcesList}>
                {resources.length === 0 && (
                  <div className="text-muted small">No resources submitted yet.</div>
                )}
                {resources.map((resource) => (
                  <a href={resource.url} key={resource.id} target="_blank" rel="noreferrer" className={styles.resourceLink}>
                    {getResourceIcon(resource.type)}
                    <span>{resource.name}</span>
                  </a>
                ))}
              </div>

              <div className={styles.divider}></div>

              <h5 className={styles.cardTitle} style={{ marginBottom: '1rem' }}>Team Leader</h5>

              {(() => {
                const leader = (team.members || []).find((m) => m.role === 'leader') || (team.members || [])[0];
                if (!leader) return <div className="text-muted small">No members.</div>;
                const initials = (leader.fullName || leader.email || 'U')
                  .split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
                return (
                  <div className={styles.mentorCard}>
                    <div className={styles.mentorAvatar}>{initials}</div>
                    <div>
                      <div className={styles.mentorName}>{leader.fullName || leader.email}</div>
                      <div className={styles.mentorRole}>{leader.role}</div>
                    </div>
                  </div>
                );
              })()}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Modal show={showLeave} onHide={() => !leaving && setShowLeave(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Leave team?</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {(() => {
            const me = (team.members || []).find((m) => m.userId === currentUser.id);
            const isLeader = me && String(me.role).toLowerCase() === 'leader';
            const count = (team.members || []).length;
            if (isLeader && count > 1) {
              return <span>You are the leader. When you leave, leadership passes to the earliest-joined member. Are you sure?</span>;
            }
            if (count <= 1) {
              return <span>You are the only member. Leaving will <strong>delete this team</strong>. Are you sure?</span>;
            }
            return <span>Are you sure you want to leave this team?</span>;
          })()}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowLeave(false)} disabled={leaving}>Cancel</Button>
          <Button variant="danger" onClick={handleLeave} disabled={leaving} className="d-flex align-items-center gap-2">
            {leaving ? <Spinner animation="border" size="sm" /> : <LogOut size={16} />} Leave team
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default MyTeam;
