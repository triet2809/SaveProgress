import { useState, useEffect, useCallback, useMemo } from 'react';
import { Card, Table, Button, Badge, Modal, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Plus, Edit, Trash2, Search } from 'lucide-react';
import {
  getEvents,
  getLogicalRounds,
  getTracks,
  getRoundCriteria,
  createRoundCriterion,
  updateRoundCriterion,
  deleteRoundCriterion,
} from '../../api/hackathonApi';

const listOf = (data) => data?.content || data || [];

const CriteriaManagement = () => {
  const [events, setEvents] = useState([]);
  const [logicalRounds, setLogicalRounds] = useState([]); // logical rounds của event đang chọn
  const [tracks, setTracks] = useState([]); // tracks của event đang chọn (để map trackId -> name)
  const [selectedEvent, setSelectedEvent] = useState('');
  const [selectedLogicalRound, setSelectedLogicalRound] = useState('');
  const [selectedTrack, setSelectedTrack] = useState(''); // trackId
  const [criteria, setCriteria] = useState([]);
  const [loadingEvents, setLoadingEvents] = useState(true);
  const [loadingRounds, setLoadingRounds] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingCriteria, setEditingCriteria] = useState(null);
  const [newCriteria, setNewCriteria] = useState({ name: '', weight: '', description: '', status: 'active' });

  // Logical round đang chọn (chứa nhiều track execution).
  const currentLogical = useMemo(
    () => logicalRounds.find((lr) => lr.logicalRoundId === selectedLogicalRound),
    [logicalRounds, selectedLogicalRound],
  );
  // Các track của logical round đang chọn (mỗi track = 1 physical round).
  const trackRounds = useMemo(() => currentLogical?.trackRounds || [], [currentLogical]);
  // Physical round id tương ứng track đang chọn → criteria gắn vào đây.
  const resolvedRound = useMemo(
    () => trackRounds.find((tr) => tr.trackId === selectedTrack),
    [trackRounds, selectedTrack],
  );
  const resolvedRoundId = resolvedRound?.id || '';
  // trackRounds (RoundResponse) không có trackName → map qua danh sách tracks của event.
  const trackNameOf = (trackId) => tracks.find((t) => t.id === trackId)?.name
    || trackRounds.find((tr) => tr.trackId === trackId)?.trackName || '—';
  const selectedTrackName = () => trackNameOf(selectedTrack);

  // Load danh sách event.
  useEffect(() => {
    (async () => {
      try {
        const data = await getEvents({ size: 100 });
        const list = listOf(data);
        setEvents(list);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoadingEvents(false);
      }
    })();
  }, []);

  // Chọn event → load logical rounds của event đó. Reset round + track.
  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    if (!selectedEvent) {
      setLogicalRounds([]);
      setSelectedLogicalRound('');
      setSelectedTrack('');
      setCriteria([]);
      return;
    }
    let active = true;
    (async () => {
      setLoadingRounds(true);
      setError('');
      setSelectedLogicalRound('');
      setSelectedTrack('');
      setCriteria([]);
      try {
        const [rounds, trackData] = await Promise.all([
          getLogicalRounds(selectedEvent),
          getTracks({ eventId: selectedEvent, size: 100 }).catch(() => []),
        ]);
        if (active) {
          setLogicalRounds(listOf(rounds));
          setTracks(listOf(trackData));
        }
      } catch (err) {
        if (active) setError(err.message);
      } finally {
        if (active) setLoadingRounds(false);
      }
    })();
    return () => { active = false; };
  }, [selectedEvent]);
  /* eslint-enable react-hooks/set-state-in-effect */

  const loadCriteria = useCallback(async (roundId) => {
    if (!roundId) { setCriteria([]); return; }
    setLoading(true);
    setError('');
    try {
      const data = await getRoundCriteria({ roundId });
      setCriteria(listOf(data));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // Chọn đủ event + round + track → resolve physical round → load criteria.
  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    loadCriteria(resolvedRoundId);
  }, [resolvedRoundId, loadCriteria]);
  /* eslint-enable react-hooks/set-state-in-effect */

  const filteredCriteria = criteria.filter((item) =>
    (item.name || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleSaveCriteria = async () => {
    if (!newCriteria.name || newCriteria.weight === '') {
      alert('Please fill all fields');
      return;
    }
    if (!resolvedRoundId) {
      alert('Select event, round and track first');
      return;
    }
    setSaving(true);
    setError('');
    const payload = {
      roundId: resolvedRoundId,
      name: newCriteria.name,
      weight: Number(newCriteria.weight),
      description: newCriteria.description || '',
      status: newCriteria.status || 'active',
    };
    try {
      if (editingCriteria) {
        await updateRoundCriterion(editingCriteria.id, {
          name: payload.name,
          weight: payload.weight,
          description: payload.description,
          status: payload.status,
        });
      } else {
        await createRoundCriterion(payload);
      }
      setEditingCriteria(null);
      setNewCriteria({ name: '', weight: '', description: '', status: 'active' });
      setShowModal(false);
      await loadCriteria(resolvedRoundId);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteCriteria = async (id) => {
    if (!window.confirm('Delete this criteria?')) return;
    setError('');
    try {
      await deleteRoundCriterion(id);
      await loadCriteria(resolvedRoundId);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Criteria Management</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Manage scoring rubrics and weighting</div>
        </div>
        <Button variant="primary" className="d-flex align-items-center gap-2" disabled={!resolvedRoundId} onClick={() => {
          setEditingCriteria(null);
          setNewCriteria({ name: '', weight: '', description: '', status: 'active' });
          setShowModal(true);
        }}>
          <Plus size={18} /> Add Criteria
        </Button>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between gap-2 flex-wrap">
          <InputGroup style={{ maxWidth: '260px' }}>
            <InputGroup.Text>
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control
              placeholder="Search criteria..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </InputGroup>
          <div className="d-flex align-items-center gap-2 flex-wrap">
            {/* Bước 1: chọn sự kiện */}
            <Form.Select style={{ maxWidth: '220px' }} value={selectedEvent} onChange={(e) => setSelectedEvent(e.target.value)} disabled={loadingEvents}>
              <option value="">{loadingEvents ? 'Loading events...' : '1. Select event'}</option>
              {events.map((ev) => (
                <option key={ev.id} value={ev.id}>{ev.title || ev.name}</option>
              ))}
            </Form.Select>
            {/* Bước 2: chọn round (logical) */}
            <Form.Select style={{ maxWidth: '200px' }} value={selectedLogicalRound} onChange={(e) => { setSelectedLogicalRound(e.target.value); setSelectedTrack(''); }} disabled={!selectedEvent || loadingRounds}>
              <option value="">{loadingRounds ? 'Loading rounds...' : '2. Select round'}</option>
              {logicalRounds.map((lr) => (
                <option key={lr.logicalRoundId} value={lr.logicalRoundId}>{lr.name}</option>
              ))}
            </Form.Select>
            {/* Bước 3: chọn track */}
            <Form.Select style={{ maxWidth: '200px' }} value={selectedTrack} onChange={(e) => setSelectedTrack(e.target.value)} disabled={!selectedLogicalRound}>
              <option value="">3. Select track</option>
              {trackRounds.map((tr) => (
                <option key={tr.id} value={tr.trackId}>{trackNameOf(tr.trackId)}</option>
              ))}
            </Form.Select>
          </div>
        </div>
        <div className="table-responsive">
          {loading ? (
            <div className="text-center py-5"><Spinner animation="border" /></div>
          ) : !resolvedRoundId ? (
            <div className="text-center text-muted py-5">Select event, round and track to view criteria.</div>
          ) : (
            <Table className="mb-0" hover>
              <thead>
                <tr>
                  <th className="border-top-0 border-bottom">Criteria Name</th>
                  <th className="border-top-0 border-bottom">Weight (%)</th>
                  <th className="border-top-0 border-bottom">Applicable Category</th>
                  <th className="border-top-0 border-bottom">Status</th>
                  <th className="border-top-0 border-bottom text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredCriteria.length === 0 && (
                  <tr><td colSpan={5} className="text-center text-muted py-4">No criteria for this track's round</td></tr>
                )}
                {filteredCriteria.map((item) => {
                  const status = (item.status || 'active').toLowerCase();
                  return (
                    <tr key={item.id}>
                      <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{item.name}</td>
                      <td className="py-3">
                        <Badge bg="secondary" className="bg-opacity-25 text-secondary border">{item.weight}%</Badge>
                      </td>
                      <td className="py-3">{selectedTrackName()}</td>
                      <td className="py-3">
                        <Badge bg={status === 'active' ? 'success' : 'secondary'}>{item.status || 'active'}</Badge>
                      </td>
                      <td className="py-3 text-end">
                        <Button variant="link" size="sm" className="p-0 text-primary me-3" onClick={() => {
                          setEditingCriteria(item);
                          setNewCriteria({
                            name: item.name,
                            weight: item.weight,
                            description: item.description || '',
                            status: item.status || 'active',
                          });
                          setShowModal(true);
                        }}>
                          <Edit size={16} />
                        </Button>
                        <Button variant="link" size="sm" className="p-0 text-danger" onClick={() => handleDeleteCriteria(item.id)}>
                          <Trash2 size={16} />
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </Table>
          )}
        </div>
      </Card>
      <Modal
        show={showModal}
        onHide={() => {
          setShowModal(false);
          setEditingCriteria(null);
        }}
      >
        <Modal.Header closeButton>
          <Modal.Title>
            {editingCriteria ? 'Edit Criteria' : 'Create Criteria'}
          </Modal.Title>
        </Modal.Header>

        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Criteria Name</Form.Label>
              <Form.Control
                value={newCriteria.name}
                onChange={(e) => setNewCriteria({ ...newCriteria, name: e.target.value })}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Weight (%)</Form.Label>
              <Form.Control
                type="number"
                step="0.01"
                value={newCriteria.weight}
                onChange={(e) => setNewCriteria({ ...newCriteria, weight: e.target.value })}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Applicable Category</Form.Label>
              <Form.Control type="text" readOnly disabled value={selectedTrackName()} />
              <Form.Text className="text-muted">
                Derived from the selected round's track.
              </Form.Text>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Status</Form.Label>
              <Form.Select
                value={newCriteria.status}
                onChange={(e) => setNewCriteria({ ...newCriteria, status: e.target.value })}
              >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </Form.Select>
            </Form.Group>

            <Form.Group>
              <Form.Label>Description</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                value={newCriteria.description}
                onChange={(e) => setNewCriteria({ ...newCriteria, description: e.target.value })}
              />
            </Form.Group>
          </Form>
        </Modal.Body>

        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => {
              setShowModal(false);
              setEditingCriteria(null);
            }}
          >
            Cancel
          </Button>

          <Button variant="primary" onClick={handleSaveCriteria} disabled={saving}>
            {saving ? 'Saving...' : editingCriteria ? 'Update Criteria' : 'Create Criteria'}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default CriteriaManagement;
