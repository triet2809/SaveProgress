import { useState, useEffect, useCallback } from 'react';
import { Card, Table, Badge, Form, InputGroup, Spinner, Alert, Button, Modal } from 'react-bootstrap';
import { Search, BarChart2, Sparkles, RefreshCw, MessageSquare, Send } from 'lucide-react';
import { getRounds, getJudgeVariance, getTracks, getVarianceAnalysis, chatVariance } from '../../api/hackathonApi';
import EventSelector from '../../components/coordinator/EventSelector';
import { useSearchParams } from 'react-router-dom';

const HIGH_VARIANCE = 10; // Ngưỡng variance cao để đánh dấu cần xem lại

const TENDENCY_BADGE = {
  LENIENT: { bg: 'info', label: 'Lenient' },
  HARSH: { bg: 'dark', label: 'Harsh' },
  INCONSISTENT: { bg: 'warning', label: 'Inconsistent' },
  BALANCED: { bg: 'success', label: 'Balanced' },
};

// Bong bóng chat: user nằm phải, AI nằm trái.
const ChatBubble = ({ role, content }) => (
  <div className={`d-flex mb-2 ${role === 'user' ? 'justify-content-end' : 'justify-content-start'}`}>
    <div className={`p-2 rounded-3 ${role === 'user' ? 'bg-primary text-white' : 'bg-light text-dark'}`} style={{ maxWidth: '85%', whiteSpace: 'pre-wrap' }}>
      {content}
    </div>
  </div>
);

