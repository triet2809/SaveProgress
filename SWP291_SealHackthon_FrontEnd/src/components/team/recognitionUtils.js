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

export const activeRecognitionModels = (recognitions) =>
  (Array.isArray(recognitions) ? recognitions : [])
    .map(recognitionViewModel)
    .filter(Boolean);
