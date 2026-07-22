/**
 * SubmissionDetail.jsx — Trang chi tiết bài nộp cho Coordinator.
 * Tải dữ liệu THẬT từ BE: submission (GET /submissions/{id}) + điểm giám khảo (GET /scores?submissionId=).
 * Trước đây trang này hiển thị mock data cứng (Neural Nexus/EduTrack AI) — đã thay bằng dữ liệu thật.
 */
import { useEffect, useState } from 'react';
import { Card, Row, Col, Badge, Button, ProgressBar, Spinner, Alert } from 'react-bootstrap';
import { ArrowLeft, ExternalLink, Code, FileText, ShieldCheck } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { getSubmission, getScores } from '../../api/hackathonApi';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const asArray = (data) => data?.content || data || [];

const reviewLabel = (s) => {
  if (!s) return 'Pending Review';
  const v = String(s).toLowerCase();
  if (v === 'reviewed') return 'Reviewed';
  if (v === 'pending') return 'Pending Review';
  return s.charAt(0).toUpperCase() + s.slice(1);
};

const fmtDate = (d) => (d ? new Date(d).toLocaleString() : '—');

const SubmissionDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submission, setSubmission] = useState(null);
  const [scores, setScores] = useState([]);

  useEffect(() => {
    let active = true;
    (async () => {
      setLoading(true);
      setError('');
      try {
        // Điểm là dữ liệu phụ; lỗi tải điểm không nên chặn hiển thị submission.
        const [sub, scs] = await Promise.all([
          getSubmission(id),
          getScores({ submissionId: id, size: 200 }).catch(() => null),
        ]);
        if (!active) return;
        setSubmission(sub);
        setScores(asArray(scs));
      } catch (err) {
        if (active) setError(err.message || 'Failed to load submission');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [id]);

  if (loading) {
    return <div className="py-5 text-center"><Spinner animation="border" variant="primary" /></div>;
  }

  if (error || !submission) {
    return (
      <div className="py-2">
        <Button variant="link" className="p-0 text-muted mb-3" onClick={() => navigate('/coordinator/submissions')}>
          <ArrowLeft size={24} />
        </Button>
        <Alert variant="danger">{error || 'Submission not found.'}</Alert>
      </div>
    );
  }

  // Gom điểm theo giám khảo (mỗi giám khảo có thể chấm nhiều tiêu chí).
  const byJudge = {};
  scores.forEach((s) => {
    const key = s.judgeId || s.judgeEmail || 'unknown';
    if (!byJudge[key]) byJudge[key] = { name: s.judgeEmail || 'Judge', criteria: [], comments: [] };
    byJudge[key].criteria.push({ name: s.criterionName || 'Criterion', score: Number(s.score) || 0 });
    if (s.comment) byJudge[key].comments.push(s.comment);
  });
  const judges = Object.values(byJudge).map((j) => {
    const avg = j.criteria.length
      ? j.criteria.reduce((a, c) => a + c.score, 0) / j.criteria.length
      : 0;
    return { ...j, avg };
  });

  // Điểm trung bình tổng: trung bình weightedScore nếu có, ngược lại trung bình các score.
  const weighted = scores.map((s) => Number(s.weightedScore)).filter((n) => Number.isFinite(n));
  const raw = scores.map((s) => Number(s.score)).filter((n) => Number.isFinite(n));
  const pool = weighted.length ? weighted : raw;
  const averageScore = pool.length ? (pool.reduce((a, b) => a + b, 0) / pool.length).toFixed(1) : null;

  // Gom tiêu chí (trung bình theo tên tiêu chí, dùng cho breakdown).
  const critMap = {};
  scores.forEach((s) => {
    const n = s.criterionName || 'Criterion';
    if (!critMap[n]) critMap[n] = [];
    if (Number.isFinite(Number(s.score))) critMap[n].push(Number(s.score));
  });
  const criteria = Object.entries(critMap).map(([name, arr]) => ({
    name,
    score: arr.length ? (arr.reduce((a, b) => a + b, 0) / arr.length) : 0,
  }));

  // Danh sách link tài nguyên thật.
  const files = [
    submission.slideUrl ? { name: 'Slides', url: submission.slideUrl } : null,
    submission.reportUrl ? { name: 'Report', url: submission.reportUrl } : null,
  ].filter(Boolean);

  const reviewed = reviewLabel(submission.reviewStatus) === 'Reviewed';

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/coordinator/submissions')}>
          <ArrowLeft size={24} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Submission Details</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Review project information and scoring analytics</div>
        </div>
      </div>

      <Row className="g-4">
        <Col lg={8}>
          <Card className="mb-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <div className="d-flex justify-content-between align-items-start mb-4">
                <div>
                  <h4 className="fw-bold mb-2">{submission.projectName || 'Untitled Project'}</h4>
                  <div className="d-flex align-items-center gap-2 text-muted small flex-wrap">
                    <span>By <strong>{submission.teamName || '—'}</strong></span>
                    <TeamRecognitionBadge recognitions={submission.recognitions || []} />
                    {submission.version && <><span>•</span><Badge bg="secondary">{submission.version}</Badge></>}
                  </div>
                </div>
                <div className="text-end">
                  <Badge bg={reviewed ? 'success' : 'warning'} text={reviewed ? 'light' : 'dark'} className="px-3 py-2 mb-2 d-inline-block">
                    {reviewLabel(submission.reviewStatus)}
                  </Badge>
                  <div className="text-muted small">Submitted: <br/><strong>{fmtDate(submission.submittedAt)}</strong></div>
                </div>
              </div>

              {submission.apiMetadata && (
                <div className="mb-4">
                  <h6 className="fw-bold text-muted text-uppercase mb-2" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Notes</h6>
                  <p className="mb-0">{submission.apiMetadata}</p>
                </div>
              )}

              <Row className="g-3 mb-4">
                <Col sm={6}>
                  <Button
                    variant="outline-primary"
                    className="w-100 d-flex align-items-center justify-content-center gap-2"
                    as="a"
                    href={submission.repoUrl || undefined}
                    target={submission.repoUrl ? '_blank' : undefined}
                    rel="noreferrer"
                    disabled={!submission.repoUrl}
                  >
                    <Code size={18} /> View Repository
                  </Button>
                </Col>
                <Col sm={6}>
                  <Button
                    variant="outline-primary"
                    className="w-100 d-flex align-items-center justify-content-center gap-2"
                    as="a"
                    href={submission.demoUrl || undefined}
                    target={submission.demoUrl ? '_blank' : undefined}
                    rel="noreferrer"
                    disabled={!submission.demoUrl}
                  >
                    <ExternalLink size={18} /> Live Demo
                  </Button>
                </Col>
              </Row>

              <div>
                <h6 className="fw-bold text-muted text-uppercase mb-3" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Attached Files</h6>
                {files.length === 0 ? (
                  <div className="text-muted small">No files attached.</div>
                ) : (
                  <div className="d-flex flex-column gap-2">
                    {files.map((file, idx) => (
                      <a
                        key={idx}
                        href={file.url}
                        target="_blank"
                        rel="noreferrer"
                        className="d-flex align-items-center justify-content-between p-3 rounded text-decoration-none"
                        style={{ backgroundColor: 'var(--cf-bg-main)', border: '1px solid var(--cf-border-color)' }}
                      >
                        <div className="d-flex align-items-center gap-2">
                          <FileText size={18} className="text-primary" />
                          <span className="fw-medium">{file.name}</span>
                        </div>
                        <ExternalLink size={14} className="text-muted" />
                      </a>
                    ))}
                  </div>
                )}
              </div>
            </Card.Body>
          </Card>

          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4">Individual Judge Reviews</h5>
              {judges.length === 0 ? (
                <div className="text-muted">No scores recorded yet.</div>
              ) : (
                <div className="d-flex flex-column gap-3">
                  {judges.map((judge, idx) => (
                    <div key={idx} className="p-3 rounded" style={{ border: '1px solid var(--cf-border-color)' }}>
                      <div className="d-flex justify-content-between align-items-center mb-2">
                        <span className="fw-bold">{judge.name}</span>
                        <Badge bg="primary" pill className="fs-6">{judge.avg.toFixed(1)}</Badge>
                      </div>
                      {judge.comments.length > 0 && (
                        <p className="text-muted mb-0 small">"{judge.comments.join(' | ')}"</p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col lg={4}>
          <Card className="mb-4 text-center" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h6 className="fw-bold text-muted text-uppercase mb-3" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Overall Average Score</h6>
              <div className="display-3 fw-bold text-primary mb-2">{averageScore ?? '—'}</div>
              <div className="text-muted small">
                {judges.length} judge{judges.length === 1 ? '' : 's'} · {scores.length} score{scores.length === 1 ? '' : 's'}
              </div>
            </Card.Body>
          </Card>

          {criteria.length > 0 && (
            <Card className="mb-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Card.Body className="p-4">
                <h6 className="fw-bold text-muted text-uppercase mb-4" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Criteria Breakdown</h6>
                <div className="d-flex flex-column gap-3">
                  {criteria.map((c, idx) => (
                    <div key={idx}>
                      <div className="d-flex justify-content-between mb-1 small">
                        <span className="fw-medium">{c.name}</span>
                        <span className="fw-bold">{c.score.toFixed(1)}</span>
                      </div>
                      <ProgressBar now={c.score} max={100} variant="primary" style={{ height: '6px' }} />
                    </div>
                  ))}
                </div>
              </Card.Body>
            </Card>
          )}

          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h6 className="fw-bold text-muted text-uppercase mb-3 d-flex align-items-center gap-2" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>
                <ShieldCheck size={16} className="text-muted" /> Submission Info
              </h6>
              <div className="d-flex flex-column gap-2 small">
                <div className="d-flex justify-content-between">
                  <span className="text-muted">Status</span>
                  <span className="fw-medium">{submission.status || '—'}</span>
                </div>
                <div className="d-flex justify-content-between">
                  <span className="text-muted">Review Status</span>
                  <span className="fw-medium">{reviewLabel(submission.reviewStatus)}</span>
                </div>
                <div className="d-flex justify-content-between">
                  <span className="text-muted">Last Updated</span>
                  <span className="fw-medium">{fmtDate(submission.updatedAt)}</span>
                </div>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default SubmissionDetail;
