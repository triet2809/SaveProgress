import React, { useEffect, useState } from 'react';
import { Card, Button, Badge, Spinner, Alert, Table } from 'react-bootstrap';
import { Check, X, Inbox, Users } from 'lucide-react';
import { getMyTeams, getTeamJoinRequests, acceptJoinRequest, rejectJoinRequest, markAllNotificationsRead } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';

const JoinRequests = () => {
  const currentUser = getStoredUser() || {};
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [team, setTeam] = useState(null);
  const [isLeader, setIsLeader] = useState(false);
  const [requests, setRequests] = useState([]);
  const [acting, setActing] = useState({}); // { [id]: 'accept'|'reject' }

  const loadRequests = async (teamId) => {
    const res = await getTeamJoinRequests(teamId, 'pending');
    const list = Array.isArray(res) ? res : (res?.content || []);
    setRequests(list);
  };

  useEffect(() => {
    markAllNotificationsRead('join_requests').catch(() => {});
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : (teams?.content || []);
        const t = list[0] || null;
        if (!active) return;
        setTeam(t);
        if (t) {
          const leader = (t.members || []).some(
            (m) => m.userId === currentUser.id && String(m.role).toLowerCase() === 'leader'
          );
          setIsLeader(leader);
          if (leader) await loadRequests(t.id);
        }
      } catch (e) {
        if (active) setError(e.message || 'Failed to load join requests');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleAct = async (req, kind) => {
    setActing((prev) => ({ ...prev, [req.id]: kind }));
    setError('');
    try {
      if (kind === 'accept') await acceptJoinRequest(req.id);
      else await rejectJoinRequest(req.id);
      // Refresh list + team (member count changes on accept).
      const teams = await getMyTeams();
      const list = Array.isArray(teams) ? teams : (teams?.content || []);
      setTeam(list[0] || team);
      await loadRequests(team.id);
    } catch (e) {
      setError(e.message || 'Action failed');
    } finally {
      setActing((prev) => {
        const next = { ...prev };
        delete next[req.id];
        return next;
      });
    }
  };

  if (loading) {
    return <div className="py-5 text-center"><Spinner animation="border" variant="primary" /></div>;
  }

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Join Requests</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            {team ? `Team ${team.name} · ${(team.members || []).length}/5 members` : 'You are not in a team yet.'}
          </div>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      {!team && (
        <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)' }}>
          <Card.Body className="p-5 text-center text-muted">
            <Users size={48} className="mb-3 opacity-50" />
            <h5>You are not part of any team.</h5>
          </Card.Body>
        </Card>
      )}

      {team && !isLeader && (
        <Alert variant="info">Only the team leader can approve join requests.</Alert>
      )}

      {team && isLeader && (
        <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <Card.Body className="p-0">
            {requests.length === 0 ? (
              <div className="p-5 text-center text-muted">
                <Inbox size={48} className="mb-3 opacity-50" />
                <h5>No pending requests.</h5>
              </div>
            ) : (
              <div className="table-responsive">
                <Table className="mb-0" hover>
                  <thead>
                    <tr>
                      <th className="border-top-0">Sender</th>
                      <th className="border-top-0">Email</th>
                      <th className="border-top-0">Message</th>
                      <th className="border-top-0 text-end">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {requests.map((req) => {
                      const busy = acting[req.id];
                      return (
                        <tr key={req.id}>
                          <td className="align-middle fw-medium">{req.userFullName || '—'}</td>
                          <td className="align-middle text-muted">{req.userEmail}</td>
                          <td className="align-middle text-muted">{req.message || <span className="fst-italic opacity-50">(none)</span>}</td>
                          <td className="align-middle text-end">
                            <div className="d-inline-flex gap-2">
                              <Button
                                size="sm"
                                variant="success"
                                className="d-flex align-items-center gap-1"
                                disabled={!!busy}
                                onClick={() => handleAct(req, 'accept')}
                              >
                                {busy === 'accept' ? <Spinner animation="border" size="sm" /> : <Check size={16} />} Approve
                              </Button>
                              <Button
                                size="sm"
                                variant="outline-danger"
                                className="d-flex align-items-center gap-1"
                                disabled={!!busy}
                                onClick={() => handleAct(req, 'reject')}
                              >
                                {busy === 'reject' ? <Spinner animation="border" size="sm" /> : <X size={16} />} Reject
                              </Button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </Table>
              </div>
            )}
          </Card.Body>
        </Card>
      )}
    </div>
  );
};

export default JoinRequests;
