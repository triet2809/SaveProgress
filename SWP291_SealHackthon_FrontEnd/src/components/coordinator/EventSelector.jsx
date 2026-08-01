import { Alert, Form, Spinner } from 'react-bootstrap';
import { useEventContext } from '../../hooks/useEventContext';

export default function EventSelector({ children }) {
  // Component chọn event dùng chung cho nhiều màn coordinator.
  // Luồng: EventSelector -> useEventContext() -> getEvents() -> người dùng chọn event -> query param eventId đổi.
  const { eventId, events, loading, error, selectEvent } = useEventContext();
  if (loading) return <Spinner animation="border" size="sm" />;
  return (
    <>
      {/* Dropdown này là điểm vào để các màn phụ thuộc event biết event nào đang được xem. */}
      <Form.Select className="mb-3" value={eventId} onChange={(e) => selectEvent(e.target.value)}>
        <option value="">Select an event</option>
        {events.map((event) => <option key={event.id} value={event.id}>{event.title}</option>)}
      </Form.Select>
      {error && <Alert variant="warning">{error}</Alert>}
      {/* Chưa chọn event thì chỉ hiện nhắc nhở; đã chọn hợp lệ mới render children phía dưới. */}
      {eventId && !error ? children : <Alert variant="info">Choose an event to view event-owned records.</Alert>}
    </>
  );
}
