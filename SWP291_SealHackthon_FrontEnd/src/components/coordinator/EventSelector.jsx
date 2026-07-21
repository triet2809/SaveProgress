import { Alert, Form, Spinner } from 'react-bootstrap';
import { useEventContext } from '../../hooks/useEventContext';

export default function EventSelector({ children }) {
  const { eventId, events, loading, error, selectEvent } = useEventContext();
  if (loading) return <Spinner animation="border" size="sm" />;
  return (
    <>
      <Form.Select className="mb-3" value={eventId} onChange={(e) => selectEvent(e.target.value)}>
        <option value="">Select an event</option>
        {events.map((event) => <option key={event.id} value={event.id}>{event.title}</option>)}
      </Form.Select>
      {error && <Alert variant="warning">{error}</Alert>}
      {eventId && !error ? children : <Alert variant="info">Choose an event to view event-owned records.</Alert>}
    </>
  );
}
