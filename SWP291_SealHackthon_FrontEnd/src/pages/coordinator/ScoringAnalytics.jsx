import { useState, useEffect, useCallback } from 'react';
import { Card, Table, Badge, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Search, BarChart2 } from 'lucide-react';
import { getRounds, getJudgeVariance, getTracks } from '../../api/hackathonApi';
import EventSelector from '../../components/coordinator/EventSelector';
import { useSearchParams } from 'react-router-dom';

const HIGH_VARIANCE = 10;

const ScoringAnalytics = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const selectedRound = searchParams.get('roundId') || '';
  const trackId = searchParams.get('trackId') || '';
  const [searchTerm, setSearchTerm] = useState('');
  const [rounds, setRounds] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const [data, trackData] = await Promise.all([
          getRounds({ eventId, size: 100 }), getTracks({ eventId, size: 100 }),
        ]);
        const list = data.content || data || [];
        setRounds(list);
        setTracks(trackData.content || trackData || []);
        if (list.length && !selectedRound) {
          const next = new URLSearchParams(searchParams);
          next.set('roundId', list[0].id); setSearchParams(next);
        }
        else setLoading(false);
      } catch (err) {
        setError(err.message);
        setLoading(false);
      }
    })();
  }, [eventId, selectedRound, searchParams, setSearchParams]);

  const loadVariance = useCallback(async (roundId) => {
    if (!roundId) return;
    setLoading(true);
    setError('');
    try {
      const data = await getJudgeVariance(eventId, roundId, trackId);
      setRows(Array.isArray(data) ? data : data?.content || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [eventId, trackId]);

  useEffect(() => {
    // Synchronize analytics with the URL-selected round.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (selectedRound) loadVariance(selectedRound);
  }, [selectedRound, loadVariance]);

  const filteredData = rows.filter((item) =>
    (item.teamName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (item.criterionName || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <>
    <EventSelector />
    {eventId && <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Scoring Analytics</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Review team performance, average scores, and judge variances</div>
        </div>
        <div className="d-flex gap-2">
          <Badge bg="warning" text="dark" className="px-3 py-2 d-flex align-items-center gap-2">
            <BarChart2 size={16} /> Needs Review (High Variance)
          </Badge>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between gap-2">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text className="bg-transparent border-end-0">
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control
              className="border-start-0"
              placeholder="Search team or criterion..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </InputGroup>
          <Form.Select style={{ maxWidth: '260px' }} value={selectedRound} onChange={(e) => {
            const next = new URLSearchParams(searchParams); next.set('roundId', e.target.value); next.delete('trackId'); setSearchParams(next);
          }}>
            {rounds.length === 0 && <option value="">No rounds available</option>}
            {rounds.map((r) => (
              <option key={r.id} value={r.id}>{r.name}</option>
            ))}
          </Form.Select>
          <Form.Select style={{ maxWidth: 220 }} value={trackId} onChange={(e) => {
            const next = new URLSearchParams(searchParams);
            if (e.target.value) next.set('trackId', e.target.value); else next.delete('trackId');
            setSearchParams(next);
          }}>
            <option value="">All Tracks</option>
            {tracks.filter((track) => !selectedRound || rounds.find((round) => round.id === selectedRound)?.trackId === track.id)
              .map((track) => <option key={track.id} value={track.id}>{track.name}</option>)}
          </Form.Select>
        </div>
        <div className="table-responsive">
          {loading ? (
            <div className="text-center py-5"><Spinner animation="border" /></div>
          ) : (
            <Table className="mb-0 text-center align-middle" hover>
              <thead className="text-start">
                <tr>
                  <th className="border-top-0 border-bottom text-start py-3">Team Name</th>
                  <th className="border-top-0 border-bottom text-start py-3">Criterion</th>
                  <th className="border-top-0 border-bottom py-3">Judges</th>
                  <th className="border-top-0 border-bottom py-3">Mean</th>
                  <th className="border-top-0 border-bottom py-3">Min</th>
                  <th className="border-top-0 border-bottom py-3">Max</th>
                  <th className="border-top-0 border-bottom py-3">Variance</th>
                  <th className="border-top-0 border-bottom text-end py-3">Status</th>
                </tr>
              </thead>
              <tbody className="text-start">
                {filteredData.length === 0 && (
                  <tr><td colSpan={8} className="text-center text-muted py-4">No scoring data for this round</td></tr>
                )}
                {filteredData.map((item, idx) => {
                  const variance = Number(item.variance ?? 0);
                  const highVariance = variance > HIGH_VARIANCE;
                  return (
                    <tr key={`${item.teamId}-${item.criterionId}-${idx}`}>
                      <td className="fw-bold" style={{ color: 'var(--cf-text-primary)' }}>{item.teamName}</td>
                      <td><Badge bg="secondary">{item.criterionName}</Badge></td>
                      <td className="text-center">{item.judgeCount}</td>
                      <td className="text-center fw-bold text-primary">{Number(item.meanScore ?? 0).toFixed(1)}</td>
                      <td className="text-center">{Number(item.minScore ?? 0).toFixed(1)}</td>
                      <td className="text-center">{Number(item.maxScore ?? 0).toFixed(1)}</td>
                      <td className="text-center">
                        <Badge bg={highVariance ? 'danger' : 'success'} className="px-2 py-1">
                          {variance.toFixed(1)}
                        </Badge>
                      </td>
                      <td className="text-end">
                        <Badge bg={highVariance ? 'warning' : 'success'} text={highVariance ? 'dark' : 'light'}>
                          {highVariance ? 'High Variance' : 'Reviewed'}
                        </Badge>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </Table>
          )}
        </div>
      </Card>
    </div>}
    </>
  );
};

export default ScoringAnalytics;
