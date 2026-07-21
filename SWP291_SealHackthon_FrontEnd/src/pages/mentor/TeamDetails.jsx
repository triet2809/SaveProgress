import { useEffect, useState } from 'react';
import { Card, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { getMentorTeams, getTeam, getSubmissions } from '../../api/hackathonApi';
import { getStoredUser, getInitials } from '../../utils/authUser';
import styles from './TeamDetails.module.css';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const progressFromStatus = (teamStatus) => {
  switch ((teamStatus || '').toLowerCase()) {
    case 'active':
      return { progress: 80, status: 'On Track' };
    case 'disqualified':
      return { progress: 30, status: 'At Risk' };
    default:
      return { progress: 60, status: 'Needs Attention' };
  }
};

const TeamDetails = () => {
  const user = getStoredUser();
  const mentorId = user?.id;
  const [teams, setTeams] = useState([]);
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
        const res = await getMentorTeams(mentorId);
        const assigned = Array.isArray(res) ? res : res?.content || [];
        // unique teams
        const uniqueTeams = [];
        const seen = new Set();
        assigned.forEach((t) => {
          if (t.teamId && !seen.has(t.teamId)) {
            seen.add(t.teamId);
            uniqueTeams.push(t);
          }
        });

        const details = await Promise.all(
          uniqueTeams.map(async (t) => {
            const [team, subRes] = await Promise.all([
              getTeam(t.teamId).catch(() => null),
              getSubmissions({ teamId: t.teamId, size: 5 }).catch(() => null),
            ]);
            const subs = subRes?.content || subRes || [];
            const sub = subs[0];
            const meta = progressFromStatus(t.teamStatus);
            return {
              id: t.teamId,
              teamInfo: { name: t.teamName, category: t.trackName },
              recognitions: team?.recognitions || t.recognitions,
              project: {
                title: sub?.repoUrl ? t.teamName : (t.teamName || 'Project'),
                subtitle: sub?.apiMetadata || t.roundName || '',
                description: sub
                  ? `Repo: ${sub.repoUrl || 'n/a'} · Demo: ${sub.demoUrl || 'n/a'}`
                  : 'No submission yet for this team.',
                tags: [t.trackName, t.roundName].filter(Boolean),
                progress: meta.progress,
                status: meta.status,
              },
              roster: (team?.members || []).map((m) => ({
                id: m.id,
                initials: getInitials(m.fullName || m.email),
                name: m.fullName || m.email,
                role: m.role,
              })),
            };
          }),
        );

        if (active) setTeams(details);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load team details');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, [mentorId]);

  const getStatusColorClass = (progress) => {
    if (progress >= 80) return { text: styles.statusGreen, bg: styles.bgGreen };
    if (progress >= 60) return { text: styles.statusOrange, bg: styles.bgOrange };
    return { text: styles.statusRed, bg: styles.bgRed };
  };

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Team Details</h1>
        <div className={styles.pageSubtitle}>
          Detailed overview of your assigned teams
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      ) : teams.length === 0 && !error ? (
        <div className="text-muted">No teams assigned yet.</div>
      ) : (
        teams.map((team) => {
          const statusColors = getStatusColorClass(team.project.progress);

          return (
            <div key={team.id} className={styles.teamSection}>
              <div className={styles.sectionHeader}>
                {team.teamInfo.name} <span className={styles.sectionCategory}>· {team.teamInfo.category}</span>
                <TeamRecognitionBadge recognitions={team.recognitions} className="ms-2" />
              </div>

              <Row>
                <Col md={7} className="mb-4 mb-md-0">
                  <Card className={styles.detailCard}>
                    <Card.Body className="p-4">
                      <h3 className={styles.cardTitle}>Project Details</h3>

                      <div className={styles.projectTitleLabel}>Title</div>
                      <div className={styles.projectTitle}>{team.project.title}</div>
                      <div className={styles.projectSubtitle}>{team.project.subtitle}</div>

                      <div className={styles.projectDescriptionLabel}>Description</div>
                      <div className={styles.projectDescription}>{team.project.description}</div>

                      <div className={styles.tagList}>
                        {team.project.tags.map((tag, idx) => (
                          <span key={idx} className={styles.techTag}>{tag}</span>
                        ))}
                      </div>

                      <div className={styles.progressContainer}>
                        <div className={styles.progressHeader}>
                          <span className={styles.progressLabel}>Overall Progress</span>
                          <span className={`${styles.progressStatus} ${statusColors.text}`}>
                            {team.project.progress}% - {team.project.status}
                          </span>
                        </div>
                        <div className={styles.progressBarContainer}>
                          <div
                            className={`${styles.progressBarFill} ${statusColors.bg}`}
                            style={{ width: `${team.project.progress}%` }}
                          ></div>
                        </div>
                      </div>

                    </Card.Body>
                  </Card>
                </Col>

                <Col md={5}>
                  <Card className={styles.detailCard}>
                    <Card.Body className="p-4">
                      <h3 className={styles.cardTitle}>Team Roster</h3>

                      <div className={styles.rosterList}>
                        {team.roster.length === 0 && (
                          <div className="text-muted">No members.</div>
                        )}
                        {team.roster.map((member) => (
                          <div key={member.id} className={styles.rosterItem}>
                            <div className={styles.memberAvatar}>{member.initials}</div>
                            <div className={styles.memberInfo}>
                              <span className={styles.memberName}>{member.name}</span>
                              <span className={styles.memberRole}>{member.role}</span>
                            </div>
                          </div>
                        ))}
                      </div>

                    </Card.Body>
                  </Card>
                </Col>
              </Row>
            </div>
          );
        })
      )}
    </div>
  );
};

export default TeamDetails;
