import React, { useEffect, useMemo, useState } from 'react';
import { Card, Button, Spinner, Alert } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { getSubmission, getRoundCriteria, getScores } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';
import styles from './ViewEvaluation.module.css';

const asArray = (data) => data?.content || data || [];

const ViewEvaluation = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const user = getStoredUser();
  const judgeId = user?.id;

  const [submission, setSubmission] = useState(null);
  const [criteria, setCriteria] = useState([]);
  const [myScores, setMyScores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const sub = await getSubmission(id);
        const [crit, scores] = await Promise.all([
          getRoundCriteria({ roundId: sub.roundId, size: 100 }).catch(() => null),
          getScores({ submissionId: id, size: 200 }).catch(() => null),
        ]);
        if (!active) return;
        setSubmission(sub);
        setCriteria(asArray(crit));
        setMyScores(asArray(scores).filter((sc) => sc.judgeId === judgeId));
      } catch (err) {
        if (active) setError(err.message || 'Failed to load evaluation');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [id, judgeId]);

  const breakdown = useMemo(() => {
    return criteria.map((c) => {
      const sc = myScores.find((s) => s.criterionId === c.id);
      return { name: c.name, score: sc ? Number(sc.score) : null };
    });
  }, [criteria, myScores]);

  const finalScore = useMemo(() => {
    const vals = breakdown.map((b) => b.score).filter((v) => v !== null);
    if (!vals.length) return 0;
    return Math.round(vals.reduce((a, b) => a + b, 0) / vals.length);
  }, [breakdown]);

  const notes = myScores.find((s) => s.comment)?.comment || 'No notes recorded.';

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
        <div>
          <h1 className={styles.pageTitle}>Evaluation Results</h1>
          <div className={styles.pageSubtitle}>
            Review your previously submitted evaluation
          </div>
        </div>
        <span className={styles.backBtn} onClick={() => navigate('/judge/submissions')}>
          &larr; Back to Submissions
        </span>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Card className={styles.viewCard}>
        <div className={styles.teamHeader}>
          <div>
            <h3 className={styles.teamName}>{submission?.teamName || 'Team'}</h3>
            <div className={styles.projectName}>Round: {submission?.roundName || submission?.roundId}</div>
          </div>
          <div className={styles.finalScoreBox}>
            <div className={styles.scoreLabel}>Final Average Score</div>
            <div className={styles.scoreValue}>{finalScore}<span style={{fontSize: '1.25rem'}}>/100</span></div>
          </div>
        </div>

        <h5 className="mb-4" style={{fontWeight: 600}}>Score Breakdown</h5>

        <div className={styles.breakdownGrid}>
          {breakdown.length === 0 && (
            <div className="text-muted">No criteria scored yet.</div>
          )}
          {breakdown.map((b, idx) => (
            <div className={styles.breakdownItem} key={idx}>
              <span className={styles.breakdownLabel}>{b.name}</span>
              <span className={styles.breakdownScore}>
                {b.score !== null ? b.score : '—'}
                <span style={{fontSize: '0.875rem', color: '#6b7280'}}>/100</span>
              </span>
            </div>
          ))}
        </div>

        <div className={styles.notesSection}>
          <div className={styles.notesLabel}>Private Notes</div>
          <div className={styles.notesContent}>
            {notes}
          </div>
        </div>

        <div className={styles.actionRow}>
          <Button
            className={styles.reevaluateBtn}
            onClick={() => navigate(`/judge/score/${id}`)}
          >
            Re-evaluate Project
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default ViewEvaluation;
