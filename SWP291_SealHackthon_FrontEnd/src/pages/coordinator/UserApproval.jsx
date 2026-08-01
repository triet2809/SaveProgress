import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Card, Table, Button, Badge, Modal, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Check, X, Eye, Search, Upload } from 'lucide-react';
import { getPendingUsers, approveUser, rejectUser, importUsers } from '../../api/userApi';

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
  // Import Excel: file input ẩn + trạng thái đang import + kết quả tổng hợp.
  const fileInputRef = useRef(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [importError, setImportError] = useState('');

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
        studentId: u.studentId || '—',
        campus: u.campusName || u.universityName || '—',
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

  // Import Excel: mở hộp chọn file khi bấm nút Import.
  const handleImportClick = () => {
    setImportError('');
    setImportResult(null);
    fileInputRef.current?.click();
  };

  // Gửi file Excel lên BE, hiện kết quả tổng hợp, rồi refresh danh sách pending.
  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImporting(true);
    setImportError('');
    setImportResult(null);
    try {
      const res = await importUsers(file);
      if (!res.ok) {
        setImportError(res.data?.message || 'Import failed');
        return;
      }
      setImportResult(res.value);
      await load();
    } catch (err) {
      setImportError(err.message || 'Could not connect to the server');
    } finally {
      setImporting(false);
      // Reset input để chọn lại cùng file được nếu cần.
      if (fileInputRef.current) fileInputRef.current.value = '';
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
        <div>
          <input
            ref={fileInputRef}
            type="file"
            accept=".xlsx,.xls"
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />
          <Button variant="primary" className="d-inline-flex align-items-center gap-2" disabled={importing} onClick={handleImportClick}>
            {importing ? <><Spinner animation="border" size="sm" /> Importing...</> : <><Upload size={16} /> Import from Excel</>}
          </Button>
        </div>
      </div>

      {importError && <Alert variant="danger" onClose={() => setImportError('')} dismissible>{importError}</Alert>}

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
                <th className="border-top-0 border-bottom">Student ID</th>
                <th className="border-top-0 border-bottom">Campus</th>
                <th className="border-top-0 border-bottom">Requested Role</th>
                <th className="border-top-0 border-bottom">Date</th>
                <th className="border-top-0 border-bottom text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={8} className="text-center py-4"><Spinner animation="border" size="sm" /></td></tr>
              )}
              {!loading && filteredUsers.length === 0 && (
                <tr><td colSpan={8} className="text-center py-4 text-muted">No pending requests.</td></tr>
              )}
              {!loading && filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>{user.name}</td>
                  <td className="py-3" style={{ color: 'var(--cf-text-secondary)' }}>{user.email}</td>
                  <td className="py-3">
                    <Badge bg="secondary" className="bg-opacity-25 text-secondary border">{user.type}</Badge>
                  </td>
                  <td className="py-3">{user.studentId}</td>
                  <td className="py-3">{user.campus}</td>
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
                <strong>Student ID:</strong>{' '}
                {selectedUser.studentId}
              </p>

              <p>
                <strong>Campus:</strong>{' '}
                {selectedUser.campus}
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

      <Modal show={!!importResult} onHide={() => setImportResult(null)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Import Result</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {importResult && (
            <>
              <div className="d-flex gap-3 mb-3">
                <Badge bg="secondary">Total: {importResult.totalRows}</Badge>
                <Badge bg="success">Created: {importResult.created}</Badge>
                <Badge bg="warning" text="dark">Skipped: {importResult.skipped}</Badge>
                <Badge bg="danger">Failed: {importResult.failed}</Badge>
              </div>
              <div className="table-responsive" style={{ maxHeight: '360px', overflowY: 'auto' }}>
                <Table size="sm" hover>
                  <thead>
                    <tr>
                      <th>Row</th>
                      <th>Email</th>
                      <th>Status</th>
                      <th>Message</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(importResult.results || []).map((r, i) => (
                      <tr key={i}>
                        <td>{r.row}</td>
                        <td>{r.email || '—'}</td>
                        <td>
                          <Badge bg={r.status === 'created' ? 'success' : r.status === 'skipped' ? 'warning' : 'danger'} text={r.status === 'skipped' ? 'dark' : undefined}>
                            {r.status}
                          </Badge>
                        </td>
                        <td className="small text-muted">{r.message || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setImportResult(null)}>Close</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default UserApproval;
