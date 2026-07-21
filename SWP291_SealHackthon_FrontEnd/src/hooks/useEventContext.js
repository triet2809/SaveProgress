import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getEvents } from '../api/hackathonApi';

export function useEventContext() {
  const [searchParams, setSearchParams] = useSearchParams();
  const eventId = searchParams.get('eventId') || '';
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getEvents({ size: 100 }).then((data) => {
      if (!active) return;
      const values = data?.content || data || [];
      setEvents(values);
      if (eventId && !values.some((event) => event.id === eventId)) {
        setError('The selected event is unavailable or no longer exists.');
      }
    }).catch((err) => active && setError(err.message || 'Failed to load events'))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [eventId]);

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
