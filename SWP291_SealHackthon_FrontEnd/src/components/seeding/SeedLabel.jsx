import { Badge } from 'react-bootstrap';

const SeedLabel = ({ assignment, suggestedTier }) => {
  if (!assignment) {
    return suggestedTier ? <Badge bg="secondary">{suggestedTier.replace('_', ' ')}</Badge> : null;
  }
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
