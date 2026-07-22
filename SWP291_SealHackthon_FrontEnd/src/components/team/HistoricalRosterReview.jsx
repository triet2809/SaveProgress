/**
 * HistoricalRosterReview.jsx — Danh sách thành viên đội cũ để chọn khi tái kích hoạt.
 * Cho phép tick chọn thành viên đưa vào đội mới và chọn leader mới (radio).
 * @param {Array} roster - Danh sách thành viên lịch sử.
 * @param {string[]} selectedIds - id thành viên đang được chọn.
 * @param {string} leaderId - id leader mới được chọn.
 * @param {(id)=>void} onMemberToggle - Callback tick/bỏ tick thành viên.
 * @param {(id)=>void} onLeaderChange - Callback chọn leader.
 * @param {boolean} disabled - Vô hiệu hoá toàn bộ.
 */
import { Badge, Form, ListGroup } from 'react-bootstrap';

const HistoricalRosterReview = ({
  roster = [],
  selectedIds = [],
  leaderId = '',
  onMemberToggle,
  onLeaderChange,
  disabled = false,
}) => {
  // Dùng Set để tra cứu nhanh thành viên đã chọn.
  const selected = new Set(selectedIds);

  return (
    <ListGroup>
      {roster.map((member) => (
        <ListGroup.Item key={member.userId} className="d-flex align-items-center gap-3">
          <Form.Check
            type="checkbox"
            checked={selected.has(member.userId)}
            onChange={() => onMemberToggle(member.userId)}
            disabled={disabled}
            aria-label={`Include ${member.fullName}`}
          />
          <div className="flex-grow-1">
            <div className="fw-semibold">{member.fullName}</div>
            <Badge bg={member.historicalRole === 'leader' ? 'primary' : 'secondary'}>
              Historical {member.historicalRole}
            </Badge>
          </div>
          <Form.Check
            type="radio"
            name="reactivationLeader"
            label="New leader"
            checked={leaderId === member.userId}
            onChange={() => onLeaderChange(member.userId)}
            disabled={disabled || !selected.has(member.userId)}
          />
        </ListGroup.Item>
      ))}
    </ListGroup>
  );
};

export default HistoricalRosterReview;
