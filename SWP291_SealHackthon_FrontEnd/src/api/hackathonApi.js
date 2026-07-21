import { apiDelete, apiDownload, apiGet, apiPatch, apiPost, apiPut } from './client';

export async function getEvents(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/events${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load events');
  return res.data;
}

export async function getEvent(id) {
  const res = await apiGet(`/events/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load event');
  return res.data;
}

export async function createEvent(payload) {
  const res = await apiPost('/events', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create event');
  return res.data;
}

export async function updateEvent(id, payload) {
  const res = await apiPatch(`/events/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update event');
  return res.data;
}

export async function openEventRegistration(id) {
  const res = await apiPost(`/events/${id}/open-registration`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to open registration');
  return res.data;
}

export async function closeEventRegistration(id) {
  const res = await apiPost(`/events/${id}/close-registration`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to close registration');
  return res.data;
}

export async function setupCompetition(id, payload) {
  const res = await apiPost(`/events/${id}/setup-competition`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to set up competition');
  return res.data;
}

export async function changeEventStatus(id, status) {
  const res = await apiPost(`/events/${id}/status`, { status });
  if (!res.ok) throw new Error(res.data?.message || 'Failed to change event status');
  return res.data;
}

export async function deleteEvent(id) {
  const res = await apiDelete(`/events/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete event');
}

export async function getTracks(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/tracks${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load tracks');
  return res.data;
}

export async function getTrack(id) {
  const res = await apiGet(`/tracks/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load track');
  return res.data;
}

export async function createTrack(payload) {
  const res = await apiPost('/tracks', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create track');
  return res.data;
}

export async function updateTrack(id, payload) {
  const res = await apiPatch(`/tracks/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update track');
  return res.data;
}

export async function deleteTrack(id) {
  const res = await apiDelete(`/tracks/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete track');
}

export async function getRounds(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/rounds${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load rounds');
  return res.data;
}

export async function createRound(payload) {
  const res = await apiPost('/rounds', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create round');
  return res.data;
}

export async function updateRound(id, payload) {
  const res = await apiPatch(`/rounds/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update round');
  return res.data;
}

export async function deleteRound(id) {
  const res = await apiDelete(`/rounds/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete round');
}

export async function getTrackMentors(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/track-mentors${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load track mentors');
  return res.data;
}

export async function assignTrackMentor(payload) {
  const res = await apiPost('/track-mentors', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to assign track mentor');
  return res.data;
}

export async function deleteTrackMentor(id) {
  const res = await apiDelete(`/track-mentors/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to remove track mentor');
}

export async function getRoundJudges(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/round-judges${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load round judges');
  return res.data;
}

export async function assignRoundJudge(payload) {
  const res = await apiPost('/round-judges', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to assign round judge');
  return res.data;
}

export async function deleteRoundJudge(id) {
  const res = await apiDelete(`/round-judges/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to remove round judge');
}

export async function getTrackJudges(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/track-judges${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load track judges');
  return res.data;
}

export async function assignTrackJudge(payload) {
  const res = await apiPost('/track-judges', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to assign track judge');
  return res.data;
}

export async function deleteTrackJudge(id) {
  const res = await apiDelete(`/track-judges/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to remove track judge');
}

export async function getTeams(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/teams${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load teams');
  return res.data;
}

export async function getTeam(id) {
  const res = await apiGet(`/teams/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load team');
  return res.data;
}

export async function getMyTeams() {
  const res = await apiGet('/teams/me');
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load my teams');
  return res.data;
}

export async function createTeam(payload) {
  const res = await apiPost('/teams', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create team');
  return res.data;
}

export async function updateTeam(id, payload) {
  const res = await apiPatch(`/teams/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update team');
  return res.data;
}

export async function moveTeamTrack(id, trackId) {
  const res = await apiPost(`/teams/${id}/move-track`, { trackId });
  if (!res.ok) throw new Error(res.data?.message || 'Failed to move team to track');
  return res.data;
}

export async function addTeamMember(teamId, payload) {
  const res = await apiPost(`/teams/${teamId}/members`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to add team member');
  return res.data;
}

export async function removeTeamMember(teamId, userId) {
  const res = await apiDelete(`/teams/${teamId}/members/${userId}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to remove team member');
}

export async function disqualifyTeam(id, reason) {
  const res = await apiPost(`/teams/${id}/disqualify`, { reason });
  if (!res.ok) throw new Error(res.data?.message || 'Failed to disqualify team');
  return res.data;
}

export async function reactivateTeam(id) {
  const res = await apiPost(`/teams/${id}/reactivate`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to reactivate team');
  return res.data;
}

export async function createLogicalRound(payload) {
  const res = await apiPost('/rounds/logical', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create logical round');
  return res.data;
}

export async function bulkTransferTeams(teamIds, targetTrackId) {
  const res = await apiPost('/teams/bulk-transfer', { teamIds, targetTrackId });
  if (!res.ok) throw new Error(res.data?.message || 'Bulk team transfer failed');
  return res.data;
}

export async function previewBalancedTeams(eventId, targetTrackIds, randomSeed = null) {
  const res = await apiPost(`/teams/events/${eventId}/balance-preview`, { targetTrackIds, randomSeed });
  if (!res.ok) throw new Error(res.data?.message || 'Balance preview failed');
  return res.data;
}

export async function applyBalancedTeams(eventId, targetTrackIds, randomSeed = null) {
  const res = await apiPost(`/teams/events/${eventId}/balance-apply`, { targetTrackIds, randomSeed });
  if (!res.ok) throw new Error(res.data?.message || 'Balanced distribution failed');
  return res.data;
}

export async function getMyTeamProfiles(targetEventId) {
  const qs = targetEventId ? `?${new URLSearchParams({ targetEventId })}` : '';
  const res = await apiGet(`/team-profiles/mine${qs}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load previous teams');
  return res.data;
}

export async function previewTeamReactivation(profileId, payload) {
  const res = await apiPost(`/team-profiles/${profileId}/reactivation-preview`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to preview team reactivation');
  return res.data;
}

export async function reactivateTeamProfile(profileId, payload) {
  const res = await apiPost(`/team-profiles/${profileId}/reactivate`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to reactivate previous team');
  return res.data;
}

export async function finalizeEventResults(eventId) {
  const res = await apiPost(`/events/${eventId}/finalize-results`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to finalize event results');
  return res.data;
}

export async function getSeedCandidates(eventId, trackId) {
  const qs = trackId ? `?${new URLSearchParams({ trackId })}` : '';
  const res = await apiGet(`/events/${eventId}/seed-candidates${qs}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load seed candidates');
  return res.data;
}

export async function getEventSeeds(eventId) {
  const res = await apiGet(`/events/${eventId}/seeds`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load seed assignments');
  return res.data;
}

export async function setEventSeed(eventId, teamId, payload) {
  const res = await apiPut(`/events/${eventId}/teams/${teamId}/seed`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to save seed decision');
  return res.data;
}

export async function removeEventSeed(eventId, teamId) {
  const res = await apiDelete(`/events/${eventId}/teams/${teamId}/seed`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to remove seed decision');
}

export async function recalculateTeamRecognitions(profileId) {
  const res = await apiPost(`/team-profiles/${profileId}/recognitions/recalculate`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to recalculate team recognition');
  return res.data;
}

export async function getTeamRecognitionEvidence(profileId) {
  const res = await apiGet(`/team-profiles/${profileId}/recognitions/evidence`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load recognition evidence');
  return res.data;
}

export async function revokeTeamRecognition(profileId, recognitionId, reason) {
  const res = await apiPost(
    `/team-profiles/${profileId}/recognitions/${recognitionId}/revoke`,
    { reason },
  );
  if (!res.ok) throw new Error(res.data?.message || 'Failed to revoke team recognition');
  return res.data;
}

export async function restoreTeamRecognition(profileId, recognitionId) {
  const res = await apiPost(
    `/team-profiles/${profileId}/recognitions/${recognitionId}/restore`,
    {},
  );
  if (!res.ok) throw new Error(res.data?.message || 'Failed to restore team recognition');
  return res.data;
}

export async function getMentorTeams(mentorId, params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/track-mentors/mentors/${mentorId}/teams${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load mentor teams');
  return res.data;
}

export async function getEventStaff(eventId) {
  const res = await apiGet(`/events/${eventId}/staff`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load event staff');
  return res.data;
}

export async function inviteEventStaff(eventId, payload) {
  const res = await apiPost(`/events/${eventId}/staff/invite`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to invite staff');
  return res.data;
}

export async function updateEventStaff(eventId, userId, payload) {
  const res = await apiPut(`/events/${eventId}/staff/${userId}/assignments`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update staff assignments');
  return res.data;
}

export async function removeEventStaff(eventId, userId, assignmentType) {
  const res = await apiDelete(`/events/${eventId}/staff/${userId}/assignments/${assignmentType}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to remove staff assignment');
  return res.data;
}

export async function getCases(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/cases${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load cases');
  return res.data;
}

export async function createCase(payload) {
  const res = await apiPost('/cases', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create case');
  return res.data;
}

export async function updateCaseStatus(id, payload) {
  const res = await apiPatch(`/cases/${id}/status`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update case');
  return res.data;
}

export async function getMyMentorTeams(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/track-mentors/me/teams${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load mentor teams');
  return res.data;
}

export async function getMentorFeedbacks(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/mentor-feedbacks${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load mentor feedback');
  return res.data;
}

export async function createMentorFeedback(payload) {
  const res = await apiPost('/mentor-feedbacks', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create mentor feedback');
  return res.data;
}

export async function updateMentorFeedback(id, payload) {
  const res = await apiPatch(`/mentor-feedbacks/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update mentor feedback');
  return res.data;
}

export async function deleteMentorFeedback(id) {
  const res = await apiDelete(`/mentor-feedbacks/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete mentor feedback');
}

export async function deleteTeam(id) {
  const res = await apiDelete(`/teams/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete team');
}

export async function getSubmissions(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/submissions${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load submissions');
  return res.data;
}

export async function getSubmission(id) {
  const res = await apiGet(`/submissions/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load submission');
  return res.data;
}

export async function upsertSubmission(payload) {
  const res = await apiPost('/submissions', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to save submission');
  return res.data;
}

export async function updateSubmission(id, payload) {
  const res = await apiPatch(`/submissions/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update submission');
  return res.data;
}

export async function deleteSubmission(id) {
  const res = await apiDelete(`/submissions/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete submission');
}

export async function getScores(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/scores${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load scores');
  return res.data;
}

export async function upsertScore(payload) {
  const res = await apiPost('/scores', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to save score');
  return res.data;
}

export async function getRoundCriteria(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/round-criteria${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load round criteria');
  return res.data;
}

export async function createRoundCriterion(payload) {
  const res = await apiPost('/round-criteria', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create round criterion');
  return res.data;
}

export async function updateRoundCriterion(id, payload) {
  const res = await apiPatch(`/round-criteria/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update round criterion');
  return res.data;
}

export async function deleteRoundCriterion(id) {
  const res = await apiDelete(`/round-criteria/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete round criterion');
}

export async function getCriteriaTemplates(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/criteria-templates${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load criteria templates');
  return res.data;
}

export async function createCriteriaTemplate(payload) {
  const res = await apiPost('/criteria-templates', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create criteria template');
  return res.data;
}

export async function getJudgeSubmissions(judgeId, params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/round-judges/judges/${judgeId}/submissions${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load judge submissions');
  return res.data;
}

export async function getRoundRankings(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/round-rankings${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load rankings');
  return res.data;
}

export async function recalculateRoundRankings(roundId, payload = {}) {
  const res = await apiPost(`/round-rankings/rounds/${roundId}/recalculate`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to recalculate rankings');
  return res.data;
}

export async function getPrizes(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/prizes${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load prizes');
  return res.data;
}

export async function getPrize(id) {
  const res = await apiGet(`/prizes/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load prize');
  return res.data;
}

export async function createPrize(payload) {
  const res = await apiPost('/prizes', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create prize');
  return res.data;
}

export async function updatePrize(id, payload) {
  const res = await apiPatch(`/prizes/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update prize');
  return res.data;
}

export async function deletePrize(id) {
  const res = await apiDelete(`/prizes/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete prize');
}

export async function getIncidents(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/incidents${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load incidents');
  return res.data;
}

export async function getIncident(id) {
  const res = await apiGet(`/incidents/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load incident');
  return res.data;
}

export async function createIncident(payload) {
  const res = await apiPost('/incidents', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create incident');
  return res.data;
}

export async function updateIncidentStatus(id, payload) {
  const res = await apiPatch(`/incidents/${id}/status`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update incident status');
  return res.data;
}

export async function addIncidentEvidence(id, payload) {
  const res = await apiPost(`/incidents/${id}/evidences`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to add evidence');
  return res.data;
}

export async function addIncidentAction(id, payload) {
  const res = await apiPost(`/incidents/${id}/actions`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to add action');
  return res.data;
}

export async function getJudgeVariance(eventId, roundId, trackId) {
  const qs = new URLSearchParams({ eventId, ...(trackId ? { trackId } : {}) }).toString();
  const res = await apiGet(`/reports/rounds/${roundId}/judge-variance?${qs}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load judge variance');
  return res.data;
}

export async function getAnonymizedDataset(roundId) {
  const res = await apiGet(`/reports/rounds/${roundId}/anonymized-dataset`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load anonymized dataset');
  return res.data;
}

export async function downloadRankingCsv(roundId) {
  const res = await apiDownload(`/reports/rounds/${roundId}/ranking.csv`);
  if (!res.ok) throw new Error(res.text || 'Failed to download ranking CSV');
  return res.text;
}

export async function getAuditLogs(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/audit-logs${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load audit logs');
  return res.data;
}

export async function getRoundParticipants(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/round-participants${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load round participants');
  return res.data;
}

export async function getNotices(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/notices${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load notices');
  return res.data;
}

export async function createNotice(payload) {
  const res = await apiPost('/notices', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create notice');
  return res.data;
}

export async function joinTeamByInviteCode(inviteCode) {
  const res = await apiPost('/teams/join', { inviteCode });
  if (!res.ok) throw new Error(res.data?.message || 'Failed to join team');
  return res.data;
}

export async function leaveTeam(teamId) {
  const res = await apiPost(`/teams/${teamId}/leave`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to leave team');
  return res.data;
}

// --- Notifications (red-dot / unread counts) ---

export async function getNotifications(unreadOnly = false) {
  const res = await apiGet(`/notifications${unreadOnly ? '?unreadOnly=true' : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load notifications');
  return res.data;
}

export async function getUnreadSummary() {
  const res = await apiGet('/notifications/unread-summary');
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load unread summary');
  return res.data;
}

export async function markNotificationRead(id) {
  const res = await apiPost(`/notifications/${id}/read`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to mark notification read');
}

export async function markAllNotificationsRead(category) {
  const qs = category ? `?category=${encodeURIComponent(category)}` : '';
  const res = await apiPost(`/notifications/read-all${qs}`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to mark notifications read');
}

// --- Team join requests (request-to-join flow) ---

export async function createJoinRequest(teamId, message) {
  const res = await apiPost('/join-requests', { teamId, message: message || null });
  if (!res.ok) throw new Error(res.data?.message || 'Failed to send join request');
  return res.data;
}

export async function getTeamJoinRequests(teamId, status) {
  const qs = new URLSearchParams({ teamId, ...(status ? { status } : {}) }).toString();
  const res = await apiGet(`/join-requests?${qs}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load join requests');
  return res.data;
}

export async function getMyJoinRequests() {
  const res = await apiGet('/join-requests/mine');
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load your join requests');
  return res.data;
}

export async function acceptJoinRequest(id) {
  const res = await apiPost(`/join-requests/${id}/accept`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to accept join request');
  return res.data;
}

export async function rejectJoinRequest(id) {
  const res = await apiPost(`/join-requests/${id}/reject`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to reject join request');
  return res.data;
}

export async function cancelJoinRequest(id) {
  const res = await apiDelete(`/join-requests/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to cancel join request');
}

export async function getTeamChatMessages(teamId) {
  const res = await apiGet(`/team-chat?teamId=${encodeURIComponent(teamId)}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load team chat');
  return res.data;
}

export async function sendTeamChatMessage(teamId, message) {
  const res = await apiPost('/team-chat', { teamId, message });
  if (!res.ok) throw new Error(res.data?.message || 'Failed to send message');
  return res.data;
}

export async function createSupportTicket(payload) {
  const res = await apiPost('/support-tickets', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to submit ticket');
  return res.data;
}

export async function getSupportTickets(requesterId) {
  const qs = requesterId ? `?requesterId=${encodeURIComponent(requesterId)}` : '';
  const res = await apiGet(`/support-tickets${qs}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load support tickets');
  return res.data;
}

export async function updateSupportTicketStatus(id, status) {
  const res = await apiPatch(`/support-tickets/${id}/status`, { status });
  if (!res.ok) {
    const message = res.status === 400
      ? res.data?.message || 'That status transition is not allowed. Refresh the ticket list and try again.'
      : res.data?.message || 'Failed to update ticket status';
    throw new Error(message);
  }
  return res.data;
}

export async function getMyJudgeSubmissions(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/round-judges/me/submissions${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load judge submissions');
  return res.data;
}

// ============================================================================
// Team Journey Timeline (dòng thời gian hành trình của đội)
// ----------------------------------------------------------------------------
// BE trả về danh sách các mốc: tạo đội, thăng hạng, bị loại, khiếu nại, giải thưởng.
// ============================================================================

// Lấy timeline của (các) đội mà user hiện tại đang tham gia. eventId là tùy chọn để lọc theo sự kiện.
export async function getMyTeamTimeline(eventId) {
  const teams = await getMyTeams();
  const team = Array.isArray(teams) ? teams[0] : teams?.content?.[0];
  if (!team?.id) return [];
  const qs = eventId ? `?eventId=${encodeURIComponent(eventId)}` : '';
  const res = await apiGet(`/teams/${team.id}/timeline${qs}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load team timeline');
  return res.data?.content || res.data || [];
}

// Lấy timeline của một đội cụ thể (EC xem mọi đội; thí sinh chỉ xem đội mình).
export async function getTeamTimeline(teamId) {
  const res = await apiGet(`/teams/${teamId}/timeline`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load team timeline');
  return res.data?.content || res.data || [];
}

// EC xem toàn bộ mốc của mọi đội trong một sự kiện (có phân trang).
export async function getEventTimeline(eventId, params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/events/${eventId}/timeline${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load event timeline');
  return res.data;
}

// ============================================================================
// Appeals — khiếu nại kết quả với cửa sổ 15 phút
// ----------------------------------------------------------------------------
// EC công bố kết quả -> mở cửa sổ 15 phút; thí sinh nộp khiếu nại trong cửa sổ đó.
// Backend luôn kiểm tra deadline, FE chỉ hiển thị countdown cho tiện.
// ============================================================================

// EC công bố kết quả vòng thi và mở cửa sổ khiếu nại 15 phút.
export async function publishRoundResults(eventId, roundId) {
  const res = await apiPost(`/events/${eventId}/rounds/${roundId}/publish-results`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Failed to publish round results');
  return res.data;
}

// Thí sinh (leader/member) nộp khiếu nại. Đội được suy ra từ tư cách thành viên ở BE.
export async function createAppeal(payload) {
  const res = await apiPost('/appeals', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to submit appeal');
  return res.data;
}

// EC duyệt danh sách khiếu nại theo sự kiện / vòng / trạng thái (có phân trang).
export async function getAppeals(params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await apiGet(`/appeals${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load appeals');
  return res.data;
}

// Xem lại các khiếu nại của một đội (EC xem mọi đội; thành viên xem đội mình).
export async function getTeamAppeals(teamId) {
  const res = await apiGet(`/appeals/teams/${teamId}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load team appeals');
  return res.data;
}

// Chi tiết một đơn khiếu nại (EC).
export async function getAppeal(id) {
  const res = await apiGet(`/appeals/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load appeal');
  return res.data;
}

// EC ghi phản hồi trung gian; đơn vẫn ở trạng thái PENDING.
export async function respondToAppeal(id, response) {
  const res = await apiPost(`/appeals/${id}/respond`, { response });
  if (!res.ok) throw new Error(res.data?.message || 'Failed to respond to appeal');
  return res.data;
}

// EC chốt kết luận: status = 'ACCEPTED' hoặc 'REJECTED'.
export async function resolveAppeal(id, payload) {
  const res = await apiPost(`/appeals/${id}/resolve`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to resolve appeal');
  return res.data;
}

export async function resumeRound(eventId, roundId) {
  const res = await apiPost(`/events/${eventId}/rounds/${roundId}/resume`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Round is not ready to resume');
  return res.data;
}

export async function advanceRound(eventId, roundId) {
  const res = await apiPost(`/events/${eventId}/rounds/${roundId}/advance`, {});
  if (!res.ok) throw new Error(res.data?.message || 'Round is not ready to advance');
  return res.data;
}

// ============================================================================
// Event Rules & Rule Acceptances — thể lệ sự kiện + xác nhận chấp thuận
// ----------------------------------------------------------------------------
// Thí sinh chỉ thấy rule PUBLIC. Checkbox "I agree" gọi acceptEventRules.
// ============================================================================

// Danh sách thể lệ của một sự kiện (thí sinh chỉ nhận rule PUBLIC).
export async function getEventRules(eventId) {
  const res = await apiGet(`/event-rules?eventId=${encodeURIComponent(eventId)}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load event rules');
  return res.data;
}

// EC tạo một điều luật mới.
export async function createEventRule(payload) {
  const res = await apiPost('/event-rules', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create rule');
  return res.data;
}

// EC cập nhật một điều luật.
export async function updateEventRule(id, payload) {
  const res = await apiPatch(`/event-rules/${id}`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to update rule');
  return res.data;
}

// EC xóa một điều luật.
export async function deleteEventRule(id) {
  const res = await apiDelete(`/event-rules/${id}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to delete rule');
}

// User chấp nhận thể lệ của một sự kiện (idempotent). payload = { eventId, accepted: true }.
export async function acceptEventRules(payload) {
  const res = await apiPost('/event-rules/acceptances', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to accept rules');
  return res.data;
}

// Kiểm tra user hiện tại đã chấp nhận thể lệ chưa (để ẩn/hiện checkbox).
export async function getMyRuleAcceptance(eventId) {
  const res = await apiGet(`/event-rules/acceptances/me?eventId=${encodeURIComponent(eventId)}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to check rule acceptance');
  return res.data;
}

// ============================================================================
// Prize Revisions — lịch sử thu hồi / chuyển giải thưởng (không xóa cứng)
// ============================================================================

// EC thu hồi giải thưởng khỏi đội hiện tại. payload = { reason, evidenceNote? }.
export async function revokePrize(id, payload) {
  const res = await apiPost(`/prizes/${id}/revoke`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to revoke prize');
  return res.data;
}

// EC chuyển giải thưởng sang đội khác. payload = { newTeamId, reason, evidenceNote? }.
export async function reassignPrize(id, payload) {
  const res = await apiPost(`/prizes/${id}/reassign`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to reassign prize');
  return res.data;
}

// Lịch sử chỉnh sửa của một giải thưởng.
export async function getPrizeRevisions(id) {
  const res = await apiGet(`/prizes/${id}/revisions`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load prize revisions');
  return res.data;
}

// ============================================================================
// Tie-break Decisions — quyết định phân định hòa thủ công (review GitHub)
// ============================================================================

// EC ghi nhận một quyết định phân định hòa thủ công cho một đội trong vòng.
export async function createTieBreakDecision(roundId, payload) {
  const res = await apiPost(`/round-rankings/rounds/${roundId}/tie-break-decisions`, payload);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to create tie-break decision');
  return res.data;
}

// Danh sách quyết định phân định hòa thủ công của một vòng.
export async function getTieBreakDecisions(roundId) {
  const res = await apiGet(`/round-rankings/rounds/${roundId}/tie-break-decisions`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load tie-break decisions');
  return res.data;
}
