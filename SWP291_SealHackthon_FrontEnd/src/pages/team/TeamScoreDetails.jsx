import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Badge, Button, ProgressBar, Spinner, Alert } from 'react-bootstrap';
import { ArrowLeft, MessageSquare } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { getSubmission, getScores, getRoundCriteria } from '../../api/hackathonApi';
import styles from './SubmissionHistory.module.css';

const asArray = (data) => data?.content || data || [];

const TeamScoreDetails = () => {
  const navigate = useNavigate();
  const { id } = useParams(); // submissionId

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submission, setSubmission] = useState(null);
  const [criteria, setCriteria] = useState([]);
  const [scores, setScores] = useState([]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const sub = await getSubmission(id);
        const crit = asArray(await getRoundCriteria({ roundId: sub.roundId, size: 100 }));
        const sc = asArray(await getScores({ submissionId: id, size: 200 }));
        if (!active) return;
        setSubmission(sub);
        setCriteria(crit);
        setScores(sc);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load score details');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [id]);

  if (loading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <Spinner animation="border" role="status" />
      </div>
    );
  }

  if (error) {
    return <Alert variant="danger" className="mt-4">{error}</Alert>;
  }

  // Group scores by criterion and compute weighted average
  const criterionScoreMap = {};
  scores.forEach((sc) => {
    if (!criterionScoreMap[sc.criterionId]) criterionScoreMap[sc.criterionId] = [];
    criterionScoreMap[sc.criterionId].push(Number(sc.score));
  });

  const criteriaWithAvg = criteria.map((c) => {
    const vals = criterionScoreMap[c.id] || [];
    const avg = vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : null;
    return { ...c, avg };
  });

  // Weighted overall score: sum(avg_i * weight_i)
  const overallScore = criteriaWithAvg.reduce((total, c) => {
    if (c.avg == null) return total;
    return total + c.avg * (c.weight || 0);
  }, 0);

  // Group feedback (comments) by judge
  const feedbackByJudge = {};
  scores.forEach((sc) => {
    if (sc.comment) {
      const judge = sc.judgeFullName || sc.judgeId || 'Judge';
      if (!feedbackByJudge[judge]) feedbackByJudge[judge] = sc.comment;
    }
  });
  const feedbackList = Object.entries(feedbackByJudge).map(([judge, comment]) => ({ judge, comment }));

  const submittedAt = submission?.submittedAt
    ? new Date(submission.submittedAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })
    : '—';

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/team/history')}>
          <ArrowLeft size={24} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Evaluation Results</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            Review the judges' feedback and your final scores
          </div>
        </div>
      </div>

      <Row className="g-4">
        {/* Left Column: Overall Score & Criteria */}
        <Col lg={8}>
          <Card className="mb-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <div className="d-flex justify-content-between align-items-start mb-4">
                <div>
                  <h5 className="fw-bold mb-1">Detailed Scoring Breakdown</h5>
                  <div className="text-muted small">
                    {submission?.roundName || 'Round'} • Submitted: {submittedAt}
                  </div>
                </div>
                <Badge bg="success" className="px-3 py-2 fs-6">
                  {criteriaWithAvg.every((c) => c.avg == null) ? 'Unscored' : `${overallScore.toFixed(1)} / 100`}
                </Badge>
              </div>

              {criteriaWithAvg.length === 0 && (
                <div className="text-muted">No criteria defined for this round.</div>
              )}

              <div className="d-flex flex-column gap-4 mt-4">
                {criteriaWithAvg.map((criterion) => (
                  <div key={criterion.id}>
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <span className="fw-medium">
                        {criterion.name}
                        <span className="text-muted ms-2 small">(weight: {((criterion.weight || 0) * 100).toFixed(0)}%)</span>
                      </span>
                      <span className="fw-bold" style={{ color: 'var(--cf-text-primary)' }}>
                        {criterion.avg != null ? `${criterion.avg.toFixed(1)}/100` : 'Not scored'}
                      </span>
                    </div>
                    <ProgressBar
                      now={criterion.avg ?? 0}
                      max={100}
                      variant="primary"
                      style={{ height: '8px', backgroundColor: 'var(--cf-bg-main)' }}
                    />
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>

        {/* Right Column: Feedback */}
        <Col lg={4}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', height: '100%' }}>
            <Card.Body className="p-4">
              <div className="d-flex align-items-center gap-2 mb-4">
                <MessageSquare size={20} className="text-primary" />
                <h5 className="fw-bold mb-0">Judge Comments</h5>
              </div>

              {feedbackList.length === 0 ? (
                <div className="text-muted small">No comments from judges yet.</div>
              ) : (
                <div className="d-flex flex-column gap-3">
                  {feedbackList.map((fb, idx) => (
                    <div key={idx} className="p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)', border: '1px solid var(--cf-border-color)' }}>
                      <div className="fw-bold mb-2 small text-primary">{fb.judge}</div>
                      <p className="mb-0 text-muted small" style={{ lineHeight: '1.5' }}>"{fb.comment}"</p>
                    </div>
                  ))}
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default TeamScoreDetails;
