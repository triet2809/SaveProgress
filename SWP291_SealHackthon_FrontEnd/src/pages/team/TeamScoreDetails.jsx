import React from 'react';
import { Card, Row, Col, Badge, Button, ProgressBar } from 'react-bootstrap';
import { ArrowLeft, MessageSquare } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import styles from './SubmissionHistory.module.css';

const TeamScoreDetails = () => {
  const navigate = useNavigate();
  const { id } = useParams();

  // Mock data for the team's view of their score
  const scoreData = {
    version: 'v1.2 (Final)',
    submittedAt: 'June 18, 2026',
    overallScore: 92.5,
    status: 'Evaluated',
    criteria: [
      { name: 'Innovation & Creativity', score: 95, max: 100 },
      { name: 'Technical Complexity', score: 90, max: 100 },
      { name: 'UI/UX Design', score: 88, max: 100 },
      { name: 'Business Viability', score: 97, max: 100 }
    ],
    feedback: [
      { judge: 'Judge 1', comment: 'Exceptional use of Transformers for the recommendation engine. UI needs slight polish.' },
      { judge: 'Judge 2', comment: 'Solid technical implementation. Business model is very viable.' },
      { judge: 'Judge 3', comment: 'Great potential. Would love to see more data on the training set used.' }
    ]
  };

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/team/history')}>
          <ArrowLeft size={24} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Evaluation Results</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Review the judges' feedback and your final scores</div>
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
                  <div className="text-muted small">Submission: {scoreData.version} • {scoreData.submittedAt}</div>
                </div>
                <Badge bg="success" className="px-3 py-2 fs-6">{scoreData.overallScore} / 100</Badge>
              </div>

              <div className="d-flex flex-column gap-4 mt-4">
                {scoreData.criteria.map((criterion, idx) => (
                  <div key={idx}>
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <span className="fw-medium">{criterion.name}</span>
                      <span className="fw-bold" style={{ color: 'var(--cf-text-primary)' }}>
                        {criterion.score}/{criterion.max}
                      </span>
                    </div>
                    <ProgressBar 
                      now={criterion.score} 
                      max={criterion.max} 
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
              
              <div className="d-flex flex-column gap-3">
                {scoreData.feedback.map((fb, idx) => (
                  <div key={idx} className="p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)', border: '1px solid var(--cf-border-color)' }}>
                    <div className="fw-bold mb-2 small text-primary">{fb.judge}</div>
                    <p className="mb-0 text-muted small" style={{ lineHeight: '1.5' }}>"{fb.comment}"</p>
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default TeamScoreDetails;
