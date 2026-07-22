/**
 * EventSelector.jsx — Dropdown chọn sự kiện; chỉ render children khi đã chọn 1 sự kiện.
 * Dùng cho các trang EC có dữ liệu gắn theo sự kiện (event-owned records).
 * @param {ReactNode} children - Nội dung hiển thị khi đã chọn sự kiện.
 */
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
      {/* Chưa chọn sự kiện -> nhắc người dùng chọn; đã chọn và không lỗi -> render children. */}
      {eventId && !error ? children : <Alert variant="info">Choose an event to view event-owned records.</Alert>}
    </>
  );
}
