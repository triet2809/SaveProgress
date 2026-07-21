# Database migration manifest

This manifest describes the standalone SQL files in this backend repository.
The supported starting point for current development is:

`database/db_query_tool_friendly.sql`

That dump is already a composed/post-gapfill schema. It contains the columns
and tables introduced by `migration_gapfill.sql`, `migration_gapfill2.sql`,
and `migration_gapfill3.sql` (and additional later gapfill content). Those
standalone gapfill files must therefore not be replayed against the current
canonical dump.

The dump may leave the session `search_path` empty. All active migrations
listed below now set `search_path TO public` themselves and qualify their
application table references where practical. No manual search-path command
is required before running an active migration. The historical gapfill files
remain unchanged and retain their original execution assumptions.

Batch 11 adds the persisted per-user authentication security version. Apply it
before starting the upgraded application. Tokens issued before this migration
do not contain a security-version claim and are intentionally invalid after
the new filter is deployed; users must authenticate again.

Batch 12 completes the event-level logical-round model. It adds shared final,
promotion-default, and lifecycle metadata; enforces event-scoped logical name
and sequence uniqueness; and stores promotion separately from next-round track
assignment. Existing operational round IDs and their result references are
unchanged. The migration deliberately fails before altering schema if existing
logical definitions contain duplicate event/sequence or case-insensitive
event/name values.

Competition setup now requires a `roundPlan`. Each logical-round item declares
its own track list, final flag, and optional default/per-track promotion counts.
The legacy `tracks`/`roundCount` funnel fields are no longer sufficient because
they cannot express changing round-specific track structures.

Batch 13 adds immutable published-result provenance to every logical promotion.
It stores the exact result version and result entry IDs, validates that the
entry is promoted for the same team and belongs to the active published
execution of the source logical round, and rejects provenance mutation. Legacy
promotion rows are backfilled only when exactly one valid candidate can be
proven; unresolved rows abort the migration with a count and must be resolved
explicitly before retrying.

Batch 14 is a data-compatibility correction for databases populated by older
seed/import scripts. Those scripts used `PUBLISHED` for both operational and
logical round lifecycle state, while the current application represents the
same post-publication phase as `APPEAL_WINDOW_OPEN`. The migration normalizes
only that legacy value and is transactional and repeatable.

The two files named `seal_hackathon_full_*.sql` are dump artifacts, not
migrations. `seal_hackathon_full_query_tool.sql` explicitly contains embedded
gapfill sections and is also not a migration stream to run together with the
standalone files.

## Canonical order from `database/db_query_tool_friendly.sql`

Run only the following standalone migrations, in this order:

1. `migration_batch1_defaults.sql`
2. `migration_batch2_event_scoping.sql`
3. `migration_batch3_activation.sql`
4. `migration_batch3_staff_assignments.sql`
5. `migration_batch5_appeals_lifecycle.sql`
6. `migration_batch6_team_profiles.sql`
7. `migration_batch7_historical_finishes_seeding.sql`
8. `migration_batch8_team_recognitions.sql`
9. `migration_batch9_timeline_foundation.sql`
10. `migration_batch10_onboarding_terms.sql`
11. `migration_batch10_logical_rounds.sql`
12. `migration_batch11_auth_security_version.sql`
13. `migration_batch12_logical_round_integrity.sql`
14. `migration_batch13_promotion_provenance.sql`
15. `migration_batch14_round_lifecycle_compatibility.sql`
16. `migration_batch15_unassigned_teams_campuses.sql`

Do not run `migration_gapfill.sql`, `migration_gapfill2.sql`, or
`migration_gapfill3.sql` after the current canonical dump; their schema changes
are already present.

## Migration dependency table

