import { useEffect, useState } from 'react';
import { Card, Row, Col, Badge, Button, ProgressBar } from 'react-bootstrap';
import { ArrowLeft, ExternalLink, Code, FileText, CheckCircle, ShieldCheck } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { getSubmission } from '../../api/hackathonApi';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const SubmissionDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [recognitions, setRecognitions] = useState([]);

  useEffect(() => {
    let active = true;
    getSubmission(id).then((data) => {
      if (active) setRecognitions(data?.recognitions || []);
    }).catch(() => {});
    return () => { active = false; };
  }, [id]);

  // Mock data for the detailed view
  const submission = {
    id: id || 1,
    teamName: 'Neural Nexus',
    projectName: 'EduTrack AI',
    track: 'AI/ML',
    round: 'Finals',
    submittedAt: 'June 18, 2026 - 14:30',
    status: 'Reviewed',
    description: 'An AI-powered learning management system that adapts to student learning paces in real-time using neural networks. It analyzes homework submission patterns and quiz results to generate personalized study plans.',
    github: 'https://github.com/neural-nexus/edutrack-ai',
    demo: 'https://edutrack-ai.demo.app',
    files: [
      { name: 'architecture_diagram.pdf', size: '2.4 MB' },
      { name: 'pitch_deck.pdf', size: '5.1 MB' }
    ],
    analytics: {
      averageScore: 92.5,
      variance: 3.2,
      criteria: [
        { name: 'Innovation', score: 95, max: 100 },
        { name: 'Technical Complexity', score: 90, max: 100 },
        { name: 'UI/UX Design', score: 88, max: 100 },
        { name: 'Business Value', score: 97, max: 100 }
      ],
      judges: [
        { name: 'Prof. James Kim', score: 94, comment: 'Exceptional use of Transformers for the recommendation engine. UI needs slight polish.' },
        { name: 'Dr. Sarah Lee', score: 91, comment: 'Solid technical implementation. Business model is very viable.' },
        { name: 'Mark Chen (Industry)', score: 92, comment: 'Great potential. Would love to see more data on the training set used.' }
      ]
    },
    integrity: {
      status: 'Passed',
      plagiarismScore: '2%',
      aiGeneratedCode: '~15% (Within limits)'
    }
  };

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
                  <h4 className="fw-bold mb-2">{submission.projectName}</h4>
                  <div className="d-flex align-items-center gap-2 text-muted small">
                    <span>By <strong>{submission.teamName}</strong></span>
                    <TeamRecognitionBadge recognitions={recognitions} />
                    <span>•</span>
                    <Badge bg="secondary">{submission.track}</Badge>
                    <Badge bg="info" text="dark">{submission.round}</Badge>
                  </div>
                </div>
                <div className="text-end">
                  <Badge bg="success" className="px-3 py-2 mb-2 d-inline-block">{submission.status}</Badge>
                  <div className="text-muted small">Submitted: <br/><strong>{submission.submittedAt}</strong></div>
                </div>
              </div>

              <div className="mb-4">
                <h6 className="fw-bold text-muted text-uppercase mb-2" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Project Description</h6>
                <p>{submission.description}</p>
              </div>

              <Row className="g-3 mb-4">
                <Col sm={6}>
                  <Button variant="outline-primary" className="w-100 d-flex align-items-center justify-content-center gap-2">
                    <Code size={18} /> View Repository
                  </Button>
                </Col>
                <Col sm={6}>
                  <Button variant="outline-primary" className="w-100 d-flex align-items-center justify-content-center gap-2">
                    <ExternalLink size={18} /> Live Demo
                  </Button>
                </Col>
              </Row>

              <div>
                <h6 className="fw-bold text-muted text-uppercase mb-3" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Attached Files</h6>
                <div className="d-flex flex-column gap-2">
                  {submission.files.map((file, idx) => (
                    <div key={idx} className="d-flex align-items-center justify-content-between p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)', border: '1px solid var(--cf-border-color)' }}>
                      <div className="d-flex align-items-center gap-2">
                        <FileText size={18} className="text-primary" />
                        <span className="fw-medium">{file.name}</span>
                      </div>
                      <span className="text-muted small">{file.size}</span>
                    </div>
                  ))}
                </div>
              </div>
            </Card.Body>
          </Card>

          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4">Individual Judge Reviews</h5>
              <div className="d-flex flex-column gap-3">
                {submission.analytics.judges.map((judge, idx) => (
                  <div key={idx} className="p-3 rounded" style={{ border: '1px solid var(--cf-border-color)' }}>
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <span className="fw-bold">{judge.name}</span>
                      <Badge bg="primary" pill className="fs-6">{judge.score}/100</Badge>
                    </div>
                    <p className="text-muted mb-0 small">"{judge.comment}"</p>
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={4}>
          <Card className="mb-4 text-center" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h6 className="fw-bold text-muted text-uppercase mb-3" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Overall Average Score</h6>
              <div className="display-3 fw-bold text-primary mb-2">{submission.analytics.averageScore}</div>
              <div className="text-muted small d-flex justify-content-center align-items-center gap-2">
                <span>Variance: <strong>{submission.analytics.variance}</strong></span>
                <span className="text-muted">•</span>
                <span>{submission.analytics.judges.length} Reviews</span>
              </div>
            </Card.Body>
          </Card>

          <Card className="mb-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h6 className="fw-bold text-muted text-uppercase mb-4" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>Criteria Breakdown</h6>
              <div className="d-flex flex-column gap-3">
                {submission.analytics.criteria.map((c, idx) => (
                  <div key={idx}>
                    <div className="d-flex justify-content-between mb-1 small">
                      <span className="fw-medium">{c.name}</span>
                      <span className="fw-bold">{c.score}/{c.max}</span>
                    </div>
                    <ProgressBar now={c.score} max={c.max} variant="primary" style={{ height: '6px' }} />
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>

          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h6 className="fw-bold text-muted text-uppercase mb-3 d-flex align-items-center gap-2" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>
                <ShieldCheck size={16} className="text-success" /> Integrity Check
              </h6>
              <div className="d-flex flex-column gap-2 small">
                <div className="d-flex justify-content-between">
                  <span className="text-muted">Automated Status</span>
                  <span className="text-success fw-bold d-flex align-items-center gap-1">
                    <CheckCircle size={14} /> {submission.integrity.status}
                  </span>
                </div>
                <div className="d-flex justify-content-between">
                  <span className="text-muted">Plagiarism Detection</span>
                  <span className="fw-medium">{submission.integrity.plagiarismScore}</span>
                </div>
                <div className="d-flex justify-content-between">
                  <span className="text-muted">AI-Generated Code</span>
                  <span className="fw-medium">{submission.integrity.aiGeneratedCode}</span>
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
