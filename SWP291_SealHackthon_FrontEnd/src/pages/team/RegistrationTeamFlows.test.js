import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = (relativePath) => readFileSync(new URL(relativePath, import.meta.url), 'utf8');

test('registration fallback contains all five FPT campuses', () => {
  const config = source('../../config/registerConfig.js');
  for (const campus of ['Ha Noi', 'Ho Chi Minh City', 'Da Nang', 'Can Tho', 'Quy Nhon']) {
    assert.match(config, new RegExp(`FPT University ${campus}`));
  }
  assert.equal((config.match(/\{ id: .*name: 'FPT University/g) || []).length, 5);
});

test('approval screen projects and safely displays campus name', () => {
  const approval = source('../coordinator/UserApproval.jsx');
  assert.match(approval, /campus: u\.campusName \|\| '—'/);
  assert.match(approval, />Campus</);
  assert.match(approval, /selectedUser\.campus/);
});

test('participant team creation sends event without a track selector', () => {
  const createTeam = source('./CreateTeam.jsx');
  assert.doesNotMatch(createTeam, /name="trackId"/);
  assert.doesNotMatch(createTeam, /getTracks/);
  assert.match(createTeam, /eventId: teamData\.eventId/);
  assert.match(createTeam, /coordinator will assign your team to a track later/);
});
