import { Badge, Form, ListGroup } from 'react-bootstrap';

const HistoricalRosterReview = ({
  roster = [],
  selectedIds = [],
  leaderId = '',
  onMemberToggle,
  onLeaderChange,
  disabled = false,
}) => {
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
