/**
 * SeedLabel.jsx — Badge hiển thị trạng thái hạt giống (seed) của một đội.
 * Nếu chưa có quyết định seed thì hiển thị tier gợi ý (nếu có), ngược lại hiển status + số seed.
 * @param {object} assignment - Quyết định seed đã gán (status, seedNumber).
 * @param {string} suggestedTier - Tier gợi ý khi chưa gán.
 */
import { Badge } from 'react-bootstrap';

const SeedLabel = ({ assignment, suggestedTier }) => {
  // Chưa gán seed: hiển tier gợi ý nếu có.
  if (!assignment) {
    return suggestedTier ? <Badge bg="secondary">{suggestedTier.replace('_', ' ')}</Badge> : null;
  }
  // Chọn màu badge theo trạng thái seed.
  const variant = assignment.status === 'confirmed'
    ? 'success'
    : assignment.status === 'overridden'
      ? 'primary'
      : assignment.status === 'rejected'
        ? 'danger'
        : 'secondary';
  const number = assignment.seedNumber ? ` #${assignment.seedNumber}` : '';
  return <Badge bg={variant}>{assignment.status}{number}</Badge>;
};

export default SeedLabel;
