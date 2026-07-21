import { useEffect, useState } from 'react';
import { Card, Table, Button, Badge, Form, InputGroup, Spinner, Alert, Modal } from 'react-bootstrap';
import { Search, Eye, Ban, RotateCcw, Plus, Shuffle } from 'lucide-react';
import { getTeams, getTracks, getSubmissions, disqualifyTeam, reactivateTeam, moveTeamTrack, bulkTransferTeams, previewBalancedTeams, applyBalancedTeams } from '../../api/hackathonApi';
import { useNavigate } from 'react-router-dom';
import { getInitials } from '../../utils/authUser';
import EventSelector from '../../components/coordinator/EventSelector';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';
import { useSearchParams } from 'react-router-dom';

const listOf = (data) => data?.content || data || [];

const TeamManagement = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('All Categories');
  const [statusFilter, setStatusFilter] = useState('All Statuses');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [teams, setTeams] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [subsByTeam, setSubsByTeam] = useState({});
  // Move-track modal state
  const [moveTeam, setMoveTeam] = useState(null);
  const [moveTargetTrack, setMoveTargetTrack] = useState('');
  const [moving, setMoving] = useState(false);
  const [selectedTeamIds, setSelectedTeamIds] = useState([]);
  const [bulkTarget, setBulkTarget] = useState('');
  const [balanceOpen, setBalanceOpen] = useState(false);
  const [balanceTracks, setBalanceTracks] = useState([]);
  const [balanceSeed, setBalanceSeed] = useState('');
  const [balancePreview, setBalancePreview] = useState(null);

  const trackName = (trackId) => tracks.find((t) => t.id === trackId)?.name || 'Unassigned';
  // Project = team's submission projectName if available, else team name.
  const projectName = (team) => subsByTeam[team.id]?.projectName || team.name || '—';

  const loadTeams = async () => {
    try {
      setLoading(true);
      setError('');
      const [tm, tk, sb] = await Promise.all([
        getTeams({ eventId, size: 100 }),
        getTracks({ eventId, size: 100 }),
        getSubmissions({ eventId, size: 200 }).catch(() => null),
      ]);
      setTeams(listOf(tm));
      setTracks(listOf(tk));
      const map = {};
      listOf(sb).forEach((s) => {
        if (s.teamId && (!map[s.teamId] || (s.submittedAt || '') > (map[s.teamId].submittedAt || ''))) {
          map[s.teamId] = s;
        }
      });
      setSubsByTeam(map);
    } catch (err) {
      setError(err.message || 'Failed to load teams');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // The loader synchronizes the page with the URL-selected event.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadTeams();
  // loadTeams is intentionally recreated with the current event id.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId]);

  const handleDisqualifyTeam = async (id) => {
    const reason = window.prompt('Disqualify this team? Enter a reason:');
    if (reason === null) return;
    try {
      setError('');
      await disqualifyTeam(id, reason || 'Disqualified by coordinator');
      await loadTeams();
    } catch (err) {
      setError(err.message || 'Failed to disqualify team');
    }
  };

  const handleReactivateTeam = async (id) => {
    if (!window.confirm('Reactivate this team?')) return;
    try {
      setError('');
      await reactivateTeam(id);
      await loadTeams();
    } catch (err) {
      setError(err.message || 'Failed to reactivate team');
    }
  };

  const openMoveModal = (team) => {
    setMoveTeam(team);
    setMoveTargetTrack('');
  };

  // Only tracks in the SAME event as the team can be move targets.
  const trackEventId = (trackId) => tracks.find((t) => t.id === trackId)?.eventId;
  const moveCandidateTracks = moveTeam
    ? tracks.filter((t) => t.eventId === trackEventId(moveTeam.trackId) && t.id !== moveTeam.trackId)
    : [];

  const handleMoveTrack = async () => {
    if (!moveTeam || !moveTargetTrack) return;
    try {
      setMoving(true);
      setError('');
      await moveTeamTrack(moveTeam.id, moveTargetTrack);
      setMoveTeam(null);
      setMoveTargetTrack('');
      await loadTeams();
    } catch (err) {
      setError(err.message || 'Failed to move team');
    } finally {
      setMoving(false);
    }
  };

  const handleBulkTransfer = async () => {
    if (!selectedTeamIds.length || !bulkTarget) return;
    try {
      setMoving(true); setError('');
      await bulkTransferTeams(selectedTeamIds, bulkTarget);
      setSelectedTeamIds([]); setBulkTarget('');
      await loadTeams();
    } catch (err) { setError(err.message || 'Bulk transfer failed'); }
    finally { setMoving(false); }
  };

  const runBalancePreview = async () => {
    try {
      setMoving(true); setError('');
      setBalancePreview(await previewBalancedTeams(eventId, balanceTracks,
        balanceSeed === '' ? null : Number(balanceSeed)));
    } catch (err) { setError(err.message || 'Balance preview failed'); }
    finally { setMoving(false); }
  };

  const runBalanceApply = async () => {
    try {
      setMoving(true); setError('');
      await applyBalancedTeams(eventId, balanceTracks, balanceSeed === '' ? null : Number(balanceSeed));
      setBalanceOpen(false); setBalancePreview(null); await loadTeams();
    } catch (err) { setError(err.message || 'Balanced distribution failed'); }
    finally { setMoving(false); }
  };

  const filteredTeams = teams.filter((team) => {
    const matchesSearch = (team.name || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory =
      categoryFilter === 'All Categories' || trackName(team.trackId) === categoryFilter;
    const matchesStatus =
      statusFilter === 'All Statuses' || (team.status || '').toLowerCase() === statusFilter.toLowerCase();
    return matchesSearch && matchesCategory && matchesStatus;
  });

  return (
    <>
    <EventSelector />
    {eventId && <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Team Management</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>View and manage registered teams</div>
        </div>
        <div className="d-flex gap-2">
          <Button variant="outline-primary" onClick={() => setBalanceOpen(true)}><Shuffle size={18} /> Balance tracks</Button>
          <Button 
            variant="primary" 
            className="d-flex align-items-center gap-2"
            onClick={() => navigate('/coordinator/teams/record')}
          >
            <Plus size={18} /> Add New Team
          </Button>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text className="bg-transparent border-end-0">
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control className="border-start-0" placeholder="Search teams..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
          </InputGroup>
          <div className="d-flex gap-2">
            <Form.Select style={{ width: 190 }} value={bulkTarget} onChange={(e) => setBulkTarget(e.target.value)}>
              <option value="">Bulk target track…</option>
              {tracks.map((track) => <option key={track.id} value={track.id}>{track.name}</option>)}
            </Form.Select>
            <Button variant="outline-primary" disabled={!selectedTeamIds.length || !bulkTarget || moving} onClick={handleBulkTransfer}>Move selected</Button>
            <Form.Select style={{ width: '160px' }} value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
              <option>All Categories</option>
              {tracks.map((t) => <option key={t.id}>{t.name}</option>)}
            </Form.Select>
            <Form.Select style={{ width: '150px' }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option>All Statuses</option>
              <option>active</option>
              <option>disqualified</option>
            </Form.Select>
          </div>
        </div>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th><Form.Check checked={filteredTeams.length > 0 && filteredTeams.every((team) => selectedTeamIds.includes(team.id))} onChange={(e) => setSelectedTeamIds(e.target.checked ? filteredTeams.map((team) => team.id) : [])} /></th>
                <th className="border-top-0 border-bottom">Team Name</th>
                <th className="border-top-0 border-bottom">Project</th>
                <th className="border-top-0 border-bottom">Category</th>
                <th className="border-top-0 border-bottom">Track / Group</th>
                <th className="border-top-0 border-bottom">Members</th>
                <th className="border-top-0 border-bottom">Status</th>
                <th className="border-top-0 border-bottom text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={8} className="text-center py-4"><Spinner animation="border" variant="primary" size="sm" /></td></tr>
              ) : filteredTeams.length === 0 ? (
                <tr><td colSpan={8} className="text-center py-4 text-muted">No teams found.</td></tr>
              ) : filteredTeams.map((team) => {
                const disqualified = (team.status || '').toLowerCase() === 'disqualified';
                return (
                  <tr key={team.id}>
                    <td><Form.Check checked={selectedTeamIds.includes(team.id)} onChange={() => setSelectedTeamIds((current) => current.includes(team.id) ? current.filter((id) => id !== team.id) : [...current, team.id])} /></td>
                    <td className="fw-medium py-3">
                      <div className="d-flex align-items-center gap-2">
                        <div className="d-flex align-items-center justify-content-center bg-primary text-white rounded-circle" style={{ width: '32px', height: '32px', fontSize: '0.75rem' }}>
                          {getInitials(team.name)}
                        </div>
                        <span style={{ color: 'var(--cf-text-primary)' }}>{team.name}</span>
                        <TeamRecognitionBadge recognitions={team.recognitions} />
                      </div>
                    </td>
                    <td className="py-3" style={{ color: 'var(--cf-text-primary)' }}>{projectName(team)}</td>
                    <td><Badge bg="secondary">{trackName(team.trackId)}</Badge></td>
                    <td><Badge bg="info" text="dark">{trackName(team.trackId)}</Badge></td>
                    <td>{team.members?.length || 0}/4</td>
                    <td>
                      <Badge bg={disqualified ? 'danger' : 'success'}>
                        {team.status || 'active'}
                      </Badge>
                    </td>
                    <td className="text-end">
                      <div className="d-flex justify-content-end gap-2">
                        <Button variant="outline-primary" size="sm" onClick={() => navigate(`/coordinator/teams/${team.id}`)}>
                          <Eye size={14} />
                        </Button>
                        <Button variant="outline-secondary" size="sm" onClick={() => openMoveModal(team)} title="Move to another track">
                          <Shuffle size={14} />
                        </Button>
                        {disqualified ? (
                          <Button variant="outline-success" size="sm" onClick={() => handleReactivateTeam(team.id)} title="Reactivate">
                            <RotateCcw size={14} />
                          </Button>
                        ) : (
                          <Button variant="outline-danger" size="sm" onClick={() => handleDisqualifyTeam(team.id)} title="Disqualify">
                            <Ban size={14} />
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        </div>
      </Card>

      <Modal show={!!moveTeam} onHide={() => setMoveTeam(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="h5">Move Team to Another Track</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-3">
            Moving <strong>{moveTeam?.name}</strong> from <Badge bg="secondary">{trackName(moveTeam?.trackId)}</Badge> to another track in the same event.
          </p>
          <Form.Group>
            <Form.Label className="fw-medium">Target Track</Form.Label>
            <Form.Select value={moveTargetTrack} onChange={(e) => setMoveTargetTrack(e.target.value)}>
              <option value="">Select a track...</option>
              {moveCandidateTracks.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </Form.Select>
            {moveCandidateTracks.length === 0 && (
              <Form.Text className="text-muted">No other tracks available in this event.</Form.Text>
            )}
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setMoveTeam(null)} disabled={moving}>Cancel</Button>
          <Button variant="primary" onClick={handleMoveTrack} disabled={moving || !moveTargetTrack}>
            {moving ? 'Moving...' : 'Move Team'}
          </Button>
        </Modal.Footer>
      </Modal>
      <Modal show={balanceOpen} onHide={() => setBalanceOpen(false)} size="lg" centered>
        <Modal.Header closeButton><Modal.Title>Balanced team distribution</Modal.Title></Modal.Header>
        <Modal.Body>
          <Form.Label>Target tracks</Form.Label>
          <div className="d-flex flex-wrap gap-3 mb-3">{tracks.map((track) => <Form.Check key={track.id} type="checkbox" label={track.name} checked={balanceTracks.includes(track.id)} onChange={() => setBalanceTracks((current) => current.includes(track.id) ? current.filter((id) => id !== track.id) : [...current, track.id])} />)}</div>
          <Form.Group className="mb-3"><Form.Label>Optional deterministic seed</Form.Label><Form.Control type="number" value={balanceSeed} onChange={(e) => setBalanceSeed(e.target.value)} /></Form.Group>
          <Button onClick={runBalancePreview} disabled={!balanceTracks.length || moving}>Preview</Button>
          {balancePreview && <div className="mt-3">
            <h6>Counts</h6>
            <ul>{balancePreview.counts?.map((count) => <li key={count.trackId}>{count.trackName}: {count.beforeCount} → {count.afterCount}</li>)}</ul>
            <p>{balancePreview.moves?.filter((move) => move.currentTrackId !== move.proposedTrackId).length || 0} teams will move.</p>
            {!!balancePreview.excluded?.length && <Alert variant="warning">{balancePreview.excluded.map((item) => `${item.teamName}: ${item.reason}`).join('; ')}</Alert>}
          </div>}
        </Modal.Body>
        <Modal.Footer><Button variant="secondary" onClick={() => setBalanceOpen(false)}>Cancel</Button><Button onClick={runBalanceApply} disabled={!balancePreview || moving}>Apply distribution</Button></Modal.Footer>
      </Modal>
    </div>}
    </>
  );
};

export default TeamManagement;
