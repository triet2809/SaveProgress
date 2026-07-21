import React, { useEffect, useMemo, useState } from 'react';
import { Card, Button, Form, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertTriangle, ExternalLink } from 'lucide-react';
import {
  getSubmission,
  getRoundCriteria,
  getScores,
  upsertScore,
} from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';
import styles from './ScoringInterface.module.css';

const asArray = (data) => data?.content || data || [];

const ScoringInterface = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const user = getStoredUser();
  const judgeId = user?.id;

  const [submission, setSubmission] = useState(null);
  const [criteria, setCriteria] = useState([]);
  const [scores, setScores] = useState({}); // criterionId -> value
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const sub = await getSubmission(id);
        const crit = asArray(await getRoundCriteria({ roundId: sub.roundId, size: 100 }));
        // Preload any existing scores by this judge for this submission
        let existing = [];
        try {
          existing = asArray(await getScores({ submissionId: id, size: 200 }))
            .filter((sc) => sc.judgeId === judgeId);
        } catch { /* ignore */ }
        if (!active) return;
        setSubmission(sub);
        setCriteria(crit);
        const seed = {};
        crit.forEach((c) => {
          const prev = existing.find((sc) => sc.criterionId === c.id);
          seed[c.id] = prev ? Number(prev.score) : 0;
        });
        setScores(seed);
        const firstComment = existing.find((sc) => sc.comment)?.comment;
        if (firstComment) setComment(firstComment);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load submission');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [id, judgeId]);

  const setScore = (criterionId, value) =>
    setScores((prev) => ({ ...prev, [criterionId]: value }));

  const averageScore = useMemo(() => {
    const vals = criteria.map((c) => Number(scores[c.id] || 0));
    if (!vals.length) return 0;
    return Math.round(vals.reduce((a, b) => a + b, 0) / vals.length);
  }, [criteria, scores]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      for (const c of criteria) {
        await upsertScore({
          submissionId: id,
          criterionId: c.id,
          score: Number(scores[c.id] || 0),
          comment: comment || undefined,
        });
      }
      navigate('/judge/submissions');
    } catch (err) {
      setError(err.message || 'Failed to submit evaluation');
      setSaving(false);
    }
  };

  const materials = submission
    ? [
        { label: 'Repository', url: submission.repoUrl },
        { label: 'Live Demo', url: submission.demoUrl },
        { label: 'Slides', url: submission.slideUrl },
        { label: 'Report', url: submission.reportUrl },
      ].filter((m) => m.url)
    : [];

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
        <h1 className={styles.pageTitle}>Scoring Interface</h1>
        <div className={styles.pageSubtitle}>
          Review materials and evaluate the submission
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <div className="d-flex justify-content-end mb-3">
        <Button
          variant="outline-danger"
          size="sm"
          className="d-flex align-items-center gap-2"
          onClick={() => navigate('/judge/incidents/create')}
        >
          <AlertTriangle size={16} /> Report Incident
        </Button>
      </div>

      <Row>
        {/* Left Pane: Submission Dossier */}
        <Col lg={7} className="mb-4 mb-lg-0">
          <Card className={styles.dossierCard}>
            <div className={styles.teamHeader}>
              <h2 className={styles.teamName}>{submission?.teamName || 'Team'}</h2>
              <div className={styles.projectName}>Round: {submission?.roundName || submission?.roundId}</div>
            </div>

            <div className={styles.sectionTitle}>Project Metadata</div>
            <p className={styles.description}>{submission?.apiMetadata || 'No description provided.'}</p>

            <div className={styles.sectionTitle}>Submitted Materials</div>
            <div className={styles.fileList}>
              {materials.length === 0 && (
                <div className="text-muted">No materials submitted.</div>
              )}
              {materials.map((file) => (
                <div key={file.label} className={styles.fileCard}>
                  <div className={styles.fileInfo}>
                    <ExternalLink size={20} className={styles.fileIcon} />
                    <div>
                      <div className={styles.fileName}>{file.label}</div>
                      <div className={styles.fileMeta}>{file.url}</div>
                    </div>
                  </div>
                  <a className={styles.downloadBtn} href={file.url} target="_blank" rel="noreferrer">View</a>
                </div>
              ))}
            </div>
          </Card>
        </Col>

        {/* Right Pane: Scoring Form */}
        <Col lg={5}>
          <Card className={styles.scoringCard}>
            <div className={styles.finalScoreBox}>
              <div className={styles.scoreLabel}>Final Average Score</div>
              <div className={styles.scoreValue}>{averageScore}<span style={{fontSize: '1.25rem', color: '#86efac'}}>/100</span></div>
            </div>

            <Form onSubmit={handleSubmit}>
              {criteria.length === 0 && (
                <div className="text-muted mb-3">No scoring criteria defined for this round.</div>
              )}
              {criteria.map((c, index) => (
                <div className={styles.criteriaGroup} key={c.id}>
                  <div className={styles.criteriaHeader}>
                    <span className={styles.criteriaTitle}>{index + 1}. {c.name}</span>
                    <span className={styles.criteriaScore}>{scores[c.id] || 0}/100</span>
                  </div>
                  <Form.Range
                    className={styles.customRange}
                    min="0"
                    max="100"
                    value={scores[c.id] || 0}
                    onChange={(e) => setScore(c.id, e.target.value)}
                  />
                </div>
              ))}

              <div className={styles.feedbackGroup}>
                <div className={styles.feedbackLabel}>Comment (Optional)</div>
                <textarea
                  className={styles.feedbackTextarea}
                  placeholder="Feedback for this submission..."
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                ></textarea>
              </div>

              <div className={styles.actionRow}>
                <Button type="submit" className={styles.submitBtn} disabled={saving || criteria.length === 0}>
                  {saving ? 'Submitting...' : 'Submit Evaluation'}
                </Button>
                <Button variant="link" className={styles.cancelBtn} onClick={() => navigate('/judge/submissions')}>
                  Cancel
                </Button>
              </div>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ScoringInterface;
