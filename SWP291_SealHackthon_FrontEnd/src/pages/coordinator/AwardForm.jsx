import React, { useState, useEffect, useCallback } from 'react';
import { Card, Button, Form, Row, Col, Spinner, Alert, Badge, Modal, Table } from 'react-bootstrap';
import { ArrowLeft, Trophy, DollarSign, Tag, CheckCircle, Users, History, XCircle, Repeat } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getPrize, createPrize, updatePrize, getEvents, getTeams,
  revokePrize, reassignPrize, getPrizeRevisions,
} from '../../api/hackathonApi';

const listOf = (data) => data?.content || data || [];

const AwardForm = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditing = !!id;

  // prizeAmount: giá trị tiền thưởng dạng số (khớp field BigDecimal prizeAmount ở BE).
  // description: mô tả tự do, tách bạch khỏi số tiền (trước đây bị nhồi chung một field).
  const [award, setAward] = useState({
    name: '',
    prizeAmount: '',
    description: '',
    teamId: '',
    eventId: '',
  });
  const [events, setEvents] = useState([]);
  const [teams, setTeams] = useState([]);
  const [revisions, setRevisions] = useState([]);
  const [loading, setLoading] = useState(isEditing);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  // Modal thu hồi / chuyển giải thưởng
  const [showRevise, setShowRevise] = useState(false);
  const [reviseMode, setReviseMode] = useState('revoke'); // 'revoke' | 'reassign'
  const [reviseForm, setReviseForm] = useState({ newTeamId: '', reason: '', evidenceNote: '' });
  const [revising, setRevising] = useState(false);

  // Tải lại lịch sử chỉnh sửa của giải thưởng (thu hồi/chuyển).
  const loadRevisions = useCallback(async () => {
    if (!isEditing) return;
    try {
      const data = await getPrizeRevisions(id);
      setRevisions(data || []);
    } catch {
      // Lịch sử trống hoặc lỗi nhẹ không nên chặn cả form
    }
  }, [id, isEditing]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setError('');
        const evData = await getEvents({ size: 100 });
        const evList = listOf(evData);
        if (!active) return;
        setEvents(evList);

        if (isEditing) {
          const prize = await getPrize(id);
          if (!active) return;
          setAward({
            name: prize.name || '',
            prizeAmount: prize.prizeAmount != null ? String(prize.prizeAmount) : '',
            description: prize.description || '',
            teamId: prize.teamId || '',
            eventId: prize.eventId || '',
          });
          const teamData = await getTeams({ eventId: prize.eventId, size: 200 });
          if (active) setTeams(listOf(teamData));
          await loadRevisions();
        } else if (evList.length === 1) {
          setAward((prev) => ({ ...prev, eventId: evList[0].id }));
        }
      } catch (err) {
        if (active) setError(err.message || 'Failed to load award');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [id, isEditing, loadRevisions]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setAward((prev) => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    if (!award.name) {
      alert('Please fill out the award name.');
      return;
    }
    if (!award.eventId) {
      alert('Please select an event for this award.');
      return;
    }
    try {
      setSaving(true);
      setError('');
      // Chuyển prizeAmount về number; để trống nếu không nhập.
      const amount = award.prizeAmount === '' ? undefined : Number(award.prizeAmount);
      if (amount != null && Number.isNaN(amount)) {
        setError('Prize amount must be a number.');
        setSaving(false);
        return;
      }
      if (isEditing) {
        const patch = {
          name: award.name,
          prizeAmount: amount,
          description: award.description || undefined,
        };
        // Gán đội thắng: set teamId + awardedAt, hoặc xóa cả hai khi bỏ gán.
        if (award.teamId) {
          patch.teamId = award.teamId;
          patch.awardedAt = new Date().toISOString().slice(0, 19);
        }
        await updatePrize(id, patch);
      } else {
        await createPrize({
          eventId: award.eventId,
          name: award.name,
          prizeAmount: amount,
          description: award.description || undefined,
        });
      }
      navigate('/coordinator/awards');
    } catch (err) {
      setError(err.message || 'Failed to save award');
    } finally {
      setSaving(false);
    }
  };

  // Mở modal thu hồi hoặc chuyển giải thưởng.
  const openRevise = (mode) => {
    setReviseMode(mode);
    setReviseForm({ newTeamId: '', reason: '', evidenceNote: '' });
    setShowRevise(true);
  };

  // Gửi yêu cầu thu hồi/chuyển; BE ghi lại lịch sử và cập nhật timeline của (các) đội.
  const submitRevise = async () => {
    if (!reviseForm.reason.trim()) {
      alert('Please provide a reason.');
      return;
    }
    if (reviseMode === 'reassign' && !reviseForm.newTeamId) {
      alert('Please select the new winning team.');
      return;
    }
    try {
      setRevising(true);
      setError('');
      if (reviseMode === 'revoke') {
        await revokePrize(id, { reason: reviseForm.reason, evidenceNote: reviseForm.evidenceNote || undefined });
      } else {
        await reassignPrize(id, {
          newTeamId: reviseForm.newTeamId,
          reason: reviseForm.reason,
          evidenceNote: reviseForm.evidenceNote || undefined,
        });
      }
      // Nạp lại giải thưởng + lịch sử để phản ánh thay đổi.
      const prize = await getPrize(id);
      setAward((prev) => ({ ...prev, teamId: prize.teamId || '' }));
      await loadRevisions();
      setShowRevise(false);
    } catch (err) {
      setError(err.message || 'Failed to revise award');
    } finally {
      setRevising(false);
    }
  };

  if (loading) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  const currentTeamName = teams.find((t) => String(t.id) === String(award.teamId))?.name || '';

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-3 mb-4">
        <Button variant="link" className="p-0 text-muted" onClick={() => navigate('/coordinator/awards')}>
          <ArrowLeft size={24} />
        </Button>
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>
            {isEditing ? 'Edit Award' : 'Create New Award'}
          </h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            {isEditing ? 'Update prize details and assign winners' : 'Configure a new prize pool'}
          </div>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Row className="justify-content-center">
        <Col lg={8}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Card.Body className="p-4 p-md-5">
              <Form>
                <div className="mb-4">
                  <h5 className="fw-bold mb-3 border-bottom pb-2">Basic Information</h5>
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-medium d-flex align-items-center gap-2"><Trophy size={16} className="text-warning" /> Award Name</Form.Label>
                    <Form.Control
                      type="text"
                      name="name"
                      placeholder="e.g. Grand Prize Winner"
                      value={award.name}
                      onChange={handleChange}
                    />
                  </Form.Group>

                  <Row>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-medium d-flex align-items-center gap-2"><DollarSign size={16} className="text-success" /> Prize Amount</Form.Label>
                        <Form.Control
                          type="number"
                          min="0"
                          step="0.01"
                          name="prizeAmount"
                          placeholder="e.g. 10000"
                          value={award.prizeAmount}
                          onChange={handleChange}
                        />
                        <Form.Text className="text-muted">Numeric value only (currency symbol is added when displayed).</Form.Text>
                      </Form.Group>
                    </Col>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-medium d-flex align-items-center gap-2"><Tag size={16} className="text-info" /> Event</Form.Label>
                        <Form.Select name="eventId" value={award.eventId} onChange={handleChange} disabled={isEditing}>
                          <option value="">Select Event</option>
                          {events.map((ev) => (
                            <option key={ev.id} value={ev.id}>{ev.title}</option>
                          ))}
                        </Form.Select>
                      </Form.Group>
                    </Col>
                  </Row>

                  <Form.Group className="mb-3">
                    <Form.Label className="fw-medium">Description</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={2}
                      name="description"
                      placeholder="Optional notes about this award"
                      value={award.description}
                      onChange={handleChange}
                    />
                  </Form.Group>
                </div>

                <div className="mb-4">
                  <h5 className="fw-bold mb-3 border-bottom pb-2">Assignment</h5>
                  <Row>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-medium d-flex align-items-center gap-2"><CheckCircle size={16} className="text-primary" /> Status</Form.Label>
                        <Form.Control type="text" value={award.teamId ? 'Assigned' : 'Unassigned'} disabled />
                      </Form.Group>
                    </Col>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-medium d-flex align-items-center gap-2"><Users size={16} className="text-secondary" /> Winner (Team)</Form.Label>
                        {isEditing ? (
                          <Form.Select name="teamId" value={award.teamId} onChange={handleChange}>
                            <option value="">— No winner (unassigned) —</option>
                            {teams.map((t) => (
                              <option key={t.id} value={t.id}>{t.name}</option>
                            ))}
                          </Form.Select>
                        ) : (
                          <Form.Control type="text" value="Save award first, then assign a winner" disabled />
                        )}
                        <Form.Text className="text-muted">Select the winning team to award this prize.</Form.Text>
                      </Form.Group>
                    </Col>
                  </Row>

                  {/* Thu hồi / chuyển giải chỉ khả dụng khi giải đã có đội thắng */}
                  {isEditing && award.teamId && (
                    <div className="d-flex gap-2 mt-2">
                      <Button variant="outline-danger" size="sm" className="d-flex align-items-center gap-2" onClick={() => openRevise('revoke')}>
                        <XCircle size={16} /> Revoke Award
                      </Button>
                      <Button variant="outline-primary" size="sm" className="d-flex align-items-center gap-2" onClick={() => openRevise('reassign')}>
                        <Repeat size={16} /> Reassign Award
                      </Button>
                    </div>
                  )}
                </div>

                <div className="d-flex justify-content-end gap-3 mt-5 pt-3 border-top">
                  <Button variant="secondary" onClick={() => navigate('/coordinator/awards')} disabled={saving}>
                    Cancel
                  </Button>
                  <Button variant="primary" onClick={handleSave} disabled={saving}>
                    {saving ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Award'}
                  </Button>
                </div>
              </Form>
            </Card.Body>
          </Card>

          {/* Lịch sử chỉnh sửa giải thưởng — không xóa cứng, luôn giữ vết */}
          {isEditing && (
            <Card className="mt-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Card.Header className="bg-transparent border-bottom p-4">
                <h5 className="fw-bold mb-0 d-flex align-items-center gap-2"><History size={18} className="text-secondary" /> Revision History</h5>
              </Card.Header>
              <Card.Body className="p-0">
                {revisions.length === 0 ? (
                  <div className="p-4 text-muted text-center">No revisions yet.</div>
                ) : (
                  <Table hover responsive className="mb-0 align-middle">
                    <thead>
                      <tr className="text-muted small">
                        <th className="px-4">Action</th>
                        <th>From</th>
                        <th>To</th>
                        <th>Reason</th>
                        <th>By</th>
                        <th>When</th>
                      </tr>
                    </thead>
                    <tbody>
                      {revisions.map((r) => (
                        <tr key={r.id}>
                          <td className="px-4">
                            <Badge bg={r.action === 'REVOKE' ? 'danger' : 'primary'}>{r.action}</Badge>
                          </td>
                          <td>{r.oldTeamName || '-'}</td>
                          <td>{r.newTeamName || '-'}</td>
                          <td className="small">
                            {r.reason}
                            {r.evidenceNote && <div className="text-muted">{r.evidenceNote}</div>}
                          </td>
                          <td className="small">{r.changedByName || '-'}</td>
                          <td className="small text-muted">{r.changedAt ? new Date(r.changedAt).toLocaleString() : '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                )}
              </Card.Body>
            </Card>
          )}
        </Col>
      </Row>

      {/* Modal thu hồi / chuyển giải thưởng */}
      <Modal show={showRevise} onHide={() => setShowRevise(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>{reviseMode === 'revoke' ? 'Revoke Award' : 'Reassign Award'}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {reviseMode === 'revoke' ? (
            <p className="text-muted">
              This removes the award from <strong>{currentTeamName || 'the current team'}</strong>. The history is kept for audit.
            </p>
          ) : (
            <Form.Group className="mb-3">
              <Form.Label className="fw-medium">New Winning Team</Form.Label>
              <Form.Select
                value={reviseForm.newTeamId}
                onChange={(e) => setReviseForm((p) => ({ ...p, newTeamId: e.target.value }))}
              >
                <option value="">Select team</option>
                {teams.filter((t) => String(t.id) !== String(award.teamId)).map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </Form.Select>
            </Form.Group>
          )}
          <Form.Group className="mb-3">
            <Form.Label className="fw-medium">Reason</Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              placeholder="Why is this award being changed?"
              value={reviseForm.reason}
              onChange={(e) => setReviseForm((p) => ({ ...p, reason: e.target.value }))}
            />
          </Form.Group>
          <Form.Group>
            <Form.Label className="fw-medium">Evidence Note (optional)</Form.Label>
            <Form.Control
              type="text"
              placeholder="Link or note supporting this decision"
              value={reviseForm.evidenceNote}
              onChange={(e) => setReviseForm((p) => ({ ...p, evidenceNote: e.target.value }))}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowRevise(false)} disabled={revising}>Cancel</Button>
          <Button variant={reviseMode === 'revoke' ? 'danger' : 'primary'} onClick={submitRevise} disabled={revising}>
            {revising ? 'Working...' : reviseMode === 'revoke' ? 'Revoke' : 'Reassign'}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default AwardForm;
