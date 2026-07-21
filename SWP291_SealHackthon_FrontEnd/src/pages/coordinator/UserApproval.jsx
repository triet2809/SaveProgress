import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Badge, Modal, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Check, X, Eye, Search } from 'lucide-react';
import { getPendingUsers, approveUser, rejectUser } from '../../api/userApi';

const roleLabel = (roles) => (roles && roles.length ? roles.join(', ') : 'Team Member');
const userType = (u) => {
  if (u.isGuest) return 'Guest';
  if (u.studentType === 'fpt') return 'FPT Student';
  if (u.studentType === 'external') return 'External';
  if (u.department || u.position) return 'Faculty';
  return 'User';
};
const fmtDate = (iso) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
};

const UserApproval = () => {
  const [pendingUsers, setPendingUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const load = async () => {
    try {
      setLoading(true);
      setError('');
      const res = await getPendingUsers();
      const users = (res.value || []).map((u) => ({
        id: u.id,
        name: u.fullName || u.email,
        email: u.email,
        type: userType(u),
        requestedRole: roleLabel(u.roles),
        date: fmtDate(u.createdAt),
      }));
      setPendingUsers(users);
    } catch (err) {
      setError(err.message || 'Failed to load pending users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleApprove = async (id) => {
    setBusyId(id);
    setActionError('');
    try {
      await approveUser(id);
      await load();
    } catch (err) {
      setActionError(err.message || 'Failed to approve user');
    } finally {
      setBusyId(null);
    }
  };

  const handleReject = async (id) => {
    if (!window.confirm('Reject this request?')) return;
    setBusyId(id);
    setActionError('');
    try {
      await rejectUser(id);
      await load();
    } catch (err) {
      setActionError(err.message || 'Failed to reject user');
    } finally {
      setBusyId(null);
    }
  };

  const filteredUsers = useMemo(() => pendingUsers.filter(
    (user) =>
      user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase())
  ), [pendingUsers, searchTerm]);

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>User Approval</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Review and approve pending account requests</div>
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}
      {actionError && <Alert variant="danger" onClose={() => setActionError('')} dismissible>{actionError}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text>
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control
              placeholder="Search users..."
              value={searchTerm}
              onChange={(e) =>
                setSearchTerm(e.target.value)
              }
            />
          </InputGroup>
        </div>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className="border-top-0 border-bottom">Name</th>
                <th className="border-top-0 border-bottom">Email</th>
                <th className="border-top-0 border-bottom">User Type</th>
                <th className="border-top-0 border-bottom">Requested Role</th>
                <th className="border-top-0 border-bottom">Date</th>
                <th className="border-top-0 border-bottom text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={6} className="text-center py-4"><Spinner animation="border" size="sm" /></td></tr>
              )}
              {!loading && filteredUsers.length === 0 && (
                <tr><td colSpan={6} className="text-center py-4 text-muted">No pending requests.</td></tr>
              )}
              {!loading && filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{user.name}</td>
                  <td className="py-3" style={{ color: 'var(--cf-text-secondary)' }}>{user.email}</td>
                  <td className="py-3">
                    <Badge bg="secondary" className="bg-opacity-25 text-secondary border">{user.type}</Badge>
                  </td>
                  <td className="py-3 fw-medium">{user.requestedRole}</td>
                  <td className="py-3">{user.date}</td>
                  <td className="py-3 text-end">
                    <Button variant="link" size="sm" className="text-primary me-2" onClick={() => { setSelectedUser(user); setShowModal(true); }} >
                      <Eye size={14} />
                    </Button>
                    <Button variant="outline-success" size="sm" className="me-2 d-inline-flex align-items-center gap-1" disabled={busyId === user.id} onClick={() => handleApprove(user.id)}>
                      {busyId === user.id ? <Spinner animation="border" size="sm" /> : <Check size={14} />} Approve
                    </Button>
                    <Button variant="outline-danger" size="sm" className="d-inline-flex align-items-center gap-1" disabled={busyId === user.id} onClick={() => handleReject(user.id)}>
                      <X size={14} /> Reject
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      </Card>
      <Modal
        show={showModal}
        onHide={() => setShowModal(false)}
      >
        <Modal.Header closeButton>
          <Modal.Title>
            User Request Details
          </Modal.Title>
        </Modal.Header>

        <Modal.Body>
          {selectedUser && (
            <>
              <p>
                <strong>Name:</strong>{' '}
                {selectedUser.name}
              </p>

              <p>
                <strong>Email:</strong>{' '}
                {selectedUser.email}
              </p>

              <p>
                <strong>User Type:</strong>{' '}
                {selectedUser.type}
              </p>

              <p>
                <strong>Requested Role:</strong>{' '}
                {selectedUser.requestedRole}
              </p>

              <p>
                <strong>Date:</strong>{' '}
                {selectedUser.date}
              </p>
            </>
          )}
        </Modal.Body>

        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => setShowModal(false)}
          >
            Close
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default UserApproval;
