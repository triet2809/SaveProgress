import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Spinner, Alert } from 'react-bootstrap';
import { ExternalLink } from 'lucide-react';
import { getMyTeams, getSubmissions } from '../../api/hackathonApi';
import styles from './SubmissionHistory.module.css';

const SubmissionHistory = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [teamName, setTeamName] = useState('');
  const [submissions, setSubmissions] = useState([]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : teams?.content || [];
        const current = list[0] || null;
        if (!active) return;
        setTeamName(current?.name || '');
        if (!current?.id) {
          setLoading(false);
          return;
        }
        const subsRes = await getSubmissions({ teamId: current.id });
        const subs = subsRes?.content || subsRes || [];
        if (active) {
          setSubmissions(
            subs
              .slice()
              .sort((a, b) => new Date(b.submittedAt || 0) - new Date(a.submittedAt || 0))
          );
        }
      } catch (e) {
        if (active) setError(e.message || 'Failed to load submission history');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const firstLink = (sub) => sub.repoUrl || sub.demoUrl || sub.slideUrl || sub.reportUrl || null;

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
        <h1 className={styles.pageTitle}>Submission History</h1>
        <div className={styles.pageSubtitle}>
          All submissions for {teamName || 'your team'}
        </div>
      </div>

      {error && <Alert variant="danger" className="mb-3">{error}</Alert>}

      <Card className={styles.tableCard}>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className={`border-top-0 ${styles.tableHeader}`}>Round</th>
                <th className={`border-top-0 ${styles.tableHeader}`}>Team</th>
                <th className={`border-top-0 ${styles.tableHeader}`}>Submitted</th>
                <th className={`border-top-0 ${styles.tableHeader}`}>Updated</th>
                <th className={`border-top-0 ${styles.tableHeader}`}>Links</th>
                <th className={`border-top-0 ${styles.tableHeader}`}></th>
              </tr>
            </thead>
            <tbody>
              {submissions.length === 0 && (
                <tr className={styles.tableRow}>
                  <td className={`${styles.tableCell} ${styles.tableCellSecondary}`} colSpan={6}>
                    No submissions yet.
                  </td>
                </tr>
              )}
              {submissions.map((submission) => {
                const link = firstLink(submission);
                const linkCount = [submission.repoUrl, submission.demoUrl, submission.slideUrl, submission.reportUrl].filter(Boolean).length;
                return (
                  <tr key={submission.id} className={styles.tableRow}>
                    <td className={styles.tableCell}>
                      <span className={styles.versionBadge}>{submission.roundId ? submission.roundId.slice(0, 8) : '—'}</span>
                    </td>
                    <td className={styles.tableCell}>
                      <span className={styles.titleText}>{submission.teamName || teamName}</span>
                    </td>
                    <td className={`${styles.tableCell} ${styles.tableCellSecondary}`}>
                      {submission.submittedAt ? new Date(submission.submittedAt).toLocaleString() : '—'}
                    </td>
                    <td className={`${styles.tableCell} ${styles.tableCellSecondary}`}>
                      {submission.updatedAt ? new Date(submission.updatedAt).toLocaleString() : '—'}
                    </td>
                    <td className={styles.tableCell}>
                      <span className={styles.statusBadge}>{linkCount} link(s)</span>
                    </td>
                    <td className={styles.tableCell}>
                      <div className="d-flex align-items-center gap-2">
                        {link ? (
                          <a className={styles.downloadBtn} href={link} target="_blank" rel="noreferrer">
                            <ExternalLink size={16} /> Open
                          </a>
                        ) : (
                          <span className="text-muted small">No links</span>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  );
};

export default SubmissionHistory;
