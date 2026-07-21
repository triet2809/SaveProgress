import { useEffect, useState, useCallback } from 'react';
import { Card, Badge, Button, Row, Col, ProgressBar, Spinner, Alert } from 'react-bootstrap';
import { ArrowLeft, Trophy, RefreshCw } from 'lucide-react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { getRoundRankings, recalculateRoundRankings } from '../../api/hackathonApi';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const statusVariant = (status) => {
  const s = (status || '').toLowerCase();
  if (s === 'advanced' || s === 'promoted') return 'success';
  if (s === 'eliminated' || s === 'rejected') return 'danger';
  return 'warning';
};

const RankingDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const roundId = searchParams.get('roundId') || '';

  const [rankings, setRankings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [recalculating, setRecalculating] = useState(false);

  const load = useCallback(async () => {
    if (!roundId) {
      setError('No round selected. Return to Ranking Management and pick a round.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const data = await getRoundRankings({ roundId });
      const list = (data.content || data || []).slice().sort((a, b) => (a.rank ?? 0) - (b.rank ?? 0));
      setRankings(list);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [roundId]);

  useEffect(() => {
    // Load rankings when the selected round changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const handleRecalculate = async () => {
    if (!roundId) return;
    setRecalculating(true);
    setError('');
    try {
      await recalculateRoundRankings(roundId);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setRecalculating(false);
    }
  };

  const teamData = rankings.find((t) => String(t.teamId) === String(id) || String(t.id) === String(id)) || rankings[0];
  const weightedTotal = Number(teamData?.weightedTotal ?? teamData?.totalScore ?? 0);
  const maxTotal = rankings.reduce((m, t) => Math.max(m, Number(t.weightedTotal ?? t.totalScore ?? 0)), 0) || 100;

  if (loading) {
    return (
      <div className="py-2">
        <div className="text-center py-5"><Spinner animation="border" /></div>
      </div>
    );
  }

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/coordinator/ranking')}>
          <ArrowLeft size={24} />
        </Button>
        <div className="flex-grow-1">
          <div className="d-flex align-items-center gap-3 mb-1">
            <h1 className="h3 fw-bold mb-0" style={{ color: 'var(--cf-text-primary)' }}>{teamData?.teamName || 'Ranking Detail'}</h1>
            <TeamRecognitionBadge recognitions={teamData?.recognitions} variant="detailed" />
            {teamData?.status && (
              <Badge bg={statusVariant(teamData.status)} className="fs-6" text={statusVariant(teamData.status) === 'warning' ? 'dark' : 'light'}>
                {teamData.status}
              </Badge>
            )}
          </div>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Round-based weighted ranking</div>
        </div>
        <Button variant="success" className="d-flex align-items-center gap-2" onClick={handleRecalculate} disabled={recalculating || !roundId}>
          {recalculating ? <Spinner size="sm" animation="border" /> : <RefreshCw size={18} />} Recalculate
        </Button>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      {!teamData ? (
        <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)' }}>
          <Card.Body className="p-5 text-center text-muted">No rankings available for this round. Recalculate to generate.</Card.Body>
        </Card>
      ) : (
        <>
          <Row className="g-4 mb-4">
            <Col lg={4}>
              <Card className="h-100 text-center" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <Card.Body className="d-flex flex-column justify-content-center p-5">
                  <div className="d-flex justify-content-center mb-3">
                    <div className="d-flex align-items-center justify-content-center rounded-circle"
                         style={{ width: '80px', height: '80px', backgroundColor: (teamData.rank ?? 99) <= 3 ? 'var(--cf-status-warning)' : 'var(--cf-border-color)', color: (teamData.rank ?? 99) <= 3 ? '#fff' : 'var(--cf-text-secondary)' }}>
                      <Trophy size={40} />
                    </div>
                  </div>
                  <h2 className="display-4 fw-bold mb-0" style={{ color: 'var(--cf-text-primary)' }}>#{teamData.rank ?? '-'}</h2>
                  <p className="text-muted mb-4">Overall Rank</p>

                  <div className="pt-4 border-top">
                    <div className="display-6 fw-bold text-primary mb-0">{weightedTotal.toFixed(2)}</div>
                    <div className="text-muted small">Weighted Total Score</div>
                  </div>
                </Card.Body>
              </Card>
            </Col>

            <Col lg={8}>
              <Card className="h-100" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <Card.Header className="bg-transparent border-bottom p-4">
                  <h5 className="fw-bold mb-0">Score Position</h5>
                  <div className="text-muted small mt-1">Weighted total relative to the top team in this round</div>
                </Card.Header>
                <Card.Body className="p-4 d-flex flex-column justify-content-center">
                  <div className="mb-2 d-flex justify-content-between">
                    <span className="fw-medium">{teamData.teamName}</span>
                    <span className="fw-bold">{weightedTotal.toFixed(2)}</span>
                  </div>
                  <ProgressBar variant="primary" now={maxTotal ? (weightedTotal / maxTotal) * 100 : 0} className="rounded-pill" style={{ height: '10px' }} />
                  {/* BE trả về tieBreakerReason giải thích vì sao đội này được xếp hạng như vậy khi hòa điểm */}
                  {(teamData.tieBreakerReason || teamData.tieBreakerNote) && (
                    <div className="text-muted small mt-3">{teamData.tieBreakerReason || teamData.tieBreakerNote}</div>
                  )}
                </Card.Body>
              </Card>
            </Col>
          </Row>

          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Header className="bg-transparent border-bottom p-4">
              <h5 className="fw-bold mb-0 d-flex align-items-center gap-2"><Trophy size={20} className="text-primary" /> Round Leaderboard</h5>
            </Card.Header>
            <Card.Body className="p-0">
              <div className="table-responsive">
                <table className="table mb-0">
                  <thead>
                    <tr>
                      <th className="px-4">Rank</th>
                      <th>Team</th>
                      <th>Weighted Total</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rankings.map((t) => {
                      const active = String(t.teamId) === String(teamData.teamId);
                      return (
                        <tr key={t.id || t.teamId} style={active ? { backgroundColor: 'var(--cf-bg-main)' } : undefined}>
                          <td className="px-4 fw-bold">#{t.rank}</td>
                          <td className={active ? 'fw-bold' : ''}>{t.teamName}</td>
                          <td>{Number(t.weightedTotal ?? t.totalScore ?? 0).toFixed(2)}</td>
                          <td>{t.status ? <Badge bg={statusVariant(t.status)} text={statusVariant(t.status) === 'warning' ? 'dark' : 'light'}>{t.status}</Badge> : '-'}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </Card.Body>
          </Card>
        </>
      )}
    </div>
  );
};

export default RankingDetail;
