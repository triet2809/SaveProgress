import React, { useEffect, useState } from 'react';
import { Card, Spinner, Alert } from 'react-bootstrap';
import { getMyTeams, getRounds } from '../../api/hackathonApi';
import styles from './DeadlinesSchedule.module.css';

const DeadlinesSchedule = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rounds, setRounds] = useState([]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : teams?.content || [];
        const current = list[0] || null;
        if (!current?.trackId) {
          if (active) setLoading(false);
          return;
        }
        const res = await getRounds({ trackId: current.trackId });
        const list2 = (res?.content || res || [])
          .slice()
          .sort((a, b) => (a.sequenceNumber || 0) - (b.sequenceNumber || 0));
        if (active) setRounds(list2);
      } catch (e) {
        if (active) setError(e.message || 'Failed to load schedule');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const typeFor = (round) => {
    if (!round.submissionDeadline) return 'default';
    const now = new Date();
    const dl = new Date(round.submissionDeadline);
    if (dl < now) return 'success';
    const days = (dl - now) / (1000 * 60 * 60 * 24);
    if (days <= 3) return 'danger';
    return 'active';
  };

  const getIndicatorClass = (type) => {
    switch (type) {
      case 'active': return styles.indicatorActive;
      case 'danger': return styles.indicatorDanger;
      case 'success': return styles.indicatorSuccess;
      default: return '';
    }
  };

  const getBadgeClass = (type) => {
    switch (type) {
      case 'active': return styles.badgeActive;
      case 'danger': return styles.badgeDanger;
      case 'success': return styles.badgeSuccess;
      default: return '';
    }
  };

  const badgeFor = (type) => {
    switch (type) {
      case 'active': return 'Upcoming';
      case 'danger': return 'Due Soon';
      case 'success': return 'Closed';
      default: return null;
    }
  };

  if (loading) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Deadlines & Schedule</h1>
        <div className={styles.pageSubtitle}>
          Key dates for your track rounds
        </div>
      </div>

      {error && <Alert variant="danger" className="mb-3">{error}</Alert>}

      <Card className={styles.timelineCard}>
        <Card.Body>
          <div className={styles.timeline}>
            {rounds.length === 0 && !error && (
              <div className="text-muted small">No rounds scheduled yet.</div>
            )}
            {rounds.map((round, idx) => {
              const type = typeFor(round);
              const dl = round.submissionDeadline ? new Date(round.submissionDeadline) : null;
              const dateStr = dl ? dl.toLocaleDateString() : 'TBD';
              const dayStr = dl ? dl.toLocaleDateString(undefined, { weekday: 'long' }) : '';
              const timeStr = dl ? dl.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
              return (
                <div key={round.id || idx} className={styles.timelineItem}>
                  <div className={`${styles.timelineIndicator} ${getIndicatorClass(type)}`}>
                    {round.sequenceNumber || idx + 1}
                  </div>
                  <div className={styles.timelineContent}>
                    <div className={styles.timelineHeader}>
                      <span className={styles.timelineDate}>{dateStr}</span>
                      {dayStr && <span className={styles.timelineDay}>· {dayStr}</span>}
                      {badgeFor(type) && (
                        <span className={`${styles.statusBadge} ${getBadgeClass(type)}`}>
                          {badgeFor(type)}
                        </span>
                      )}
                    </div>
                    <ul className={styles.eventList}>
                      <li className={styles.eventItem}>
                        {round.name} — Submission deadline {timeStr}
                      </li>
                      {round.topNToPromote ? (
                        <li className={styles.eventItem}>
                          Top {round.topNToPromote} team(s) promoted to next round
                        </li>
                      ) : null}
                    </ul>
                  </div>
                </div>
              );
            })}
          </div>
        </Card.Body>
      </Card>
    </div>
  );
};

export default DeadlinesSchedule;
