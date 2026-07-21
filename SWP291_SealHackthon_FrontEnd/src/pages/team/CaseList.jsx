import { useEffect, useState } from 'react';
import { Alert, Badge, Card, Spinner, Table } from 'react-bootstrap';
import { getCases } from '../../api/hackathonApi';

export default function CaseList() {
  const [items, setItems] = useState([]); const [error, setError] = useState(''); const [loading, setLoading] = useState(true);
  useEffect(() => { const timer = setTimeout(() => { getCases().then((data) => setItems(data?.content || [])).catch((e) => setError(e.message)).finally(() => setLoading(false)); }, 0); return () => clearTimeout(timer); }, []);
  return <Card><Card.Body><h1 className="h4">Report &amp; Support</h1>{error && <Alert variant="danger">{error}</Alert>}{loading ? <Spinner animation="border" /> : <Table responsive><thead><tr><th>Case</th><th>Subject</th><th>Status</th></tr></thead><tbody>{items.length === 0 && <tr><td colSpan={3}>No cases submitted.</td></tr>}{items.map((item) => <tr key={item.id}><td>{item.id}</td><td>{item.subject}</td><td><Badge>{item.status}</Badge></td></tr>)}</tbody></Table>}</Card.Body></Card>;
}