| File | Purpose | Objects affected | Dependencies | Required from current canonical dump? | Repeatable/safe | Overlap | Recommended order |
|---|---|---|---|---|---|---|---|
| `migration_batch1_defaults.sql` | Normalize nullable legacy values to application defaults. | Updates `submissions.review_status` and `round_criteria.status`. | Requires both columns to exist. | Yes, if existing rows may contain NULL; no schema change is needed. | Yes; updates only NULL rows. | Column creation/defaults overlap `migration_gapfill.sql`, but the data normalization does not. | 1 |
| `migration_batch2_event_scoping.sql` | Add the `PUBLISH_RESULTS` audit action enum value when absent. | `public.audit_action` enum. | Requires canonical `audit_action` type. | Yes; current canonical enum does not contain `PUBLISH_RESULTS`. | Yes; guarded by catalog checks. | No material overlap with other standalone migrations. | 2 |
| `migration_batch3_activation.sql` | Add single-use hashed account activation tokens. | `account_activation_tokens`, indexes `idx_activation_user`, `idx_activation_expires`. | Requires `users` and `pgcrypto`/`gen_random_uuid()`. | Yes; table is absent from current canonical dump. | Yes; `IF NOT EXISTS` guards tables/indexes. | No overlap with staff or gapfill migrations. | 3 |
| `migration_batch3_staff_assignments.sql` | Remove event/user uniqueness that prevents one staff member working on multiple tracks. | Drops `uq_track_mentors_event_user` and `uq_track_judges_event_user`. | Requires `track_mentors` and `track_judges` plus their constraints. | Yes; current canonical dump contains both constraints. | Yes; catalog and `IF EXISTS` guarded. | It reverses the event/user constraint created by `migration_gapfill2.sql`. | 4 |
| `migration_batch5_appeals_lifecycle.sql` | Add immutable result publication versions, appeal version linkage, and round lifecycle state/versioning. | `round_result_versions`, `round_result_version_entries`, appeals columns/index, rounds columns. | Requires `rounds`, `appeals`, `teams`, `users`, and `pgcrypto`. Must follow all earlier application migrations. | Yes; result-version tables and lifecycle columns are absent. | Yes for fresh/repeated application; `IF NOT EXISTS` guards tables/columns/indexes. Existing appeal rows remain with nullable `result_version_id`. | No duplicate standalone migration found. | 5 |
| `migration_batch6_team_profiles.sql` | Add persistent team profiles, backfill one profile per legacy registration, and enforce cross-event registration integrity. | `team_profiles`; profile/source/roster columns and indexes on `teams`; validation trigger/function. | Requires `teams`, `team_members`, `tracks`, `users`, and all earlier active migrations. | Yes. | Guarded and safe to rerun; backfill touches only teams without a profile and never merges names. Trigger/function definitions are replaceable. | No duplicate migration. | 6 |
| `migration_batch7_historical_finishes_seeding.sql` | Add immutable final-finish snapshots and coordinator-controlled event seed metadata. | `event_team_finishes`, `event_seed_assignments`, indexes, hierarchy/immutability triggers, audit enum values. | Requires Batch 5 immutable result versions and Batch 6 team profiles. | Yes. | Guarded DDL and replaceable triggers/functions; it performs no historical finish backfill. | No duplicate migration. | 7 |
| `migration_batch8_team_recognitions.sql` | Add persistent, auditable team-profile recognition records without automatic SQL awards, including explicit restore auditing. | `team_recognitions`, partial active-recognition uniqueness, indexes, recognition audit enum values. | Requires Batch 6 team profiles, Batch 7 immutable finishes, and `users`. | Yes. | Guarded table/index/enum DDL; no existing row is rewritten and no mutable history is used for backfill. | No duplicate migration. | 8 |
| `migration_batch9_timeline_foundation.sql` | Extend the legacy user-facing team timeline with visibility scopes, typed source references, JSONB metadata, optional team association, and idempotency. | `team_timeline_events`, scope/source columns, nullable `team_id`, track FK, and chronological/type/idempotency indexes. | Requires canonical `team_timeline_events`; follows Batch 1–8. | Yes; the canonical table lacks these guarantees. | Yes; guarded columns/indexes/constraint and deterministic `event_type` backfill. | Intentionally extends the legacy timeline table; it does not overlap audit logs or gapfills. | 9 |
| `migration_batch10_onboarding_terms.sql` | Add temporary-password onboarding state and auditable terms/privacy acceptance versions. | `users.must_change_password`, `terms_accepted_at`, `terms_version`, `privacy_version`. | Requires `users`; follows Batch 1–9. | Yes. | Yes; guarded additive columns with a safe default. | No overlap with activation tokens; the legacy activation flow remains supported. | 10 |
| `migration_batch10_logical_rounds.sql` | Add event-level logical round definitions and backfill existing per-track rounds without changing operational round IDs. | `round_definitions`; `rounds.logical_round_id`; FK and logical-round/track uniqueness. | Requires `events`, `tracks`, and `rounds`; follows the current canonical dump and Batch 1–9. | Yes. | Yes; guarded DDL, repeatable backfill, and null-only updates. | It normalizes round identity but does not replace per-track execution rows. | 11 |
| `migration_batch11_auth_security_version.sql` | Add the per-user security version used to invalidate credentials and security-sensitive sessions. | `users.security_version`, default, and non-null constraint. | Requires `users`; follows Batch 1–10. | Yes. | Yes; transactional, guarded, and backfills only NULL values. | Complements revoked-token storage; it does not replace it. | 12 |
| `migration_batch12_logical_round_integrity.sql` | Enforce event-level logical-round identity and separate promotion from later track assignment. | `round_definitions` shared metadata/uniqueness; `logical_round_promotions`, indexes, and adjacency trigger. | Requires Batch 10 logical rounds and Batch 6 team identity; follows Batch 11 in canonical deployment order. | Yes. | Yes after duplicate preflight succeeds; transactional, guarded DDL, and replaceable trigger/function. Operational round IDs are untouched. | Extends Batch 10 without replacing its backfill. | 13 |
| `migration_batch13_promotion_provenance.sql` | Persist and validate the exact published result version and promoted result entry behind each logical promotion. | `logical_round_promotions` provenance columns/FKs/triggers; result-entry composite uniqueness. | Requires Batch 5 result-version tables and Batch 12 logical promotions. | Yes. | Repeatable only when all legacy rows are resolvable; otherwise fails transactionally with unresolved-row count. No provenance is invented. | Extends Batch 12 promotion state without changing operational round IDs. | 14 |
| `migration_batch14_round_lifecycle_compatibility.sql` | Normalize the legacy `PUBLISHED` round lifecycle value to the current `APPEAL_WINDOW_OPEN` state. | Updates `rounds.lifecycle_state` and `round_definitions.lifecycle_state`. | Requires Batch 5 lifecycle columns and Batch 10 logical rounds. | Yes when older seed/import data is present; otherwise it is a no-op. | Yes; transactional and updates only the legacy value. | Data compatibility only; no schema overlap. | 15 |
| `migration_batch15_unassigned_teams_campuses.sql` | Restore all five FPT campuses and separate team event registration from later track allocation. | Adds/backfills `teams.event_id`, makes `teams.track_id` nullable, updates team-profile validation, and inserts missing FPT campuses. | Requires `universities`, `campuses`, `events`, `tracks`, teams, and Batch 6 team profiles. | Yes. | Transactional and guarded; existing team event identity is backfilled only from its current track. | Extends the Batch 6 registration-integrity trigger. | 16 |
| `migration_gapfill.sql` | Add event date/prize fields, submission metadata, incident fields, user contact fields, criterion status, and notice target team. | Alters `events`, `submissions`, `incident_reports`, `users`, `round_criteria`, `notices`. | Requires base dump tables. | No; these changes are already in the current canonical dump. | Mostly yes; transactional and `IF NOT EXISTS`. | Overlaps current canonical dump and Batch 1’s two normalized columns. | Historical raw-dump order: 1 |
| `migration_gapfill2.sql` | Add mentor/judge profile fields and create track-level judge assignments. | Alters `users`; creates `track_judges` and indexes; creates `uq_track_judges_event_user`. | Requires `users`, `events`, `tracks`; must precede staff-constraint removal if starting from a raw base dump. | No; already present in current canonical dump. | Yes for schema replay; uniqueness can fail if pre-existing duplicate data exists. | Directly overlaps current canonical dump and is the prerequisite for Batch 3 staff-assignment correction. | Historical raw-dump order: 2 |
| `migration_gapfill3.sql` | Add invite codes and team join requests. | Alters `teams`; creates `team_join_requests`, indexes, partial pending-request uniqueness. | Requires `teams` and `users`. | No; already present in current canonical dump. | Schema operations are guarded; NULL invite-code backfill is repeatable after completion. Random-code collisions are theoretically possible on a partially populated database. | Overlaps current canonical dump. | Historical raw-dump order: 3 |

