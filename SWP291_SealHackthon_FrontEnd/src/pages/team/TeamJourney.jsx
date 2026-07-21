import { useEffect, useState, useCallback } from 'react';
import { Card, Spinner, Alert, Badge, Button, Modal, Form, Row, Col } from 'react-bootstrap';
import { Route as RouteIcon, MessageSquareWarning, Clock } from 'lucide-react';
import {
  getMyTeams, getMyTeamTimeline, getRounds,
  getTeamAppeals, createAppeal,
} from '../../api/hackathonApi';
import TeamTimeline from '../../components/timeline/TeamTimeline';
import { getStoredUser } from '../../utils/authUser';

const listOf = (data) => (Array.isArray(data) ? data : data?.content || []);

// Đếm ngược thời gian còn lại tới hạn chót khiếu nại. Trả về chuỗi mm:ss hoặc null nếu đã hết hạn.
function useCountdown(deadline, serverRemainingSeconds) {
  const [remaining, setRemaining] = useState(() =>
    serverRemainingSeconds != null
      ? Math.max(0, serverRemainingSeconds * 1000)
      : deadline ? Math.max(0, new Date(deadline).getTime() - Date.now()) : 0
  );
  useEffect(() => {
    const target = serverRemainingSeconds != null
      ? Date.now() + serverRemainingSeconds * 1000
      : new Date(deadline).getTime();
    const tick = () => setRemaining(deadline ? Math.max(0, target - Date.now()) : 0);
    const first = setTimeout(tick, 0);
    const t = setInterval(tick, 1000);
    return () => {
      clearTimeout(first);
      clearInterval(t);
    };
  }, [deadline, serverRemainingSeconds]);
  return remaining;
}

const fmt = (ms) => {
  const total = Math.floor(ms / 1000);
  const m = String(Math.floor(total / 60)).padStart(2, '0');
  const s = String(total % 60).padStart(2, '0');
  return `${m}:${s}`;
};

