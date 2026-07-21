import { useState } from 'react';
import { Alert, Button, Card, Form } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { createCase } from '../../api/hackathonApi';

export default function CaseForm() {
  const navigate = useNavigate(); const [form, setForm] = useState({ eventId: '', category: 'technical_support', subject: '', description: '' }); const [error, setError] = useState('');
  const submit = async (e) => { e.preventDefault(); try { await createCase(form); navigate('/team/cases'); } catch (err) { setError(err.message || 'Unable to create case'); } };
  return <Card><Card.Body><h1 className="h4">Report &amp; Support</h1>{error && <Alert variant="danger">{error}</Alert>}<Form onSubmit={submit}><Form.Group className="mb-2"><Form.Label>Event ID</Form.Label><Form.Control required value={form.eventId} onChange={(e) => setForm({ ...form, eventId: e.target.value })} /></Form.Group><Form.Group className="mb-2"><Form.Label>Category</Form.Label><Form.Select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>{['technical_support', 'rule_question', 'scoring_issue', 'submission_issue', 'team_issue', 'conduct_incident', 'other'].map((x) => <option key={x}>{x}</option>)}</Form.Select></Form.Group><Form.Group className="mb-2"><Form.Label>Subject</Form.Label><Form.Control required value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} /></Form.Group><Form.Group className="mb-2"><Form.Label>Description</Form.Label><Form.Control as="textarea" required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Form.Group><Button type="submit">Submit case</Button></Form></Card.Body></Card>;
}
