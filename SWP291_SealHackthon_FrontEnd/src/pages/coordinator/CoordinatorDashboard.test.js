import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import {
  coordinatorDashboardMetrics,
  loadCoordinatorDashboardData,
} from './coordinatorDashboardData.js';

const events = [
  { id: 'ongoing-event', title: 'E2E Event', status: 'ongoing' },
  { id: 'draft-event', title: 'Draft', status: 'draft' },
];
const teams = [
  { id: 'alpha', name: 'Alpha Innovators', status: 'active' },
  { id: 'beta', name: 'Beta Builders', status: 'active' },
];
const submissions = [
  { id: 'one', teamName: 'Alpha Innovators', status: 'submitted' },
  { id: 'two', teamName: 'Beta Builders', status: 'submitted' },
  { id: 'three', teamName: 'Alpha Innovators', status: 'submitted' },
];

test('successful response exposes actual backend fields and metrics', async () => {
  const calls = [];
  const data = await loadCoordinatorDashboardData({
    getEvents: async () => ({ content: events }),
    getTeams: async (params) => { calls.push(['teams', params]); return { content: teams }; },
    getSubmissions: async (params) => { calls.push(['submissions', params]); return { content: submissions }; },
  });

  assert.deepEqual(coordinatorDashboardMetrics(data), {
    totalEvents: '2', activeEvents: '1', registeredTeams: '2', disqualifiedTeams: '0', submissions: '3',
  });
  assert.deepEqual(data.teams.map((team) => team.name), ['Alpha Innovators', 'Beta Builders']);
  assert.deepEqual(data.submissions.map((submission) => submission.teamName),
    ['Alpha Innovators', 'Beta Builders', 'Alpha Innovators']);
  assert.deepEqual(calls, [
    ['teams', { eventId: 'ongoing-event', size: 100 }],
    ['submissions', { eventId: 'ongoing-event', size: 100 }],
  ]);
});

test('a failed section is not represented as successful zero-valued data', async () => {
  const data = await loadCoordinatorDashboardData({
    getEvents: async () => ({ content: events }),
    getTeams: async () => ({ content: teams }),
    getSubmissions: async () => { throw new Error('Failed to load submissions'); },
  });

  assert.equal(data.submissionsError, 'Failed to load submissions');
  assert.equal(coordinatorDashboardMetrics(data).submissions, '—');
  assert.equal(coordinatorDashboardMetrics(data).registeredTeams, '2');
});

test('event request failure rejects the dashboard load', async () => {
  await assert.rejects(() => loadCoordinatorDashboardData({
    getEvents: async () => { throw new Error('Failed to load events'); },
    getTeams: async () => [],
    getSubmissions: async () => [],
  }), /Failed to load events/);
});

test('successful empty response remains distinguishable from failure', async () => {
  const data = await loadCoordinatorDashboardData({
    getEvents: async () => ({ content: [] }),
    getTeams: async () => { throw new Error('must not be called'); },
    getSubmissions: async () => { throw new Error('must not be called'); },
  });

  assert.deepEqual(coordinatorDashboardMetrics(data), {
    totalEvents: '0', activeEvents: '0', registeredTeams: '0', disqualifiedTeams: '0', submissions: '0',
  });
  assert.equal(data.teamsError, '');
  assert.equal(data.submissionsError, '');
});

test('component retains distinct loading, request-failure, and empty states', async () => {
  const source = await readFile(new URL('./CoordinatorDashboard.jsx', import.meta.url), 'utf8');
  assert.match(source, /if \(loading\)/);
  assert.match(source, /<Spinner/);
  assert.match(source, /if \(error\)/);
  assert.match(source, /<Alert variant="danger">\{error\}<\/Alert>/);
  assert.match(source, /No teams registered yet\./);
  assert.match(source, /No submissions yet\./);
});
