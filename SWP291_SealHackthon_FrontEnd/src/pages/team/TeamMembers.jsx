import { useEffect, useState } from 'react';
import { Card, Table, Spinner, Alert } from 'react-bootstrap';
import { getMyTeams } from '../../api/hackathonApi';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';
import styles from './TeamMembers.module.css';

const getInitials = (name = '') =>
  name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase() || 'U';

const TeamMembers = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [team, setTeam] = useState(null);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const teams = await getMyTeams();
        const list = Array.isArray(teams) ? teams : teams?.content || [];
        if (active) setTeam(list[0] || null);
      } catch (e) {
        if (active) setError(e.message || 'Failed to load team members');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  if (loading) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (error) {
    return <Alert variant="danger" className="my-3">{error}</Alert>;
  }

  const members = team?.members || [];

  return (
    <div className="py-2">
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Team Members</h1>
        <div className={styles.pageSubtitle}>
          {members.length} members{team ? ` · ${team.name}` : ''}
        </div>
        <TeamRecognitionBadge recognitions={team?.recognitions} className="mt-2" />
      </div>

      <Card className={styles.tableCard}>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className={`border-top-0 ${styles.tableHeader}`}>Member</th>
                <th className={`border-top-0 ${styles.tableHeader}`}>Role</th>
                <th className={`border-top-0 ${styles.tableHeader}`}>Email</th>
                <th className={`border-top-0 ${styles.tableHeader}`}>Joined</th>
              </tr>
            </thead>
            <tbody>
              {members.length === 0 && (
                <tr>
                  <td colSpan={4} className={`${styles.tableCell} text-center text-muted`}>
                    No members found.
                  </td>
                </tr>
              )}
              {members.map((member) => (
                <tr key={member.id} className={styles.tableRow}>
                  <td className={styles.tableCell}>
                    <div className={styles.memberCell}>
                      <div className={styles.memberAvatar}>
                        {getInitials(member.fullName || member.email)}
                      </div>
                      <span className={styles.memberName}>{member.fullName || member.email}</span>
                    </div>
                  </td>
                  <td className={styles.tableCell}>{member.role}</td>
                  <td className={styles.tableCell}>{member.email}</td>
                  <td className={styles.tableCell}>
                    {member.joinedAt ? new Date(member.joinedAt).toLocaleDateString() : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>
  );
};

export default TeamMembers;
