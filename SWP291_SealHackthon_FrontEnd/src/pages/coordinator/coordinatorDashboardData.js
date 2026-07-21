export const listOf = (data) => data?.content || data || [];

export async function loadCoordinatorDashboardData({ getEvents, getTeams, getSubmissions }) {
  const eventList = listOf(await getEvents({ size: 100 }));
  const scopedEventId = eventList.find(
    (event) => (event.status || '').toLowerCase() === 'ongoing',
  )?.id || eventList[0]?.id;

  if (!scopedEventId) {
    return { events: eventList, teams: [], submissions: [], teamsError: '', submissionsError: '' };
  }

  const [teamsResult, submissionsResult] = await Promise.allSettled([
    getTeams({ eventId: scopedEventId, size: 100 }),
    getSubmissions({ eventId: scopedEventId, size: 100 }),
  ]);

  return {
    events: eventList,
    teams: teamsResult.status === 'fulfilled' ? listOf(teamsResult.value) : [],
    submissions: submissionsResult.status === 'fulfilled' ? listOf(submissionsResult.value) : [],
    teamsError: teamsResult.status === 'rejected'
      ? teamsResult.reason?.message || 'Failed to load teams'
      : '',
    submissionsError: submissionsResult.status === 'rejected'
      ? submissionsResult.reason?.message || 'Failed to load submissions'
      : '',
  };
}

export function coordinatorDashboardMetrics({ events, teams, submissions, teamsError, submissionsError }) {
  const activeEvents = events.filter((event) => (event.status || '').toLowerCase() === 'ongoing');
  const disqualifiedTeams = teams.filter((team) => (team.status || '').toLowerCase() === 'disqualified');
  return {
    totalEvents: String(events.length),
    activeEvents: String(activeEvents.length),
    registeredTeams: teamsError ? '—' : String(teams.length),
    disqualifiedTeams: teamsError ? '—' : String(disqualifiedTeams.length),
    submissions: submissionsError ? '—' : String(submissions.length),
  };
}
