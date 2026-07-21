import { useEffect, useState } from 'react';
import { Card, Button, Spinner, Alert } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle } from 'lucide-react';
import { getMentorTeams, getSubmissions, markAllNotificationsRead } from '../../api/hackathonApi';
import { getStoredUser } from '../../utils/authUser';
import styles from './SubmissionReview.module.css';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const SubmissionReview = () => {
  const navigate = useNavigate();
  const user = getStoredUser();
  const mentorId = user?.id;
  const [submissions, setSubmissions] = useState([]);
  const [reviewedIds, setReviewedIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    markAllNotificationsRead('submissions').catch(() => {});
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
        const teamIds = [...new Set(assigned.map((t) => t.teamId).filter(Boolean))];
        const teamNameById = new Map(assigned.map((t) => [t.teamId, t.teamName]));

        const subResults = await Promise.all(
          teamIds.map((id) => getSubmissions({ teamId: id, size: 50 }).catch(() => null)),
        );
        const all = [];
        subResults.forEach((sr) => {
          const items = sr?.content || sr || [];
          items.forEach((s) => all.push({ ...s, teamNameResolved: teamNameById.get(s.teamId) || s.teamName }));
        });
        if (active) setSubmissions(all);
      } catch (err) {
        if (active) setError(err.message || 'Failed to load submissions');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, [mentorId]);

  const getStatusClass = (isReviewed) =>
    isReviewed ? styles.statusReviewed : styles.statusPending;

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Submission Review</h1>
        <div className={styles.pageSubtitle}>
          Review and provide feedback on team submissions
        </div>
      </div>

      <div className="d-flex justify-content-end mb-3">
        <Button
          variant="outline-danger"
          size="sm"
          className="d-flex align-items-center gap-2"
          onClick={() => navigate('/mentor/incidents/create')}
        >
          <AlertTriangle size={16} /> Report Incident
        </Button>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" role="status" />
        </div>
      ) : (
        <div className={styles.submissionList}>
          {submissions.length === 0 && !error && (
            <div className="text-muted">No submissions from your teams yet.</div>
          )}
          {submissions.map((submission) => {
            const isReviewed = reviewedIds.has(submission.id);
            return (
              <Card key={submission.id} className={styles.submissionCard}>
                <Card.Body className="p-4">

                  <div className={styles.cardHeader}>
                    <div className={styles.projectInfo}>
                      <div className={styles.teamAndProject}>
                        {submission.teamNameResolved} — {submission.repoUrl ? 'Repo submitted' : 'Submission'}
                        <TeamRecognitionBadge recognitions={submission.recognitions} className="ms-2" />
                      </div>
                      <div className={styles.submissionDetails}>
                        Submitted {submission.submittedAt ? new Date(submission.submittedAt).toLocaleDateString() : '—'}
                      </div>
                    </div>

                    <div className={`${styles.statusBadge} ${getStatusClass(isReviewed)}`}>
                      {isReviewed ? 'Reviewed' : 'Pending Review'}
                    </div>
                  </div>

                  <div className={styles.actionRow}>
                    <Button
                      variant="outline-secondary"
                      className={styles.viewBtn}
                      as="a"
                      href={submission.repoUrl || submission.demoUrl || '#'}
                      target={submission.repoUrl || submission.demoUrl ? '_blank' : undefined}
                      rel="noopener noreferrer"
                    >
                      View Files
                    </Button>
                    <Button
                      className={styles.feedbackBtn}
                      onClick={() => {
                        setReviewedIds((prev) => new Set(prev).add(submission.id));
                        navigate('/mentor/feedback');
                      }}
                    >
                      Give Feedback
                    </Button>
                  </div>

                </Card.Body>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default SubmissionReview;
