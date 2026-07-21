import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Form, Spinner, Table } from 'react-bootstrap';
import { useSearchParams } from 'react-router-dom';
import EventSelector from '../../components/coordinator/EventSelector';
import { getEventStaff, getRounds, getTracks, inviteEventStaff, removeEventStaff } from '../../api/hackathonApi';

const values = (data) => data?.content || data || [];
const blank = { fullName: '', email: '', temporaryPassword: '', roles: ['mentor'], mentorTrackIds: [], judgeTrackIds: [], judgeRoundIds: [] };

export default function StaffManagement() {
  const [searchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const [staff, setStaff] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [rounds, setRounds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [form, setForm] = useState(blank);

  const load = async () => {
    if (!eventId) return;
    setLoading(true); setError('');
    try {
      const [people, trackData, roundData] = await Promise.all([
        getEventStaff(eventId), getTracks({ eventId, size: 200 }), getRounds({ eventId, size: 200 }),
      ]);
      setStaff(values(people)); setTracks(values(trackData)); setRounds(values(roundData));
    } catch (err) { setError(err.message || 'Failed to load event staff'); }
    finally { setLoading(false); }
  };
  useEffect(() => {
    const timer = setTimeout(() => { load(); }, 0);
    return () => clearTimeout(timer);
    // load is intentionally scoped to the selected event.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId]);

  const toggleRole = (role) => setForm((current) => ({
    ...current, roles: current.roles.includes(role) ? current.roles.filter((r) => r !== role) : [...current.roles, role],
  }));
  const toggleSet = (field, id) => setForm((current) => ({
    ...current, [field]: current[field].includes(id) ? current[field].filter((value) => value !== id) : [...current[field], id],
  }));
  const save = async (event) => {
    event.preventDefault(); setError(''); setNotice('');
    try {
      await inviteEventStaff(eventId, form);
      setNotice(form.temporaryPassword
        ? 'Staff account approved. The temporary password must be changed during first-login onboarding.'
        : 'Existing staff account assigned without changing its password.');
      setForm(blank); await load();
    } catch (err) { setError(err.message || 'Failed to save staff assignment'); }
  };
  const remove = async (person, type) => {
    if (!window.confirm(`Remove ${type} assignment for ${person.fullName} from this event?`)) return;
    setError(''); setNotice('');
    try { await removeEventStaff(eventId, person.userId, type); setNotice(`${type} assignment removed.`); await load(); }
    catch (err) { setError(err.message || 'Failed to remove assignment'); }
  };

  return (
    <>
      <EventSelector />
      {eventId && <div className="py-2">
        <h1 className="h3 fw-bold mb-3">Judge &amp; Mentor Management</h1>
        {error && <Alert variant="danger">{error}</Alert>}
        {notice && <Alert variant="success">{notice}</Alert>}
        <Card className="mb-4"><Card.Body>
          <h2 className="h5">Invite or assign staff</h2>
          <Form onSubmit={save}>
            <div className="row g-2">
              <Form.Group className="col-md-4"><Form.Label>Full name</Form.Label><Form.Control required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></Form.Group>
              <Form.Group className="col-md-4"><Form.Label>Email</Form.Label><Form.Control required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Form.Group>
              <Form.Group className="col-md-4"><Form.Label>Event roles</Form.Label><div>{['judge', 'mentor'].map((role) => <Form.Check inline key={role} type="checkbox" label={role === 'judge' ? 'Judge' : 'Mentor'} checked={form.roles.includes(role)} onChange={() => toggleRole(role)} />)}</div></Form.Group>
              <Form.Group className="col-md-4"><Form.Label>Temporary password</Form.Label><Form.Control type="password" minLength={8} value={form.temporaryPassword} onChange={(e) => setForm({ ...form, temporaryPassword: e.target.value })} /><Form.Text>Required only for a new account; leave blank to assign an existing approved or pending account.</Form.Text></Form.Group>
            </div>
            <Form.Label className="mt-3">Mentor tracks</Form.Label>
            <div className="d-flex flex-wrap gap-3">{tracks.map((track) => <Form.Check key={`m-${track.id}`} inline type="checkbox" label={track.name} checked={form.mentorTrackIds.includes(track.id)} onChange={() => toggleSet('mentorTrackIds', track.id)} />)}</div>
            <Form.Label className="mt-3">Judge tracks</Form.Label>
            <div className="d-flex flex-wrap gap-3">{tracks.map((track) => <Form.Check key={`j-${track.id}`} inline type="checkbox" label={track.name} checked={form.judgeTrackIds.includes(track.id)} onChange={() => toggleSet('judgeTrackIds', track.id)} />)}</div>
            <Form.Label className="mt-3">Judge rounds</Form.Label>
            <div className="d-flex flex-wrap gap-3">{rounds.map((round) => <Form.Check key={round.id} inline type="checkbox" label={round.name} checked={form.judgeRoundIds.includes(round.id)} onChange={() => toggleSet('judgeRoundIds', round.id)} />)}</div>
            <Button className="mt-3" type="submit" disabled={loading || form.roles.length === 0}>Save staff assignment</Button>
          </Form>
        </Card.Body></Card>
        <Card><Card.Body>
          <h2 className="h5">Assigned staff</h2>
          {loading ? <Spinner animation="border" size="sm" /> : <Table responsive hover><thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Event roles</th><th>Tracks</th><th>Rounds</th><th>Actions</th></tr></thead><tbody>
            {staff.length === 0 && <tr><td colSpan={7} className="text-muted">No staff assigned in this event.</td></tr>}
            {staff.map((person) => <tr key={person.userId}><td>{person.fullName}</td><td>{person.email}</td><td>{person.accountStatus}</td><td>{person.eventRoles?.map((role) => <Badge className="me-1" key={role}>{role}</Badge>)}</td><td>{[...(person.mentorTracks || []), ...(person.judgeTracks || [])].join(', ') || '—'}</td><td>{(person.judgeRounds || []).join(', ') || '—'}</td><td>{person.eventRoles?.includes('mentor') && <Button size="sm" variant="outline-danger" className="me-1" onClick={() => remove(person, 'mentor')}>Remove mentor</Button>}{person.eventRoles?.includes('judge') && <Button size="sm" variant="outline-danger" onClick={() => remove(person, 'judge')}>Remove judge</Button>}</td></tr>)}
          </tbody></Table>}
        </Card.Body></Card>
      </div>}
    </>
  );
}