const TeamJourney = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [team, setTeam] = useState(null);
  const [events, setEvents] = useState([]);
  // Vòng thi đã công bố kết quả và còn trong cửa sổ khiếu nại 15 phút.
  const [openRounds, setOpenRounds] = useState([]);
  const [existingAppeals, setExistingAppeals] = useState([]);

  // Modal nộp khiếu nại
  const [showAppeal, setShowAppeal] = useState(false);
  const [appealRound, setAppealRound] = useState(null);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const teams = await getMyTeams();
      const current = listOf(teams)[0] || null;
      setTeam(current);
      if (!current) return;

      // Timeline của đội hiện tại.
      const tl = await getMyTeamTimeline();
      setEvents(tl || []);

      // Các khiếu nại đội đã nộp (để chặn nộp trùng + hiển thị trạng thái).
      try {
        const appeals = await getTeamAppeals(current.id);
        setExistingAppeals(appeals || []);
      } catch { /* optional */ }

      // Tìm các vòng thuộc track của đội đang mở cửa sổ khiếu nại.
      if (current.trackId) {
        try {
          const rounds = listOf(await getRounds({ trackId: current.trackId, size: 100 }));
          const open = rounds.filter(
            (r) => r.resultPublishedAt && r.appealDeadline && r.lifecycleState === 'APPEAL_WINDOW_OPEN'
          );
          setOpenRounds(open);
        } catch { /* optional */ }
      }
    } catch (e) {
      setError(e.message || 'Failed to load team journey');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const openAppealModal = (round) => {
    setAppealRound(round);
    setReason('');
    setShowAppeal(true);
  };

  const submitAppeal = async () => {
    if (!reason.trim()) { alert('Please provide a reason for the appeal.'); return; }
    try {
      setSubmitting(true);
      setError('');
      // Backend luôn kiểm tra lại deadline; nếu quá hạn sẽ trả lỗi và ta hiển thị cho user.
      await createAppeal({ roundId: appealRound.id, reason });
      setShowAppeal(false);
      await load();
    } catch (e) {
      setError(e.message || 'Failed to submit appeal');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="py-5 text-center"><Spinner animation="border" variant="primary" /></div>;
  }

  if (!team) {
    return <Alert variant="info">You are not part of a team yet. Join or create a team to see your journey.</Alert>;
  }

  // Đội đã có khiếu nại PENDING cho vòng nào thì ẩn nút nộp mới cho vòng đó.
  const hasPendingForRound = (roundId) =>
    existingAppeals.some((a) => String(a.roundId) === String(roundId) && a.status === 'PENDING');
  const userId = getStoredUser()?.id;
  const isLeader = team.members?.some((m) => String(m.userId) === String(userId) && String(m.role).toLowerCase() === 'leader');

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-2 mb-4">
        <RouteIcon size={24} className="text-primary" />
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Team Journey</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            {team.name} — your milestones from creation to results
          </div>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      {/* Cửa sổ khiếu nại đang mở: hiển thị đếm ngược + nút nộp */}
      {openRounds.length > 0 && (
        <Card className="mb-4 border-warning" style={{ borderRadius: 'var(--cf-radius-lg)' }}>
          <Card.Header className="bg-transparent border-bottom p-3">
            <h6 className="fw-bold mb-0 d-flex align-items-center gap-2">
              <Clock size={18} className="text-warning" /> Appeal Window Open
            </h6>
          </Card.Header>
          <Card.Body>
            {openRounds.map((r) => (
              <AppealRow
                key={r.id}
                round={r}
                pending={hasPendingForRound(r.id)}
                leader={isLeader}
                onAppeal={() => openAppealModal(r)}
              />
            ))}
          </Card.Body>
        </Card>
      )}

      {/* Danh sách khiếu nại đã nộp và trạng thái */}
      {existingAppeals.length > 0 && (
        <Card className="mb-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <Card.Header className="bg-transparent border-bottom p-3">
            <h6 className="fw-bold mb-0">My Appeals</h6>
          </Card.Header>
          <Card.Body className="d-flex flex-column gap-2">
            {existingAppeals.map((a) => (
              <div key={a.id} className="d-flex align-items-center gap-2 flex-wrap">
                <Badge bg={a.status === 'PENDING' ? 'warning' : a.status === 'ACCEPTED' ? 'success' : 'danger'}>
                  {a.status}
                </Badge>
                <span className="small">{a.roundName}</span>
                <span className="text-muted small">Version {a.resultVersion}</span>
                <span className="text-muted small">— {a.reason}</span>
                {a.response && <span className="text-muted small">· Response: {a.response}</span>}
              </div>
            ))}
          </Card.Body>
        </Card>
      )}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Card.Header className="bg-transparent border-bottom p-4">
          <h5 className="fw-bold mb-0">Timeline</h5>
        </Card.Header>
        <Card.Body className="p-4">
          <TeamTimeline items={events} />
        </Card.Body>
      </Card>

      {/* Modal nộp khiếu nại */}
      <Modal show={showAppeal} onHide={() => setShowAppeal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="d-flex align-items-center gap-2">
            <MessageSquareWarning size={20} className="text-warning" /> Submit Appeal
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="text-muted small">
            Round: <strong>{appealRound?.name}</strong>. Appeals are only accepted before the deadline.
            The system validates the deadline on the server.
          </p>
          <Form.Group>
            <Form.Label className="fw-medium">Reason</Form.Label>
            <Form.Control
              as="textarea"
              rows={4}
              placeholder="Explain why you are appealing the result"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowAppeal(false)} disabled={submitting}>Cancel</Button>
          <Button variant="warning" onClick={submitAppeal} disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit Appeal'}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

// Một dòng vòng thi đang mở cửa sổ khiếu nại kèm đồng hồ đếm ngược.
const AppealRow = ({ round, pending, leader, onAppeal }) => {
  const remaining = useCountdown(round.appealDeadline, round.remainingSeconds);
  const expired = remaining <= 0;
  return (
    <Row className="align-items-center py-2 border-bottom">
      <Col xs={12} md={5}><span className="fw-semibold">{round.name}</span></Col>
      <Col xs={6} md={4}>
        <span className={`d-flex align-items-center gap-1 ${expired ? 'text-muted' : 'text-danger'}`}>
          <Clock size={14} /> {expired ? 'Window closed' : `Closes in ${fmt(remaining)}`}
        </span>
      </Col>
      <Col xs={6} md={3} className="text-end">
        {pending ? (
          <Badge bg="warning">Appeal pending</Badge>
        ) : (
          <Button size="sm" variant="outline-warning" disabled={expired || !leader} onClick={onAppeal}>
            {leader ? 'Appeal' : 'Leader only'}
          </Button>
        )}
      </Col>
    </Row>
  );
};

export default TeamJourney;