## Gapfill explanation

The gapfill files are historical schema-completion scripts. The current
`database/db_query_tool_friendly.sql` already contains their resulting objects:

- gapfill 1 fields are present on events, submissions, incident reports,
  users, round criteria, and notices;
- gapfill 2 user profile fields and `track_judges` are present;
- gapfill 3 `teams.invite_code` and `team_join_requests` are present;
- the canonical dump also contains later gapfill-derived objects such as
  notifications, appeals, timeline/rule/prize-revision tables, and related
  fields.

`seal_hackathon_full_query_tool.sql` is a separate composed artifact whose
header states that gapfill1 through gapfill6 are embedded. It should not be
used as a base and then followed by the standalone gapfills.

## Conflicts or overlaps

1. **Batch 3 ordering conflict resolved:** if building from an older raw dump,
   `migration_gapfill2.sql` must run before
   `migration_batch3_staff_assignments.sql`. Otherwise Batch 3 staff
   migration sees no `track_judges` table, does nothing, and gapfill2 can later
   recreate the event/user uniqueness constraint that Batch 3 was intended to
   remove.
2. `migration_batch3_activation.sql` and
   `migration_batch3_staff_assignments.sql` do not depend on one another.
   They are ordered activation first for a deterministic Batch 3 sequence.
3. `migration_gapfill.sql` overlaps Batch 1 at the column/default level, but
   Batch 1 still has a distinct data-normalization purpose.
