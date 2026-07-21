import React, { useState, useRef, useEffect } from 'react';
import { Card, Form, InputGroup, Button, Badge, Spinner, Alert } from 'react-bootstrap';
import { Send, MoreVertical, Search, CheckCheck } from 'lucide-react';
import { getMyTeams, getTeamChatMessages, sendTeamChatMessage } from '../../api/hackathonApi';
import { getStoredUser, getInitials } from '../../utils/authUser';

const TeamChat = () => {
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const [team, setTeam] = useState(null);
  const [messages, setMessages] = useState([]);
  const messagesEndRef = useRef(null);

  const currentUser = getStoredUser();
  const currentUserId = currentUser?.id;

  const normalize = (list) =>
    (list || []).map((m) => ({
      id: m.id,
      sender: m.senderName || m.senderEmail || 'Member',
      initials: getInitials(m.senderName || m.senderEmail || 'Member'),
      time: m.createdAt ? new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '',
      text: m.message,
      isMine: !!currentUserId && m.senderId === currentUserId,
    }));

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : teams?.content || [];
        const current = list[0] || null;
        if (!active) return;
        setTeam(current);
        if (!current?.id) {
          setLoading(false);
          return;
        }
        const res = await getTeamChatMessages(current.id);
        const msgs = res?.content || res || [];
        if (active) setMessages(normalize(msgs));
      } catch (e) {
        if (active) setError(e.message || 'Failed to load team chat');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!message.trim() || !team?.id || sending) return;
    setSending(true);
    setError('');
    const text = message.trim();
    setMessage('');
    try {
      const created = await sendTeamChatMessage(team.id, text);
      if (created) {
        setMessages((prev) => [...prev, ...normalize([created])]);
      } else {
        const res = await getTeamChatMessages(team.id);
        setMessages(normalize(res?.content || res || []));
      }
    } catch (err) {
      setError(err.message || 'Failed to send message');
      setMessage(text);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="py-2 h-100 d-flex flex-column" style={{ minHeight: 'calc(100vh - 120px)' }}>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Team Chat</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Collaborate with your team members in real-time</div>
        </div>
        <div className="d-flex gap-2">
          <Badge bg="primary" pill className="d-flex align-items-center px-3 py-2">
            {(team?.members?.length || 0)} Members
          </Badge>
        </div>
      </div>

      {error && <Alert variant="danger" className="mb-3">{error}</Alert>}

      <Card className="flex-grow-1 d-flex flex-column" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', overflow: 'hidden' }}>
        {/* Chat Header */}
        <div className="p-3 border-bottom d-flex justify-content-between align-items-center" style={{ backgroundColor: 'var(--cf-bg-main)' }}>
          <div className="d-flex align-items-center gap-3">
            <div className="d-flex align-items-center justify-content-center bg-primary text-white rounded-circle fw-bold" style={{ width: '40px', height: '40px', fontSize: '1.2rem' }}>
              #
            </div>
            <div>
              <h6 className="fw-bold mb-0">General Discussion</h6>
              <small className="text-muted">{team?.name ? `${team.name} Team Space` : 'Team Space'}</small>
            </div>
          </div>
          <div className="d-flex gap-2">
            <Button variant="link" className="p-1 text-muted"><Search size={20} /></Button>
            <Button variant="link" className="p-1 text-muted"><MoreVertical size={20} /></Button>
          </div>
        </div>

        {/* Chat Messages Area */}
        <div className="flex-grow-1 p-4 overflow-auto" style={{ backgroundColor: 'var(--cf-bg-body)', maxHeight: '550px' }}>
          {loading ? (
            <div className="py-5 text-center">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : (
            <div className="d-flex flex-column gap-4">
              {messages.length === 0 && (
                <div className="text-center text-muted small">No messages yet. Start the conversation.</div>
              )}
              {messages.map((msg) => (
                <div key={msg.id} className={`d-flex ${msg.isMine ? 'justify-content-end' : 'justify-content-start'}`}>
                  <div className={`d-flex gap-2 max-w-75 ${msg.isMine ? 'flex-row-reverse' : ''}`} style={{ maxWidth: '75%' }}>
                    {/* Avatar */}
                    <div className="flex-shrink-0">
                      <div className={`d-flex align-items-center justify-content-center text-white rounded-circle fw-bold ${msg.isMine ? 'bg-primary' : 'bg-secondary'}`} style={{ width: '36px', height: '36px', fontSize: '0.85rem' }}>
                        {msg.initials}
                      </div>
                    </div>

                    {/* Message Content */}
                    <div className={`d-flex flex-column ${msg.isMine ? 'align-items-end' : 'align-items-start'}`}>
                      <div className="d-flex align-items-center gap-2 mb-1">
                        <span className="fw-medium small" style={{ color: 'var(--cf-text-primary)' }}>{msg.sender}</span>
                        <span className="text-muted" style={{ fontSize: '0.7rem' }}>{msg.time}</span>
                      </div>
                      <div className={`p-3 rounded-3 shadow-sm ${msg.isMine ? 'text-white' : ''}`} style={{ backgroundColor: msg.isMine ? 'var(--cf-primary)' : 'var(--cf-bg-surface)', border: msg.isMine ? 'none' : '1px solid var(--cf-border-color)', borderTopRightRadius: msg.isMine ? '4px' : '', borderTopLeftRadius: msg.isMine ? '' : '4px' }}>
                        <p className="mb-0" style={{ fontSize: '0.95rem' }}>{msg.text}</p>
                      </div>
                      {msg.isMine && (
                        <div className="mt-1 d-flex justify-content-end">
                          <CheckCheck size={14} className="text-primary" />
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>

        {/* Chat Input Area */}
        <div className="p-3 border-top" style={{ backgroundColor: 'var(--cf-bg-surface)' }}>
          <Form onSubmit={handleSend}>
            <InputGroup className="align-items-end">
              <Form.Control
                as="textarea"
                rows={1}
                placeholder="Type a message..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSend(e);
                  }
                }}
                disabled={!team || sending}
                style={{ resize: 'none', backgroundColor: 'var(--cf-bg-main)' }}
                className="py-3 shadow-none"
              />
              <Button type="submit" variant="primary" className="px-4 d-flex align-items-center justify-content-center rounded-end h-100" disabled={!message.trim() || !team || sending} style={{ borderTopLeftRadius: 0, borderBottomLeftRadius: 0 }}>
                <Send size={18} />
              </Button>
            </InputGroup>
            <div className="text-muted mt-2 text-end" style={{ fontSize: '0.75rem' }}>
              Press Enter to send, Shift+Enter for new line
            </div>
          </Form>
        </div>
      </Card>
    </div>
  );
};

export default TeamChat;
