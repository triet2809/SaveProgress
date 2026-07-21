import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Badge, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Trophy, Plus, Edit, Trash2, Search } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getPrizes, deletePrize } from '../../api/hackathonApi';

const listOf = (data) => data?.content || data || [];

const AwardsManagement = () => {
  const navigate = useNavigate();
  const [awards, setAwards] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAwards = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await getPrizes({ size: 100 });
      setAwards(listOf(data));
    } catch (err) {
      setError(err.message || 'Failed to load awards');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAwards();
  }, []);

  const filteredAwards = (awards || []).filter(
    (award) =>
      (award.name || '')
        .toLowerCase()
        .includes(searchTerm.toLowerCase())
  );

  const handleDeleteAward = async (id) => {
    if (!window.confirm('Delete this award?')) return;
    try {
      setError('');
      await deletePrize(id);
      await loadAwards();
    } catch (err) {
      setError(err.message || 'Failed to delete award');
    }
  };

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Awards Management</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Configure prize pools and assign winners</div>
        </div>
        <Button variant="primary" className="d-flex align-items-center gap-2" onClick={() => navigate('/coordinator/awards/new')}>
          <Plus size={18} /> Add Award
        </Button>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom">
          <InputGroup
            style={{ maxWidth: '300px' }}>
            <InputGroup.Text>
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control placeholder="Search awards..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)
            }
            />
          </InputGroup>
        </div>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className="border-top-0 border-bottom">Award Name</th>
                <th className="border-top-0 border-bottom">Prize</th>
                <th className="border-top-0 border-bottom">Category</th>
                <th className="border-top-0 border-bottom">Status</th>
                <th className="border-top-0 border-bottom">Winner</th>
                <th className="border-top-0 border-bottom text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="text-center py-4"><Spinner animation="border" variant="primary" size="sm" /></td></tr>
              ) : filteredAwards.length === 0 ? (
                <tr><td colSpan={6} className="text-center py-4 text-muted">No awards found.</td></tr>
              ) : filteredAwards.map((award) => {
                const status = award.teamId ? 'Assigned' : 'Unassigned';
                return (
                  <tr key={award.id}>
                    <td className="fw-medium py-3 d-flex align-items-center gap-2">
                      <Trophy size={16} className="text-warning" />
                      <span style={{ color: 'var(--cf-text-primary)' }}>{award.name}</span>
                    </td>
                    <td className="py-3 text-success fw-medium">{award.description || '-'}</td>
                    <td className="py-3">{award.trackName || 'Overall'}</td>
                    <td className="py-3">
                      <Badge bg={status === 'Assigned' ? 'success' : 'secondary'}>
                        {status}
                      </Badge>
                    </td>
                    <td className="py-3 fw-medium" style={{ color: 'var(--cf-text-primary)' }}>{award.teamName || '-'}</td>
                    <td className="py-3 text-end">
                      <Button variant="link" size="sm" className="p-0 text-primary" onClick={() => navigate(`/coordinator/awards/${award.id}/edit`)}>
                        <Edit size={16} />
                      </Button>
                      <Button variant="link" size="sm" className="p-0 text-danger" onClick={() => handleDeleteAward(award.id)}  >
                        <Trash2 size={16} />
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  );
};

export default AwardsManagement;
