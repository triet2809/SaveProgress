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
        // BE bắt buộc query param eventId cho /submissions; gọi theo từng team kèm eventId của nó.
        // (Trước đây chỉ truyền teamId → BE trả 400 → .catch nuốt lỗi → danh sách rỗng.)
        const teamNameById = new Map(assigned.map((t) => [t.teamId, t.teamName]));
        // Khử trùng theo cặp (teamId, eventId).
        const teamEventPairs = [
          ...new Map(
            assigned
              .filter((t) => t.teamId && t.eventId)
              .map((t) => [`${t.teamId}:${t.eventId}`, { teamId: t.teamId, eventId: t.eventId }])
          ).values(),
        ];

        const subResults = await Promise.all(
          teamEventPairs.map((p) =>
            getSubmissions({ eventId: p.eventId, teamId: p.teamId, size: 50 }).catch(() => null),
          ),
        );
        const all = [];
        const seen = new Set();
        subResults.forEach((sr) => {
          const items = sr?.content || sr || [];
          items.forEach((s) => {
            if (seen.has(s.id)) return;
            seen.add(s.id);
            all.push({ ...s, teamNameResolved: teamNameById.get(s.teamId) || s.teamName });
          });
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

  // Chỉ mở link file nếu là URL http(s) ngoài, KHÔNG trỏ về chính app.
  // (submission giả hay để trống repoUrl = URL app → mở ra tab route app → redirect theo role → nhảy judge dashboard.)
  const safeFileUrl = (submission) => {
    const raw = submission.repoUrl || submission.demoUrl || submission.slideUrl || submission.reportUrl || '';
    if (!raw) return '';
    try {
      const u = new URL(raw, window.location.origin);
      if (u.protocol !== 'http:' && u.protocol !== 'https:') return '';
      if (u.origin === window.location.origin) return ''; // link nội bộ app → không phải file thật
      return u.href;
    } catch {
      return '';
    }
  };

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
                    {(() => {
                      const fileUrl = safeFileUrl(submission);
                      return (
                        <Button
                          variant="outline-secondary"
                          className={styles.viewBtn}
                          as={fileUrl ? 'a' : 'button'}
                          href={fileUrl || undefined}
                          target={fileUrl ? '_blank' : undefined}
                          rel={fileUrl ? 'noopener noreferrer' : undefined}
                          disabled={!fileUrl}
                          title={fileUrl ? fileUrl : 'No external file link submitted'}
                        >
                          View Files
                        </Button>
                      );
                    })()}
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
