import { TIMELINE_EVENT_TYPES } from './timelineEventTypes';

export default function TimelineFilters({ value = {}, onChange, coordinator = false }) {
  return (
    <div className="row g-2 mb-3">
      <div className="col-md-4"><select className="form-select form-select-sm" value={value.eventType || ''} onChange={(e) => onChange?.({ ...value, eventType: e.target.value })}>
        <option value="">All event types</option>
        {Object.entries(TIMELINE_EVENT_TYPES).map(([key, meta]) => <option key={key} value={key}>{meta.label}</option>)}
      </select>
      </div>
      <div className="col-md-3"><input className="form-control form-control-sm" placeholder="Round ID" value={value.roundId || ''} onChange={(e) => onChange?.({ ...value, roundId: e.target.value })} /></div>
      <div className="col-md-3"><input className="form-control form-control-sm" placeholder="Track ID" value={value.trackId || ''} onChange={(e) => onChange?.({ ...value, trackId: e.target.value })} /></div>
      {coordinator && <div className="col-md-2"><select className="form-select form-select-sm" value={value.visibility || ''} onChange={(e) => onChange?.({ ...value, visibility: e.target.value })}>
        <option value="">All scopes</option>
        {['EVENT_PUBLIC', 'EVENT_PARTICIPANTS', 'TEAM_PRIVATE', 'STAFF_PRIVATE', 'COORDINATOR_PRIVATE'].map((scope) => <option key={scope}>{scope}</option>)}
      </select></div>}
    </div>
  );
}
