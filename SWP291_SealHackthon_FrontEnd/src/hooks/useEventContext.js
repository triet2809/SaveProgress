import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getEvents } from '../api/hackathonApi';

// Hook này giữ event đang chọn đồng bộ với URL.
// Luồng: URL eventId -> tải danh sách event -> tìm selectedEvent -> component bên trên dùng lại.
export function useEventContext() {
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Tải danh sách event mỗi khi eventId đổi; dùng cờ active để tránh setState sau unmount.
  useEffect(() => {
    let active = true;
    getEvents({ size: 100 }).then((data) => {
      if (!active) return;
      const values = data?.content || data || [];
      setEvents(values);
      // eventId đang chọn nhưng không còn tồn tại trong danh sách -> báo lỗi.
      if (eventId && !values.some((event) => event.id === eventId)) {
        setError('The selected event is unavailable or no longer exists.');
      }
    }).catch((err) => active && setError(err.message || 'Failed to load events'))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [eventId]);

  // Cập nhật query param eventId; nếu rỗng thì xóa khỏi URL.
  const selectEvent = useCallback((id) => {
    const next = new URLSearchParams(searchParams);
    if (id) next.set('eventId', id); else next.delete('eventId');
    // Xóa round/track cũ để không lẫn sang event mới
    // (gây lỗi backend "Round does not belong to the selected event").
    next.delete('roundId');
    next.delete('trackId');
    setSearchParams(next);
  }, [searchParams, setSearchParams]);

  return useMemo(() => ({
    eventId, events, selectedEvent: events.find((event) => event.id === eventId) || null,
    loading, error, selectEvent,
  }), [eventId, events, loading, error, selectEvent]);
}
