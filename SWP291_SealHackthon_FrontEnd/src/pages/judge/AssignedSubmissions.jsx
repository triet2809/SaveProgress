import { useEffect, useMemo, useState } from 'react';
import { Card, Table, Spinner, Alert, Form } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { getMyJudgeSubmissions, getScores, markAllNotificationsRead } from '../../api/hackathonApi';
import { getStoredUser, getInitials } from '../../utils/authUser';
import styles from './AssignedSubmissions.module.css';
import { useSearchParams } from 'react-router-dom';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const asArray = (data) => data?.content || data || [];

const AssignedSubmissions = () => {
  const navigate = useNavigate();
  const user = getStoredUser();
  const judgeId = user?.id;
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const roundId = searchParams.get('roundId') || '';
  const trackId = searchParams.get('trackId') || '';
  const [assignments, setAssignments] = useState([]);

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    markAllNotificationsRead('submissions').catch(() => {});
    markAllNotificationsRead('assignments').catch(() => {});
    if (!judgeId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setError('No logged-in judge found.');
      setLoading(false);
      return;
    }
    let active = true;
    (async () => {
      try {
        const all = asArray(await getMyJudgeSubmissions());
        const subs = eventId ? asArray(await getMyJudgeSubmissions({ eventId, ...(roundId ? { roundId } : {}), ...(trackId ? { trackId } : {}) })) : all;
        setAssignments(all);
        const scoreLists = await Promise.all(
          subs.map((submission) => getScores({ submissionId: submission.submissionId, size: 200 }).catch(() => null))
        );
        const myScores = scoreLists
          .flatMap((r) => asArray(r))
          .filter((sc) => sc.judgeId === judgeId);

        // average this judge's scores per submission
        const bySubmission = {};
        myScores.forEach((sc) => {
          (bySubmission[sc.submissionId] ||= []).push(Number(sc.score));
        });

        const mapped = subs.map((s) => {
          const vals = bySubmission[s.submissionId] || [];
          const scored = vals.length > 0;
          const avg = scored ? vals.reduce((a, b) => a + b, 0) / vals.length : null;
          return {
            id: s.submissionId,
            initials: getInitials(s.teamName),
            teamName: s.teamName,
            recognitions: s.recognitions,
            project: s.roundName,
            event: s.eventName,
            track: s.trackName,
            round: s.roundName,
            repoUrl: s.repoUrl,
            presentationUrl: s.presentationUrl,
            demoUrl: s.demoUrl,
            reviewStatus: s.reviewStatus,
            submitted: s.submittedAt ? new Date(s.submittedAt).toLocaleDateString() : '—',
            status: scored ? 'Completed' : 'Pending',
            score: scored ? `${avg.toFixed(0)}/100` : null,
          };
        });
        if (active) setRows(mapped);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load submissions');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [judgeId, eventId, roundId, trackId]);

  const totals = useMemo(() => {
    const total = rows.length;
    const completed = rows.filter((s) => s.status === 'Completed').length;
    return { total, completed, pending: total - completed };
  }, [rows]);

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <Spinner animation="border" role="status" />
      </div>
    );
  }

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Assigned Submissions</h1>
        <div className={styles.pageSubtitle}>
          {totals.total} total · {totals.completed} completed · {totals.pending} pending
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
        <Form.Select value={roundId} disabled={!eventId} onChange={(e) => {
          const next = new URLSearchParams(searchParams);
          if (e.target.value) {
            next.set('roundId', e.target.value);
            const selected = assignments.find((a) => a.roundId === e.target.value); if (selected?.trackId) next.set('trackId', selected.trackId);
          } else { next.delete('roundId'); next.delete('trackId'); }
          setSearchParams(next);
        }}>
          <option value="">All assigned rounds</option>
          {[...new Map(assignments.filter((a) => a.eventId === eventId).map((a) => [a.roundId, a])).values()].map((a) => <option key={a.roundId} value={a.roundId}>{a.roundName}</option>)}
        </Form.Select>
      </div>

      <Card className={styles.tableCard}>
        <Table responsive className={styles.judgeTable}>
          <thead>
            <tr>
              <th>TEAM</th>
              <th>PROJECT</th>
              <th>TRACK</th>
              <th>ROUND</th>
              <th>RESOURCES</th>
              <th>SUBMITTED</th>
              <th>STATUS</th>
              <th>SCORE</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr>
                <td colSpan={9} className="text-center text-muted py-4">
                  No assigned submissions.
                </td>
              </tr>
            )}
            {rows.map((submission) => (
              <tr key={submission.id}>
                <td>
                  <div className={styles.teamCell}>
                    <div className={`${styles.teamAvatar} ${styles.avatarBg}`}>
                      {submission.initials}
                    </div>
                    <span className={styles.teamName}>{submission.teamName}</span>
                    <TeamRecognitionBadge recognitions={submission.recognitions} />
                  </div>
                </td>
                <td>{submission.project}</td>
                <td>
                  <span className={styles.categoryBadge} style={{ backgroundColor: 'var(--cf-primary-subtle)', color: 'var(--cf-primary)' }}>{submission.track}</span>
                </td>
                <td>
                  <span className={styles.categoryBadge}>{submission.round}</span>
                </td>
                <td>
                  {submission.repoUrl && <a href={submission.repoUrl} target="_blank" rel="noreferrer">Repo</a>}
                  {submission.presentationUrl && <> · <a href={submission.presentationUrl} target="_blank" rel="noreferrer">Slides</a></>}
                  {submission.demoUrl && <> · <a href={submission.demoUrl} target="_blank" rel="noreferrer">Demo</a></>}
                </td>
                <td>{submission.submitted}</td>
                <td>
                  <span className={`${styles.statusBadge} ${submission.status === 'Completed' ? styles.statusCompleted : styles.statusPending}`}>
                    {submission.status}
                  </span>
                </td>
                <td>
                  {submission.score ? (
                    <span className={styles.scoreValue}>{submission.score}</span>
                  ) : (
                    <span className={styles.scoreDash}>—</span>
                  )}
                </td>
                <td>
                  {submission.status === 'Pending' ? (
                    <span
                      className={styles.actionEvaluate}
                      onClick={() => navigate('/judge/score/' + submission.id)}
                    >
                      Evaluate &rarr;
                    </span>
                  ) : (
                    <span
                      className={styles.actionView}
                      onClick={() => navigate('/judge/view-evaluation/' + submission.id)}
                    >
                      View
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      </Card>
    </div>
  );
};

export default AssignedSubmissions;
