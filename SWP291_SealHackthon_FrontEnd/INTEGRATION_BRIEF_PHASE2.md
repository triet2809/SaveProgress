# Phase 2 — Restore lost UI + wire to NEW backend fields

Context: earlier wiring dropped UI fields that had no BE equivalent. BE now has those
fields (Phase 1 done). Restore the ORIGINAL UI from the pristine backup-fe-old and wire
each restored field to the real backend. Pristine copy is at /tmp/bfo/src/pages/... —
use `diff /tmp/bfo/src/pages/<file> src/pages/<file>` to see exactly what was removed,
then re-add those JSX blocks/columns/inputs on top of the CURRENT wired file (do NOT
overwrite the whole file — keep the existing API wiring that already works).

Follow /Users/mac/.openclaw/workspace/hackathonproject/fe/INTEGRATION_BRIEF.md for the
API helper layer and rules. BE base is env-driven (already configured). BE is live at
http://192.168.100.214:8080/api; login POST /api/auth/login {"email":"coordinator@seal.local","password":"Password123!"}.

## NEW backend fields now available (exact JSON key names)

Event (GET /events, /events/{id}; POST/PATCH accept these):
- term (String), prizePool (String)
- registrationStart, registrationEnd, eventStart, eventEnd (ISO datetime strings, e.g. "2026-06-20T00:00:00")
- tracksCount (int), roundsCount (int), participantsCount (int)  -- READ-ONLY, derived by BE
Create/update payload: send term, prizePool, registrationStart, registrationEnd,
eventStart, eventEnd alongside title/description. Counts are derived; do NOT send them.

Submission (GET /submissions, POST /submissions upsert):
- projectName (String), version (String), reviewStatus (String, e.g. pending/reviewed)
Send these in the upsert payload; they round-trip in the response.

Incident (GET /incidents, /incidents/{id}; POST /incidents):
- severity (String, e.g. low/medium/high/critical), category (String)
Send severity+category on create; they appear in the response.

User profile (GET /users/me; PATCH /users/me):
- phone (String), department (String), position (String)
PATCH /users/me now accepts { fullName?, phone?, department?, position? }.
The user object (also in localStorage seal_user, and userApi getMe) returns these keys.

RoundCriterion (GET /round-criteria?roundId=; POST/PATCH):
- status (String, default "active")
Send status on create/update; appears in response.

Notice (GET /notices; POST /notices):
- targetTeamId (UUID, optional) — for targeting a specific team.
Send targetTeamId when targeting a team.

## Field mapping note (FE label -> BE key)
- "Term / Semester" -> term ; "Prize Pool" -> prizePool
- "Number of Rounds" -> roundsCount (read-only display) ; "Number of Tracks" -> tracksCount (read-only)
- "Participants" -> participantsCount (read-only)
- "Registration Start/End" -> registrationStart/registrationEnd
- "Event Start/End" -> eventStart/eventEnd
- Datetime inputs: FE uses <input type="date"> (yyyy-mm-dd). When sending to BE append
  "T00:00:00" if BE needs full datetime; when displaying, slice first 10 chars.
- Criteria "Applicable Category" == the track; "Weight (%)" -> weight ; "Status" -> status
- Submission "Project Name"->projectName, "Version"->version, "Review Status"->reviewStatus
- Incident "Severity"->severity ; category->category
- Profile phone/department/position -> same keys (now editable & persisted)

## Rules
- Merge, don't overwrite: keep existing working API calls; ADD BACK the removed inputs/columns and bind them.
- Restore validation logic that existed in pristine (e.g. event date ordering) where sensible.
- Rounds/Tracks/Participants counts are read-only (BE derives) — show them, don't make them editable inputs on the create form UNLESS pristine had them as inputs; if pristine had them as create-form inputs, keep the inputs but they won't persist (counts are derived) — prefer showing derived counts in the table instead. Use judgment; note what you did.
- Keep JSX structure + className so styling stays.
- Verify each edited file compiles: npx esbuild <file> --loader=jsx --format=esm >/dev/null
- Do NOT run npm run build (parent builds once at end).
- Report per file: what UI you restored + which BE field each control is bound to.
