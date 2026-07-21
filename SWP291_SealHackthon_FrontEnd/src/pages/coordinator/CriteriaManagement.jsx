import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Badge, Modal, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Plus, Edit, Trash2, Search } from 'lucide-react';
import {
  getRounds,
  getTracks,
  getRoundCriteria,
  createRoundCriterion,
  updateRoundCriterion,
  deleteRoundCriterion,
} from '../../api/hackathonApi';

const listOf = (data) => data?.content || data || [];

const CriteriaManagement = () => {
  const [rounds, setRounds] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [selectedRound, setSelectedRound] = useState('');
  const [criteria, setCriteria] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingCriteria, setEditingCriteria] = useState(null);
  const [newCriteria, setNewCriteria] = useState({ name: '', weight: '', description: '', status: 'active' });

  // Applicable Category == the track that the round belongs to
  const trackNameOf = (trackId) => tracks.find((t) => t.id === trackId)?.name || '—';
  const roundOf = (id) => rounds.find((r) => r.id === id);
  const selectedTrackName = () => trackNameOf(roundOf(selectedRound)?.trackId);

  useEffect(() => {
    (async () => {
      try {
        const [roundData, trackData] = await Promise.all([
          getRounds({ size: 100 }),
          getTracks({ size: 100 }).catch(() => []),
        ]);
        const list = listOf(roundData);
        setTracks(listOf(trackData));
        setRounds(list);
        if (list.length) setSelectedRound(list[0].id);
        else setLoading(false);
      } catch (err) {
        setError(err.message);
        setLoading(false);
      }
    })();
  }, []);

  const loadCriteria = useCallback(async (roundId) => {
    if (!roundId) return;
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

  useEffect(() => {
    if (selectedRound) loadCriteria(selectedRound);
  }, [selectedRound, loadCriteria]);

  const filteredCriteria = criteria.filter((item) =>
    (item.name || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleSaveCriteria = async () => {
    if (!newCriteria.name || newCriteria.weight === '') {
      alert('Please fill all fields');
      return;
    }
    if (!selectedRound) {
      alert('Select a round first');
      return;
    }
    setSaving(true);
    setError('');
    const payload = {
      roundId: selectedRound,
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
      await loadCriteria(selectedRound);
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
      await loadCriteria(selectedRound);
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
        <Button variant="primary" className="d-flex align-items-center gap-2" disabled={!selectedRound} onClick={() => {
          setEditingCriteria(null);
          setNewCriteria({ name: '', weight: '', description: '', status: 'active' });
          setShowModal(true);
        }}>
          <Plus size={18} /> Add Criteria
        </Button>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between gap-2">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text>
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control
              placeholder="Search criteria..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </InputGroup>
          <Form.Select style={{ maxWidth: '260px' }} value={selectedRound} onChange={(e) => setSelectedRound(e.target.value)}>
            {rounds.length === 0 && <option value="">No rounds available</option>}
            {rounds.map((r) => (
              <option key={r.id} value={r.id}>{r.name}</option>
            ))}
          </Form.Select>
        </div>
        <div className="table-responsive">
          {loading ? (
            <div className="text-center py-5"><Spinner animation="border" /></div>
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
                  <tr><td colSpan={5} className="text-center text-muted py-4">No criteria for this round</td></tr>
                )}
                {filteredCriteria.map((item) => {
                  const status = (item.status || 'active').toLowerCase();
                  return (
                    <tr key={item.id}>
                      <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{item.name}</td>
                      <td className="py-3">
                        <Badge bg="secondary" className="bg-opacity-25 text-secondary border">{item.weight}%</Badge>
                      </td>
                      <td className="py-3">{trackNameOf(roundOf(item.roundId ?? selectedRound)?.trackId)}</td>
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
