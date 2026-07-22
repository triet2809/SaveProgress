/**
 * useEventContext.js — Custom hook đồng bộ "sự kiện đang chọn" với query param `eventId` trên URL.
 * Tải danh sách sự kiện, phát hiện eventId không hợp lệ, và cung cấp hàm chọn sự kiện.
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getEvents } from '../api/hackathonApi';

/**
 * @returns {{eventId, events, selectedEvent, loading, error, selectEvent}}
 *  - eventId: id sự kiện hiện tại (từ URL)
 *  - events: danh sách sự kiện đã tải
 *  - selectedEvent: object sự kiện khớp eventId (hoặc null)
 *  - selectEvent(id): cập nhật query param eventId
 */
export function useEventContext() {
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Tải danh sách sự kiện mỗi khi eventId đổi; dùng cờ `active` để tránh setState sau unmount.
  useEffect(() => {
    let active = true;
    getEvents({ size: 100 }).then((data) => {
      if (!active) return;
      const values = data?.content || data || [];
      setEvents(values);
      // eventId được chọn nhưng không có trong danh sách -> báo lỗi.
      if (eventId && !values.some((event) => event.id === eventId)) {
        setError('The selected event is unavailable or no longer exists.');
      }
    }).catch((err) => active && setError(err.message || 'Failed to load events'))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [eventId]);

  // Cập nhật query param eventId (xóa nếu id rỗng).
  const selectEvent = useCallback((id) => {
    const next = new URLSearchParams(searchParams);
    if (id) next.set('eventId', id); else next.delete('eventId');
    setSearchParams(next);
  }, [searchParams, setSearchParams]);

  return useMemo(() => ({
    eventId, events, selectedEvent: events.find((event) => event.id === eventId) || null,
    loading, error, selectEvent,
  }), [eventId, events, loading, error, selectEvent]);
}