// Panel AI phân tích variance: FE chỉ hiển thị số liệu và câu trả lời BE trả về.
const AiAnalysisPanel = ({ ai, loading, error, onRefresh }) => {
  const stats = ai?.stats;
  const narrative = ai?.ai;

  return (
    <Card className="mb-4" style={{ border: '1px solid var(--cf-border, #e5e7eb)', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)' }}>
      <div className="p-3 border-bottom d-flex align-items-center justify-content-between">
        <div className="d-flex align-items-center gap-2 fw-bold" style={{ color: 'var(--cf-text-primary)' }}>
          <Sparkles size={18} /> AI Variance Analysis
        </div>
        <Button variant="outline-secondary" size="sm" className="d-flex align-items-center gap-2"
          disabled={loading} onClick={onRefresh}>
          <RefreshCw size={14} /> Refresh
        </Button>
      </div>

      <div className="p-3">
        {loading && (
          <div className="text-center py-4"><Spinner animation="border" /> <span className="ms-2">Analyzing...</span></div>
        )}

        {!loading && error && (
          <Alert variant="warning" className="mb-3">
            AI unavailable: {error}. Showing code-generated statistics below.
          </Alert>
        )}

        {!loading && stats && (
          <>
            <div className="d-flex flex-wrap gap-3 mb-3">
              <Badge bg="secondary" className="px-3 py-2">Groups: {stats.groupCount}</Badge>
              <Badge bg="danger" className="px-3 py-2">High variance: {stats.highVarianceCount}</Badge>
              <Badge bg="info" className="px-3 py-2">Average variance: {Number(stats.avgVariance ?? 0).toFixed(2)}</Badge>
            </div>

            {narrative?.summary && (
              <div className="mb-3">
                <div className="fw-bold mb-1">Overview</div>
                <div style={{ color: 'var(--cf-text-secondary)' }}>{narrative.summary}</div>
              </div>
            )}

            {narrative?.recommendations?.length > 0 && (
              <div className="mb-3">
                <div className="fw-bold mb-1">Recommendations</div>
                <ul className="mb-0">
                  {narrative.recommendations.map((r, i) => <li key={i}>{r}</li>)}
                </ul>
              </div>
            )}

            {narrative?.judgeNotes?.length > 0 && (
              <div className="mb-3">
                <div className="fw-bold mb-1">Judge Notes</div>
                <ul className="mb-0">
                  {narrative.judgeNotes.map((n, i) => <li key={i}>{n}</li>)}
                </ul>
              </div>
            )}

            {stats.judgeBiases?.length > 0 && (
              <div className="mb-2">
                <div className="fw-bold mb-2">Judge Scoring Tendencies</div>
                <div className="d-flex flex-wrap gap-2">
                  {stats.judgeBiases.map((b) => {
                    const t = TENDENCY_BADGE[b.tendency] || { bg: 'secondary', label: b.tendency };
                    return (
                      <Badge key={b.judgeId} bg={t.bg} className="px-2 py-2">
                        {b.judgeName}: {t.label} ({Number(b.avgDeviation ?? 0) > 0 ? '+' : ''}{Number(b.avgDeviation ?? 0).toFixed(1)})
                      </Badge>
                    );
                  })}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </Card>
  );
};

const ScoringAnalytics = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const selectedRound = searchParams.get('roundId') || '';
  const trackId = searchParams.get('trackId') || '';
  const [searchTerm, setSearchTerm] = useState('');
  const [rounds, setRounds] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [ai, setAi] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState('');

  const [showChat, setShowChat] = useState(false);
  const [chatMessages, setChatMessages] = useState([]);
  const [chatInput, setChatInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [chatError, setChatError] = useState('');

  // Đổi event / round / track thì reset state AI để tránh giữ dữ liệu cũ.
  useEffect(() => {
    if (!eventId) {
      setRounds([]);
      setTracks([]);
      setRows([]);
      setLoading(false);
      return;
    }
    (async () => {
      setLoading(true);
      try {
        const [data, trackData] = await Promise.all([
          getRounds({ eventId, size: 100 }), getTracks({ eventId, size: 100 }),
        ]);
        const list = data.content || data || [];
        setRounds(list);
        setTracks(trackData.content || trackData || []);
        if (list.length && !selectedRound) {
          const next = new URLSearchParams(searchParams);
          next.set('roundId', list[0].id);
          setSearchParams(next);
        }
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [eventId, selectedRound, searchParams, setSearchParams]);

  // Tải số liệu variance từ BE theo event / round / track đang chọn.
  const loadVariance = useCallback(async (roundId) => {
    if (!roundId || !eventId) return;
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
    if (selectedRound && eventId) loadVariance(selectedRound);
  }, [selectedRound, eventId, loadVariance]);

  useEffect(() => {
    setAi(null);
    setAiError('');
    setChatMessages([]);
    setChatInput('');
    setChatError('');
  }, [selectedRound, trackId]);

  // Gọi BE sinh phân tích AI cho round hiện tại.
  const runAiAnalysis = useCallback(async ({ refresh = false } = {}) => {
    if (!selectedRound) return;
    setAiLoading(true);
    setAiError('');
    try {
      const data = await getVarianceAnalysis(selectedRound, { eventId, trackId, refresh });
      setAi(data);
      if (data && data.aiAvailable === false && data.aiError) {
        setAiError(data.aiError);
      }
    } catch (err) {
      setAiError(err.message);
    } finally {
      setAiLoading(false);
    }
  }, [selectedRound, eventId, trackId]);

  // Gửi câu hỏi chat sang BE, rồi nối câu trả lời AI vào modal.
  const sendChat = useCallback(async () => {
    if (!selectedRound || !eventId || !chatInput.trim()) return;
    const nextMessages = [...chatMessages, { role: 'user', content: chatInput.trim() }];
    setChatMessages(nextMessages);
    setChatInput('');
    setChatLoading(true);
    setChatError('');
    try {
      const data = await chatVariance(selectedRound, { eventId, trackId, messages: nextMessages });
      setChatMessages([...nextMessages, { role: 'assistant', content: data.reply || 'No reply' }]);
      if (data && data.aiAvailable === false && data.aiError) setChatError(data.aiError);
    } catch (err) {
      setChatError(err.message);
    } finally {
      setChatLoading(false);
    }
  }, [chatInput, chatMessages, eventId, selectedRound, trackId]);

  const filteredData = rows.filter((item) =>
    (item.teamName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (item.criterionName || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="py-2">
      <EventSelector />

      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Scoring Analytics</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Review team performance, average scores, and judge variances</div>
        </div>
        <div className="d-flex gap-2">
          <Badge bg="warning" text="dark" className="px-3 py-2 d-flex align-items-center gap-2">
            <BarChart2 size={16} /> Needs Review (High Variance)
          </Badge>
          <Button
            variant="outline-primary"
            className="d-flex align-items-center gap-2"
            disabled={!selectedRound || !eventId}
            onClick={() => runAiAnalysis({ refresh: false })}
          >
            {aiLoading ? <Spinner animation="border" size="sm" /> : <Sparkles size={16} />}
            AI Analysis
          </Button>
          <Button
            variant="primary"
            className="d-flex align-items-center gap-2"
            disabled={!selectedRound || !eventId}
            onClick={() => setShowChat(true)}
          >
            <MessageSquare size={16} />
            AI Chat
          </Button>
        </div>
      </div>

      {/* Chưa chọn event thì chưa thể load analytics. */}
      {!eventId && <Alert variant="info">Choose event in selector above. Then Scoring Analytics loads.</Alert>}
      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      {/* Chỉ hiện panel AI khi đã có kết quả AI, đang loading, hoặc có lỗi AI. */}
      {(ai || aiLoading || aiError) && (
        <AiAnalysisPanel
          ai={ai}
          loading={aiLoading}
          error={aiError}
          onRefresh={() => runAiAnalysis({ refresh: true })}
        />
      )}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between gap-2 flex-wrap">
          {/* Bộ lọc FE: search text + chọn round + chọn track. */}
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
          {/* Dropdown round đổi query param để đồng bộ URL và state màn hình. */}
          <Form.Select style={{ maxWidth: '260px' }} value={selectedRound} onChange={(e) => {
            const next = new URLSearchParams(searchParams); next.set('roundId', e.target.value); next.delete('trackId'); setSearchParams(next);
          }} disabled={!eventId}>
            {rounds.length === 0 && <option value="">No rounds available</option>}
            {rounds.map((r) => (
              <option key={r.id} value={r.id}>{r.name}</option>
            ))}
          </Form.Select>
          {/* Dropdown track lọc thêm trên dữ liệu variance của round đã chọn. */}
          <Form.Select style={{ maxWidth: 220 }} value={trackId} onChange={(e) => {
            const next = new URLSearchParams(searchParams);
            if (e.target.value) next.set('trackId', e.target.value); else next.delete('trackId');
            setSearchParams(next);
          }} disabled={!eventId}>
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
              {/* Bảng dưới chỉ render dữ liệu variance đã được FE lọc theo searchTerm. */}
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
                  // Mỗi dòng là 1 tổ hợp team + criterion, trạng thái cảnh báo dựa trên HIGH_VARIANCE.
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

      {/* Modal chat AI: giữ hội thoại theo state FE rồi gửi toàn bộ messages sang BE mỗi lượt. */}
      <Modal show={showChat} onHide={() => setShowChat(false)} size="lg" centered>
        <Modal.Header closeButton>
              <Modal.Title className="d-flex align-items-center gap-2"><MessageSquare size={18} /> AI Variance Chat</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="small text-muted mb-3">
            Round: {selectedRound || 'not selected'} {trackId ? `• Track: ${trackId}` : ''}
          </div>
          <div style={{ maxHeight: '50vh', overflowY: 'auto' }} className="mb-3 border rounded p-2 bg-white">
            {chatMessages.length === 0 && <div className="text-muted text-center py-4">Ask about hotspots, high variance, judge bias, or recommended actions.</div>}
            {chatMessages.map((m, i) => <ChatBubble key={i} role={m.role} content={m.content} />)}
            {chatLoading && <div className="text-muted small">AI is replying...</div>}
          </div>
          {chatError && <Alert variant="warning">{chatError}</Alert>}
          <InputGroup>
            <Form.Control
              placeholder="Example: Why is variance so high in this round?"
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') sendChat(); }}
            />
            <Button onClick={sendChat} disabled={chatLoading || !chatInput.trim() || !selectedRound || !eventId}>
              <Send size={16} />
            </Button>
          </InputGroup>
        </Modal.Body>
      </Modal>
    </div>
  );
};

export default ScoringAnalytics;
