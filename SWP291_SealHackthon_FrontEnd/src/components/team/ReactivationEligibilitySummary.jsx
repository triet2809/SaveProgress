/**
 * ReactivationEligibilitySummary.jsx — Tóm tắt điều kiện tái kích hoạt đội.
 * Hiển thị trạng thái đủ/thiếu điều kiện, số thành viên quay lại, xung đột thành viên và cảnh báo.
 * @param {object} preview - Kết quả preview từ API (eligible, returningMemberCount, missingRequirements...).
 */
import { Alert, Badge, ListGroup } from 'react-bootstrap';

const ReactivationEligibilitySummary = ({ preview }) => {
  if (!preview) return null;

  return (
    <div className="mt-4">
      <Alert variant={preview.eligible ? 'success' : 'warning'}>
        <div className="d-flex justify-content-between align-items-center">
          <strong>{preview.eligible ? 'Ready to reactivate' : 'Not ready to reactivate'}</strong>
          <Badge bg={preview.returningMemberCount >= 3 ? 'success' : 'danger'}>
            {preview.returningMemberCount} returning
          </Badge>
        </div>
      </Alert>

      {/* Danh sách điều kiện còn thiếu (nếu có). */}
      {preview.missingRequirements?.length > 0 && (
        <ListGroup className="mb-3">
          {preview.missingRequirements.map((item) => (
            <ListGroup.Item key={item} variant="danger">{item}</ListGroup.Item>
          ))}
        </ListGroup>
      )}

      {/* Xung đột thành viên: đã thuộc đội khác... */}
      {preview.memberConflicts?.length > 0 && (
        <ListGroup className="mb-3">
          {preview.memberConflicts.map((conflict) => (
            <ListGroup.Item key={`${conflict.userId}-${conflict.conflictingTeamId}`} variant="danger">
              <strong>{conflict.fullName}:</strong> {conflict.reason}
            </ListGroup.Item>
          ))}
        </ListGroup>
      )}

      {/* Cảnh báo không chặn việc tái kích hoạt. */}
      {preview.warnings?.length > 0 && (
        <ListGroup>
          {preview.warnings.map((warning) => (
            <ListGroup.Item key={warning} variant="warning">{warning}</ListGroup.Item>
          ))}
        </ListGroup>
      )}
    </div>
  );
};

export default ReactivationEligibilitySummary;
