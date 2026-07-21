import { useEffect, useState, useCallback } from 'react';
import { Card, Spinner, Alert, Form, Button } from 'react-bootstrap';
import { Route as RouteIcon } from 'lucide-react';
import { getEvents, getEventTimeline } from '../../api/hackathonApi';
import EventTimelineList from '../../components/timeline/EventTimeline';
import TimelineFilters from '../../components/timeline/TimelineFilters';

const listOf = (data) => (Array.isArray(data) ? data : data?.content || []);

// EC xem toàn bộ dòng thời gian của mọi đội trong một sự kiện.
// Endpoint BE có phân trang; ở đây nạp thêm theo nút "Load more".
const EventTimeline = () => {
  const [events, setEvents] = useState([]);
  const [eventId, setEventId] = useState('');
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({});

  // Tải sự kiện để chọn.
  useEffect(() => {
    (async () => {
      try {
        const evList = listOf(await getEvents({ size: 100 }));
        setEvents(evList);
        if (evList.length) setEventId(evList[0].id);
      } catch (e) {
        setError(e.message || 'Failed to load events');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // Tải một trang timeline của sự kiện đang chọn.
  const loadPage = useCallback(async (evId, pageNo, append) => {
    const params = Object.fromEntries(Object.entries({ page: pageNo, size: 30, ...filters }).filter(([, value]) => value));
    const data = await getEventTimeline(evId, params);
    const content = data?.content || [];
    setItems((prev) => (append ? [...prev, ...content] : content));
    // Còn trang nếu chưa phải trang cuối.
    setHasMore(data && data.last === false);
  }, [filters]);

  // Khi đổi sự kiện: reset và tải trang đầu.
  useEffect(() => {
    if (!eventId) return;
    let active = true;
    (async () => {
      try {
        setError('');
        setPage(0);
        await loadPage(eventId, 0, false);
      } catch (e) {
        if (active) setError(e.message || 'Failed to load timeline');
      }
    })();
    return () => { active = false; };
  }, [eventId, loadPage]);

  const handleLoadMore = async () => {
    try {
      setLoadingMore(true);
      const next = page + 1;
      await loadPage(eventId, next, true);
      setPage(next);
    } catch (e) {
      setError(e.message || 'Failed to load more');
    } finally {
      setLoadingMore(false);
    }
  };

  if (loading) {
    return <div className="py-5 text-center"><Spinner animation="border" variant="primary" /></div>;
  }

  return (
    <div className="py-2">
      <div className="d-flex align-items-center gap-2 mb-4">
        <RouteIcon size={24} className="text-primary" />
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Team Timeline</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>
            All team milestones across an event
          </div>
        </div>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card className="mb-4" style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Card.Body className="p-3">
          <Form.Group className="d-flex align-items-center gap-3">
            <Form.Label className="mb-0 fw-medium">Event</Form.Label>
            <Form.Select style={{ maxWidth: 360 }} value={eventId} onChange={(e) => setEventId(e.target.value)}>
              {events.map((ev) => <option key={ev.id} value={ev.id}>{ev.title}</option>)}
            </Form.Select>
          </Form.Group>
        </Card.Body>
      </Card>

      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Card.Body className="p-4">
          <TimelineFilters value={filters} onChange={setFilters} coordinator />
          <EventTimelineList items={items} />
          {hasMore && (
            <div className="text-center mt-3">
              <Button variant="outline-secondary" size="sm" onClick={handleLoadMore} disabled={loadingMore}>
                {loadingMore ? 'Loading...' : 'Load more'}
              </Button>
            </div>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default EventTimeline;