4. The current canonical dump already includes the gapfill results. Replaying
   gapfills against it is redundant and can create avoidable constraint/index
   conflicts.
5. `migration_batch5_appeals_lifecycle.sql` is not present in either canonical
   dump and must be applied once after the Batch 1–3 changes.

## Historical raw-dump order

If a database is created from the older raw dump
`seal_hackathon_full_2026-07-04.sql` rather than the current canonical dump,
the safe dependency order for the standalone files is:

1. `migration_gapfill.sql`
2. `migration_gapfill2.sql`
3. `migration_gapfill3.sql`
4. `migration_batch1_defaults.sql`
5. `migration_batch2_event_scoping.sql`
6. `migration_batch3_activation.sql`
7. `migration_batch3_staff_assignments.sql`
8. `migration_batch5_appeals_lifecycle.sql`
9. `migration_batch6_team_profiles.sql`
10. `migration_batch7_historical_finishes_seeding.sql`
11. `migration_batch8_team_recognitions.sql`
12. `migration_batch9_timeline_foundation.sql`

This historical stream is incomplete relative to the current canonical dump
because later gapfill4–6 content is embedded in the composed query-tool
artifact rather than present as standalone backend files. Do not mix those
artifacts casually.

## Static review result

- PostgreSQL syntax was reviewed statically: `DO $$ ... $$`, catalog checks,
  `ALTER TYPE ... ADD VALUE`, `CREATE INDEX ... WHERE`, `IF NOT EXISTS`,
  foreign keys, partial unique indexes, and transactional DDL are structurally
  valid for PostgreSQL 17.
- No migration was executed against an existing database.
- No standalone migration was removed or merged.
- The canonical dump was not modified.
- Active migrations are self-contained with respect to `search_path`; no
  manual session setup is required.
- No migration was executed against an existing database.
