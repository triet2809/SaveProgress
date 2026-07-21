import { useEffect, useState } from 'react';
import { Card, Table, Spinner, Alert, Form } from 'react-bootstrap';
import { getMyMentorTeams } from '../../api/hackathonApi';
import { useSearchParams } from 'react-router-dom';
import styles from './AssignedTeams.module.css';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const statusFromTeam = (teamStatus) => {
  switch ((teamStatus || '').toLowerCase()) {
    case 'active':
      return { label: 'On Track', progress: 80 };
    case 'disqualified':
      return { label: 'At Risk', progress: 30 };
    default:
      return { label: 'Needs Attention', progress: 60 };
  }
};

const AssignedTeams = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const trackId = searchParams.get('trackId') || '';
  const roundId = searchParams.get('roundId') || '';
  const [assignments, setAssignments] = useState([]);
  const [teams, setTeams] = useState([]);
  // Map teamId -> submission projectName (BE project data)
  const [projectByTeam] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const all = await getMyMentorTeams();
        const res = eventId ? await getMyMentorTeams({ eventId, ...(trackId ? { trackId } : {}), ...(roundId ? { roundId } : {}) }) : all;
        const list = Array.isArray(res) ? res : res?.content || [];
        if (!active) return;
        setAssignments(Array.isArray(all) ? all : all?.content || []);
        setTeams(list);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load teams');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, [eventId, trackId, roundId]);

  const getProgressColor = (progress) => {
    if (progress >= 80) return styles.fillGreen;
    if (progress >= 60) return styles.fillOrange;
    return styles.fillRed;
  };

  const getStatusClass = (status) => {
    switch (status) {
      case 'On Track': return styles.statusOnTrack;
      case 'Needs Attention': return styles.statusNeedsAttention;
      case 'At Risk': return styles.statusAtRisk;
      default: return '';
    }
  };

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Assigned Teams</h1>
        <div className={styles.pageSubtitle}>
          {teams.length} teams under your mentorship
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}
      <div className="d-flex gap-2 mb-3">
        <Form.Select value={eventId} onChange={(e) => {
          const next = new URLSearchParams(); if (e.target.value) next.set('eventId', e.target.value); setSearchParams(next);
        }}>
          <option value="">Select assigned event</option>
          {[...new Map(assignments.map((a) => [a.eventId, a])).values()].map((a) => <option key={a.eventId} value={a.eventId}>{a.eventName}</option>)}
        </Form.Select>
        <Form.Select value={trackId} disabled={!eventId} onChange={(e) => {
          const next = new URLSearchParams(searchParams); if (e.target.value) next.set('trackId', e.target.value); else next.delete('trackId'); next.delete('roundId'); setSearchParams(next);
        }}>
          <option value="">All assigned tracks</option>
          {[...new Map(assignments.filter((a) => a.eventId === eventId).map((a) => [a.trackId, a])).values()].map((a) => <option key={a.trackId} value={a.trackId}>{a.trackName}</option>)}
        </Form.Select>
        <Form.Select value={roundId} disabled={!trackId} onChange={(e) => {
          const next = new URLSearchParams(searchParams); if (e.target.value) next.set('roundId', e.target.value); else next.delete('roundId'); setSearchParams(next);
        }}>
          <option value="">All rounds</option>
          {[...new Map(assignments.filter((a) => a.eventId === eventId && a.trackId === trackId && a.roundId).map((a) => [a.roundId, a])).values()].map((a) => <option key={a.roundId} value={a.roundId}>{a.roundName}</option>)}
        </Form.Select>
      </div>

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      ) : (
        <Card className={styles.tableCard}>
          <div className="table-responsive">
            <Table className="mb-0" hover>
              <thead>
                <tr>
                  <th className={`border-top-0 ${styles.tableHeader}`}>Team</th>
                  <th className={`border-top-0 ${styles.tableHeader}`}>Project</th>
                  <th className={`border-top-0 ${styles.tableHeader}`}>Members</th>
                  <th className={`border-top-0 ${styles.tableHeader}`}>Category</th>
                  <th className={`border-top-0 ${styles.tableHeader}`}>Progress</th>
                  <th className={`border-top-0 ${styles.tableHeader}`}>Last Active</th>
                  <th className={`border-top-0 ${styles.tableHeader}`}>Status</th>
                </tr>
              </thead>
              <tbody>
                {teams.length === 0 && (
                  <tr>
                    <td colSpan={7} className="text-muted text-center py-4">
                      No teams assigned yet.
                    </td>
                  </tr>
                )}
                {teams.map((team) => {
                  const meta = statusFromTeam(team.teamStatus);
                  const project = projectByTeam[team.teamId] || team.projectName || '—';
                  const memberCount =
                    team.memberCount ??
                    team.membersCount ??
                    (Array.isArray(team.members) ? team.members.length : team.members);
                  return (
                    <tr key={team.teamId} className={styles.tableRow}>

                      <td className={styles.tableCell}>
                        <div className={styles.teamNameCell}>
                          <div className={styles.teamAvatar}>{(team.teamName || '?').charAt(0)}</div>
                          <span className={styles.teamName}>{team.teamName}</span>
                          <TeamRecognitionBadge recognitions={team.recognitions} />
                        </div>
                      </td>

                      <td className={styles.tableCell}>
                        <span className={styles.projectText}>{project}</span>
                      </td>

                      <td className={styles.tableCell}>
                        {memberCount ?? '—'}
                      </td>

                      <td className={styles.tableCell}>
                        <span className={styles.categoryBadge}>{team.trackName}</span>
                      </td>

                      <td className={styles.tableCell}>
                        {/* Progress has no BE field; derived from team status (display-only) */}
                        <div className={styles.progressWrapper}>
                          <div className={styles.progressBarContainer}>
                            <div
                              className={`${styles.progressBarFill} ${getProgressColor(meta.progress)}`}
                              style={{ width: `${meta.progress}%` }}
                            ></div>
                          </div>
                          <span className={styles.progressText}>{meta.progress}%</span>
                        </div>
                      </td>

                      <td className={styles.tableCell}>
                        {/* Last Active has no BE field (display-only) */}
                        {team.lastActive || '—'}
                      </td>

                      <td className={styles.tableCell}>
                        <span className={`${styles.statusBadge} ${getStatusClass(meta.label)}`}>
                          {meta.label}
                        </span>
                      </td>

                    </tr>
                  );
                })}
              </tbody>
            </Table>
          </div>
        </Card>
      )}
    </div>
  );
};

export default AssignedTeams;
