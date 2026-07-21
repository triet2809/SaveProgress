import { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Badge, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Eye, Search, RefreshCw, Send } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getRounds, getRoundRankings, getTracks, recalculateRoundRankings, publishRoundResults } from '../../api/hackathonApi';
import EventSelector from '../../components/coordinator/EventSelector';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';
import { useSearchParams } from 'react-router-dom';

const statusVariant = (status) => {
  const s = (status || '').toLowerCase();
  if (s === 'advanced' || s === 'promoted') return 'success';
  if (s === 'eliminated' || s === 'rejected') return 'danger';
  return 'warning';
};

const RankingManagement = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const selectedRound = searchParams.get('roundId') || '';
  const trackId = searchParams.get('trackId') || '';
  const [rounds, setRounds] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [rankings, setRankings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [recalculating, setRecalculating] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [notice, setNotice] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');

  useEffect(() => {
    (async () => {
      try {
        const [data, trackData] = await Promise.all([getRounds({ eventId, size: 100 }), getTracks({ eventId, size: 100 })]);
        const list = data.content || data || [];
        setRounds(list);
        setTracks(trackData.content || trackData || []);
        if (list.length && !selectedRound) {
          const next = new URLSearchParams(searchParams); next.set('roundId', list[0].id); setSearchParams(next);
        }
        else setLoading(false);
      } catch (err) {
        setError(err.message);
        setLoading(false);
      }
    })();
  }, [eventId, selectedRound, searchParams, setSearchParams]);

  const loadRankings = useCallback(async (roundId) => {
    if (!roundId) return;
    setLoading(true);
    setError('');
    try {
      const data = await getRoundRankings({ eventId, roundId, ...(trackId ? { trackId } : {}) });
      setRankings(data.content || data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [eventId, trackId]);

  useEffect(() => {
    // Refresh rankings when the URL-selected round changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (selectedRound) loadRankings(selectedRound);
  }, [selectedRound, loadRankings]);

  const filteredRankings = rankings
    .slice()
    .sort((a, b) => (a.rank ?? 0) - (b.rank ?? 0))
    .filter((team) => {
      const matchesSearch = (team.teamName || '').toLowerCase().includes(searchTerm.toLowerCase());
      const matchesStatus = statusFilter === 'All' ? true : (team.status || '').toLowerCase() === statusFilter.toLowerCase();
      return matchesSearch && matchesStatus;
    });

  const handleRecalculate = async () => {
    if (!selectedRound) return;
    setRecalculating(true);
    setError('');
    try {
      await recalculateRoundRankings(selectedRound);
      await loadRankings(selectedRound);
    } catch (err) {
      setError(err.message);
    } finally {
      setRecalculating(false);
    }
  };

  // Công bố kết quả vòng thi -> BE mở cửa sổ khiếu nại 15 phút cho các đội.
  const handlePublish = async () => {
    if (!selectedRound) return;
    if (!window.confirm('Publish results for this round? This opens a 15-minute appeal window for teams.')) return;
    setPublishing(true);
    setError('');
    setNotice('');
    try {
      const round = await publishRoundResults(eventId, selectedRound);
      setNotice(`Results published at ${round?.resultPublishedAt || 'now'}.`);
    } catch (err) {
      setError(err.message);
    } finally {
      setPublishing(false);
    }
  };

  return (
    <>
    <EventSelector />
    {eventId && <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Ranking Management</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Review leaderboards and manage team advancement</div>
        </div>
        <div className="d-flex gap-2">
          <Button variant="outline-primary" className="d-flex align-items-center gap-2" onClick={handlePublish} disabled={publishing || !selectedRound}>
            {publishing ? <Spinner size="sm" animation="border" /> : <Send size={18} />} Publish Results
          </Button>
          <Button variant="success" className="d-flex align-items-center gap-2" onClick={handleRecalculate} disabled={recalculating || !selectedRound}>
            {recalculating ? <Spinner size="sm" animation="border" /> : <RefreshCw size={18} />} Recalculate Rankings
          </Button>
        </div>
      </div>

      {notice && <Alert variant="success" onClose={() => setNotice('')} dismissible>{notice}</Alert>}

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex justify-content-between gap-2 flex-wrap">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text>
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control placeholder="Search team..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
          </InputGroup>
          <div className="d-flex gap-2">
            <Form.Select style={{ width: '220px' }} value={selectedRound} onChange={(e) => {
              const next = new URLSearchParams(searchParams); next.set('roundId', e.target.value); next.delete('trackId'); setSearchParams(next);
            }}>
              {rounds.length === 0 && <option value="">No rounds available</option>}
              {rounds.map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </Form.Select>
            <Form.Select style={{ width: 190 }} value={trackId} onChange={(e) => {
              const next = new URLSearchParams(searchParams);
              if (e.target.value) next.set('trackId', e.target.value); else next.delete('trackId');
              setSearchParams(next);
            }}>
              <option value="">All Tracks</option>
              {tracks.filter((track) => !selectedRound || rounds.find((round) => round.id === selectedRound)?.trackId === track.id)
                .map((track) => <option key={track.id} value={track.id}>{track.name}</option>)}
            </Form.Select>
            <Form.Select
              style={{ width: '180px' }}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="All">All Statuses</option>
              <option value="promoted">Promoted</option>
              <option value="pending">Pending</option>
              <option value="eliminated">Eliminated</option>
            </Form.Select>
          </div>
        </div>

        <div className="table-responsive">
          {loading ? (
            <div className="text-center py-5"><Spinner animation="border" /></div>
          ) : (
            <Table className="mb-0" hover>
              <thead>
                <tr>
                  <th className="border-top-0 border-bottom">Rank</th>
                  <th className="border-top-0 border-bottom">Team Name</th>
                  <th className="border-top-0 border-bottom">Track</th>
                  <th className="border-top-0 border-bottom">Final Score</th>
                  <th className="border-top-0 border-bottom">Advancement Status</th>
                  <th className="border-top-0 border-bottom">Publication</th>
                  <th className="border-top-0 border-bottom text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredRankings.length === 0 && (
                  <tr><td colSpan={7} className="text-center text-muted py-4">No rankings. Recalculate to generate.</td></tr>
                )}
                {filteredRankings.map((team) => (
                  <tr key={team.id || team.teamId}>
                    <td className="fw-bold py-3" style={{ color: team.rank <= 3 ? 'var(--cf-status-warning)' : 'var(--cf-text-secondary)' }}>
                      #{team.rank}
                    </td>
                    <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>
                      <span className="me-2">{team.teamName}</span>
                      <TeamRecognitionBadge recognitions={team.recognitions} />
                    </td>
                    <td>{team.trackName || '—'}</td>
                    <td className="py-3 fw-bold" style={{ color: 'var(--cf-text-primary)' }}>{team.totalScore == null ? 'Unscored' : Number(team.totalScore).toFixed(1)}</td>
                    <td className="py-3">
                      <Badge bg={statusVariant(team.status)} text={statusVariant(team.status) === 'warning' ? 'dark' : 'light'}>
                        {team.status}
                      </Badge>
                    </td>
                    <td><Badge bg={team.resultPublishedAt ? 'success' : 'secondary'}>{team.resultPublishedAt ? 'Published' : 'Unpublished'}</Badge></td>
                    <td className="py-3 text-end">
                      <Button variant="link" size="sm" className="p-0 text-primary" onClick={() => navigate(`/coordinator/ranking/${team.teamId}?roundId=${selectedRound}`)}>
                        <Eye size={16} />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </div>
      </Card>
    </div>}
    </>
  );
};

export default RankingManagement;
