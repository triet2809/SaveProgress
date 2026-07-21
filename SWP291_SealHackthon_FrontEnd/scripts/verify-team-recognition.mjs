import assert from 'node:assert/strict';
import {
  activeRecognitionModels,
  recognitionViewModel,
} from '../src/components/team/recognitionUtils.js';

const active = recognitionViewModel({
  code: 'HACKATHON_VETERAN',
  label: 'Hackathon Veteran',
  displayText: '4+ Seasons',
  qualificationCount: 4,
  active: true,
});
assert.equal(active.accessibleText, 'Hackathon Veteran, 4+ Seasons');
assert.equal(recognitionViewModel({ code: 'HACKATHON_VETERAN', active: false }), null);
assert.equal(activeRecognitionModels([{ code: 'HACKATHON_VETERAN', active: false }]).length, 0);
assert.equal(
  recognitionViewModel({ code: 'FUTURE_BADGE', qualificationCount: 1, active: true }).label,
  'FUTURE_BADGE',
);
assert.equal(
  recognitionViewModel({ code: 'FUTURE_BADGE', qualificationCount: 1, active: true }).displayText,
  '1+ Seasons',
);

console.log('Team recognition rendering verification passed.');
