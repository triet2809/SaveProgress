import { useEffect, useState } from 'react';
import { Card, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { Shield, Tag } from 'lucide-react';
import { getMentorTeams, getSubmissions } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';
import styles from './AssignedCategories.module.css';

const AssignedCategories = () => {
  const user = getStoredUser();
  const mentorId = user?.id;
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    async function load() {
      if (!mentorId) {
        setError('No mentor session found.');
        setLoading(false);
        return;
      }
      try {
        const teamsRes = await getMentorTeams(mentorId);
        const teams = Array.isArray(teamsRes) ? teamsRes : teamsRes?.content || [];

        // group teams by track
        const byTrack = new Map();
        teams.forEach((t) => {
          if (!t.trackId) return;
          if (!byTrack.has(t.trackId)) {
            byTrack.set(t.trackId, {
              id: t.trackId,
              name: t.trackName || 'Track',
              teamIds: new Set(),
            });
          }
          byTrack.get(t.trackId).teamIds.add(t.teamId);
        });

        // fetch submissions per track to compute submission rate
        const trackList = Array.from(byTrack.values());
        const subResults = await Promise.all(
          trackList.map((tr) =>
            getSubmissions({ eventId: tr.eventId, trackId: tr.id, size: 200 }).catch(() => null),
          ),
        );

        const built = trackList.map((tr, idx) => {
          const totalTeams = tr.teamIds.size;
          const subRes = subResults[idx];
          const subs = subRes?.content || subRes || [];
          const submittedTeamIds = new Set(subs.map((s) => s.teamId));
          const submittedTeams = [...tr.teamIds].filter((id) => submittedTeamIds.has(id)).length;
          return {
            id: tr.id,
            name: tr.name,
            description: 'Teams and submissions under your mentorship for this track.',
            totalTeams,
            submittedTeams,
            icon: idx % 2 === 0 ? 'Shield' : 'Tag',
            color: idx % 2 === 0 ? 'blue' : 'purple',
          };
        });

        if (active) setCategories(built);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load categories');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, [mentorId]);

  const renderIcon = (iconName, color) => {
    const iconClass = color === 'blue' ? styles.iconBlue : styles.iconPurple;
    const IconComponent = iconName === 'Shield' ? Shield : Tag;

    return (
      <div className={`${styles.iconWrapper} ${iconClass}`}>
        <IconComponent size={24} />
      </div>
    );
  };

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Assigned Categories</h1>
        <div className={styles.pageSubtitle}>
          {categories.length} active categor{categories.length === 1 ? 'y' : 'ies'} under your mentorship
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      ) : (
        <Row>
          {categories.length === 0 && !error && (
            <Col>
              <div className="text-muted">No categories assigned yet.</div>
            </Col>
          )}
          {categories.map((category) => {
            const progressPercentage = category.totalTeams
              ? Math.round((category.submittedTeams / category.totalTeams) * 100)
              : 0;

            return (
              <Col md={6} key={category.id} className="mb-4">
                <Card className={styles.categoryCard}>
                  <Card.Body className="p-4 d-flex flex-column">

                    <div className={styles.cardHeader}>
                      {renderIcon(category.icon, category.color)}
                      <div>
                        <h3 className={styles.categoryName}>{category.name}</h3>
                        <p className={styles.categoryDesc}>{category.description}</p>
                      </div>
                    </div>

                    <div className={styles.statsGrid}>
                      <div className={styles.statBox}>
                        <span className={styles.statValue}>{category.totalTeams}</span>
                        <span className={styles.statLabel}>Total Teams</span>
                      </div>
                      <div className={styles.statBox}>
                        <span className={styles.statValue}>{category.submittedTeams}</span>
                        <span className={styles.statLabel}>Submitted</span>
                      </div>
                    </div>

                    <div className={styles.progressSection}>
                      <div className={styles.progressBarContainer}>
                        <div
                          className={`${styles.progressBarFill} ${category.color === 'blue' ? styles.fillBlue : styles.fillPurple}`}
                          style={{ width: `${progressPercentage}%` }}
                        ></div>
                      </div>
                      <p className={styles.progressLabel}>{progressPercentage}% submission rate</p>
                    </div>

                  </Card.Body>
                </Card>
              </Col>
            );
          })}
        </Row>
      )}
    </div>
  );
};

export default AssignedCategories;
