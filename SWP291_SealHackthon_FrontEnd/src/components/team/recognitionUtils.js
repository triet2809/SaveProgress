/**
 * recognitionUtils.js — Tiện ích chuyển dữ liệu danh hiệu (recognition) của đội
 * thành view model để hiển thị (label, displayText, accessibleText cho screen reader).
 */

/**
 * Chuẩn hoá một recognition thành view model; trả null nếu không active.
 * @param {object} recognition - Dữ liệu danh hiệu từ BE.
 * @returns {object|null} view model gồm code, label, displayText, accessibleText, earnedAt.
 */
export const recognitionViewModel = (recognition) => {
  if (!recognition?.active) return null;
  const count = Number(recognition.qualificationCount || 0);
  const knownVeteran = recognition.code === 'HACKATHON_VETERAN';
  const label = recognition.label || (knownVeteran ? 'Hackathon Veteran' : recognition.code) || 'Team recognition';
  const displayText = recognition.displayText || (count > 0 ? `${count}+ Seasons` : 'Recognized team');
  return {
    code: recognition.code || 'UNKNOWN_RECOGNITION',
    label,
    displayText,
    accessibleText: `${label}, ${displayText}`,
    earnedAt: recognition.earnedAt || null,
  };
};

// Lọc + chuyển danh sách recognition thành các view model active (bỏ null).
export const activeRecognitionModels = (recognitions) =>
  (Array.isArray(recognitions) ? recognitions : [])
    .map(recognitionViewModel)
    .filter(Boolean);
