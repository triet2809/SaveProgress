import { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Badge, Form, InputGroup, Spinner, Alert } from 'react-bootstrap';
import { Search, Download, Eye } from 'lucide-react';
import { getSubmissions, getRounds } from '../../api/hackathonApi';
import { useNavigate } from 'react-router-dom';
import EventSelector from '../../components/coordinator/EventSelector';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';
import { useSearchParams } from 'react-router-dom';

const asArray = (data) => data?.content || data || [];

const reviewLabel = (s) => {
  if (!s) return 'Pending Review';
  const v = String(s).toLowerCase();
  if (v === 'reviewed') return 'Reviewed';
  if (v === 'pending') return 'Pending Review';
  return s.charAt(0).toUpperCase() + s.slice(1);
};

const SubmissionManagement = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const [searchTerm, setSearchTerm] = useState('');
  const [roundFilter, setRoundFilter] = useState('All Rounds');
  const [submissions, setSubmissions] = useState([]);
  const [rounds, setRounds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const [subs, rds] = await Promise.all([
          getSubmissions({ eventId, size: 200 }),
          getRounds({ eventId, size: 200 }).catch(() => null),
        ]);
        if (!active) return;
        setSubmissions(asArray(subs));
        setRounds(asArray(rds));
      } catch (err) {
        if (active) setError(err.message || 'Failed to load submissions');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [eventId]);

  const roundName = useMemo(() => {
    const map = {};
    rounds.forEach((r) => { map[r.id] = r.name; });
    return (id) => map[id] || '—';
  }, [rounds]);

  const filteredSubmissions = submissions.filter((sub) => {
    const q = searchTerm.toLowerCase();
    const project = (sub.projectName || '').toLowerCase();
    const team = (sub.teamName || '').toLowerCase();
    const matchesSearch = project.includes(q) || team.includes(q);
    const matchesRound = roundFilter === 'All Rounds' || sub.roundId === roundFilter;
    return matchesSearch && matchesRound;
  });

  const handleExport = () => {
    const header = ['Team', 'Project', 'Version', 'Round', 'Repo', 'Demo', 'Submitted', 'Review Status'];
    const rows = filteredSubmissions.map((s) => [
      s.teamName || '',
      s.projectName || '',
      s.version || '',
      roundName(s.roundId),
      s.repoUrl || '',
      s.demoUrl || '',
      s.submittedAt || '',
      reviewLabel(s.reviewStatus),
    ]);
    const csv = [header, ...rows]
      .map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(','))
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'submissions.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <>
    <EventSelector />
    {eventId && <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Submission Management</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Review all team submissions</div>
        </div>
        <Button variant="outline-primary" className="d-flex align-items-center gap-2" onClick={handleExport} >
          <Download size={18} /> Export All
        </Button>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between">
          <InputGroup style={{ maxWidth: '300px' }}>
            <InputGroup.Text className="bg-transparent border-end-0">
              <Search size={16} />
            </InputGroup.Text>
            <Form.Control className="border-start-0" placeholder="Search projects..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
          </InputGroup>
          <div className="d-flex gap-2">
            <Form.Select style={{ width: '200px' }} value={roundFilter} onChange={(e) => setRoundFilter(e.target.value)}>
              <option value="All Rounds">All Rounds</option>
              {rounds.map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </Form.Select>
          </div>
        </div>
        <div className="table-responsive">
          <Table className="mb-0" hover>
            <thead>
              <tr>
                <th className="border-top-0 border-bottom">Team Name</th>
                <th className="border-top-0 border-bottom">Project Name</th>
                <th className="border-top-0 border-bottom">Version</th>
                <th className="border-top-0 border-bottom">Repository</th>
                <th className="border-top-0 border-bottom">Round</th>
                <th className="border-top-0 border-bottom">Submitted Date</th>
                <th className="border-top-0 border-bottom">Review Status</th>
                <th className="border-top-0 border-bottom text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={8} className="text-center py-4"><Spinner animation="border" variant="primary" size="sm" /></td></tr>
              ) : filteredSubmissions.length === 0 ? (
                <tr><td colSpan={8} className="text-center py-4 text-muted">No submissions found.</td></tr>
              ) : filteredSubmissions.map((sub) => {
                const label = reviewLabel(sub.reviewStatus);
                const reviewed = label === 'Reviewed';
                return (
                <tr key={sub.id}>
                  <td className="fw-medium py-3" style={{ color: 'var(--cf-text-primary)' }}>
                    <span className="me-2">{sub.teamName || '—'}</span>
                    <TeamRecognitionBadge recognitions={sub.recognitions} />
                  </td>
                  <td className="py-3">{sub.projectName || '—'}</td>
                  <td className="py-3">{sub.version ? <Badge bg="secondary">{sub.version}</Badge> : '—'}</td>
                  <td className="py-3">
                    {sub.repoUrl ? (
                      <a href={sub.repoUrl} target="_blank" rel="noreferrer" className="text-truncate d-inline-block" style={{ maxWidth: '220px' }}>{sub.repoUrl}</a>
                    ) : '—'}
                  </td>
                  <td className="py-3"><Badge bg="info" text="dark">{roundName(sub.roundId)}</Badge></td>
                  <td className="py-3">{sub.submittedAt ? new Date(sub.submittedAt).toLocaleString() : '—'}</td>
                  <td className="py-3">
                    <Badge bg={reviewed ? 'success' : 'warning'} text={reviewed ? 'light' : 'dark'}>
                      {label}
                    </Badge>
                  </td>
                  <td className="text-end">
                    <Button
                      variant="link"
                      size="sm"
                      className="p-0 text-primary"
                      onClick={() => navigate(`/coordinator/submissions/${sub.id}`)}
                    >
                      <Eye size={18} />
                    </Button>
                  </td>
                </tr>
                );
              })}
            </tbody>
          </Table>
        </div>
      </Card>
    </div>}
    </>
  );
};

export default SubmissionManagement;
