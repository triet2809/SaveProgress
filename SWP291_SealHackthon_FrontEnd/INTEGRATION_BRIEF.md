# FE ↔ BE Integration Brief (backup-fe-old FE)

Goal: replace mock data in FE pages with real backend calls. Keep the existing visual
layout/JSX and CSS modules. Only swap data source: mock imports -> API calls, add
loading/error states, wire forms/buttons to real create/update/delete.

## Runtime facts
- BE base URL: from `src/config/registerConfig.js` `API_BASE_URL` (env `VITE_API_BASE_URL`
  = `http://192.168.100.214:8080/api`). NEVER hardcode a URL in pages.
- Auth token stored in localStorage `seal_access_token`; the api client attaches it.
- Logged-in user object in localStorage `seal_user` (see `src/utils/authUser.js`
  `getStoredUser()` -> { id, email, fullName, roles:[], universityId, campusId, ... }).

## Use the existing API layer — DO NOT write raw fetch in pages
Import from these modules:
- `src/api/client.js`: apiGet/apiPost/apiPatch/apiPut/apiDelete/apiDownload (usually not needed directly)
- `src/api/authApi.js`: login, registerFpt, registerExternal, logout, me
- `src/api/userApi.js`: getMe, updateMe, getUsers({role,status,search}), getPendingUsers,
  updateUserStatus, approveUser, rejectUser, updateUserRoles, addUserRole, removeUserRole
- `src/api/universityApi.js`: getCampuses(params)
- `src/api/hackathonApi.js`: full CRUD helpers. Available functions:
  events: getEvents,getEvent,createEvent,updateEvent,changeEventStatus,deleteEvent
  tracks (== FE "Category"): getTracks,getTrack,createTrack,updateTrack,deleteTrack
  rounds: getRounds,createRound,updateRound,deleteRound
  track-mentors: getTrackMentors,assignTrackMentor,deleteTrackMentor,getMentorTeams
  round-judges: getRoundJudges,assignRoundJudge,deleteRoundJudge,getJudgeSubmissions
  teams: getTeams,getTeam,getMyTeams,createTeam,updateTeam,addTeamMember,removeTeamMember,
    disqualifyTeam,reactivateTeam,deleteTeam,joinTeamByInviteCode
  mentor feedback: getMentorFeedbacks,createMentorFeedback,updateMentorFeedback,deleteMentorFeedback
  submissions: getSubmissions,getSubmission,upsertSubmission,updateSubmission,deleteSubmission
  scores: getScores,upsertScore
  round criteria: getRoundCriteria,createRoundCriterion,updateRoundCriterion,deleteRoundCriterion
  criteria templates: getCriteriaTemplates,createCriteriaTemplate
  rankings: getRoundRankings,recalculateRoundRankings
  prizes (== FE "Awards"): getPrizes,getPrize,createPrize,updatePrize,deletePrize
  incidents: getIncidents,getIncident,createIncident,updateIncidentStatus,addIncidentEvidence,addIncidentAction
  reports: getJudgeVariance,getAnonymizedDataset,downloadRankingCsv
  audit: getAuditLogs
  round participants: getRoundParticipants
  notices: getNotices,createNotice
  team chat: getTeamChatMessages,sendTeamChatMessage
  support tickets: createSupportTicket

## Concept mapping (FE label -> BE)
- FE "Category" == BE Track (`/tracks`); track needs `eventId`.
- FE "Awards" == BE Prize (`/prizes`).
- Round needs `trackId`. Round-criteria & round-rankings need `roundId` query param (mandatory).
- Users list supports `?status=` and `?email=` only; role filtering is client-side (userApi already does it).

## Response shapes
- List endpoints are Spring `Page`: `{ content:[...], totalElements, ... }`. Helpers already
  return `res.data`; in pages use `data.content || data`. userApi already unwraps to `.value`.
- Auth/me: `{ id, email, roles:[] }`. users/me returns UserResponse.
- Enums are lowercase-ish strings from BE (status: approved/pending/rejected; event status etc).
- When BE lacks a field the mock had (e.g. progress %, colors), keep a sensible default/derived
  value in the page; do not invent new BE calls.

## Rules
- Keep JSX structure + className usage intact so styling stays.
- Add `useState`/`useEffect` to load data; show a Spinner while loading and an Alert on error
  (react-bootstrap already used).
- Guard against empty/undefined arrays.
- If a page's data has NO backend equivalent (pure static marketing/among mock), leave it but
  prefer real data where an endpoint exists.
- Do NOT run `npm run build` (parallel builds clobber). Verify each edited file's syntax with:
  `npx esbuild <file> --loader=jsx --format=esm >/dev/null` (esbuild ships with vite).
- Do NOT edit files outside your assigned page list except the page's own imports.
- Report: list of files changed + which endpoints wired + any BE gaps you hit.
