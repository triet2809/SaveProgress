import React from 'react';
import { Badge } from 'react-bootstrap';
import {
  Flag, Ban, RefreshCw, BarChart3, ArrowUpCircle, XCircle,
  MessageSquareWarning, CheckCircle2, Scale, Trophy, Repeat,
} from 'lucide-react';

// Map mỗi loại mốc timeline (khớp enum TimelineEventType ở BE) sang icon + màu hiển thị.
// Giữ nguyên value tiếng Anh của enum; chỉ phần label hiển thị mới thân thiện với người dùng.
const TYPE_META = {
  TEAM_CREATED: { icon: Flag, variant: 'primary', label: 'Team Created' },
  TEAM_DISQUALIFIED: { icon: Ban, variant: 'danger', label: 'Disqualified' },
  TEAM_REACTIVATED: { icon: RefreshCw, variant: 'success', label: 'Reactivated' },
  RANKING_RECALCULATED: { icon: BarChart3, variant: 'info', label: 'Ranking Updated' },
  TEAM_PROMOTED: { icon: ArrowUpCircle, variant: 'success', label: 'Promoted' },
  TEAM_ELIMINATED: { icon: XCircle, variant: 'danger', label: 'Eliminated' },
  APPEAL_SUBMITTED: { icon: MessageSquareWarning, variant: 'warning', label: 'Appeal Submitted' },
  APPEAL_RESOLVED: { icon: CheckCircle2, variant: 'info', label: 'Appeal Resolved' },
  TIE_BREAK_DECISION: { icon: Scale, variant: 'secondary', label: 'Tie-break Decision' },
  PRIZE_REVOKED: { icon: Trophy, variant: 'danger', label: 'Prize Revoked' },
  PRIZE_REASSIGNED: { icon: Repeat, variant: 'primary', label: 'Prize Reassigned' },
};

const metaFor = (type) => TYPE_META[type] || { icon: Flag, variant: 'secondary', label: type };

/**
 * Component hiển thị dòng thời gian (timeline) hành trình của đội.
 * Props:
 *  - events: mảng TeamTimelineEventResponse từ BE (đã sắp xếp theo occurredAt).
 *  - showTeam: có hiển thị tên đội trên mỗi mốc không (dùng cho view của EC gộp nhiều đội).
 */
const TeamJourneyTimeline = ({ events = [], showTeam = false }) => {
  if (!events.length) {
    return <div className="text-muted text-center py-4">No timeline events yet.</div>;
  }

  return (
    <div style={{ position: 'relative', paddingLeft: '2rem' }}>
      {/* Đường kẻ dọc nối các mốc */}
      <div
        style={{
          position: 'absolute',
          left: '0.65rem',
          top: '0.5rem',
          bottom: '0.5rem',
          width: '2px',
          backgroundColor: 'var(--cf-border-color)',
        }}
      />
      {events.map((ev) => {
        const meta = metaFor(ev.type);
        const Icon = meta.icon;
        return (
          <div key={ev.id} style={{ position: 'relative', marginBottom: '1.25rem' }}>
            {/* Chấm tròn + icon của mốc */}
            <div
              className={`bg-${meta.variant}`}
              style={{
                position: 'absolute',
                left: '-2rem',
                top: 0,
                width: '1.4rem',
                height: '1.4rem',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
              }}
            >
              <Icon size={12} />
            </div>
            <div className="d-flex align-items-center gap-2 flex-wrap">
              <Badge bg={meta.variant}>{meta.label}</Badge>
              <span className="fw-semibold">{ev.title}</span>
              {showTeam && ev.teamName && <span className="text-muted small">· {ev.teamName}</span>}
              {ev.roundName && <span className="text-muted small">· {ev.roundName}</span>}
            </div>
            {ev.description && <div className="text-muted small mt-1">{ev.description}</div>}
            <div className="d-flex align-items-center gap-3 mt-1">
              {ev.rankSnapshot != null && <span className="small text-muted">Rank #{ev.rankSnapshot}</span>}
              {ev.scoreSnapshot != null && <span className="small text-muted">Score {Number(ev.scoreSnapshot).toFixed(2)}</span>}
              {ev.statusSnapshot && <span className="small text-muted">Status: {ev.statusSnapshot}</span>}
              <span className="small text-muted">{ev.occurredAt ? new Date(ev.occurredAt).toLocaleString() : ''}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default TeamJourneyTimeline;
