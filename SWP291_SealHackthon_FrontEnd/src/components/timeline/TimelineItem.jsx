import { Badge } from 'react-bootstrap';
import { timelineTypeMeta } from './timelineEventTypes';

export default function TimelineItem({ item, showTeam = false }) {
  const meta = timelineTypeMeta(item.eventType);
  return (
    <div className="border-bottom py-3">
      <div className="d-flex justify-content-between gap-3">
        <div className="d-flex align-items-center gap-2">
          <Badge bg={meta.variant}>{meta.label}</Badge>
          <strong>{item.title || meta.label}</strong>
        </div>
        <time className="text-muted small">
          {item.occurredAt ? new Date(item.occurredAt).toLocaleString() : ''}
        </time>
      </div>
      {showTeam && item.teamId && <div className="text-muted small">Team {item.teamId}</div>}
      {item.description && <div className="text-muted small mt-1">{item.description}</div>}
    </div>
  );
}
