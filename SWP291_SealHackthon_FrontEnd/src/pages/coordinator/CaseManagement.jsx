import { useEffect, useState } from 'react';
import { Alert, Badge, Card, Spinner, Table } from 'react-bootstrap';
import { useSearchParams } from 'react-router-dom';
import EventSelector from '../../components/coordinator/EventSelector';
import { getCases } from '../../api/hackathonApi';

export default function CaseManagement() {
  const [params] = useSearchParams(); const eventId = params.get('eventId') || '';
  const [cases, setCases] = useState([]); const [error, setError] = useState(''); const [loading, setLoading] = useState(false);
  useEffect(() => {
    if (!eventId) return undefined;
    const timer = setTimeout(() => { setLoading(true); getCases({ eventId }).then((data) => setCases(data?.content || [])).catch((e) => setError(e.message)).finally(() => setLoading(false)); }, 0);
    return () => clearTimeout(timer);
  }, [eventId]);
  return <><EventSelector />{eventId && <Card className="mt-3"><Card.Body><h1 className="h4">Case Management</h1>{error && <Alert variant="danger">{error}</Alert>}{loading ? <Spinner animation="border" /> : <Table responsive><thead><tr><th>ID</th><th>Category</th><th>Subject</th><th>Team</th><th>Status</th><th>Created</th></tr></thead><tbody>{cases.length === 0 && <tr><td colSpan={6}>No cases found.</td></tr>}{cases.map((item) => <tr key={item.id}><td>{item.id}</td><td>{item.category}</td><td>{item.subject}</td><td>{item.teamId || '—'}</td><td><Badge>{item.status}</Badge></td><td>{item.createdAt ? new Date(item.createdAt).toLocaleString() : '—'}</td></tr>)}</tbody></Table>}</Card.Body></Card>}</>;
}
