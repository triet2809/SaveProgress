/**
 * TeamRecognitionBadge.jsx — Badge hiển thị các danh hiệu (recognition) của đội.
 * Lọc ra danh hiệu active, render badge pill kèm tooltip và aria-label cho trợ năng.
 * @param {Array} recognitions - Danh sách danh hiệu thô.
 * @param {'compact'|'detailed'} variant - 'detailed' hiển thêm displayText và icon lớn hơn.
 * @param {string} className - Class bổ sung.
 */
import PropTypes from 'prop-types';
import { Award } from 'lucide-react';
import { activeRecognitionModels } from './recognitionUtils';

const TeamRecognitionBadge = ({ recognitions, variant = 'compact', className = '' }) => {
  const models = activeRecognitionModels(recognitions);
  // Không có danh hiệu active -> không render gì.
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
