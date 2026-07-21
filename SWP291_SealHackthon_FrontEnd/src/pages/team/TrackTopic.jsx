import React, { useEffect, useState } from 'react';
import { Card, ListGroup, Badge, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { Target, CheckCircle, Award, BookOpen } from 'lucide-react';
import { getMyTeams, getTrack, getRounds, getRoundCriteria } from '../../api/hackathonApi';

const DEFAULT_REQUIREMENTS = [
  'Submit your source code repository link (GitHub) under Submission Management.',
  'Include a working prototype and a short demo (video or live URL).',
  'Ensure all deliverables are uploaded before the round submission deadline.',
];

const TrackTopic = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [track, setTrack] = useState(null);
  const [criteria, setCriteria] = useState([]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : teams?.content || [];
        const current = list[0] || null;
        if (!current?.trackId) {
          if (active) setLoading(false);
          return;
        }
        const trk = await getTrack(current.trackId).catch(() => null);
        if (active) setTrack(trk);
        try {
          const roundsRes = await getRounds({ trackId: current.trackId });
          const rounds = (roundsRes?.content || roundsRes || [])
            .slice()
            .sort((a, b) => (a.sequenceNumber || 0) - (b.sequenceNumber || 0));
          const firstRound = rounds[0];
          if (firstRound?.id) {
            const critRes = await getRoundCriteria({ roundId: firstRound.id });
            if (active) setCriteria(critRes?.content || critRes || []);
          }
        } catch { /* criteria optional */ }
      } catch (e) {
        if (active) setError(e.message || 'Failed to load track');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

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

  if (!track) {
    return <Alert variant="info" className="my-3">No track assigned to your team yet.</Alert>;
  }

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Track Topic</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>View your assigned track theme and project requirements</div>
        </div>
      </div>

      <Row className="g-4">
        <Col lg={8}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }} className="mb-4">
            <Card.Body className="p-4">
              <div className="d-flex align-items-center gap-3 mb-3">
                <div 
                  className="d-flex align-items-center justify-content-center bg-primary-subtle text-primary rounded" 
                  style={{ width: '48px', height: '48px' }}
                >
                  <Target size={24} />
                </div>
                <div>
                  <Badge bg="primary" className="mb-1">{track.name}</Badge>
                  <h4 className="fw-bold mb-0" style={{ color: 'var(--cf-text-primary)' }}>{track.name}</h4>
                </div>
              </div>
              <p style={{ color: 'var(--cf-text-secondary)', lineHeight: '1.6' }} className="mb-0">
                {track.description || 'No description provided for this track.'}
              </p>
            </Card.Body>
          </Card>

          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
                <CheckCircle size={20} className="text-success" /> Technical & Submission Requirements
              </h5>
              <ListGroup variant="flush">
                {DEFAULT_REQUIREMENTS.map((req, index) => (
                  <ListGroup.Item 
                    key={index} 
                    className="px-0 py-3 bg-transparent"
                    style={{ borderBottom: index === DEFAULT_REQUIREMENTS.length - 1 ? 'none' : '1px solid var(--cf-border-color)' }}
                  >
                    <div className="d-flex align-items-start gap-3">
                      <div className="mt-1" style={{ color: 'var(--cf-text-secondary)' }}>
                        <BookOpen size={16} />
                      </div>
                      <span style={{ color: 'var(--cf-text-primary)' }}>{req}</span>
                    </div>
                  </ListGroup.Item>
                ))}
              </ListGroup>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={4}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4 d-flex align-items-center gap-2" style={{ color: 'var(--cf-text-primary)' }}>
                <Award size={20} className="text-warning" /> Evaluation Criteria
              </h5>
              <div className="d-flex flex-column gap-4">
                {criteria.length === 0 && (
                  <div className="text-muted small">Evaluation criteria will be published by the organizers.</div>
                )}
                {criteria.map((criterion, index) => (
                  <div key={criterion.id || index}>
                    <div className="d-flex justify-content-between align-items-center mb-1">
                      <span className="fw-bold" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>{criterion.name}</span>
                      <Badge bg="light" text="dark" style={{ border: '1px solid var(--cf-border-color)' }}>{criterion.weight}</Badge>
                    </div>
                    <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.8125rem', lineHeight: '1.5' }}>
                      {criterion.description || ''}
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
};

export default TrackTopic;
