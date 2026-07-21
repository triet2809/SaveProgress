import PropTypes from 'prop-types';
import { Award } from 'lucide-react';
import { activeRecognitionModels } from './recognitionUtils';

const TeamRecognitionBadge = ({ recognitions, variant = 'compact', className = '' }) => {
  const models = activeRecognitionModels(recognitions);
  if (!models.length) return null;

  return (
    <span className={`d-inline-flex flex-wrap gap-1 ${className}`.trim()}>
      {models.map((model) => {
        const detailed = variant === 'detailed';
        const title = model.earnedAt
          ? `${model.accessibleText}. Earned ${new Date(model.earnedAt).toLocaleDateString()}`
          : model.accessibleText;
        return (
          <span
            key={model.code}
            className="badge rounded-pill text-bg-warning d-inline-flex align-items-center gap-1"
            title={title}
            aria-label={model.accessibleText}
          >
            <Award size={detailed ? 15 : 13} aria-hidden="true" />
            <span>{model.label}</span>
            {detailed && <span className="fw-normal">· {model.displayText}</span>}
          </span>
        );
      })}
    </span>
  );
};

TeamRecognitionBadge.propTypes = {
  recognitions: PropTypes.arrayOf(PropTypes.shape({
    code: PropTypes.string,
    label: PropTypes.string,
    displayText: PropTypes.string,
    qualificationCount: PropTypes.number,
    earnedAt: PropTypes.string,
    active: PropTypes.bool,
  })),
  variant: PropTypes.oneOf(['compact', 'detailed']),
  className: PropTypes.string,
};

export default TeamRecognitionBadge;
