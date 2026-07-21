import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Spinner, Alert } from 'react-bootstrap';
import {
  getMentorTeams,
  getMentorFeedbacks,
  createMentorFeedback,
  updateMentorFeedback,
  deleteMentorFeedback,
} from '../../api/hackathonApi';
import { getStoredUser, getInitials } from '../../utils/authUser';
import styles from './FeedbackCenter.module.css';

const FeedbackCenter = () => {
  const user = getStoredUser();
  const mentorId = user?.id;
  const [teams, setTeams] = useState([]);
  const [activeTeamId, setActiveTeamId] = useState(null);
  const [feedbacks, setFeedbacks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editText, setEditText] = useState('');

  const loadFeedbacks = async () => {
    if (!mentorId) return;
    const res = await getMentorFeedbacks({ mentorId, size: 200 });
    setFeedbacks(res?.content || res || []);
  };

  useEffect(() => {
    let active = true;
    async function load() {
      if (!mentorId) {
        setError('No mentor session found.');
        setLoading(false);
        return;
      }
      try {
        const res = await getMentorTeams(mentorId);
        const assigned = Array.isArray(res) ? res : res?.content || [];
        // unique teams keep the track-mentor + round info for creating feedback
        const uniqueTeams = [];
        const seen = new Set();
        assigned.forEach((t) => {
          if (t.teamId && !seen.has(t.teamId)) {
            seen.add(t.teamId);
            uniqueTeams.push(t);
          }
        });
        if (!active) return;
        setTeams(uniqueTeams);
        setActiveTeamId(uniqueTeams[0]?.teamId || null);
        await loadFeedbacks();
      } catch (err) {
        if (active) setError(err.message || 'Failed to load feedback center');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mentorId]);

  const activeTeam = teams.find((t) => t.teamId === activeTeamId);
  const feedbackHistory = feedbacks
    .filter((f) => f.teamId === activeTeamId)
    .sort((a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0));

  const handleSend = async () => {
    if (!message.trim() || !activeTeam) return;
    setSaving(true);
    setError('');
    try {
      await createMentorFeedback({
        trackMentorId: activeTeam.trackMentorId,
        teamId: activeTeam.teamId,
        roundId: activeTeam.roundId,
        content: message.trim(),
      });
      setMessage('');
      await loadFeedbacks();
    } catch (err) {
      setError(err.message || 'Failed to send feedback');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveEdit = async (id) => {
    if (!editText.trim()) return;
    setSaving(true);
    setError('');
    try {
      await updateMentorFeedback(id, { content: editText.trim() });
      setEditingId(null);
      setEditText('');
      await loadFeedbacks();
    } catch (err) {
      setError(err.message || 'Failed to update feedback');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    setSaving(true);
    setError('');
    try {
      await deleteMentorFeedback(id);
      await loadFeedbacks();
    } catch (err) {
      setError(err.message || 'Failed to delete feedback');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Feedback Center</h1>
        <div className={styles.pageSubtitle}>
          Send and manage feedback for your teams
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      ) : (
        <Row>
          {/* Left Sidebar - Team Selection */}
          <Col md={3} className="mb-4 mb-md-0">
            <div className={styles.teamList}>
              {teams.length === 0 && (
                <div className="text-muted">No teams assigned.</div>
              )}
              {teams.map((team) => (
                <div
                  key={team.teamId}
                  className={`${styles.teamBtn} ${activeTeamId === team.teamId ? styles.activeTeamBtn : ''}`}
                  onClick={() => setActiveTeamId(team.teamId)}
                >
                  <div className={styles.teamInitials}>{getInitials(team.teamName)}</div>
                  <div className={styles.teamNameText}>{team.teamName}</div>
                </div>
              ))}
            </div>
          </Col>

          {/* Right Main Area - Feedback Thread */}
          <Col md={9}>
            <Card className={styles.feedbackMainCard}>
              <Card.Body className="p-4">
                <div className={styles.cardHeader}>
                  Feedback — {activeTeam?.teamName || 'Select a team'}
                </div>

                {/* History List */}
                <div className={styles.historyContainer}>
                  {feedbackHistory.length === 0 && (
                    <div className="text-muted">No feedback yet for this team.</div>
                  )}
                  {feedbackHistory.map((item) => (
                    <div key={item.id} className={styles.historyCard}>
                      <div className={styles.historyMeta}>
                        <span className={styles.historyAuthor}>{item.mentorEmail || user?.fullName}</span>
                        <span className={styles.historyDate}>
                          {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : ''}
                        </span>
                      </div>
                      {editingId === item.id ? (
                        <div>
                          <textarea
                            className={styles.feedbackTextarea}
                            value={editText}
                            onChange={(e) => setEditText(e.target.value)}
                          />
                          <div className="d-flex gap-2 mt-2">
                            <button
                              className={styles.sendBtn}
                              disabled={saving}
                              onClick={() => handleSaveEdit(item.id)}
                            >
                              Save
                            </button>
                            <button
                              className={styles.sendBtn}
                              style={{ background: '#6b7280' }}
                              onClick={() => { setEditingId(null); setEditText(''); }}
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <div className={styles.historyMessage}>
                            {item.content}
                          </div>
                          <div className="d-flex gap-3 mt-2">
                            <button
                              type="button"
                              className="btn btn-link p-0 text-decoration-none"
                              style={{ fontSize: '0.8rem' }}
                              onClick={() => { setEditingId(item.id); setEditText(item.content); }}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              className="btn btn-link p-0 text-decoration-none text-danger"
                              style={{ fontSize: '0.8rem' }}
                              disabled={saving}
                              onClick={() => handleDelete(item.id)}
                            >
                              Delete
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  ))}
                </div>

                {/* New Feedback Form */}
                <div className={styles.newFeedbackSection}>
                  <div className={styles.newFeedbackLabel}>
                    New Feedback for {activeTeam?.teamName || '...'}
                  </div>
                  <textarea
                    className={styles.feedbackTextarea}
                    placeholder={`Write constructive feedback for ${activeTeam?.teamName || 'the team'}...`}
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    disabled={!activeTeam}
                  />
                  <button
                    className={styles.sendBtn}
                    disabled={saving || !activeTeam || !message.trim()}
                    onClick={handleSend}
                  >
                    {saving ? 'Sending...' : 'Send Feedback'}
                  </button>
                </div>

              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
};

export default FeedbackCenter;
