--
-- PostgreSQL database dump
--

\restrict c65MpVmowLeGWtftMpEEH5cJgaE30oLYSShBEFbf4khUhK56HSc0Z1syM1AxoDV

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS '';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: account_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.account_status AS ENUM (
    'pending',
    'approved',
    'rejected'
);


ALTER TYPE public.account_status OWNER TO postgres;

--
-- Name: audit_action; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.audit_action AS ENUM (
    'CREATE',
    'UPDATE',
    'DELETE',
    'APPROVE_USER',
    'REJECT_USER',
    'SUBMIT_PROJECT',
    'SCORE_SUBMISSION',
    'UPDATE_SCORE',
    'DISQUALIFY_TEAM',
    'PROMOTE_TEAM',
    'ELIMINATE_TEAM',
    'CREATE_INCIDENT',
    'PROCESS_INCIDENT',
    'RESOLVE_INCIDENT',
    'REJECT_INCIDENT',
    'PUBLISH_RESULTS',
    'FINALIZE_RESULTS',
    'ASSIGN_SEED',
    'REMOVE_SEED',
    'TEAM_RECOGNITION_AWARDED',
    'TEAM_RECOGNITION_REVOKED',
    'TEAM_RECOGNITION_RESTORED'
);


ALTER TYPE public.audit_action OWNER TO postgres;

--
-- Name: event_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.event_status AS ENUM (
    'draft',
    'published',
    'ongoing',
    'completed',
    'cancelled'
);


ALTER TYPE public.event_status OWNER TO postgres;

--
-- Name: incident_action_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.incident_action_type AS ENUM (
    'warning',
    'require_resubmission',
    'score_adjustment',
    'disqualify_team',
    'reject_report',
    'other'
);


ALTER TYPE public.incident_action_type OWNER TO postgres;

--
-- Name: incident_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.incident_status AS ENUM (
    'reported',
    'under_review',
    'resolved',
    'rejected'
);


ALTER TYPE public.incident_status OWNER TO postgres;

--
-- Name: incident_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.incident_type AS ENUM (
    'cheating',
    'plagiarism',
    'invalid_submission',
    'rule_violation',
    'technical_issue',
    'other'
);


ALTER TYPE public.incident_type OWNER TO postgres;

--
-- Name: promotion_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.promotion_status AS ENUM (
    'pending',
    'promoted',
    'eliminated',
    'disqualified'
);


ALTER TYPE public.promotion_status OWNER TO postgres;

--
-- Name: round_participant_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.round_participant_status AS ENUM (
    'pending',
    'active',
    'promoted',
    'eliminated',
    'disqualified'
);


ALTER TYPE public.round_participant_status OWNER TO postgres;

--
-- Name: student_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.student_type AS ENUM (
    'fpt',
    'external',
    'none'
);


ALTER TYPE public.student_type OWNER TO postgres;

--
-- Name: team_member_role; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.team_member_role AS ENUM (
    'leader',
    'member'
);


ALTER TYPE public.team_member_role OWNER TO postgres;

--
-- Name: team_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.team_status AS ENUM (
    'active',
    'disqualified'
);


ALTER TYPE public.team_status OWNER TO postgres;

--
-- Name: reject_event_team_finish_mutation(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.reject_event_team_finish_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'Historical finish snapshots are immutable'
        USING ERRCODE = '55000';
END $$;


ALTER FUNCTION public.reject_event_team_finish_mutation() OWNER TO postgres;

--
-- Name: reject_promotion_provenance_mutation(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.reject_promotion_provenance_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.source_result_version_id <> OLD.source_result_version_id
       OR NEW.source_result_entry_id <> OLD.source_result_entry_id THEN
        RAISE EXCEPTION 'Logical promotion result provenance is immutable';
    END IF;
    RETURN NEW;
END $$;


ALTER FUNCTION public.reject_promotion_provenance_mutation() OWNER TO postgres;

--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO postgres;

--
-- Name: validate_event_seed_assignment(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_event_seed_assignment() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    team_track_id uuid;
    team_profile uuid;
    track_event_id uuid;
    source_profile uuid;
BEGIN
    SELECT t.track_id, t.team_profile_id
      INTO team_track_id, team_profile
      FROM public.teams t WHERE t.id = NEW.team_id;
    SELECT tr.event_id INTO track_event_id
      FROM public.tracks tr WHERE tr.id = NEW.track_id;
    IF team_track_id IS NULL OR team_track_id <> NEW.track_id
       OR team_profile IS NULL OR team_profile <> NEW.team_profile_id
       OR track_event_id IS NULL OR track_event_id <> NEW.event_id THEN
        RAISE EXCEPTION 'Seed assignment hierarchy mismatch' USING ERRCODE = '23514';
    END IF;
    IF NEW.candidate_source_finish_id IS NOT NULL THEN
        SELECT f.team_profile_id INTO source_profile
          FROM public.event_team_finishes f
         WHERE f.id = NEW.candidate_source_finish_id;
        IF source_profile IS NULL OR source_profile <> NEW.team_profile_id THEN
            RAISE EXCEPTION 'Seed source finish must belong to the same profile'
                USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END $$;


ALTER FUNCTION public.validate_event_seed_assignment() OWNER TO postgres;

--
-- Name: validate_event_team_finish(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_event_team_finish() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    team_track_id uuid;
    team_profile uuid;
    track_event_id uuid;
    round_track_id uuid;
    version_round_id uuid;
    version_status varchar(20);
    highest_sequence integer;
    selected_sequence integer;
BEGIN
    SELECT t.track_id, t.team_profile_id
      INTO team_track_id, team_profile
      FROM public.teams t WHERE t.id = NEW.team_id;
    SELECT tr.event_id INTO track_event_id
      FROM public.tracks tr WHERE tr.id = NEW.track_id;
    SELECT r.track_id, r.sequence_number
      INTO round_track_id, selected_sequence
      FROM public.rounds r WHERE r.id = NEW.final_round_id;
    SELECT rv.round_id, rv.status
      INTO version_round_id, version_status
      FROM public.round_result_versions rv WHERE rv.id = NEW.result_version_id;
    SELECT max(r.sequence_number) INTO highest_sequence
      FROM public.rounds r WHERE r.track_id = NEW.track_id;

    IF team_track_id IS NULL OR team_track_id <> NEW.track_id
       OR team_profile IS NULL OR team_profile <> NEW.team_profile_id
       OR track_event_id IS NULL OR track_event_id <> NEW.event_id
       OR round_track_id IS NULL OR round_track_id <> NEW.track_id
       OR version_round_id IS NULL OR version_round_id <> NEW.final_round_id THEN
        RAISE EXCEPTION 'Historical finish hierarchy mismatch' USING ERRCODE = '23514';
    END IF;
    IF version_status <> 'published' THEN
        RAISE EXCEPTION 'Historical finish requires an active published result version'
            USING ERRCODE = '23514';
    END IF;
    IF highest_sequence IS NULL OR selected_sequence <> highest_sequence THEN
        RAISE EXCEPTION 'Historical finish requires the final round for its track'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;


ALTER FUNCTION public.validate_event_team_finish() OWNER TO postgres;

--
-- Name: validate_logical_round_promotion(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_logical_round_promotion() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    source_event uuid;
    source_sequence integer;
    target_event uuid;
    target_sequence integer;
BEGIN
    SELECT event_id, sequence_number INTO source_event, source_sequence
    FROM public.round_definitions WHERE id = NEW.source_logical_round_id;
    SELECT event_id, sequence_number INTO target_event, target_sequence
    FROM public.round_definitions WHERE id = NEW.target_logical_round_id;
    IF source_event IS NULL OR target_event IS NULL
       OR source_event <> target_event
       OR target_sequence <> source_sequence + 1 THEN
        RAISE EXCEPTION 'Promotion must target the immediately following logical round in the same event';
    END IF;
    RETURN NEW;
END $$;


ALTER FUNCTION public.validate_logical_round_promotion() OWNER TO postgres;

--
-- Name: validate_logical_round_promotion_provenance(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_logical_round_promotion_provenance() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    source_event uuid;
    source_sequence integer;
    target_event uuid;
    target_sequence integer;
    version_round uuid;
    version_status varchar;
    active_published uuid;
    entry_version uuid;
    entry_team uuid;
    entry_status varchar;
    active_count integer;
BEGIN
    SELECT event_id, sequence_number INTO source_event, source_sequence
    FROM public.round_definitions WHERE id = NEW.source_logical_round_id;
    SELECT event_id, sequence_number INTO target_event, target_sequence
    FROM public.round_definitions WHERE id = NEW.target_logical_round_id;
    SELECT r.logical_round_id, v.status
      INTO version_round, version_status
    FROM public.round_result_versions v
    JOIN public.rounds r ON r.id = v.round_id
    WHERE v.id = NEW.source_result_version_id;
    SELECT COUNT(*) INTO active_count
    FROM public.round_result_versions v
    WHERE v.round_id = (SELECT round_id FROM public.round_result_versions WHERE id = NEW.source_result_version_id)
      AND v.status = 'published';
    IF active_count = 1 THEN
        SELECT v.id INTO active_published
        FROM public.round_result_versions v
        WHERE v.round_id = (SELECT round_id FROM public.round_result_versions WHERE id = NEW.source_result_version_id)
          AND v.status = 'published';
    END IF;
    SELECT result_version_id, team_id, promotion_status
      INTO entry_version, entry_team, entry_status
    FROM public.round_result_version_entries
    WHERE id = NEW.source_result_entry_id;

    IF source_event IS NULL OR target_event IS NULL
       OR source_event <> target_event
       OR target_sequence <> source_sequence + 1
       OR version_round IS NULL
       OR version_round <> NEW.source_logical_round_id
       OR version_status <> 'published'
       OR active_count <> 1
       OR active_published <> NEW.source_result_version_id
       OR entry_version <> NEW.source_result_version_id
       OR entry_team <> NEW.team_id
       OR entry_status <> 'promoted' THEN
        RAISE EXCEPTION
            'Promotion provenance must reference the active published promoted entry from the immediately preceding logical round';
    END IF;
    RETURN NEW;
END $$;


ALTER FUNCTION public.validate_logical_round_promotion_provenance() OWNER TO postgres;

--
-- Name: validate_team_profile_registration(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_team_profile_registration() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    target_event_id uuid;
    source_profile_id uuid;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.team_profile_id::text, 0));

    SELECT tr.event_id INTO target_event_id
      FROM public.tracks tr
     WHERE tr.id = NEW.track_id;

    IF target_event_id IS NULL THEN
        RAISE EXCEPTION 'Team track does not exist'
            USING ERRCODE = '23503';
    END IF;

    IF NEW.source_team_id IS NOT NULL THEN
        SELECT t.team_profile_id INTO source_profile_id
          FROM public.teams t
         WHERE t.id = NEW.source_team_id;
        IF source_profile_id IS NULL OR source_profile_id <> NEW.team_profile_id THEN
            RAISE EXCEPTION 'Source team must belong to the same team profile'
                USING ERRCODE = '23514';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.teams existing
          JOIN public.tracks existing_track ON existing_track.id = existing.track_id
         WHERE existing.team_profile_id = NEW.team_profile_id
           AND existing_track.event_id = target_event_id
           AND existing.id <> NEW.id
    ) THEN
        RAISE EXCEPTION 'Team profile already has a registration in this event'
            USING ERRCODE = '23505';
    END IF;

    RETURN NEW;
END $$;


ALTER FUNCTION public.validate_team_profile_registration() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account_activation_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.account_activation_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    used_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by uuid
);


ALTER TABLE public.account_activation_tokens OWNER TO postgres;

--
-- Name: appeals; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.appeals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    round_id uuid NOT NULL,
    team_id uuid NOT NULL,
    submitted_by uuid NOT NULL,
    reason text NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    response text,
    resolved_by uuid,
    result_published_at timestamp without time zone,
    appeal_deadline timestamp without time zone,
    resolved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    result_version_id uuid,
    decision character varying(40),
    recalculation_required boolean DEFAULT false NOT NULL
);


ALTER TABLE public.appeals OWNER TO postgres;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    team_id uuid,
    incident_id uuid,
    action public.audit_action NOT NULL,
    target_type character varying(100) NOT NULL,
    target_id uuid NOT NULL,
    old_value text,
    new_value text,
    details text,
    occurred_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.audit_logs OWNER TO postgres;

--
-- Name: campuses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.campuses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    university_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    address text,
    city character varying(100),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE public.campuses OWNER TO postgres;

--
-- Name: criteria_templates; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.criteria_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    default_weight numeric(10,2) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT criteria_templates_default_weight_check CHECK ((default_weight >= (0)::numeric))
);


ALTER TABLE public.criteria_templates OWNER TO postgres;

--
-- Name: event_rules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_rules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE public.event_rules OWNER TO postgres;

--
-- Name: event_seed_assignments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_seed_assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    track_id uuid NOT NULL,
    team_id uuid NOT NULL,
    team_profile_id uuid NOT NULL,
    competition_stage character varying(40) DEFAULT 'event_setup'::character varying NOT NULL,
    seed_number integer,
    seed_tier character varying(20),
    candidate_source_finish_id uuid,
    continuity_count integer DEFAULT 0 NOT NULL,
    status character varying(20) NOT NULL,
    rationale text,
    assigned_by uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    row_version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT chk_event_seed_override_rationale CHECK ((((status)::text <> 'overridden'::text) OR (length(TRIM(BOTH FROM COALESCE(rationale, ''::text))) > 0))),
    CONSTRAINT event_seed_assignments_continuity_count_check CHECK ((continuity_count >= 0)),
    CONSTRAINT event_seed_assignments_seed_number_check CHECK (((seed_number IS NULL) OR (seed_number > 0))),
    CONSTRAINT event_seed_assignments_status_check CHECK (((status)::text = ANY (ARRAY[('suggested'::character varying)::text, ('confirmed'::character varying)::text, ('rejected'::character varying)::text, ('overridden'::character varying)::text])))
);


ALTER TABLE public.event_seed_assignments OWNER TO postgres;

--
-- Name: event_team_finishes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_team_finishes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    track_id uuid NOT NULL,
    team_id uuid NOT NULL,
    team_profile_id uuid NOT NULL,
    final_round_id uuid NOT NULL,
    result_version_id uuid NOT NULL,
    final_rank integer NOT NULL,
    final_score numeric(12,4),
    completion_status character varying(30) NOT NULL,
    completed_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by uuid,
    row_version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT event_team_finishes_completion_status_check CHECK (((completion_status)::text = ANY (ARRAY[('completed'::character varying)::text, ('disqualified'::character varying)::text]))),
    CONSTRAINT event_team_finishes_final_rank_check CHECK ((final_rank > 0))
);


ALTER TABLE public.event_team_finishes OWNER TO postgres;

--
-- Name: events; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    status public.event_status DEFAULT 'draft'::public.event_status NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    term character varying(255),
    prize_pool character varying(255),
    registration_start timestamp without time zone,
    registration_end timestamp without time zone,
    event_start timestamp without time zone,
    event_end timestamp without time zone
);


ALTER TABLE public.events OWNER TO postgres;

--
-- Name: incident_actions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.incident_actions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_id uuid NOT NULL,
    action_by uuid NOT NULL,
    action_type public.incident_action_type NOT NULL,
    target_type character varying(100),
    target_id uuid,
    old_value text,
    new_value text,
    note text,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.incident_actions OWNER TO postgres;

--
-- Name: incident_evidences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.incident_evidences (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_id uuid NOT NULL,
    file_url character varying(500),
    external_url character varying(500),
    description text,
    uploaded_by uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.incident_evidences OWNER TO postgres;

--
-- Name: incident_reports; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.incident_reports (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    track_id uuid,
    round_id uuid,
    team_id uuid,
    submission_id uuid,
    reporter_id uuid NOT NULL,
    assigned_coordinator_id uuid,
    type public.incident_type NOT NULL,
    status public.incident_status DEFAULT 'reported'::public.incident_status NOT NULL,
    title character varying(255) NOT NULL,
    description text NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    resolved_at timestamp without time zone,
    severity character varying(50),
    category character varying(100)
);


ALTER TABLE public.incident_reports OWNER TO postgres;

--
-- Name: logical_round_promotions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.logical_round_promotions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    source_logical_round_id uuid NOT NULL,
    target_logical_round_id uuid NOT NULL,
    team_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    source_result_version_id uuid NOT NULL,
    source_result_entry_id uuid NOT NULL,
    CONSTRAINT ck_logical_round_promotions_distinct_rounds CHECK ((source_logical_round_id <> target_logical_round_id))
);


ALTER TABLE public.logical_round_promotions OWNER TO postgres;

--
-- Name: mentor_feedbacks; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mentor_feedbacks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    track_mentor_id uuid NOT NULL,
    team_id uuid NOT NULL,
    round_id uuid,
    content text NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE public.mentor_feedbacks OWNER TO postgres;

--
-- Name: notices; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notices (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    priority character varying(20) DEFAULT 'normal'::character varying NOT NULL,
    target_role character varying(100),
    target_event_id uuid,
    target_track_id uuid,
    author_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now(),
    target_team_id uuid
);


ALTER TABLE public.notices OWNER TO postgres;

--
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type character varying(40) NOT NULL,
    title character varying(255) NOT NULL,
    body text,
    category character varying(40) NOT NULL,
    ref_type character varying(40),
    ref_id uuid,
    read_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- Name: prize_revisions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prize_revisions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    prize_id uuid NOT NULL,
    action character varying(20) NOT NULL,
    old_team_id uuid,
    new_team_id uuid,
    reason text NOT NULL,
    evidence_note text,
    changed_by uuid NOT NULL,
    changed_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.prize_revisions OWNER TO postgres;

--
-- Name: prizes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prizes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    track_id uuid,
    team_id uuid,
    name character varying(255) NOT NULL,
    prize_amount numeric(12,2),
    description text,
    awarded_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE public.prizes OWNER TO postgres;

--
-- Name: revoked_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.revoked_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.revoked_tokens OWNER TO postgres;

--
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    description text
);


ALTER TABLE public.roles OWNER TO postgres;

--
-- Name: round_criteria; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.round_criteria (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    round_id uuid NOT NULL,
    template_id uuid,
    name character varying(255) NOT NULL,
    weight numeric(10,2) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    status character varying(50) DEFAULT 'active'::character varying,
    CONSTRAINT round_criteria_weight_check CHECK ((weight >= (0)::numeric))
);


ALTER TABLE public.round_criteria OWNER TO postgres;

--
-- Name: round_definitions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.round_definitions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    sequence_number integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    is_final boolean DEFAULT false NOT NULL,
    default_top_n_to_promote integer,
    lifecycle_state character varying(40) DEFAULT 'SCORING'::character varying NOT NULL,
    CONSTRAINT round_definitions_sequence_number_check CHECK ((sequence_number > 0))
);


ALTER TABLE public.round_definitions OWNER TO postgres;

--
-- Name: round_judges; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.round_judges (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    round_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.round_judges OWNER TO postgres;

--
-- Name: round_participants; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.round_participants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    round_id uuid NOT NULL,
    team_id uuid NOT NULL,
    status public.round_participant_status DEFAULT 'pending'::public.round_participant_status NOT NULL,
    note text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE public.round_participants OWNER TO postgres;

--
-- Name: round_rankings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.round_rankings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    round_id uuid NOT NULL,
    team_id uuid NOT NULL,
    total_score numeric(10,2),
    rank integer,
    status public.promotion_status DEFAULT 'pending'::public.promotion_status NOT NULL,
    tie_breaker_criterion_id uuid,
    tie_breaker_score numeric(10,2),
    tie_breaker_reason text,
    calculated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT round_rankings_rank_check CHECK (((rank IS NULL) OR (rank > 0)))
);


ALTER TABLE public.round_rankings OWNER TO postgres;

--
-- Name: round_result_version_entries; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.round_result_version_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    result_version_id uuid NOT NULL,
    team_id uuid NOT NULL,
    rank integer,
    total_score numeric(10,2),
    promotion_status character varying(30) NOT NULL,
    tie_breaker_score numeric(10,2),
    tie_breaker_reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.round_result_version_entries OWNER TO postgres;

--
-- Name: round_result_versions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.round_result_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    round_id uuid NOT NULL,
    version_number integer NOT NULL,
    status character varying(20) NOT NULL,
    published_at timestamp without time zone NOT NULL,
    appeal_deadline timestamp without time zone NOT NULL,
    published_by uuid,
    source_version_id uuid,
    reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.round_result_versions OWNER TO postgres;

--
-- Name: rounds; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rounds (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    track_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    sequence_number integer NOT NULL,
    submission_deadline timestamp without time zone NOT NULL,
    top_n_to_promote integer NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    result_published_at timestamp without time zone,
    appeal_deadline timestamp without time zone,
    lifecycle_state character varying(40) DEFAULT 'SCORING'::character varying NOT NULL,
    lifecycle_version bigint DEFAULT 0 NOT NULL,
    logical_round_id uuid NOT NULL,
    CONSTRAINT rounds_sequence_number_check CHECK ((sequence_number > 0)),
    CONSTRAINT rounds_top_n_to_promote_check CHECK ((top_n_to_promote > 0))
);


ALTER TABLE public.rounds OWNER TO postgres;

--
-- Name: rule_acceptances; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rule_acceptances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    event_id uuid NOT NULL,
    accepted_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.rule_acceptances OWNER TO postgres;

--
-- Name: scores; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.scores (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    submission_id uuid NOT NULL,
    judge_id uuid NOT NULL,
    criterion_id uuid NOT NULL,
    score numeric(10,2) NOT NULL,
    weighted_score numeric(10,2),
    criterion_average_score numeric(10,2),
    criterion_variance numeric(10,4),
    criterion_stddev numeric(10,4),
    comment text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT scores_score_check CHECK ((score >= (0)::numeric))
);


ALTER TABLE public.scores OWNER TO postgres;

--
-- Name: submissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.submissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    round_id uuid NOT NULL,
    team_id uuid NOT NULL,
    repo_url character varying(500),
    demo_url character varying(500),
    slide_url character varying(500),
    report_url character varying(500),
    api_metadata text,
    submitted_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    project_name character varying(255),
    version character varying(50),
    review_status character varying(50) DEFAULT 'pending'::character varying,
    status character varying(20) DEFAULT 'draft'::character varying NOT NULL
);


ALTER TABLE public.submissions OWNER TO postgres;

--
-- Name: support_tickets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.support_tickets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    requester_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    priority character varying(20) NOT NULL,
    subject character varying(255) NOT NULL,
    description text NOT NULL,
    status character varying(30) DEFAULT 'open'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.support_tickets OWNER TO postgres;

--
-- Name: team_chat_messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.team_chat_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    team_id uuid NOT NULL,
    sender_id uuid NOT NULL,
    message text NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.team_chat_messages OWNER TO postgres;

--
-- Name: team_join_requests; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.team_join_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    team_id uuid NOT NULL,
    user_id uuid NOT NULL,
    status character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    message text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    responded_at timestamp without time zone,
    CONSTRAINT chk_tjr_status CHECK (((status)::text = ANY (ARRAY[('pending'::character varying)::text, ('accepted'::character varying)::text, ('rejected'::character varying)::text, ('cancelled'::character varying)::text])))
);


ALTER TABLE public.team_join_requests OWNER TO postgres;

--
-- Name: team_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.team_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    team_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role public.team_member_role DEFAULT 'member'::public.team_member_role NOT NULL,
    joined_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.team_members OWNER TO postgres;

--
-- Name: team_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.team_profiles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    canonical_name character varying(255) NOT NULL,
    logo_url character varying(500),
    created_by uuid,
    status character varying(20) DEFAULT 'active'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    row_version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT chk_team_profiles_status CHECK (((status)::text = ANY (ARRAY[('active'::character varying)::text, ('inactive'::character varying)::text, ('dissolved'::character varying)::text])))
);


ALTER TABLE public.team_profiles OWNER TO postgres;

--
-- Name: team_recognitions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.team_recognitions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    team_profile_id uuid NOT NULL,
    recognition_code character varying(80) NOT NULL,
    label character varying(160) NOT NULL,
    qualification_count integer DEFAULT 0 NOT NULL,
    earned_at timestamp without time zone NOT NULL,
    active boolean DEFAULT true NOT NULL,
    revoked_at timestamp without time zone,
    revoked_by uuid,
    revoke_reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    row_version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT chk_team_recognition_revocation CHECK ((((active = true) AND (revoked_at IS NULL) AND (revoked_by IS NULL) AND (revoke_reason IS NULL)) OR ((active = false) AND (revoked_at IS NOT NULL) AND (length(TRIM(BOTH FROM COALESCE(revoke_reason, ''::text))) > 0)))),
    CONSTRAINT team_recognitions_qualification_count_check CHECK ((qualification_count >= 0))
);


ALTER TABLE public.team_recognitions OWNER TO postgres;

--
-- Name: team_timeline_events; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.team_timeline_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    team_id uuid,
    round_id uuid,
    type character varying(40) NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    score_snapshot numeric(10,2),
    rank_snapshot integer,
    status_snapshot character varying(40),
    occurred_at timestamp without time zone DEFAULT now() NOT NULL,
    visibility_scope character varying(32) DEFAULT 'EVENT_PARTICIPANTS'::character varying NOT NULL,
    event_type character varying(64) NOT NULL,
    source_type character varying(64),
    source_id uuid,
    track_id uuid,
    submission_id uuid,
    appeal_id uuid,
    incident_id uuid,
    support_ticket_id uuid,
    seed_id uuid,
    recognition_id uuid,
    metadata jsonb,
    idempotency_key character varying(255)
);


ALTER TABLE public.team_timeline_events OWNER TO postgres;

--
-- Name: teams; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.teams (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    track_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    status public.team_status DEFAULT 'active'::public.team_status NOT NULL,
    disqualified_reason text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    invite_code character varying(12),
    team_profile_id uuid NOT NULL,
    source_team_id uuid,
    activated_from_profile_at timestamp without time zone,
    roster_confirmed_at timestamp without time zone,
    roster_confirmed_by uuid
);


ALTER TABLE public.teams OWNER TO postgres;

--
-- Name: tie_break_decisions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tie_break_decisions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    round_id uuid NOT NULL,
    team_id uuid NOT NULL,
    decided_by uuid NOT NULL,
    decided_at timestamp without time zone DEFAULT now() NOT NULL,
    reason text NOT NULL,
    evidence_url character varying(500),
    note text
);


ALTER TABLE public.tie_break_decisions OWNER TO postgres;

--
-- Name: track_judges; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.track_judges (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    track_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.track_judges OWNER TO postgres;

--
-- Name: track_mentors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.track_mentors (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    track_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.track_mentors OWNER TO postgres;

--
-- Name: tracks; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tracks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    max_teams integer
);


ALTER TABLE public.tracks OWNER TO postgres;

--
-- Name: universities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.universities (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    short_name character varying(100),
    country character varying(100),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE public.universities OWNER TO postgres;

--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    student_type public.student_type DEFAULT 'none'::public.student_type NOT NULL,
    student_id character varying(100),
    campus_id uuid,
    is_guest boolean DEFAULT false NOT NULL,
    status public.account_status DEFAULT 'pending'::public.account_status NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    university_id uuid,
    phone character varying(50),
    department character varying(255),
    "position" character varying(255),
    company character varying(255),
    expertise character varying(255),
    bio text,
    must_change_password boolean DEFAULT false NOT NULL,
    terms_accepted_at timestamp without time zone,
    terms_version character varying(100),
    privacy_version character varying(100),
    security_version bigint DEFAULT 1 NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: view_judge_submissions; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.view_judge_submissions AS
 SELECT rj.id AS round_judge_id,
    rj.user_id AS judge_id,
    r.id AS round_id,
    r.name AS round_name,
    team.id AS team_id,
    team.name AS team_name,
    s.id AS submission_id,
    s.submitted_at
   FROM (((public.round_judges rj
     JOIN public.rounds r ON ((r.id = rj.round_id)))
     JOIN public.submissions s ON ((s.round_id = r.id)))
     JOIN public.teams team ON ((team.id = s.team_id)));


ALTER VIEW public.view_judge_submissions OWNER TO postgres;

--
-- Name: view_mentor_teams; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.view_mentor_teams AS
 SELECT tm.id AS track_mentor_id,
    tm.user_id AS mentor_id,
    t.id AS track_id,
    t.name AS track_name,
    r.id AS round_id,
    r.name AS round_name,
    team.id AS team_id,
    team.name AS team_name,
    team.status AS team_status
   FROM (((public.track_mentors tm
     JOIN public.tracks t ON ((t.id = tm.track_id)))
     LEFT JOIN public.rounds r ON ((r.track_id = t.id)))
     LEFT JOIN public.teams team ON ((team.track_id = t.id)));


ALTER VIEW public.view_mentor_teams OWNER TO postgres;

--
-- Data for Name: account_activation_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.account_activation_tokens (id, user_id, token_hash, expires_at, used_at, created_at, created_by) FROM stdin;
4cc3d361-f1ee-45ee-b3ed-9db6402867f7	7aa03131-bf6f-43f1-916d-5c5b0e952c6c	088ff447256921e3f8416c6aa41a8cd5f52ddd16f62ee5d1dbfdf11e63ae143f	2026-07-20 03:20:02.831724	\N	2026-07-20 02:20:02.831724	\N
a2828d08-ade7-46db-8f54-0c8e4ce8814f	47d30644-2923-4d66-a879-cd83a2041d38	a7c2f52f3685048f3f9a8d4e6489de774bcb70eb7c13a15681451c66f44e7d38	2026-07-20 03:21:41.357458	\N	2026-07-20 02:21:41.357458	\N
\.


--
-- Data for Name: appeals; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.appeals (id, event_id, round_id, team_id, submitted_by, reason, status, response, resolved_by, result_published_at, appeal_deadline, resolved_at, created_at, updated_at, result_version_id, decision, recalculation_required) FROM stdin;
\.


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_logs (id, user_id, team_id, incident_id, action, target_type, target_id, old_value, new_value, details, occurred_at) FROM stdin;
3f4384d4-319a-4e16-aa94-d39607ebda08	b9debf4b-c200-4907-abdf-5ce2208f8eb7	\N	\N	DISQUALIFY_TEAM	team	8b0bb0fc-1857-4b95-964b-92b791c352f6	active	disqualified	Smoke test	2026-07-02 07:59:40.317851
dc96fdbb-4510-4c52-be75-b4cae9ed42dc	b9debf4b-c200-4907-abdf-5ce2208f8eb7	\N	\N	DISQUALIFY_TEAM	team	8b0bb0fc-1857-4b95-964b-92b791c352f6	active	disqualified	tao thich thi t khoa	2026-07-02 08:46:33.735737
643c4fa8-beb8-4b74-b544-9b763987e145	b9debf4b-c200-4907-abdf-5ce2208f8eb7	\N	\N	DISQUALIFY_TEAM	team	8b0bb0fc-1857-4b95-964b-92b791c352f6	active	disqualified	bo m thich ban	2026-07-02 17:43:44.492644
0eb3a454-7687-4374-aec7-302486de8970	b9debf4b-c200-4907-abdf-5ce2208f8eb7	5f12db5b-b46e-440b-98e4-bc6b0b592d74	\N	PROMOTE_TEAM	team	5f12db5b-b46e-440b-98e4-bc6b0b592d74	082553c7-dbed-4cb2-9629-0401e9fd3a7d	118d1506-a833-4a9f-877f-58b3bf4b3772	Moved team to track AI	2026-07-04 01:53:09.197129
cc78e52a-763e-43e3-9550-a9e18025749b	b9debf4b-c200-4907-abdf-5ce2208f8eb7	5f12db5b-b46e-440b-98e4-bc6b0b592d74	\N	PROMOTE_TEAM	team	5f12db5b-b46e-440b-98e4-bc6b0b592d74	118d1506-a833-4a9f-877f-58b3bf4b3772	082553c7-dbed-4cb2-9629-0401e9fd3a7d	Moved team to track Regression Track 232026	2026-07-04 01:53:09.239013
48969fb3-6fc4-400a-b700-670f56bbd5d7	b9debf4b-c200-4907-abdf-5ce2208f8eb7	5f12db5b-b46e-440b-98e4-bc6b0b592d74	\N	PROMOTE_TEAM	team	5f12db5b-b46e-440b-98e4-bc6b0b592d74	082553c7-dbed-4cb2-9629-0401e9fd3a7d	e18a5239-caf9-4997-8c11-e0bea2ca0f6e	Moved team to track Track B	2026-07-04 02:01:38.614675
0752ac16-a226-46fc-aa0a-c035b5dc06d0	a1000000-0000-4000-8000-000000000001	75ff0336-137b-437c-9142-0b6e08afaf76	\N	DISQUALIFY_TEAM	team	75ff0336-137b-437c-9142-0b6e08afaf76	active	disqualified	Không đủ thành viên khi đóng đăng ký (2/3)	2026-07-19 10:54:17.446095
889903eb-5299-4668-be36-0170611a8380	a1000000-0000-4000-8000-000000000001	2431c70e-5860-4536-9f99-34aabbafa6e7	\N	DISQUALIFY_TEAM	team	2431c70e-5860-4536-9f99-34aabbafa6e7	active	disqualified	Không đủ thành viên khi đóng đăng ký (2/3)	2026-07-19 10:54:17.449602
2656c4af-f520-425d-b5cc-ca993975989a	a1000000-0000-4000-8000-000000000001	d7712ef4-9dc5-4e15-9033-23f52a8e5810	\N	DISQUALIFY_TEAM	team	d7712ef4-9dc5-4e15-9033-23f52a8e5810	active	disqualified	Disqualified by coordinator	2026-07-20 09:09:11.744955
\.


--
-- Data for Name: campuses; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.campuses (id, university_id, name, address, city, created_at, updated_at) FROM stdin;
22222222-2222-2222-2222-222222222221	11111111-1111-1111-1111-111111111111	FPT University Ho Chi Minh City	\N	Ho Chi Minh City	2026-07-02 14:13:12.1265	\N
22222222-2222-2222-2222-222222222222	11111111-1111-1111-1111-111111111111	FPT University Ha Noi	\N	Ha Noi	2026-07-02 14:13:12.1265	\N
22222222-2222-2222-2222-222222222223	11111111-1111-1111-1111-111111111111	FPT University Da Nang	\N	Da Nang	2026-07-02 14:13:12.1265	\N
22222222-2222-2222-2222-222222222224	11111111-1111-1111-1111-111111111111	FPT University Can Tho	\N	Can Tho	2026-07-02 14:13:12.1265	\N
22222222-2222-2222-2222-222222222225	11111111-1111-1111-1111-111111111111	FPT University Quy Nhon	\N	Quy Nhon	2026-07-02 14:13:12.1265	\N
\.


--
-- Data for Name: criteria_templates; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.criteria_templates (id, name, description, default_weight, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: event_rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_rules (id, event_id, title, content, visibility, display_order, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: event_seed_assignments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_seed_assignments (id, event_id, track_id, team_id, team_profile_id, competition_stage, seed_number, seed_tier, candidate_source_finish_id, continuity_count, status, rationale, assigned_by, assigned_at, updated_at, row_version) FROM stdin;
\.


--
-- Data for Name: event_team_finishes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_team_finishes (id, event_id, track_id, team_id, team_profile_id, final_round_id, result_version_id, final_rank, final_score, completion_status, completed_at, created_at, created_by, row_version) FROM stdin;
3412977b-9b68-4aa2-884a-123c22d18b2c	1663a859-c386-4ad3-a70e-b402fc5a79c4	0af9c2de-b486-4540-b8bc-5f1d3fa9e796	df7a8f14-422d-441c-b5aa-155fed861d8a	1de43813-38b1-4ebe-a7e9-f08d67dffa3b	4a2da400-2849-4112-9ef9-21134009a22d	77eef39d-8ec2-4771-9675-c474e961d6cf	1	89.0000	completed	2024-04-20 17:00:00	2024-04-20 17:00:00	\N	0
8b9fa8ae-1dce-46ab-9721-70957bc4cb58	e64a6be2-8c67-4f2f-8d61-cbd4693ce942	fe3a7ba6-bdba-4ec0-87c1-7f200dac7bdb	5545fb21-255a-4df1-be04-6a9387052429	1de43813-38b1-4ebe-a7e9-f08d67dffa3b	182454f8-f79a-4ad4-b508-77f297eae2a6	2ffac82f-7bdf-47a6-91b4-612bd74c00c6	2	88.0000	completed	2025-10-18 17:00:00	2025-10-18 17:00:00	\N	0
92c1b51c-587f-4071-a3e0-20c93d90cf3b	c6484635-5026-4486-943a-8974b2176a3d	a9a53e66-eb94-42ce-8335-cbe4f1a574df	7977250b-57f9-4049-8b82-47051309b353	1de43813-38b1-4ebe-a7e9-f08d67dffa3b	18e62d3a-67b1-4391-8b23-8f4ad4e6c77e	688baacc-19fe-4d48-b8c7-0a9b8b5b0a92	3	87.0000	completed	2026-07-12 17:00:00	2026-07-12 17:00:00	\N	0
\.


--
-- Data for Name: events; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.events (id, title, description, status, created_at, updated_at, term, prize_pool, registration_start, registration_end, event_start, event_end) FROM stdin;
9b42520a-bce1-45ac-a165-73aa556d8c82	Regression Event 232026	full regression smoke	draft	2026-07-02 16:20:26.418208	2026-07-02 16:20:26.418208	\N	\N	\N	\N	\N	\N
32ef48b8-9ac3-4df0-8ad4-52f43f8eb9ae	SMOKE EDITED	tmp	published	2026-07-03 15:00:12.731716	2026-07-03 23:45:26.621204			\N	\N	\N	\N
81a4656e-8a2d-4152-958d-6f302ec23efe	SMOKE PUB DEL	tmp	ongoing	2026-07-03 15:00:26.054602	2026-07-04 08:37:21.259811			\N	\N	\N	\N
a6ec61f7-2f7e-4d77-86d5-861a62fc060c	kghj	gfh	ongoing	2026-07-03 16:46:07.145649	2026-07-04 08:37:32.055564	Spring	4	2026-07-02 17:00:00	2026-07-03 17:00:00	2026-07-04 17:00:00	2026-07-06 17:00:00
aaaa0000-0000-4000-8000-000000000001	DEMO Event (test accounts)	Seed event for manual feature testing.	ongoing	2026-07-10 02:49:49.985313	\N	Summer 2026	10.000.000 VND	2026-07-03 02:49:49.985313	2026-07-17 02:49:49.985313	2026-07-09 02:49:49.985313	2026-08-09 02:49:49.985313
5c133817-ddd1-49ff-ae1b-98d366b950e4	Hackathon 2026	AI in Testing topic	ongoing	2026-07-19 09:49:55.385525	2026-07-19 17:54:17.436292	Summer	60000	2026-07-19 17:00:00	2026-07-21 17:00:00	2026-07-22 17:00:00	2026-07-29 17:00:00
7f8d71f3-2086-4cfe-a6e2-2497c674dcc4	NewEvent	df	published	2026-07-20 08:59:29.33365	2026-07-20 15:59:33.400259	Summer	500000000	2026-07-20 17:00:00	2026-07-21 17:00:00	2026-07-22 17:00:00	2026-07-23 17:00:00
66c58f1a-b289-42e1-8d22-83f6031b60a8	Hackathon Load Test - 50 Teams	Generated local test event containing 50 teams and 250 approved accounts.	ongoing	2026-07-20 16:05:18.538542	2026-07-20 16:08:07.605354	Summer 2026	Load-test prize pool	2026-07-06 16:05:18.538542	2026-08-03 16:05:18.538542	2026-08-10 16:05:18.538542	2026-08-12 16:05:18.538542
1663a859-c386-4ad3-a70e-b402fc5a79c4	Phoenix Hackathon Spring 2024	Completed seed event used to qualify Veteran Phoenix Team for the veteran badge.	completed	2024-02-20 17:00:00	2024-04-20 17:00:00	Spring 2024	Veteran qualification seed	2024-03-06 17:00:00	2024-03-21 17:00:00	2024-04-18 17:00:00	2024-04-20 17:00:00
e64a6be2-8c67-4f2f-8d61-cbd4693ce942	Phoenix Hackathon Fall 2025	Completed seed event used to qualify Veteran Phoenix Team for the veteran badge.	completed	2025-08-19 17:00:00	2025-10-18 17:00:00	Fall 2025	Veteran qualification seed	2025-09-03 17:00:00	2025-09-18 17:00:00	2025-10-16 17:00:00	2025-10-18 17:00:00
c6484635-5026-4486-943a-8974b2176a3d	Phoenix Hackathon Summer 2026	Completed seed event used to qualify Veteran Phoenix Team for the veteran badge.	completed	2026-05-13 17:00:00	2026-07-12 17:00:00	Summer 2026	Veteran qualification seed	2026-05-28 17:00:00	2026-06-12 17:00:00	2026-07-10 17:00:00	2026-07-12 17:00:00
\.


--
-- Data for Name: incident_actions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident_actions (id, incident_id, action_by, action_type, target_type, target_id, old_value, new_value, note, created_at) FROM stdin;
5e81b5c6-0f2a-4f3b-bf12-b76ce060ebc7	62febed3-aae9-44f6-95d5-b88cd452f7b5	a1000000-0000-4000-8000-000000000001	other	\N	\N	reported	under_review	Decision: Under Review	2026-07-19 10:19:58.929878
2f4bdc28-e964-45e4-86a7-57c117839764	62febed3-aae9-44f6-95d5-b88cd452f7b5	a1000000-0000-4000-8000-000000000001	other	\N	\N	under_review	under_review	Decision: Under Review	2026-07-19 10:20:00.602819
6ee38e6c-3b2b-47fc-aa91-7b5e6710e114	62febed3-aae9-44f6-95d5-b88cd452f7b5	a1000000-0000-4000-8000-000000000001	other	\N	\N	under_review	resolved	Decision: Resolved	2026-07-19 10:20:01.78565
ac6683b1-1f11-41a8-ae53-fbe577cf22b3	62febed3-aae9-44f6-95d5-b88cd452f7b5	a1000000-0000-4000-8000-000000000001	other	\N	\N	resolved	under_review	Decision: Under Review	2026-07-19 10:20:02.98408
\.


--
-- Data for Name: incident_evidences; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident_evidences (id, incident_id, file_url, external_url, description, uploaded_by, created_at) FROM stdin;
\.


--
-- Data for Name: incident_reports; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident_reports (id, event_id, track_id, round_id, team_id, submission_id, reporter_id, assigned_coordinator_id, type, status, title, description, created_at, updated_at, resolved_at, severity, category) FROM stdin;
62febed3-aae9-44f6-95d5-b88cd452f7b5	9b42520a-bce1-45ac-a165-73aa556d8c82	082553c7-dbed-4cb2-9629-0401e9fd3a7d	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	5f12db5b-b46e-440b-98e4-bc6b0b592d74	cc3658ed-a72b-4b02-8942-a62c87eefc76	b9debf4b-c200-4907-abdf-5ce2208f8eb7	\N	other	under_review	Regression Incident	incident smoke	2026-07-02 16:21:52.711207	2026-07-19 17:20:02.978824	2026-07-19 10:20:01.782664	\N	\N
\.


--
-- Data for Name: logical_round_promotions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.logical_round_promotions (id, source_logical_round_id, target_logical_round_id, team_id, created_at, source_result_version_id, source_result_entry_id) FROM stdin;
\.


--
-- Data for Name: mentor_feedbacks; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.mentor_feedbacks (id, track_mentor_id, team_id, round_id, content, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: notices; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notices (id, title, content, priority, target_role, target_event_id, target_track_id, author_id, created_at, updated_at, target_team_id) FROM stdin;
09870d52-2805-4279-b23b-23669e3fc5e6	Smoke notice	Notice endpoint smoke	normal	judge	\N	\N	b9debf4b-c200-4907-abdf-5ce2208f8eb7	2026-07-02 16:13:55.004049	2026-07-02 16:13:55.004049	\N
284bcc67-2640-4ab2-99a8-247827290585	Regression Notice	full regression notice	normal	judge	\N	\N	b9debf4b-c200-4907-abdf-5ce2208f8eb7	2026-07-02 16:21:09.857379	2026-07-02 16:21:09.857379	\N
9e601cd6-9492-4ea5-b971-4647869e6f0a	Team notice smoke	team notice smoke	normal	team_member	\N	\N	b9debf4b-c200-4907-abdf-5ce2208f8eb7	2026-07-02 16:45:43.297306	2026-07-02 16:45:43.297306	\N
57fdae09-f8f8-4d8b-b241-2a6ade4cfa3d	Mentor notice smoke	mentor notice smoke	normal	mentor	\N	\N	b9debf4b-c200-4907-abdf-5ce2208f8eb7	2026-07-02 16:45:43.309762	2026-07-02 16:45:43.309762	\N
a2010f6a-1dca-4fbf-a842-03731fb9eead	probe	probe body	normal	all	\N	\N	b9debf4b-c200-4907-abdf-5ce2208f8eb7	2026-07-03 13:59:58.965111	2026-07-03 13:59:58.965111	\N
14aa0dcc-3ba0-415b-a886-cf46624ba41d	probe2	probe body	high	judge	\N	\N	b9debf4b-c200-4907-abdf-5ce2208f8eb7	2026-07-03 13:59:58.978187	2026-07-03 13:59:58.978187	\N
4ab6abfb-fc85-45a4-ac1c-86d3061a94cc	probe notice	probe	normal	team_member	\N	\N	b9debf4b-c200-4907-abdf-5ce2208f8eb7	2026-07-03 14:00:32.447079	2026-07-03 14:00:32.447079	\N
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (id, user_id, type, title, body, category, ref_type, ref_id, read_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: prize_revisions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prize_revisions (id, prize_id, action, old_team_id, new_team_id, reason, evidence_note, changed_by, changed_at) FROM stdin;
\.


--
-- Data for Name: prizes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prizes (id, event_id, track_id, team_id, name, prize_amount, description, awarded_at, created_at, updated_at) FROM stdin;
4399f90a-cf9c-4b5c-b508-c8c5a18231e7	9b42520a-bce1-45ac-a165-73aa556d8c82	082553c7-dbed-4cb2-9629-0401e9fd3a7d	\N	Regression Prize	\N	smoke	\N	2026-07-02 16:21:09.846969	2026-07-04 08:28:17.95402
\.


--
-- Data for Name: revoked_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.revoked_tokens (id, token_hash, expires_at, created_at, updated_at) FROM stdin;
e13a98db-d85d-4ed7-bcd1-0753b494596f	2032ca614f2ce0c3420762a8d70a0bc39c0606898b16d466eeff739e764ab2df	2026-07-20 16:49:34+07	2026-07-20 08:53:14.428709	2026-07-20 08:53:14.428709
510868f4-2981-46ae-9b2b-1f1d4cf6d625	9b58569e91ca267e7f891ba9506a6efb03924908a3d9ed8153e0fe787155c48f	2026-07-27 15:49:34+07	2026-07-20 08:53:14.483983	2026-07-20 08:53:14.483983
70f205fd-282f-463e-baaf-fda33d30d5b0	192904470156ed1c89cca71263508e0ddd85279ba576de09bfa4f6f0c040a4b2	2026-07-20 16:58:41+07	2026-07-20 09:26:09.707811	2026-07-20 09:26:09.707811
d0627f6a-77fe-4700-9ee7-66e84e1f9cde	0cff0d843b5c0f9644185321f3c94c5e1b925c54b4f8d5f347895876b4f796b5	2026-07-27 15:58:41+07	2026-07-20 09:26:09.710126	2026-07-20 09:26:09.710126
37b09ab9-4a37-478a-8e47-fe339d5b1e0f	a6e9dd8e891a6e9d1de77e3e52aea0cd7bb9e57ea2522be9b776d18a13c9714c	2026-07-20 17:26:14+07	2026-07-20 09:28:28.974585	2026-07-20 09:28:28.974585
284af9e9-836f-4555-b817-78bd2489de11	5bb0ae66ae49d23b1babcba5e4ebaf9d0a3c2bed02c16eae9eb4ac0ae300784d	2026-07-27 16:26:14+07	2026-07-20 09:28:28.97704	2026-07-20 09:28:28.97704
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.roles (id, name, description) FROM stdin;
e9e4426a-4b07-4969-a24d-54e7440cd964	coordinator	Event Coordinator from SE Department or PDP Staff
6f4f98b4-aff1-42df-801b-11f61e93db48	team_leader	Leader of a hackathon team
5afe0808-f611-48b4-82b6-49383a50efb4	team_member	Member of a hackathon team
28c09a8e-65bd-44b9-8bf2-fa999efbf8d1	mentor	Mentor assigned to a track
ed0ca372-ef82-4f26-bf88-474156fd7d67	judge	Judge assigned to a round
\.


--
-- Data for Name: round_criteria; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_criteria (id, round_id, template_id, name, weight, description, created_at, updated_at, status) FROM stdin;
99dc7e5c-2b3e-48a4-9e3b-657c8ae7a89a	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	\N	Regression Criterion	1.00	smoke	2026-07-02 16:20:26.473488	2026-07-02 16:20:26.473503	active
553cda26-eb47-4f54-a04b-8397d3bc5c55	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	\N	E2ECrit6279	25.00		2026-07-03 17:20:08.319536	2026-07-03 17:20:08.319559	active
73038e4b-d91d-4b89-93c1-5c7d1bb37531	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	\N	E2ECrit9937	25.00		2026-07-03 17:23:31.957485	2026-07-03 17:23:31.957522	active
9f38f646-492c-41bf-b4a5-1a5f784b572e	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	\N	E2ECrit72446	25.00		2026-07-03 17:27:54.460143	2026-07-03 17:27:54.46017	active
aba75b80-95d5-41e9-8a73-a5bfe93aeefe	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	\N	E2ECrit58423	30.00		2026-07-03 17:32:40.434234	2026-07-04 00:48:44.686425	active
92f2cea9-3c0d-4e84-a104-170a6fb11203	1a0cef4f-3aae-4e78-8f75-09dd77767eac	\N	1	20.00		2026-07-19 11:33:59.483943	2026-07-19 11:33:59.483943	active
0e99cdab-6e34-4d3a-a295-0a9d9e538ae1	1a0cef4f-3aae-4e78-8f75-09dd77767eac	\N	2	80.00		2026-07-19 11:34:05.379852	2026-07-19 11:34:05.379852	active
\.


--
-- Data for Name: round_definitions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_definitions (id, event_id, name, sequence_number, created_at, updated_at, is_final, default_top_n_to_promote, lifecycle_state) FROM stdin;
b62b7adb-c40b-4c28-8d19-b21cf034acc6	5c133817-ddd1-49ff-ae1b-98d366b950e4	round 1	1	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
e03d08e7-f699-45d9-9a6a-863fd3aeeb00	e64a6be2-8c67-4f2f-8d61-cbd4693ce942	Final Round	1	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
1eecfb3f-fe37-4487-a935-ea38f1e937d0	9b42520a-bce1-45ac-a165-73aa556d8c82	round 2	13	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
699e00d1-dcc9-451f-80bc-10f309bebb5d	c6484635-5026-4486-943a-8974b2176a3d	Final Round	1	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
80ed976c-db78-49d6-a030-a9cddef69169	1663a859-c386-4ad3-a70e-b402fc5a79c4	Final Round	1	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
701d5c4f-198a-4c78-a135-9a5846be51a9	9b42520a-bce1-45ac-a165-73aa556d8c82	Regression Round 232026	1	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
2905a9ed-1e07-4a4e-b7db-7bbea932ae11	9b42520a-bce1-45ac-a165-73aa556d8c82	E2ERound53846	99955	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
a7e7f7b1-628e-4823-971b-ad0b04a1f5cb	9b42520a-bce1-45ac-a165-73aa556d8c82	E2ERound67889	99669	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
336b4a14-18ee-4908-8d74-c707d7c22aed	66c58f1a-b289-42e1-8d22-83f6031b60a8	Loại 1	1	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
4b782179-6d92-418a-81dd-0a450bcb236f	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	1	9	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
f0112bb4-b469-4877-b569-2597f53801e2	9b42520a-bce1-45ac-a165-73aa556d8c82	E2ERound5363	99407	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
16088f49-2ca8-4d67-9d63-c0d31fa0bced	aaaa0000-0000-4000-8000-000000000001	DEMO Round 1	1	2026-07-21 13:05:23.425893	\N	f	\N	SCORING
\.


--
-- Data for Name: round_judges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_judges (id, round_id, user_id, assigned_at) FROM stdin;
a3000000-0000-4000-8000-000000000003	aaaa0000-0000-4000-8000-000000000003	a1000000-0000-4000-8000-000000000003	2026-07-10 02:49:49.985313
e390f5f2-b066-4dc1-bf61-61f2873f8926	1a0cef4f-3aae-4e78-8f75-09dd77767eac	a1000000-0000-4000-8000-000000000003	2026-07-19 11:33:03.019129
\.


--
-- Data for Name: round_participants; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_participants (id, round_id, team_id, status, note, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: round_rankings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_rankings (id, round_id, team_id, total_score, rank, status, tie_breaker_criterion_id, tie_breaker_score, tie_breaker_reason, calculated_at, updated_at) FROM stdin;
47fd33bb-9d6c-48a4-b0cf-03b15ecd3b54	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	5f12db5b-b46e-440b-98e4-bc6b0b592d74	8080.00	1	pending	\N	\N	Ranked by total weighted score; team name used for deterministic ordering on ties	2026-07-03 17:48:38.274602	2026-07-03 17:48:38.274616
\.


--
-- Data for Name: round_result_version_entries; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_result_version_entries (id, result_version_id, team_id, rank, total_score, promotion_status, tie_breaker_score, tie_breaker_reason, created_at) FROM stdin;
f0da80f8-1fcc-4ea0-a317-7fdbf25661aa	77eef39d-8ec2-4771-9675-c474e961d6cf	df7a8f14-422d-441c-b5aa-155fed861d8a	1	89.00	promoted	\N	\N	2024-04-20 16:00:00
931fc66c-8e64-4612-8750-5417b07b3df5	2ffac82f-7bdf-47a6-91b4-612bd74c00c6	5545fb21-255a-4df1-be04-6a9387052429	2	88.00	promoted	\N	\N	2025-10-18 16:00:00
a8848fd5-fe0b-4a8e-ba7f-27b090ae9ea1	688baacc-19fe-4d48-b8c7-0a9b8b5b0a92	7977250b-57f9-4049-8b82-47051309b353	3	87.00	promoted	\N	\N	2026-07-12 16:00:00
\.


--
-- Data for Name: round_result_versions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_result_versions (id, round_id, version_number, status, published_at, appeal_deadline, published_by, source_version_id, reason, created_at) FROM stdin;
77eef39d-8ec2-4771-9675-c474e961d6cf	4a2da400-2849-4112-9ef9-21134009a22d	1	published	2024-04-20 16:00:00	2024-04-21 17:00:00	\N	\N	Final result for veteran qualification seed.	2024-04-20 16:00:00
2ffac82f-7bdf-47a6-91b4-612bd74c00c6	182454f8-f79a-4ad4-b508-77f297eae2a6	1	published	2025-10-18 16:00:00	2025-10-19 17:00:00	\N	\N	Final result for veteran qualification seed.	2025-10-18 16:00:00
688baacc-19fe-4d48-b8c7-0a9b8b5b0a92	18e62d3a-67b1-4391-8b23-8f4ad4e6c77e	1	published	2026-07-12 16:00:00	2026-07-13 17:00:00	\N	\N	Final result for veteran qualification seed.	2026-07-12 16:00:00
\.


--
-- Data for Name: rounds; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.rounds (id, track_id, name, sequence_number, submission_deadline, top_n_to_promote, created_at, updated_at, result_published_at, appeal_deadline, lifecycle_state, lifecycle_version, logical_round_id) FROM stdin;
f00c85fe-3caa-4602-a6a4-ed0ddadbc4e4	082553c7-dbed-4cb2-9629-0401e9fd3a7d	E2ERound53846	99955	2026-08-31 22:00:00	5	2026-07-03 17:32:35.902038	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	2905a9ed-1e07-4a4e-b7db-7bbea932ae11
a206b707-43b9-4d52-98ab-2fb58ea85627	082553c7-dbed-4cb2-9629-0401e9fd3a7d	E2ERound67889	99669	2026-08-31 22:00:00	5	2026-07-03 17:27:49.925515	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	a7e7f7b1-628e-4823-971b-ad0b04a1f5cb
27451535-d487-4d47-a015-230c2f53236b	082553c7-dbed-4cb2-9629-0401e9fd3a7d	E2ERound5363	99407	2026-08-31 22:00:00	5	2026-07-03 17:23:27.425048	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	f0112bb4-b469-4877-b569-2597f53801e2
876945ae-d236-4b69-89d4-2a515c55d9f7	082553c7-dbed-4cb2-9629-0401e9fd3a7d	round 2	13	2026-07-04 17:43:00	5	2026-07-02 17:43:13.774367	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	1eecfb3f-fe37-4487-a935-ea38f1e937d0
46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	082553c7-dbed-4cb2-9629-0401e9fd3a7d	Regression Round 232026	1	2026-07-09 16:20:26	3	2026-07-02 16:20:26.454913	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	701d5c4f-198a-4c78-a135-9a5846be51a9
47325e63-2e70-4281-909a-dcf8d45c9542	d892f925-3347-4f5e-890b-dc7ca7558e33	1	9	2026-07-04 18:37:00	3	2026-07-04 01:37:59.20713	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	4b782179-6d92-418a-81dd-0a450bcb236f
aaaa0000-0000-4000-8000-000000000003	aaaa0000-0000-4000-8000-000000000002	DEMO Round 1	1	2026-07-24 02:49:49.985313	3	2026-07-10 02:49:49.985313	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	16088f49-2ca8-4d67-9d63-c0d31fa0bced
1a0cef4f-3aae-4e78-8f75-09dd77767eac	5ff86b4c-12b5-4701-bc24-c5ab4cea0307	round 1	1	2026-07-20 03:56:00	2	2026-07-19 10:56:40.785183	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	b62b7adb-c40b-4c28-8d19-b21cf034acc6
894d2c0c-140c-49da-a6e2-382acba114dd	72d25793-3750-4e35-8a8c-065df0fe7769	Loại 1	1	2026-07-25 02:06:00	5	2026-07-20 09:06:48.390055	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	336b4a14-18ee-4908-8d74-c707d7c22aed
40f29a11-d73f-4f43-8f4f-f4460f41f988	f1b6e4bf-fdce-4a7f-a700-267edfb985eb	Loại 1	1	2026-07-25 02:07:00	5	2026-07-20 09:07:34.648842	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	336b4a14-18ee-4908-8d74-c707d7c22aed
6c067090-84fb-4615-8d94-3e904bf5af0c	9ac4329e-65f1-436d-82be-9568d9818f28	Loại 1	1	2026-07-25 02:07:00	5	2026-07-20 09:07:04.402339	2026-07-21 13:05:23.425893	\N	\N	SCORING	0	336b4a14-18ee-4908-8d74-c707d7c22aed
4a2da400-2849-4112-9ef9-21134009a22d	0af9c2de-b486-4540-b8bc-5f1d3fa9e796	Final Round	1	2024-04-20 14:00:00	10	2024-03-21 17:00:00	2026-07-21 13:05:23.425893	2024-04-20 16:00:00	2024-04-21 17:00:00	FINALIZED	1	80ed976c-db78-49d6-a030-a9cddef69169
182454f8-f79a-4ad4-b508-77f297eae2a6	fe3a7ba6-bdba-4ec0-87c1-7f200dac7bdb	Final Round	1	2025-10-18 14:00:00	10	2025-09-18 17:00:00	2026-07-21 13:05:23.425893	2025-10-18 16:00:00	2025-10-19 17:00:00	FINALIZED	1	e03d08e7-f699-45d9-9a6a-863fd3aeeb00
18e62d3a-67b1-4391-8b23-8f4ad4e6c77e	a9a53e66-eb94-42ce-8335-cbe4f1a574df	Final Round	1	2026-07-12 14:00:00	10	2026-06-12 17:00:00	2026-07-21 13:05:23.425893	2026-07-12 16:00:00	2026-07-13 17:00:00	FINALIZED	1	699e00d1-dcc9-451f-80bc-10f309bebb5d
\.


--
-- Data for Name: rule_acceptances; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.rule_acceptances (id, user_id, event_id, accepted_at) FROM stdin;
\.


--
-- Data for Name: scores; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.scores (id, submission_id, judge_id, criterion_id, score, weighted_score, criterion_average_score, criterion_variance, criterion_stddev, comment, created_at, updated_at) FROM stdin;
29172851-ebef-4a9b-bb93-8ce07f825aa6	cc3658ed-a72b-4b02-8942-a62c87eefc76	b9debf4b-c200-4907-abdf-5ce2208f8eb7	99dc7e5c-2b3e-48a4-9e3b-657c8ae7a89a	80.00	80.00	\N	\N	\N	logic weight test	2026-07-02 16:21:09.811477	2026-07-04 00:48:34.734182
791c554c-f7b6-4c54-a3fb-e62a5ca07e40	cc3658ed-a72b-4b02-8942-a62c87eefc76	b9debf4b-c200-4907-abdf-5ce2208f8eb7	553cda26-eb47-4f54-a04b-8397d3bc5c55	80.00	2000.00	\N	\N	\N	logic weight test	2026-07-03 17:20:39.304769	2026-07-04 00:48:34.752666
c0fba8d9-244c-4b90-ae3f-44679b487ee7	cc3658ed-a72b-4b02-8942-a62c87eefc76	b9debf4b-c200-4907-abdf-5ce2208f8eb7	73038e4b-d91d-4b89-93c1-5c7d1bb37531	80.00	2000.00	\N	\N	\N	logic weight test	2026-07-03 17:24:15.578017	2026-07-04 00:48:34.768731
bfa27de4-4ba4-4e4c-afdd-8c4757749557	cc3658ed-a72b-4b02-8942-a62c87eefc76	b9debf4b-c200-4907-abdf-5ce2208f8eb7	9f38f646-492c-41bf-b4a5-1a5f784b572e	80.00	2000.00	\N	\N	\N	logic weight test	2026-07-03 17:28:39.043044	2026-07-04 00:48:34.781078
ababb520-ab3f-4c20-b576-e4183aac5b72	cc3658ed-a72b-4b02-8942-a62c87eefc76	b9debf4b-c200-4907-abdf-5ce2208f8eb7	aba75b80-95d5-41e9-8a73-a5bfe93aeefe	80.00	2000.00	\N	\N	\N	logic weight test	2026-07-03 17:33:24.44238	2026-07-04 00:48:34.792798
bba07086-348c-4a43-89a7-287f70928c05	69966311-3bbb-4165-b266-cf794ef8b99e	a1000000-0000-4000-8000-000000000003	92f2cea9-3c0d-4e84-a104-170a6fb11203	100.00	2000.00	\N	\N	\N	\N	2026-07-19 11:34:14.5742	2026-07-19 11:34:14.5742
956c35e9-29d0-4b7b-8f5e-39f8243b539a	69966311-3bbb-4165-b266-cf794ef8b99e	a1000000-0000-4000-8000-000000000003	0e99cdab-6e34-4d3a-a295-0a9d9e538ae1	100.00	8000.00	\N	\N	\N	\N	2026-07-19 11:34:14.591691	2026-07-19 11:34:14.591691
\.


--
-- Data for Name: submissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.submissions (id, round_id, team_id, repo_url, demo_url, slide_url, report_url, api_metadata, submitted_at, updated_at, project_name, version, review_status, status) FROM stdin;
cc3658ed-a72b-4b02-8942-a62c87eefc76	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	5f12db5b-b46e-440b-98e4-bc6b0b592d74	https://github.com/e2e/tail-64618	https://demo.example.com	https://example.com/slide	https://example.com/report	e2e-deep notes	2026-07-02 16:20:27.090629	2026-07-10 02:49:49.985313	\N	\N	pending	submitted
69966311-3bbb-4165-b266-cf794ef8b99e	1a0cef4f-3aae-4e78-8f75-09dd77767eac	75ff0336-137b-437c-9142-0b6e08afaf76	http://localhost:5173/team/submissions	http://localhost:5173/team/submissions	http://localhost:5173/team/submissions	http://localhost:5173/team/submissions	ád	2026-07-19 11:02:25.043825	2026-07-20 15:40:47.416268	\N	\N	pending	draft
\.


--
-- Data for Name: support_tickets; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.support_tickets (id, requester_id, category, priority, subject, description, status, created_at, updated_at) FROM stdin;
e8b1bb75-2563-4201-908a-ae20a5726793	b9debf4b-c200-4907-abdf-5ce2208f8eb7	technical	low	Smoke ticket	support smoke	open	2026-07-02 17:11:24.132142	2026-07-02 17:11:24.132142
5afe23da-3b18-40d3-8fea-afae1e601334	83bbb9b4-c32e-4b55-aea4-eb3e294f71b1	technical	high	j	h	open	2026-07-19 10:27:49.236532	2026-07-19 10:27:49.236532
\.


--
-- Data for Name: team_chat_messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_chat_messages (id, team_id, sender_id, message, created_at, updated_at) FROM stdin;
0e90e5bb-38df-48b2-88b2-d320c28213fa	5f12db5b-b46e-440b-98e4-bc6b0b592d74	b9debf4b-c200-4907-abdf-5ce2208f8eb7	chat smoke	2026-07-02 17:11:24.094925	2026-07-02 17:11:24.094925
ca651c29-72d4-4223-a38f-861c2908576c	5f12db5b-b46e-440b-98e4-bc6b0b592d74	b9debf4b-c200-4907-abdf-5ce2208f8eb7	e2e-deep hello 49193	2026-07-03 17:20:49.236438	2026-07-03 17:20:49.236438
ddb3a80a-cca0-4294-9d12-75c16d0960c8	5f12db5b-b46e-440b-98e4-bc6b0b592d74	b9debf4b-c200-4907-abdf-5ce2208f8eb7	tail hello 54914	2026-07-03 17:40:54.941669	2026-07-03 17:40:54.941669
\.


--
-- Data for Name: team_join_requests; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_join_requests (id, team_id, user_id, status, message, created_at, responded_at) FROM stdin;
abe8b077-9be8-44be-874f-d2b8636a630f	75ff0336-137b-437c-9142-0b6e08afaf76	4eb1d119-fe2e-48cf-ace8-f295984731f9	accepted	\N	2026-07-19 10:48:40.509653	2026-07-19 10:52:33.877931
\.


--
-- Data for Name: team_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_members (id, team_id, user_id, role, joined_at) FROM stdin;
d5efffe7-e1ca-453d-b636-a88a53702951	5f12db5b-b46e-440b-98e4-bc6b0b592d74	b9debf4b-c200-4907-abdf-5ce2208f8eb7	leader	2026-07-02 16:20:27.063579
41950771-417b-4571-874f-9296829607bb	5f12db5b-b46e-440b-98e4-bc6b0b592d74	52e07429-90f0-4b2a-821e-77954881d2b1	member	2026-07-02 16:20:27.065781
5bf82639-b24b-459b-80cf-1437ea74baba	5f12db5b-b46e-440b-98e4-bc6b0b592d74	450560ce-da8d-43f6-955d-f39d553b77c3	member	2026-07-02 16:20:27.067951
a2000000-0000-4000-8000-000000000001	aaaa0000-0000-4000-8000-000000000004	a1000000-0000-4000-8000-000000000004	leader	2026-07-10 02:49:49.985313
a2000000-0000-4000-8000-000000000002	aaaa0000-0000-4000-8000-000000000004	a1000000-0000-4000-8000-000000000005	member	2026-07-10 02:49:49.985313
205e4811-c3f9-4299-be8e-34674ac9b05c	75ff0336-137b-437c-9142-0b6e08afaf76	83bbb9b4-c32e-4b55-aea4-eb3e294f71b1	leader	2026-07-19 10:26:00.338753
0f274899-3521-4d2f-9fa2-4b321cf74b34	2431c70e-5860-4536-9f99-34aabbafa6e7	ac5dc174-c461-4b2b-aff3-c8a9acd02f8f	leader	2026-07-19 10:41:04.931057
074f33f0-3567-4cb4-bb0a-e323c9d4bfeb	2431c70e-5860-4536-9f99-34aabbafa6e7	bec002b0-8bfd-4798-bac8-f2cb35e96b48	member	2026-07-19 10:48:58.337775
eb33bb42-2045-4764-96d9-bf06b734715f	75ff0336-137b-437c-9142-0b6e08afaf76	4eb1d119-fe2e-48cf-ace8-f295984731f9	member	2026-07-19 10:52:33.878965
5b7eabc9-8ab6-4869-a7fd-39b8a5a01d36	d7712ef4-9dc5-4e15-9033-23f52a8e5810	ac5e1fad-f17e-4e32-9983-82fbb0ef4566	leader	2026-07-20 16:05:18.538542
0bee31e9-dad2-44ff-a86c-b44576071fe1	d7712ef4-9dc5-4e15-9033-23f52a8e5810	91248f86-5f06-403f-8b56-425a3f978f26	member	2026-07-20 16:05:18.538542
0e831457-11db-4948-858a-582f887022b1	d7712ef4-9dc5-4e15-9033-23f52a8e5810	e739a2a0-fcfa-44cf-ba66-52e0c38ac25c	member	2026-07-20 16:05:18.538542
95d9969d-6e26-4f62-bd31-c08a8d76f9de	d7712ef4-9dc5-4e15-9033-23f52a8e5810	f9088069-eefc-434b-a8b0-96bea0b15394	member	2026-07-20 16:05:18.538542
250acca5-de4c-4b2d-884a-d3f94255cc47	d7712ef4-9dc5-4e15-9033-23f52a8e5810	966469de-d2a6-49f6-b6f3-9d5f5148f9c0	member	2026-07-20 16:05:18.538542
a1eff114-56ae-436c-bd76-abee24f09185	ac4a34dd-3533-443f-ac54-4b2620492d06	e80f247f-d89a-4f20-acb4-85201cf56843	leader	2026-07-20 16:05:18.538542
ff4c0a80-cdfa-4678-9864-3ef39c2dde62	ac4a34dd-3533-443f-ac54-4b2620492d06	8bea3f2e-98b9-4387-a01c-c829c7b5787e	member	2026-07-20 16:05:18.538542
722db473-52eb-467e-83fe-6319c2abf001	ac4a34dd-3533-443f-ac54-4b2620492d06	360a728d-428f-4311-9e98-91de88c3559d	member	2026-07-20 16:05:18.538542
b9b7d6e3-1188-4282-a392-d29697177d0c	ac4a34dd-3533-443f-ac54-4b2620492d06	51b76d36-4e6a-4333-a27b-7afe2a4f9d1a	member	2026-07-20 16:05:18.538542
8584d059-593b-4ee8-95f7-49248d8cfba2	ac4a34dd-3533-443f-ac54-4b2620492d06	9305157f-ee32-40d8-8b63-894419c8b400	member	2026-07-20 16:05:18.538542
69b4f650-222b-4ac7-965b-ea92f42001af	84163cb0-561f-4d35-bbc2-5711274e3760	f573bdcf-0392-4bbd-9d86-0f10b5a9d10d	leader	2026-07-20 16:05:18.538542
9dcc438d-2a76-4d14-8208-f40c64c34c5b	84163cb0-561f-4d35-bbc2-5711274e3760	188c3a51-61ab-44dc-b09d-5f6e984f5de5	member	2026-07-20 16:05:18.538542
51d7495b-359f-4a35-bc6b-9baed0ee09dd	84163cb0-561f-4d35-bbc2-5711274e3760	10b97850-e810-4081-a268-07ca73770c49	member	2026-07-20 16:05:18.538542
07942e5b-c0dc-45cd-8649-fe567d83fcc8	84163cb0-561f-4d35-bbc2-5711274e3760	02b24b64-72c3-4fad-a6db-c2832b7e5356	member	2026-07-20 16:05:18.538542
81d6586c-aabd-4f9d-9223-5413445c5781	84163cb0-561f-4d35-bbc2-5711274e3760	b68c9e10-0cbe-421d-8622-8972b934888c	member	2026-07-20 16:05:18.538542
dc7d79ec-ecc3-4fea-9a7b-c27e921b644c	343b705b-dbda-431d-9783-13d6d65090a9	8033af9a-0e8a-42a6-b8cb-2520757b51e7	leader	2026-07-20 16:05:18.538542
11a10799-e43d-4536-93e2-c12e039a5ac6	343b705b-dbda-431d-9783-13d6d65090a9	e041f93f-aedf-4c52-b1f5-dd79b38fb1d3	member	2026-07-20 16:05:18.538542
6f383a8a-56d4-42cf-9061-eb9c2a186c65	343b705b-dbda-431d-9783-13d6d65090a9	3e322db8-bf88-47aa-8549-55afe5ecf35c	member	2026-07-20 16:05:18.538542
a12ae45f-b5fc-43b4-bd37-3779bc68d442	343b705b-dbda-431d-9783-13d6d65090a9	d64a01ff-129d-4d71-b5a0-866209d46273	member	2026-07-20 16:05:18.538542
1d0e30ac-e972-4799-aea5-52a4f83a640e	343b705b-dbda-431d-9783-13d6d65090a9	d98ebf02-2089-4cc6-8db6-6c27fcd186d3	member	2026-07-20 16:05:18.538542
0e4658f5-2767-4f0f-a625-e0b6c7896cf3	4a888f3e-b823-4e48-a89d-78717f81523a	45c59a91-6d75-4875-9e5b-5fc18a962917	leader	2026-07-20 16:05:18.538542
3e6b14a8-934f-49cb-8274-d092fb2cd054	4a888f3e-b823-4e48-a89d-78717f81523a	e5fc740b-69fc-49a3-913d-693b2e0465b3	member	2026-07-20 16:05:18.538542
560c0fb7-b67e-4c8b-8d7b-597053d542da	4a888f3e-b823-4e48-a89d-78717f81523a	cec83f5b-fa9f-4a44-a215-a64d7e45b86a	member	2026-07-20 16:05:18.538542
6d4e6982-9eba-40e6-91ea-7868733fe33d	4a888f3e-b823-4e48-a89d-78717f81523a	c59eeb36-dac0-44e5-90eb-5e4597af6d3d	member	2026-07-20 16:05:18.538542
e0747b93-f53b-4a7e-a3f7-301276c85bd3	4a888f3e-b823-4e48-a89d-78717f81523a	197a6e30-fc7c-4546-86a5-daddaf02be58	member	2026-07-20 16:05:18.538542
f6bae673-2ff6-45e0-9739-8fcd1059b567	6f9c5467-188a-4e4c-8ad4-2a7385d61e07	b70c1256-ecae-4639-b6ca-c2db0b3f6d2e	leader	2026-07-20 16:05:18.538542
45bde663-f409-4478-bff2-e3cbfbb0d688	6f9c5467-188a-4e4c-8ad4-2a7385d61e07	723745d5-7fe8-4ab8-9264-770dcb019a05	member	2026-07-20 16:05:18.538542
ee721b17-585b-4199-a355-0075a2fc2791	6f9c5467-188a-4e4c-8ad4-2a7385d61e07	34104d7c-6650-41d0-bd63-d5ee2c821656	member	2026-07-20 16:05:18.538542
fb1ee51e-591e-4e27-a899-1f3adef242a6	6f9c5467-188a-4e4c-8ad4-2a7385d61e07	98c052f7-664d-475d-bde2-952893559d99	member	2026-07-20 16:05:18.538542
74685caf-d2d1-4ce1-b015-d8d5cf32bd8b	6f9c5467-188a-4e4c-8ad4-2a7385d61e07	ac169d77-6589-4ef2-8845-9f591436e6ca	member	2026-07-20 16:05:18.538542
e5e31bcd-7495-4113-8b60-aadae0e7c8c8	69eef567-52af-4cf3-b628-7b187e28bbff	85493223-f120-4d65-870c-e350628c21f7	leader	2026-07-20 16:05:18.538542
0bc59b1b-dc41-4aa2-a55d-7a5e9440dfa6	69eef567-52af-4cf3-b628-7b187e28bbff	ed67eb39-7111-4e23-b5fc-9945d92a4b25	member	2026-07-20 16:05:18.538542
b501f82a-c89b-4de1-a88c-6e237644cdde	69eef567-52af-4cf3-b628-7b187e28bbff	bd12a549-3270-44f5-ab2b-4d4348cf4395	member	2026-07-20 16:05:18.538542
0657f458-9f16-4365-9854-e563b83af963	69eef567-52af-4cf3-b628-7b187e28bbff	1d71f914-3895-4c39-8ea1-de7440110a10	member	2026-07-20 16:05:18.538542
795ba926-2dc9-4b5d-ad6f-00e60b513590	69eef567-52af-4cf3-b628-7b187e28bbff	b5093c46-6842-4911-b12c-55f38f7b7b51	member	2026-07-20 16:05:18.538542
95945861-e918-4b6a-be19-35df6ab7b830	6e028ee2-c138-4374-b935-447baa612216	797b66d7-5020-46fb-9daf-115c739fd016	leader	2026-07-20 16:05:18.538542
b9d014b8-1a74-4f1d-9aee-be64e187ca5a	6e028ee2-c138-4374-b935-447baa612216	7bed58e8-143f-42c6-bf26-f2770cd5cb3c	member	2026-07-20 16:05:18.538542
5db57688-f525-4726-9dfb-920134c75b58	6e028ee2-c138-4374-b935-447baa612216	94a9c5ad-1f58-4a22-af2e-ab8615164d0b	member	2026-07-20 16:05:18.538542
e847013a-e02a-485c-9003-9e752eb4c703	6e028ee2-c138-4374-b935-447baa612216	72d1095b-b100-4b53-84ad-6bc9f06a57a6	member	2026-07-20 16:05:18.538542
2bb45eda-b584-42d9-8da5-9e018effd0a8	6e028ee2-c138-4374-b935-447baa612216	c7999bcd-dc53-4827-947d-c0bf3d36dca2	member	2026-07-20 16:05:18.538542
b8561f24-a851-46ac-85c5-7b796b20e51e	2cadc36f-ebfa-442e-b495-8d5247a44c43	57db3f95-400e-4e77-bdbc-4a80d8859814	leader	2026-07-20 16:05:18.538542
6e03afdf-3616-435c-8b30-f781b640f054	2cadc36f-ebfa-442e-b495-8d5247a44c43	3b70339f-43b3-4c7b-a4bd-bf37421301c2	member	2026-07-20 16:05:18.538542
37f88cd5-33e1-4010-8b93-0108cd8d1f80	2cadc36f-ebfa-442e-b495-8d5247a44c43	36ece553-1f0f-4d10-9510-7770061db2cf	member	2026-07-20 16:05:18.538542
9a73eabc-215e-45e3-a1db-816e6b58b4ff	2cadc36f-ebfa-442e-b495-8d5247a44c43	781a9844-1985-422f-a280-267cfb804cbc	member	2026-07-20 16:05:18.538542
33f6e059-b0e6-46a2-a1b8-d7cbcda7e68e	2cadc36f-ebfa-442e-b495-8d5247a44c43	a54f839c-1f71-4bba-b66e-bb6d15e4bac7	member	2026-07-20 16:05:18.538542
e7569e96-59b2-453f-b7be-78f637c44f8a	8070dc1e-a71b-4120-8a3d-b330115742d5	6e84f560-95ea-418b-9b2d-350a0aed795b	leader	2026-07-20 16:05:18.538542
3e1376d5-60e4-4d1c-b2c2-3d5c8b440b48	8070dc1e-a71b-4120-8a3d-b330115742d5	c9641a17-9da5-4a11-89a2-1122b8050d30	member	2026-07-20 16:05:18.538542
6f57aadf-3ec9-4c3e-996e-6c08eb73e6b4	8070dc1e-a71b-4120-8a3d-b330115742d5	af796791-12a4-42bb-932d-15b8ed0a05c6	member	2026-07-20 16:05:18.538542
df0b58cb-9be7-4c00-96ed-2c48ac5c2fa0	8070dc1e-a71b-4120-8a3d-b330115742d5	d46537bc-511f-48a8-bc6a-b267f0a02ee2	member	2026-07-20 16:05:18.538542
9763697c-f97c-45e7-8d54-1120084bfc04	8070dc1e-a71b-4120-8a3d-b330115742d5	ed105c49-c897-48d7-bb8e-8ff04711e240	member	2026-07-20 16:05:18.538542
4ea66eab-c263-49ec-a9a7-2249f7bc871f	f5c72ff9-25fe-48c7-a7e7-2855bb6f3a27	7dcf36c2-b8c6-411c-b5b3-d8fb6df96e87	leader	2026-07-20 16:05:18.538542
d994936b-f0d4-4723-ab79-a6606148f81d	f5c72ff9-25fe-48c7-a7e7-2855bb6f3a27	96c0d0d9-e176-4029-a058-98ad21ea7f3c	member	2026-07-20 16:05:18.538542
df34e133-888a-455f-a4d7-df6d8163120c	f5c72ff9-25fe-48c7-a7e7-2855bb6f3a27	6b999807-e8dd-42b4-ad4f-0aa0105f7817	member	2026-07-20 16:05:18.538542
bc9d1b8a-d935-4b0d-ac60-5162bac8d065	f5c72ff9-25fe-48c7-a7e7-2855bb6f3a27	e5621eba-1354-4971-959a-d9cd548200a6	member	2026-07-20 16:05:18.538542
a5a534e6-4c46-487b-9807-a58e7a352ac3	f5c72ff9-25fe-48c7-a7e7-2855bb6f3a27	17491aa9-a573-4b76-806a-4c8e4ec71852	member	2026-07-20 16:05:18.538542
df1e641d-27b1-488f-b202-def2f055e0f1	f244d75a-e5ec-43f9-91bb-d84bdbd02e93	19a9fb26-5dbe-4ef0-a5ea-a32d88f242f8	leader	2026-07-20 16:05:18.538542
97bf10a1-0687-453f-9719-c9e2cb30c4d5	f244d75a-e5ec-43f9-91bb-d84bdbd02e93	230776cc-0a67-4f94-9c6b-280dbbbfa138	member	2026-07-20 16:05:18.538542
ce695531-57e5-42ad-93c8-f5dff7ca4f74	f244d75a-e5ec-43f9-91bb-d84bdbd02e93	e7e2fddd-05f1-4218-9476-5f9b96abf556	member	2026-07-20 16:05:18.538542
e3df06f9-ee11-4d28-ad23-1da634ff5ef4	f244d75a-e5ec-43f9-91bb-d84bdbd02e93	0dc1bd41-2c34-4899-86a5-0369a8c32f8c	member	2026-07-20 16:05:18.538542
b645d4e8-808e-45b1-bc4b-7be459e48d64	f244d75a-e5ec-43f9-91bb-d84bdbd02e93	eff7c4b0-a475-4316-8841-f0099267c6ba	member	2026-07-20 16:05:18.538542
62b76c90-44de-407f-8c43-46c619e0fa43	d205af05-e260-46df-b01e-808307bb624f	ff9fd982-afbd-4be1-af50-2c19472bf3a4	leader	2026-07-20 16:05:18.538542
373c269b-e359-4ff1-938b-df4feeff4097	d205af05-e260-46df-b01e-808307bb624f	a5ed3d96-1181-4ecb-8ef5-f98959c4cf97	member	2026-07-20 16:05:18.538542
8db041da-718a-424f-bc33-44c40a76077e	d205af05-e260-46df-b01e-808307bb624f	69e2562d-328d-4a87-bf77-aa2a0a9199db	member	2026-07-20 16:05:18.538542
ea69c329-8963-44dc-a2eb-c19e42b0c66e	d205af05-e260-46df-b01e-808307bb624f	194b48ba-d3d5-44da-aae6-f0c6efcb7a24	member	2026-07-20 16:05:18.538542
ce7e19ee-d322-45f9-b5b9-771d3413f31b	d205af05-e260-46df-b01e-808307bb624f	f10781ef-8e93-481a-871b-fe4ea690f49f	member	2026-07-20 16:05:18.538542
ea9a31af-1b04-44f4-8d4f-a1d66ed7bf00	c4623d20-bbe5-478c-bb98-d64bed04a98d	165f75be-0399-4ddf-abcd-c89a3a48386c	leader	2026-07-20 16:05:18.538542
a3b3a9e6-f490-401a-adde-653a20900a4e	c4623d20-bbe5-478c-bb98-d64bed04a98d	40e6fee3-129d-47ac-966e-18e289aedb97	member	2026-07-20 16:05:18.538542
a8168102-e4c6-430a-a2e7-4e4c2fa8d9ba	c4623d20-bbe5-478c-bb98-d64bed04a98d	2f5230c2-c9b8-44fa-83ec-05bf6b7b7c01	member	2026-07-20 16:05:18.538542
cc7461ff-5eb8-4401-9194-adde0e296c70	c4623d20-bbe5-478c-bb98-d64bed04a98d	d13904ce-4e9c-433c-95d6-2451f07bef18	member	2026-07-20 16:05:18.538542
e11800c2-630b-482a-ba4a-0ac7ba2ce8d1	c4623d20-bbe5-478c-bb98-d64bed04a98d	a2d3a0ec-8add-44d2-a936-bd01b5639a7c	member	2026-07-20 16:05:18.538542
d2ad1a92-e4ae-484f-8c1f-827e4eaa2293	84e72391-6e71-44f3-af55-748bbb56e9f5	7bdbc5b9-0310-43d2-9f17-90102bba58db	leader	2026-07-20 16:05:18.538542
a51df9c9-fe36-4106-bd19-11f3c97b95a8	84e72391-6e71-44f3-af55-748bbb56e9f5	100e84d3-b2fb-425c-8b35-3729e689588e	member	2026-07-20 16:05:18.538542
31b54100-ae39-459f-9aa9-2ae848d1f3e5	84e72391-6e71-44f3-af55-748bbb56e9f5	e7363c10-3ed0-4613-a240-319842190baa	member	2026-07-20 16:05:18.538542
5e90461d-b02d-4443-aec0-368dd3434f9b	84e72391-6e71-44f3-af55-748bbb56e9f5	b8aff81f-bfc6-4dd9-b358-8600ea84a6fb	member	2026-07-20 16:05:18.538542
10f2054e-023a-430c-a34c-941d93bf5911	84e72391-6e71-44f3-af55-748bbb56e9f5	72a0e674-b0cc-42ca-9450-554803e841a5	member	2026-07-20 16:05:18.538542
a613359d-c045-4508-8dfd-c787a0d80934	cd4aa94c-f542-42f4-9f2e-aa82b708d73d	d8b0f9b4-13da-4c74-81c8-e50434d93793	leader	2026-07-20 16:05:18.538542
ccb280a0-abc0-4cbd-ae67-879379ddcd2c	cd4aa94c-f542-42f4-9f2e-aa82b708d73d	07d45e4f-e584-43ae-857d-9ed885cef57f	member	2026-07-20 16:05:18.538542
9c2767de-c8e9-44a3-9982-c6d7f2168d43	cd4aa94c-f542-42f4-9f2e-aa82b708d73d	6662ae40-923c-4349-acb7-658ee9f79c54	member	2026-07-20 16:05:18.538542
38ded5e5-54e2-47d2-a496-47ea036ae9ea	cd4aa94c-f542-42f4-9f2e-aa82b708d73d	8785a35a-0f05-4394-b8b7-e7084ea3399f	member	2026-07-20 16:05:18.538542
3fdcf54a-d50f-49cf-a9a7-636263c64241	cd4aa94c-f542-42f4-9f2e-aa82b708d73d	d685fa98-9e1d-45c3-b64d-5988b7302b02	member	2026-07-20 16:05:18.538542
1708cf27-ba9c-4079-a4ca-8963cf116ed0	71a29d76-beea-410d-b759-e016b48dafa5	eae6e74e-ac28-47c2-b38b-155e3e89553c	leader	2026-07-20 16:05:18.538542
40f3dddd-f806-4cd2-aa18-a4d5614a7f01	71a29d76-beea-410d-b759-e016b48dafa5	34e2928d-bed9-4776-8b22-fb363754f3c4	member	2026-07-20 16:05:18.538542
ef1f4752-896b-4666-bd0c-35a32fafdf29	71a29d76-beea-410d-b759-e016b48dafa5	860f5d22-d084-44ea-9643-e9a5f5afe187	member	2026-07-20 16:05:18.538542
86070a5c-833c-460d-93f2-24f2301acb43	71a29d76-beea-410d-b759-e016b48dafa5	83e82f7a-a5af-4937-b162-fbae703f36f7	member	2026-07-20 16:05:18.538542
156805b9-913c-485c-b1eb-ec0a041aa331	71a29d76-beea-410d-b759-e016b48dafa5	300872c5-23c3-4929-91e3-42b412da0f8d	member	2026-07-20 16:05:18.538542
ff03c385-89d3-4cdb-96a7-a9c090062922	324abf91-659e-48da-aadc-0fcd28573484	0c939f9b-f3f9-4d38-9eec-93e2304b64f3	leader	2026-07-20 16:05:18.538542
7c0b98fe-e467-4bde-86c4-b13872efd7a0	324abf91-659e-48da-aadc-0fcd28573484	d7e1414c-6c45-4349-a8b0-25bbc65d98a9	member	2026-07-20 16:05:18.538542
75057906-4886-4995-a9ad-9f1820837fc5	324abf91-659e-48da-aadc-0fcd28573484	2ec4fc87-e231-46db-be35-59089989642a	member	2026-07-20 16:05:18.538542
9ad192db-3821-479d-8480-3166cfb1286f	324abf91-659e-48da-aadc-0fcd28573484	02b24689-e5ca-4da4-82f4-f1f0ca679686	member	2026-07-20 16:05:18.538542
824328af-5833-4c2e-a83f-50e560a57d18	324abf91-659e-48da-aadc-0fcd28573484	c95ebe49-da2d-4d5d-98e0-1f3819715093	member	2026-07-20 16:05:18.538542
d814d73c-fcb8-458b-ab86-e7e0ea27c9f3	d511714b-6a95-437d-a294-f48bb4a76552	f225c3a7-7ed2-438d-85e9-a24cc25ca37c	leader	2026-07-20 16:05:18.538542
c9cc0083-8ade-40d2-824b-f80f06b8ee74	d511714b-6a95-437d-a294-f48bb4a76552	52ddc965-3aa9-440f-9b89-df69b1e0bd39	member	2026-07-20 16:05:18.538542
f266313e-038e-494a-8f04-7d3676d69edb	d511714b-6a95-437d-a294-f48bb4a76552	b3d416de-1d9e-4295-9514-4b4d1c85689a	member	2026-07-20 16:05:18.538542
10f52ac3-9795-4331-95ab-95eae1414f7c	d511714b-6a95-437d-a294-f48bb4a76552	2e75a5c8-0644-4a06-9a5c-e801402d0cfa	member	2026-07-20 16:05:18.538542
1584eb39-5873-4f35-9504-5c0839e7553e	d511714b-6a95-437d-a294-f48bb4a76552	538b9e6f-9e1a-4902-acc1-4accee1f68e4	member	2026-07-20 16:05:18.538542
559fe51f-c728-4b04-a97e-bcaf520c9551	af390c7d-5bb0-4dc9-95a9-1bf8402d9bde	78b6a018-ebb1-4a16-afd8-4f8d0a0d7954	leader	2026-07-20 16:05:18.538542
e9963a72-c5a4-46e6-88c1-af274e14acdc	af390c7d-5bb0-4dc9-95a9-1bf8402d9bde	9e691daa-21d5-406e-8c1f-bad5fe4d8e3b	member	2026-07-20 16:05:18.538542
a33d7c6d-e641-41d2-ac00-1edbc546c06e	af390c7d-5bb0-4dc9-95a9-1bf8402d9bde	0d5752c6-c60b-4f9e-a433-89abf9a5b15b	member	2026-07-20 16:05:18.538542
5d8d579b-7663-43f5-b084-57cb599efed2	af390c7d-5bb0-4dc9-95a9-1bf8402d9bde	5f40b8b0-a263-48b3-9c40-3f16ab39d3e7	member	2026-07-20 16:05:18.538542
2b0c1b79-ef3e-455d-9b54-fcf7de8deec7	af390c7d-5bb0-4dc9-95a9-1bf8402d9bde	6f8ab192-4cf7-47d8-8ac8-d9cb69117253	member	2026-07-20 16:05:18.538542
a54836d2-8c40-4c06-8817-8e0b212a1ecf	f5a276cf-acd1-40e3-9a88-4ff8c372fd69	4d922fba-ab0a-485c-9d30-77c8dc0bc3c8	leader	2026-07-20 16:05:18.538542
aa4e6ce5-608b-456f-bd8f-ef038811fb48	f5a276cf-acd1-40e3-9a88-4ff8c372fd69	62ea75cb-7d26-4f12-8cb1-22a6f3411930	member	2026-07-20 16:05:18.538542
0fba40dc-dd60-437d-9a87-9b148314feeb	f5a276cf-acd1-40e3-9a88-4ff8c372fd69	ed2efcde-2b9a-4816-910a-e2a31a65fbe4	member	2026-07-20 16:05:18.538542
32cad3be-d39d-4016-a278-5fc9c9f0ad1c	f5a276cf-acd1-40e3-9a88-4ff8c372fd69	47c6db2a-c336-4eb5-b039-e4fdebd8c353	member	2026-07-20 16:05:18.538542
0c77aba3-126b-4835-9022-c39089113c32	f5a276cf-acd1-40e3-9a88-4ff8c372fd69	428ab127-3a8c-4274-a905-29709eb4528a	member	2026-07-20 16:05:18.538542
8404c37b-e672-428e-8ffd-cd690ee9fc33	e2ea6c06-bade-4696-910b-25459fe9adf6	2107ba26-24e1-4bb5-8afc-6520b220f165	leader	2026-07-20 16:05:18.538542
9875e5af-75ef-497e-89b3-ca99bb2902cd	e2ea6c06-bade-4696-910b-25459fe9adf6	edb91073-4725-4914-89ec-880803b039e4	member	2026-07-20 16:05:18.538542
2e543efb-e900-4e78-bea0-759d5513f3d0	e2ea6c06-bade-4696-910b-25459fe9adf6	12b2d080-56dc-4d5f-ad84-35f050ffe46d	member	2026-07-20 16:05:18.538542
ea6e0972-28cc-4d6a-977c-6a7fcbd69171	e2ea6c06-bade-4696-910b-25459fe9adf6	b79e4f5a-246e-416e-9b12-2e00f527e7e8	member	2026-07-20 16:05:18.538542
c5195de9-f4d5-4bf9-964a-4038f8bf294d	e2ea6c06-bade-4696-910b-25459fe9adf6	8f7a6ea9-74a9-4900-8ea3-80f27068c57c	member	2026-07-20 16:05:18.538542
6329a9f9-3dd7-4be0-a856-45651b0d90cf	32c65b59-23a6-4911-a7fa-e998637f461e	9676b1d1-a16a-4286-b60b-24bc2da19ecf	leader	2026-07-20 16:05:18.538542
74993c28-67b7-4244-bd24-b8d46f391d7a	32c65b59-23a6-4911-a7fa-e998637f461e	1116a155-8617-44cb-ae61-9e3d11f33191	member	2026-07-20 16:05:18.538542
3cf90fb0-5419-41f6-8e2b-7834c2930715	32c65b59-23a6-4911-a7fa-e998637f461e	a77cf26d-232a-4afd-a33a-81876f1b079f	member	2026-07-20 16:05:18.538542
8ca85313-5d3e-4101-b06f-ad51826ba66e	32c65b59-23a6-4911-a7fa-e998637f461e	e5a48451-ff0a-403b-aaeb-3f8c8bdb999f	member	2026-07-20 16:05:18.538542
c58e543d-1deb-4c47-9ec7-f06a6f1fb97b	32c65b59-23a6-4911-a7fa-e998637f461e	ef628105-8f34-443f-a5d2-74a0d0139728	member	2026-07-20 16:05:18.538542
a9d10c17-8a08-4ce5-bd96-97ad728cf711	47e60ffb-8926-4ebc-8d98-315cbb3d123c	1a426158-18a8-48c9-906b-1c427a1851e9	leader	2026-07-20 16:05:18.538542
5d913316-9f78-43f2-bbf2-942d4cda07a9	47e60ffb-8926-4ebc-8d98-315cbb3d123c	27568529-c5b7-47d9-9dda-d6be88170714	member	2026-07-20 16:05:18.538542
ffe0cfe3-a7f4-41bf-9a6d-26842e5b30b5	47e60ffb-8926-4ebc-8d98-315cbb3d123c	1aab72b3-0334-49b8-974c-6b29e657ab14	member	2026-07-20 16:05:18.538542
b867a2a7-125e-4460-98d6-6c140edadb35	47e60ffb-8926-4ebc-8d98-315cbb3d123c	53d6119a-a32f-43b6-9dc9-e28e5eb10866	member	2026-07-20 16:05:18.538542
e42cb619-69ba-4c85-b12f-2de790aa5dbd	47e60ffb-8926-4ebc-8d98-315cbb3d123c	2d2c2e2a-4149-421b-9fb5-fb65f429a184	member	2026-07-20 16:05:18.538542
635d2206-ad91-4d66-8003-9322c5e47fb1	97c064ad-4d5a-4fc7-9fe7-092b232d0293	d0275d3c-02f4-4d50-861a-4cd5eec661b6	leader	2026-07-20 16:05:18.538542
70ac4fab-0d9b-477f-8ca5-4da32b6945a3	97c064ad-4d5a-4fc7-9fe7-092b232d0293	828d0627-b909-40c4-80e0-5ee455b215fe	member	2026-07-20 16:05:18.538542
cf85258d-5600-408c-be35-8a1e1521a92a	97c064ad-4d5a-4fc7-9fe7-092b232d0293	1a60021a-5cce-48fa-b567-a8b23bf413a7	member	2026-07-20 16:05:18.538542
4cd25b99-924e-46be-ad3f-1c852a6d397b	97c064ad-4d5a-4fc7-9fe7-092b232d0293	e4e96ad5-53c7-423e-af36-ea420e03bdc3	member	2026-07-20 16:05:18.538542
9059fa7a-6ced-4b7c-a626-702b9aee7cf1	97c064ad-4d5a-4fc7-9fe7-092b232d0293	7922e855-c5ee-4809-97f8-9f86b7b29bb1	member	2026-07-20 16:05:18.538542
19be4b5b-6e3f-432b-833c-01e64929d5fb	5b61fae6-04ce-473d-8efb-dad033a14711	b0e921c4-c0cc-44f8-a26b-f23779ecdd53	leader	2026-07-20 16:05:18.538542
e4f6300e-7210-484c-ba3c-32bf3a135446	5b61fae6-04ce-473d-8efb-dad033a14711	8f912c19-2931-4e44-91dc-ccb113a076b9	member	2026-07-20 16:05:18.538542
a8719cf9-ba26-4ea0-b56e-8abdf72a0335	5b61fae6-04ce-473d-8efb-dad033a14711	2ea5d823-06f8-47c4-8d4a-22702687842e	member	2026-07-20 16:05:18.538542
49953507-3165-41b7-844b-f3bab08df523	5b61fae6-04ce-473d-8efb-dad033a14711	aae29548-d234-4514-9d95-2e890cc512fb	member	2026-07-20 16:05:18.538542
5832b305-5a94-40ad-ac8d-826424d0c112	5b61fae6-04ce-473d-8efb-dad033a14711	3fd620cd-4f51-4809-a9df-63416a3605bc	member	2026-07-20 16:05:18.538542
613be22d-8e93-408d-bb06-2144ec7afb42	4a03b99d-150a-4d11-8da7-177f14d5c9a6	f6e0d867-34a9-46a4-843d-4824d7a32598	leader	2026-07-20 16:05:18.538542
258d9c7c-47b2-4b7f-94d7-063da74a67ce	4a03b99d-150a-4d11-8da7-177f14d5c9a6	db666de3-de7a-474e-933e-3384f6626cb9	member	2026-07-20 16:05:18.538542
613e315a-7235-49bc-9a1b-4f9ff14d952f	4a03b99d-150a-4d11-8da7-177f14d5c9a6	52f21c1c-3f76-4167-9a32-03eb688d6fbb	member	2026-07-20 16:05:18.538542
9f7b59e6-15a2-4e88-8982-aee8f9d34a8c	4a03b99d-150a-4d11-8da7-177f14d5c9a6	eb824860-4827-43ca-af31-8493b402453b	member	2026-07-20 16:05:18.538542
930c088c-e1c8-4648-9f57-43f3ede46519	4a03b99d-150a-4d11-8da7-177f14d5c9a6	c902375c-1f4d-4c50-a53a-f613d7c1958d	member	2026-07-20 16:05:18.538542
2bafa19e-f9de-433e-85ab-d9cbec2e1cd7	3a577d0c-bfea-4489-80ad-767fd052fd05	72de78e4-ff36-44c8-a924-d831dac3cb95	leader	2026-07-20 16:05:18.538542
3de92dc8-4678-42f5-b8a1-bc03dbc33490	3a577d0c-bfea-4489-80ad-767fd052fd05	225b4686-13f4-4944-994b-48190ad8f968	member	2026-07-20 16:05:18.538542
cbd4cda4-20e1-4e8b-929b-8d614f0062fc	3a577d0c-bfea-4489-80ad-767fd052fd05	ca8be037-daa0-4e24-8ec5-5884db29ad31	member	2026-07-20 16:05:18.538542
2d7cbce9-1053-4df8-b534-ccae1d2472b7	3a577d0c-bfea-4489-80ad-767fd052fd05	3b002876-a2a3-4633-b00d-de84be7b5c43	member	2026-07-20 16:05:18.538542
11541818-004b-43c7-9287-1967b131fce0	3a577d0c-bfea-4489-80ad-767fd052fd05	d17e4468-10c1-4f3e-8c0a-653cf71b795d	member	2026-07-20 16:05:18.538542
f05d0043-0fb7-4f80-b6bb-c05a2d9be97d	e0294f9e-1959-4cd1-b294-72bb01640eea	af819e7a-cb7f-4b4f-841b-b31545fbdcdd	leader	2026-07-20 16:05:18.538542
8226d68f-fc5a-4046-b1b9-db1039d72dd3	e0294f9e-1959-4cd1-b294-72bb01640eea	69077bc2-0da5-45a1-8d9a-9d5af2bca03e	member	2026-07-20 16:05:18.538542
33139728-f45a-4ccd-9497-27f45959fcc5	e0294f9e-1959-4cd1-b294-72bb01640eea	7cb76340-d751-44e1-9167-4c11230557f6	member	2026-07-20 16:05:18.538542
289b6cf5-a485-412f-8ba2-43c41809c815	e0294f9e-1959-4cd1-b294-72bb01640eea	332acaa1-7461-4d3e-ac61-74c0ebc57c78	member	2026-07-20 16:05:18.538542
fceffb0f-f3e5-46cd-bdd6-66216def11d5	e0294f9e-1959-4cd1-b294-72bb01640eea	1f4048c8-eda6-4241-95a0-1b9d2d1938b6	member	2026-07-20 16:05:18.538542
009b07dc-b83b-4b08-8b0b-df2493e91922	6a2bff8b-a2fb-4e88-9cfb-072f801f3c7a	2fca9cfe-9e1f-4a0b-a410-195ecf586967	leader	2026-07-20 16:05:18.538542
7f295716-bdcf-4a5c-99f1-0b40f0744c9a	6a2bff8b-a2fb-4e88-9cfb-072f801f3c7a	4ae33e93-3607-453a-92c2-c56573e11ae0	member	2026-07-20 16:05:18.538542
6babbacf-173b-4630-94c2-12f5f3c3b0d3	6a2bff8b-a2fb-4e88-9cfb-072f801f3c7a	748f4437-85ae-4671-8392-6334aaba4002	member	2026-07-20 16:05:18.538542
9b4a33ae-8e45-4e16-ab7a-e909d1d6999c	6a2bff8b-a2fb-4e88-9cfb-072f801f3c7a	bb9adeeb-148a-4e19-bc34-61818c51951b	member	2026-07-20 16:05:18.538542
fba7c4c5-770f-4415-b8d7-324c9dcc50be	6a2bff8b-a2fb-4e88-9cfb-072f801f3c7a	b6f669d6-6579-43e7-9c07-ea6dfd30d9aa	member	2026-07-20 16:05:18.538542
77f96d33-aae8-4eb7-b2a6-89ffa599790c	38eb8b6c-c4b0-402f-b8a9-de5133c53a84	f42a9548-746b-4f3f-bb05-824604cbc024	leader	2026-07-20 16:05:18.538542
375a41b6-ba2f-4e97-a3d4-d81209f15b3e	38eb8b6c-c4b0-402f-b8a9-de5133c53a84	908e5bc2-8a15-476d-807a-a10b71f703f0	member	2026-07-20 16:05:18.538542
a8b0d9ae-2290-4716-89fd-c4660bf23448	38eb8b6c-c4b0-402f-b8a9-de5133c53a84	71dc3b5d-592f-4115-a77e-293f45d0305a	member	2026-07-20 16:05:18.538542
a1653ca7-dfa6-4855-85be-199604c47982	38eb8b6c-c4b0-402f-b8a9-de5133c53a84	51f7ccd7-6a89-470f-9015-7fb18c038bca	member	2026-07-20 16:05:18.538542
c7358e13-f6f6-4782-9b4d-9c9efbe6c662	38eb8b6c-c4b0-402f-b8a9-de5133c53a84	164b6b97-25d0-43f9-b3e6-8e59f5bfc746	member	2026-07-20 16:05:18.538542
89ed2b70-213b-4f36-82cb-c7597757c504	af6709e8-9b63-4536-8f87-020044b8165e	bff69db3-83eb-4132-90ca-6b1318b38cae	leader	2026-07-20 16:05:18.538542
d9340e0b-800e-4a77-9623-f19a2afca614	af6709e8-9b63-4536-8f87-020044b8165e	862e215c-bfaa-4988-9622-88c47d860caf	member	2026-07-20 16:05:18.538542
9faf89f4-10c8-4e35-af42-5d0a1fd2d85b	af6709e8-9b63-4536-8f87-020044b8165e	5a17f6de-5036-45d3-a294-07da5d9eb33e	member	2026-07-20 16:05:18.538542
db07fa55-c4a8-48d7-827e-0400a326526a	af6709e8-9b63-4536-8f87-020044b8165e	5a746fb1-f102-4422-a4cc-25f1657812bf	member	2026-07-20 16:05:18.538542
18bf0c09-e596-42bd-841c-e9aedf972344	af6709e8-9b63-4536-8f87-020044b8165e	98876449-bbc6-470e-aef6-8ae388718bb5	member	2026-07-20 16:05:18.538542
835a8935-5e0f-4ef0-8d46-e3033c8c36bd	bc8b26ac-c9b3-4431-8c9f-c5fa75a20400	c51c1507-0f5b-47d5-9f80-3d5baa5ece75	leader	2026-07-20 16:05:18.538542
bb43a625-0fd8-4087-8cb4-30984e3c7ea8	bc8b26ac-c9b3-4431-8c9f-c5fa75a20400	4daa7be1-45c6-4d32-a0db-a908b0905d8c	member	2026-07-20 16:05:18.538542
c21c6bf1-3212-4f12-8ba0-4a35ead80d27	bc8b26ac-c9b3-4431-8c9f-c5fa75a20400	b6193f95-7899-4101-b342-75dc15d6c1fe	member	2026-07-20 16:05:18.538542
48063fa3-c903-4e91-9248-e169027d3385	bc8b26ac-c9b3-4431-8c9f-c5fa75a20400	aca27709-bfe1-4237-968d-86061c4e1a89	member	2026-07-20 16:05:18.538542
e3c5ca86-b14f-4d63-8db3-a282c6c4c2da	bc8b26ac-c9b3-4431-8c9f-c5fa75a20400	53ea1d0c-cf63-4633-8df0-a4d8f4ff1a9b	member	2026-07-20 16:05:18.538542
b3a17cdf-99f2-4f15-b39b-4e384abf03e4	23860630-2338-467d-b98f-18532762faa2	d3248a56-9ff8-4e0e-b0ac-e5d7e2806d33	leader	2026-07-20 16:05:18.538542
368a1777-d187-458e-a5fa-a8b705d67d79	23860630-2338-467d-b98f-18532762faa2	fcb76f4e-7530-4d25-b0ee-532f89bcd9fd	member	2026-07-20 16:05:18.538542
930b15f6-be10-4344-a336-e0ef8d0a701a	23860630-2338-467d-b98f-18532762faa2	84d107a9-9132-47ed-ab81-ce4f39ac2dbb	member	2026-07-20 16:05:18.538542
b42c71b9-ca44-4ae1-a0fb-a43b21f5b199	23860630-2338-467d-b98f-18532762faa2	ace33de1-628b-43d9-a0d2-1430f5e5e350	member	2026-07-20 16:05:18.538542
8c95e299-d2d4-437f-95d1-93732c44f443	23860630-2338-467d-b98f-18532762faa2	a564543b-d1b3-46a8-85ae-e08353919e2b	member	2026-07-20 16:05:18.538542
c10e34b3-331a-4f52-93f5-4653a923716d	ddad2704-ee2a-4000-87e9-79b3d75ffcf6	8870f78d-1b1f-4315-b374-3f2df2328739	leader	2026-07-20 16:05:18.538542
3462ac3c-70ac-4774-aa14-8d7f4bd67ad8	ddad2704-ee2a-4000-87e9-79b3d75ffcf6	eae623d2-549b-46fc-9eb5-a616c8a6a143	member	2026-07-20 16:05:18.538542
27e38210-d883-4a49-a537-da405447ec21	ddad2704-ee2a-4000-87e9-79b3d75ffcf6	5f3e503b-3db9-4489-a3d2-e124ec1ecf3a	member	2026-07-20 16:05:18.538542
cb1289c7-713a-4e95-8ba7-2e2b87864cb0	ddad2704-ee2a-4000-87e9-79b3d75ffcf6	1cf458dc-5f92-4aed-a579-eccafb920b46	member	2026-07-20 16:05:18.538542
3b840493-d71b-422a-941c-1410482011a2	ddad2704-ee2a-4000-87e9-79b3d75ffcf6	8f13e625-daa1-4046-8a2e-7c1d7edd77b9	member	2026-07-20 16:05:18.538542
e5b4706a-6fce-4b18-b138-2fc130d78950	83cf50f6-c524-41f0-9a7b-cd63e0a7b797	f8990fbb-3848-45ff-8b2e-e215616f7f17	leader	2026-07-20 16:05:18.538542
daa9aeae-83b8-41a9-bd0c-938e6b5cd11e	83cf50f6-c524-41f0-9a7b-cd63e0a7b797	c87ad2b9-ab7e-4bed-b366-dee826d86703	member	2026-07-20 16:05:18.538542
6b65ba1b-ad75-4c66-979f-760dc2f19955	83cf50f6-c524-41f0-9a7b-cd63e0a7b797	67028e60-2f00-4973-a1be-89238aae90ca	member	2026-07-20 16:05:18.538542
1f0a25c3-a69a-416e-b658-c96ea6e987d5	83cf50f6-c524-41f0-9a7b-cd63e0a7b797	b070e04b-de4c-442c-8a90-ca640de9ba32	member	2026-07-20 16:05:18.538542
80599653-0cf4-4c9e-9730-0fc5ae32454c	83cf50f6-c524-41f0-9a7b-cd63e0a7b797	a678073a-7617-47d2-87ce-21f4913918af	member	2026-07-20 16:05:18.538542
9d0cf55a-a8a5-45b7-96a4-653e70170ea5	2a876712-2d61-458d-9544-94fffb94b54a	e44d154e-9278-4fc3-a363-a033053d4d47	leader	2026-07-20 16:05:18.538542
47ea3760-b176-4c2a-b624-e76915f9d767	2a876712-2d61-458d-9544-94fffb94b54a	2cfdeb1d-2a10-426f-918d-17c4e93d7a2a	member	2026-07-20 16:05:18.538542
43b5d551-3489-41d5-966b-3606fc1dc6e6	2a876712-2d61-458d-9544-94fffb94b54a	99993bb1-c074-4e22-882d-c4dd4839c057	member	2026-07-20 16:05:18.538542
acf8180c-cc21-43b3-ad33-5a8a9b85bb40	2a876712-2d61-458d-9544-94fffb94b54a	066abec9-ef07-49f6-ad60-77372b51ed95	member	2026-07-20 16:05:18.538542
dd15d861-8b32-49bc-98e3-414cc9312628	2a876712-2d61-458d-9544-94fffb94b54a	3acf805e-845e-46b4-b604-5444235146c0	member	2026-07-20 16:05:18.538542
dcb9dfe7-cd0a-46a8-9513-b76c93c06e93	ddf2f727-ab17-45e3-8667-3d9de522b2f5	4b24ba59-b6cd-4d9f-a703-76d55d7a9841	leader	2026-07-20 16:05:18.538542
418db7fc-b8e8-463e-98e9-7eac4a1b5807	ddf2f727-ab17-45e3-8667-3d9de522b2f5	2003a597-f57b-405a-a33b-2564291e1c6e	member	2026-07-20 16:05:18.538542
58838ed0-74be-4743-9e7d-5f8927c70650	ddf2f727-ab17-45e3-8667-3d9de522b2f5	4cdf0aae-6be7-46c2-87cb-cfbe7a362553	member	2026-07-20 16:05:18.538542
86311d95-8ca6-4816-8698-3271e0506307	ddf2f727-ab17-45e3-8667-3d9de522b2f5	b5ce3dac-9d4e-44b1-89a5-b5cc0d5e8591	member	2026-07-20 16:05:18.538542
8e8d707f-fa7b-47ac-8278-2439a9d9e4d3	ddf2f727-ab17-45e3-8667-3d9de522b2f5	6c6b356a-f644-4f47-a585-51acf05510c0	member	2026-07-20 16:05:18.538542
1b2cb28e-6f99-4a65-a2ef-3bc57bd3141d	6fbd3939-4c39-4a6a-ba22-97f85652fbc1	4668d31d-9d4f-4224-8c57-55f7e1307fb9	leader	2026-07-20 16:05:18.538542
6df6c166-f77d-4d78-ac66-d5e298788da8	6fbd3939-4c39-4a6a-ba22-97f85652fbc1	c1bec177-c94c-4c41-9e11-89db28a3a436	member	2026-07-20 16:05:18.538542
d417b0da-affd-49ad-85d4-4c2a9971b8ff	6fbd3939-4c39-4a6a-ba22-97f85652fbc1	e425358d-0ebf-4a72-b8ff-704e98b9350e	member	2026-07-20 16:05:18.538542
bcbb31f9-0a66-4a6e-bfa0-31648765e89a	6fbd3939-4c39-4a6a-ba22-97f85652fbc1	fb40f875-3425-456d-b947-f332de0caf79	member	2026-07-20 16:05:18.538542
6dc8aac5-1a66-4d97-b2cf-01d79d3e625d	6fbd3939-4c39-4a6a-ba22-97f85652fbc1	9bf93423-27ef-4050-a869-ff8ae7dc7398	member	2026-07-20 16:05:18.538542
2a2f8295-76db-4a93-9bbc-9f303248c783	71c9068e-865a-4664-a273-ca632a08a90a	721b614b-22a0-4f08-b7e3-996eefdd3673	leader	2026-07-20 16:05:18.538542
39595680-212d-44da-9aca-7e7839d84cbb	71c9068e-865a-4664-a273-ca632a08a90a	5c2deb39-1aee-4382-b611-012943cca8b0	member	2026-07-20 16:05:18.538542
0abab52b-1dab-4580-a417-f786a2845652	71c9068e-865a-4664-a273-ca632a08a90a	201de504-3c7d-46c8-ba89-32113f932876	member	2026-07-20 16:05:18.538542
de008462-7f19-4498-968f-906897cede5e	71c9068e-865a-4664-a273-ca632a08a90a	85804d6a-54d2-4d08-9132-e08efc331fce	member	2026-07-20 16:05:18.538542
4e787cdd-4398-4ad0-ba62-6bc5857fc6db	71c9068e-865a-4664-a273-ca632a08a90a	15ce379d-c278-478c-bc3a-2f939c91afbb	member	2026-07-20 16:05:18.538542
4beb5baf-75fb-44c2-a6c7-a139e3faaf72	cfb4feb2-26b0-41ee-a771-0e7100a7203e	0c2f5d44-4ced-4353-a7e2-4d629f617158	leader	2026-07-20 16:05:18.538542
56074c9e-03a7-4adb-a22a-4bb690a8fc15	cfb4feb2-26b0-41ee-a771-0e7100a7203e	28e5ff84-819b-4514-88a1-ac101fc265c1	member	2026-07-20 16:05:18.538542
0c1ccab7-00ef-48e1-a97d-6408ab697553	cfb4feb2-26b0-41ee-a771-0e7100a7203e	81654a5f-b3cd-4058-80b9-2eb81bd9a193	member	2026-07-20 16:05:18.538542
d7f8d2b8-5972-4054-a0b7-7034546cdb3d	cfb4feb2-26b0-41ee-a771-0e7100a7203e	0dae5854-0971-474b-9ed1-78cc169ac87f	member	2026-07-20 16:05:18.538542
f52a221c-be55-4844-965a-9d50b2d35438	cfb4feb2-26b0-41ee-a771-0e7100a7203e	6dbe25df-9695-4859-a8f6-aa24ec24fbef	member	2026-07-20 16:05:18.538542
27b685fe-1ea2-4dca-a3a3-960d181e9608	8a5db466-e3ec-433f-a781-874d4fcb04b0	2c693a29-70c6-416f-a4ea-a47135770409	leader	2026-07-20 16:05:18.538542
afe8585b-f812-4b56-b4f3-a92a315a1cd5	8a5db466-e3ec-433f-a781-874d4fcb04b0	8754f344-d08b-4ae9-8db0-416b9611802a	member	2026-07-20 16:05:18.538542
675d8ed7-1790-40a6-ae08-18a5937a1295	8a5db466-e3ec-433f-a781-874d4fcb04b0	ab139287-cab6-4f9a-b576-dd0ab0446ef6	member	2026-07-20 16:05:18.538542
31fba8a5-53b5-440f-a3f6-2bd2d86865b6	8a5db466-e3ec-433f-a781-874d4fcb04b0	91b8b4cb-c307-45ee-9670-54a1f49582cd	member	2026-07-20 16:05:18.538542
0bd53a1f-4bde-4e65-ad8c-290545172f2e	8a5db466-e3ec-433f-a781-874d4fcb04b0	69da9c9a-60f5-48de-978f-aaba04fb89b8	member	2026-07-20 16:05:18.538542
d9d4d385-1d6a-4b22-9f62-03dfd8e7dde6	29032de7-54b8-4036-ad26-f63dd34ac354	355a4b8a-598c-4057-bd9e-14c5e9178cf4	leader	2026-07-20 16:05:18.538542
e39f39c7-095c-4dac-adff-2658e1dd3428	29032de7-54b8-4036-ad26-f63dd34ac354	43630b9d-b6bf-4d8b-9460-7469fd4180f0	member	2026-07-20 16:05:18.538542
5064369b-a3d0-484a-8586-bb69900fc3dd	29032de7-54b8-4036-ad26-f63dd34ac354	7e57fd81-5b7c-4bf2-8272-954417ef0206	member	2026-07-20 16:05:18.538542
9e9e5d24-6852-48fc-b303-06c9137a52bc	29032de7-54b8-4036-ad26-f63dd34ac354	f55c778a-7188-4676-8f64-02aee4567ce3	member	2026-07-20 16:05:18.538542
17a49c5d-fd89-4573-b951-209246af69ee	29032de7-54b8-4036-ad26-f63dd34ac354	81f8d309-90df-4dc6-ad0b-95da9ff29c3e	member	2026-07-20 16:05:18.538542
a6fc285c-d2f0-42fa-a9ea-8240ea161d07	61ff86d0-281c-4b1f-9cd1-f7647850711f	29d20518-78f0-41bd-a62a-5e8acbfb7016	leader	2026-07-20 16:05:18.538542
5ca8f532-e613-409f-b2b1-f369662f3415	61ff86d0-281c-4b1f-9cd1-f7647850711f	eab91587-aec2-49fb-8421-3d33cd6a5663	member	2026-07-20 16:05:18.538542
6794bfc4-9810-4c19-a50f-a3b241aa52b3	61ff86d0-281c-4b1f-9cd1-f7647850711f	790f27b8-b07a-4a89-8722-7e82eed6e662	member	2026-07-20 16:05:18.538542
a5920aa0-7b46-46b7-9cdd-bd8326d28de3	61ff86d0-281c-4b1f-9cd1-f7647850711f	50f96c31-d149-49e3-a238-b51454b2c388	member	2026-07-20 16:05:18.538542
47523836-78b1-4d4a-91a9-29e2e21a4fc5	61ff86d0-281c-4b1f-9cd1-f7647850711f	3bf95b77-9bc1-4546-b400-5c9442968636	member	2026-07-20 16:05:18.538542
5dae9986-9524-497f-a90c-f06efdf13e60	0b23000e-b11b-4020-9367-86a0b91f517d	8b089e4b-c40d-4b33-9c0a-f82676254a8c	leader	2026-07-20 16:05:18.538542
a719bf04-ab0a-475e-b0be-74adf7431f87	0b23000e-b11b-4020-9367-86a0b91f517d	7a295313-5833-4738-a7cb-a69c5c38a75a	member	2026-07-20 16:05:18.538542
57d44e55-d637-4354-80af-31e5c392b123	0b23000e-b11b-4020-9367-86a0b91f517d	220560c8-51f5-43c6-9808-33ded241fb40	member	2026-07-20 16:05:18.538542
fb61f9a6-fb55-4ed1-8b35-0cbbdd3fd074	0b23000e-b11b-4020-9367-86a0b91f517d	d1b9bd2c-a515-4478-83e4-28d9a8d1a836	member	2026-07-20 16:05:18.538542
1a168d55-79af-4d9e-8676-767e820f752b	0b23000e-b11b-4020-9367-86a0b91f517d	beba5d84-b26a-4f1a-9e80-3bfcafade02f	member	2026-07-20 16:05:18.538542
10128271-1372-4c66-9f24-1e43efee1038	335f5a1a-66d5-4d08-b599-98441e5b1655	6167279d-c6b7-48e4-a24a-1adb7d472f8c	leader	2026-07-20 16:05:18.538542
7925edf4-f9ac-47e7-8b03-d6e10490665b	335f5a1a-66d5-4d08-b599-98441e5b1655	15b847fc-b0fb-4ba1-bf7d-072c296436a4	member	2026-07-20 16:05:18.538542
528775ca-17a0-4c70-a411-68b25235cc50	335f5a1a-66d5-4d08-b599-98441e5b1655	df8ff35a-c3c5-4f4a-88a7-12d68bbc7082	member	2026-07-20 16:05:18.538542
f2da3bc1-8907-4834-b7d4-ca271c4e9f0f	335f5a1a-66d5-4d08-b599-98441e5b1655	082481c4-0220-4fe1-b404-26bd0c32f019	member	2026-07-20 16:05:18.538542
327f8450-e349-427a-9da9-9ffce4b3e67a	335f5a1a-66d5-4d08-b599-98441e5b1655	5491474d-c13e-4dd4-bd03-7e382cd0c4f2	member	2026-07-20 16:05:18.538542
1e2b729e-b80d-4b43-8d1b-db5a3eb640cf	c9be444c-6737-407d-8e7f-31f68b208743	12bbd935-08cc-44d5-a435-72a44fef7f57	leader	2026-07-20 16:05:18.538542
c46545c7-1d01-42f9-aa0b-855760626b3f	c9be444c-6737-407d-8e7f-31f68b208743	05d7c568-0e47-4ff9-863a-e62df00d2137	member	2026-07-20 16:05:18.538542
26942628-d6c3-4c37-88b2-988fb773e133	c9be444c-6737-407d-8e7f-31f68b208743	e567ce9d-86f7-4d5a-813d-d5f3e7cd122d	member	2026-07-20 16:05:18.538542
8cf8ef78-4ac7-4949-a8fa-7b65d665fb7e	c9be444c-6737-407d-8e7f-31f68b208743	75ab45f8-6c59-4509-bb0a-f6e30d881683	member	2026-07-20 16:05:18.538542
9a0cac33-4629-433c-a7a0-ea42d1fcd6a0	c9be444c-6737-407d-8e7f-31f68b208743	1ba988f5-b8d8-4cd3-a538-e3e0ee5aadb9	member	2026-07-20 16:05:18.538542
0122d38f-2fff-4760-9d50-2c07ddf21b64	d2c7f836-def8-4ae8-b768-5fa59f28c21d	596f1fd0-ec69-405a-b02c-3ebdf9c99cd9	leader	2026-07-20 16:05:18.538542
12ab7083-f7d2-4b69-9562-68a0e6af2500	d2c7f836-def8-4ae8-b768-5fa59f28c21d	65d1696d-c510-4d32-a121-56571026b694	member	2026-07-20 16:05:18.538542
8a49b405-b872-4bb4-a63f-d338103302e1	d2c7f836-def8-4ae8-b768-5fa59f28c21d	c1e69b6d-e76d-40b5-8691-d416adc9d700	member	2026-07-20 16:05:18.538542
e98ea2ba-e290-4682-beae-1a4456fbaa7d	d2c7f836-def8-4ae8-b768-5fa59f28c21d	42975653-b484-4957-9c49-abd8bcf99840	member	2026-07-20 16:05:18.538542
e679555e-7647-414a-88fd-25b334692a0e	d2c7f836-def8-4ae8-b768-5fa59f28c21d	c1d031a8-4429-4ff8-812e-aeb8f13400fe	member	2026-07-20 16:05:18.538542
dc09f981-402a-4301-86ca-6b616376ee60	8fd110ae-394a-49b0-9d9e-1594d07f3f7c	b97fffe1-12dd-4dec-9858-16b30312dd52	leader	2026-07-20 16:05:18.538542
f784d543-437f-4278-aab7-aa9123e33a46	8fd110ae-394a-49b0-9d9e-1594d07f3f7c	8fcf12ca-575a-4c2f-b8cd-48335f8b4ccc	member	2026-07-20 16:05:18.538542
e98811fb-cc6b-4298-bec6-c2442e22ecf5	8fd110ae-394a-49b0-9d9e-1594d07f3f7c	c304a29d-ed17-4f24-8537-afe1e5cee510	member	2026-07-20 16:05:18.538542
2963dded-e1c4-46d9-a196-ba07e107787a	8fd110ae-394a-49b0-9d9e-1594d07f3f7c	1692d7b2-14f6-46c4-b92a-1c925ed550a9	member	2026-07-20 16:05:18.538542
8ff79a4d-ec62-4b9c-910b-26ec3ad01062	8fd110ae-394a-49b0-9d9e-1594d07f3f7c	21d68ab3-3acc-47a9-8c57-3986d83d8961	member	2026-07-20 16:05:18.538542
cb9bf7ee-da82-4c88-a090-12b7a7781a9c	79fc6452-9e3c-4ceb-992d-43f4cdfbaa5b	0d47b41a-b9ce-49cd-bccf-c2d519e7ad0e	leader	2026-07-20 16:05:18.538542
fdffb663-6c6a-411e-b685-97d8a024f9b3	79fc6452-9e3c-4ceb-992d-43f4cdfbaa5b	7b6b68ae-8d59-402d-b616-6879c86b0176	member	2026-07-20 16:05:18.538542
2e710b12-595a-4547-ba19-c9cc65d8a317	79fc6452-9e3c-4ceb-992d-43f4cdfbaa5b	9a0f8c96-90f5-4bba-9104-6444a3579814	member	2026-07-20 16:05:18.538542
6c4bc87a-1089-44c1-aaf0-2ee523be4046	79fc6452-9e3c-4ceb-992d-43f4cdfbaa5b	8a2bb866-8704-4cdd-b58b-673d7eeacdfb	member	2026-07-20 16:05:18.538542
9927590b-0386-4f38-a23a-63bf04d24bc4	79fc6452-9e3c-4ceb-992d-43f4cdfbaa5b	2091b2ed-d649-4ce5-a935-2445e4238c9f	member	2026-07-20 16:05:18.538542
71dea0bf-0412-47d0-b720-76371be0118b	df7a8f14-422d-441c-b5aa-155fed861d8a	7f5de3c3-d202-45c6-a796-b0035e005ef4	leader	2024-03-22 17:00:00
7af7cd77-28ca-4300-9153-b47e3591cacf	df7a8f14-422d-441c-b5aa-155fed861d8a	ff9f5da7-bca5-4a14-a67d-d05e021cfc92	member	2024-03-22 17:00:00
22352f4b-f151-4645-b3b0-76c0a76e4ed0	df7a8f14-422d-441c-b5aa-155fed861d8a	58b50b1a-27f0-4eb5-9e1c-cc524d6fb016	member	2024-03-22 17:00:00
87bc6024-ecce-4541-8c04-1aa238ec4f26	df7a8f14-422d-441c-b5aa-155fed861d8a	6d6003f6-4b94-4960-888b-5c5cb1d070ba	member	2024-03-22 17:00:00
2ca40b76-0879-4dff-ab8f-f5774d3d0351	df7a8f14-422d-441c-b5aa-155fed861d8a	3639c58e-a515-4fe6-b35b-4af11e42c2cf	member	2024-03-22 17:00:00
77b41979-0be7-4695-b3ab-398824e8d201	5545fb21-255a-4df1-be04-6a9387052429	7f5de3c3-d202-45c6-a796-b0035e005ef4	leader	2025-09-19 17:00:00
085dd220-0404-4840-8d61-b24e559984c9	5545fb21-255a-4df1-be04-6a9387052429	ff9f5da7-bca5-4a14-a67d-d05e021cfc92	member	2025-09-19 17:00:00
73688c54-3cdf-4d61-97d4-23df89027321	5545fb21-255a-4df1-be04-6a9387052429	58b50b1a-27f0-4eb5-9e1c-cc524d6fb016	member	2025-09-19 17:00:00
fdd44eab-eb32-4123-b623-ccb60a7d9e9a	5545fb21-255a-4df1-be04-6a9387052429	6d6003f6-4b94-4960-888b-5c5cb1d070ba	member	2025-09-19 17:00:00
68148d46-a152-4049-8f83-17c97d4bfb82	5545fb21-255a-4df1-be04-6a9387052429	3639c58e-a515-4fe6-b35b-4af11e42c2cf	member	2025-09-19 17:00:00
5e8aadd1-cecc-44cb-bfef-808248f6727d	7977250b-57f9-4049-8b82-47051309b353	7f5de3c3-d202-45c6-a796-b0035e005ef4	leader	2026-06-13 17:00:00
a4fc1d5d-b4bc-4e14-bb68-ff16d522eeaa	7977250b-57f9-4049-8b82-47051309b353	ff9f5da7-bca5-4a14-a67d-d05e021cfc92	member	2026-06-13 17:00:00
c6774b1b-5852-4868-8fec-762232982372	7977250b-57f9-4049-8b82-47051309b353	58b50b1a-27f0-4eb5-9e1c-cc524d6fb016	member	2026-06-13 17:00:00
da57279c-ccc7-4ae3-a596-cb0ad935679c	7977250b-57f9-4049-8b82-47051309b353	6d6003f6-4b94-4960-888b-5c5cb1d070ba	member	2026-06-13 17:00:00
11d605b1-254c-45b1-99c6-4e9ae07cf9e4	7977250b-57f9-4049-8b82-47051309b353	3639c58e-a515-4fe6-b35b-4af11e42c2cf	member	2026-06-13 17:00:00
\.


--
-- Data for Name: team_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_profiles (id, canonical_name, logo_url, created_by, status, created_at, updated_at, row_version) FROM stdin;
2574ca0f-9b7f-4f52-af3a-39fc72077f27	i	\N	ac5dc174-c461-4b2b-aff3-c8a9acd02f8f	active	2026-07-20 15:41:39.30836	2026-07-20 15:41:39.30836	0
08c16751-ef2e-417b-8384-7216321ea03f	Regression Team 232026	\N	b9debf4b-c200-4907-abdf-5ce2208f8eb7	active	2026-07-20 15:41:39.30836	2026-07-20 15:41:39.30836	0
928af9e2-4817-45a4-a2df-2e5ce27d9c85	Vô địch	\N	83bbb9b4-c32e-4b55-aea4-eb3e294f71b1	active	2026-07-20 15:41:39.30836	2026-07-20 15:41:39.30836	0
06c4fef8-54b0-4113-bbed-8699b1a20d8c	DEMO Team	\N	a1000000-0000-4000-8000-000000000004	active	2026-07-20 15:41:39.30836	2026-07-20 15:41:39.30836	0
1eb1871e-1266-40d9-855b-d07c7e79b248	Load Test Team 001	\N	ac5e1fad-f17e-4e32-9983-82fbb0ef4566	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
0eddac4b-d8e6-4fbc-af30-8e5a5868a887	Load Test Team 002	\N	e80f247f-d89a-4f20-acb4-85201cf56843	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
39d4bd27-2551-4a6b-8bdf-805e1d8b5a2d	Load Test Team 003	\N	f573bdcf-0392-4bbd-9d86-0f10b5a9d10d	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
67ffb48c-c7b2-45ec-90fa-202d6112636c	Load Test Team 004	\N	8033af9a-0e8a-42a6-b8cb-2520757b51e7	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
7df592b2-a059-4e98-a7f1-20af965bf5a8	Load Test Team 005	\N	45c59a91-6d75-4875-9e5b-5fc18a962917	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
40e6eb07-5cb0-49d9-a94b-9aa75ee6d5d8	Load Test Team 006	\N	b70c1256-ecae-4639-b6ca-c2db0b3f6d2e	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
fba7ca51-9625-4d6d-9516-a4e8c2db795b	Load Test Team 007	\N	85493223-f120-4d65-870c-e350628c21f7	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
865a9828-ec75-4317-90d3-06f4a92001fa	Load Test Team 008	\N	797b66d7-5020-46fb-9daf-115c739fd016	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
06f2aa30-6f82-4347-aa4d-0fb75d782174	Load Test Team 009	\N	57db3f95-400e-4e77-bdbc-4a80d8859814	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
90336c87-29d3-430b-ad3e-f1a439e9f46c	Load Test Team 010	\N	6e84f560-95ea-418b-9b2d-350a0aed795b	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
79cd02d6-5d34-4754-b42f-b971c8f6144d	Load Test Team 011	\N	7dcf36c2-b8c6-411c-b5b3-d8fb6df96e87	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
55383a26-a11e-48be-8100-b418d5f07bd2	Load Test Team 012	\N	19a9fb26-5dbe-4ef0-a5ea-a32d88f242f8	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
18875f93-ce72-4904-8b31-98af3c4e2eaa	Load Test Team 013	\N	ff9fd982-afbd-4be1-af50-2c19472bf3a4	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
39c58db1-d4ba-48d5-9028-e93f4579b7b5	Load Test Team 014	\N	165f75be-0399-4ddf-abcd-c89a3a48386c	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
edb0f6b2-e3a1-4edd-ad8d-fdd9d36937bc	Load Test Team 015	\N	7bdbc5b9-0310-43d2-9f17-90102bba58db	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
5477acfb-d74b-4670-9196-081ce74a191b	Load Test Team 016	\N	d8b0f9b4-13da-4c74-81c8-e50434d93793	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
c68cee03-14d9-417f-8ad5-52ade16c6716	Load Test Team 017	\N	eae6e74e-ac28-47c2-b38b-155e3e89553c	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
09062d7b-67d5-4e51-86bf-d261a24b059a	Load Test Team 018	\N	0c939f9b-f3f9-4d38-9eec-93e2304b64f3	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
116fad75-f55c-4ef5-9e46-cb95d049e763	Load Test Team 019	\N	f225c3a7-7ed2-438d-85e9-a24cc25ca37c	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
28d74ff3-e3a7-4eda-971a-ea5e234b54c0	Load Test Team 020	\N	78b6a018-ebb1-4a16-afd8-4f8d0a0d7954	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
85b3a045-b2dc-4907-9b81-0baf7b5ad8fd	Load Test Team 021	\N	4d922fba-ab0a-485c-9d30-77c8dc0bc3c8	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
5547c30a-ec1d-4098-b6a4-c6407cf095a0	Load Test Team 022	\N	2107ba26-24e1-4bb5-8afc-6520b220f165	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
6bdca292-7471-4c7c-ba4d-78f076fa1619	Load Test Team 023	\N	9676b1d1-a16a-4286-b60b-24bc2da19ecf	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
dfc94064-34b9-4879-b807-3bc3c38cf5fd	Load Test Team 024	\N	1a426158-18a8-48c9-906b-1c427a1851e9	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
e8612462-ea7e-4d86-a8ac-4bc79f91c021	Load Test Team 025	\N	d0275d3c-02f4-4d50-861a-4cd5eec661b6	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
eeb266de-c6e0-4ae6-b0ea-a28d881ad538	Load Test Team 026	\N	b0e921c4-c0cc-44f8-a26b-f23779ecdd53	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
df3344a3-2ee5-4875-900b-dcbf51fd4a10	Load Test Team 027	\N	f6e0d867-34a9-46a4-843d-4824d7a32598	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
05cb91a6-80ca-49bd-83c8-909836b32418	Load Test Team 028	\N	72de78e4-ff36-44c8-a924-d831dac3cb95	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
d6e8b63f-9ced-4bd0-86d9-fd302a118e19	Load Test Team 029	\N	af819e7a-cb7f-4b4f-841b-b31545fbdcdd	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
c71fa126-98d2-48f2-801f-e03f4ce11745	Load Test Team 030	\N	2fca9cfe-9e1f-4a0b-a410-195ecf586967	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
a4ee6770-6195-4297-acf8-009c00a7a454	Load Test Team 031	\N	f42a9548-746b-4f3f-bb05-824604cbc024	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
03212bd1-3187-424e-a0d7-7b1ab4d70ac9	Load Test Team 032	\N	bff69db3-83eb-4132-90ca-6b1318b38cae	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
94ce2fa7-391a-4496-8727-044ebdcbcf4a	Load Test Team 033	\N	c51c1507-0f5b-47d5-9f80-3d5baa5ece75	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
8f2951a6-a080-4558-926e-4e220522f3eb	Load Test Team 034	\N	d3248a56-9ff8-4e0e-b0ac-e5d7e2806d33	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
5dda4a2d-ff36-434e-9612-c8fba3f16ff3	Load Test Team 035	\N	8870f78d-1b1f-4315-b374-3f2df2328739	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
7501b048-25bc-4044-b705-4053c17e3de4	Load Test Team 036	\N	f8990fbb-3848-45ff-8b2e-e215616f7f17	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
2b3984d5-f786-49fa-bd58-d81f6f46c5e2	Load Test Team 037	\N	e44d154e-9278-4fc3-a363-a033053d4d47	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
c5c4003a-d1d3-491d-ae75-1efd8803388b	Load Test Team 038	\N	4b24ba59-b6cd-4d9f-a703-76d55d7a9841	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
5bca7cc3-1ac7-4ec0-88f9-858a2951e871	Load Test Team 039	\N	4668d31d-9d4f-4224-8c57-55f7e1307fb9	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
1e79849a-60c9-4aad-9a26-cb226c1d1f1b	Load Test Team 040	\N	721b614b-22a0-4f08-b7e3-996eefdd3673	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
eb3c0204-85fc-416d-9ce0-c02d67c51748	Load Test Team 041	\N	0c2f5d44-4ced-4353-a7e2-4d629f617158	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
f0e47356-561b-4acf-bc33-d82943989aae	Load Test Team 042	\N	2c693a29-70c6-416f-a4ea-a47135770409	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
ddefd614-f2cd-42d2-8941-c97d4c883f95	Load Test Team 043	\N	355a4b8a-598c-4057-bd9e-14c5e9178cf4	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
b68da9fc-d398-4ddf-a86e-ea465d0b5311	Load Test Team 044	\N	29d20518-78f0-41bd-a62a-5e8acbfb7016	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
95243f36-df18-4152-b9d4-37ece7607f36	Load Test Team 045	\N	8b089e4b-c40d-4b33-9c0a-f82676254a8c	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
1543f7bf-f816-4659-8cb8-934205171d81	Load Test Team 046	\N	6167279d-c6b7-48e4-a24a-1adb7d472f8c	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
7d987a6f-1c9f-4595-9fbb-61cdb171b799	Load Test Team 047	\N	12bbd935-08cc-44d5-a435-72a44fef7f57	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
d14d8334-aabc-4d13-9280-5128346b4aaa	Load Test Team 048	\N	596f1fd0-ec69-405a-b02c-3ebdf9c99cd9	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
8da36fb6-f70f-4d59-b98a-7a5cfa8bf644	Load Test Team 049	\N	b97fffe1-12dd-4dec-9858-16b30312dd52	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
1bdfc693-e7f9-4d4a-9e8e-b421ec2aaed3	Load Test Team 050	\N	0d47b41a-b9ce-49cd-bccf-c2d519e7ad0e	active	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0
1de43813-38b1-4ebe-a7e9-f08d67dffa3b	Veteran Phoenix Team	\N	7f5de3c3-d202-45c6-a796-b0035e005ef4	active	2026-07-20 16:17:20.991673	2026-07-20 16:17:20.991673	0
\.


--
-- Data for Name: team_recognitions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_recognitions (id, team_profile_id, recognition_code, label, qualification_count, earned_at, active, revoked_at, revoked_by, revoke_reason, created_at, updated_at, row_version) FROM stdin;
3e82ef34-5b63-406b-bcca-80c2eec280e5	1de43813-38b1-4ebe-a7e9-f08d67dffa3b	HACKATHON_VETERAN	Hackathon Veteran	3	2026-07-12 17:00:00	t	\N	\N	\N	2026-07-12 17:00:00	2026-07-12 17:00:00	0
\.


--
-- Data for Name: team_timeline_events; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_timeline_events (id, event_id, team_id, round_id, type, title, description, score_snapshot, rank_snapshot, status_snapshot, occurred_at, visibility_scope, event_type, source_type, source_id, track_id, submission_id, appeal_id, incident_id, support_ticket_id, seed_id, recognition_id, metadata, idempotency_key) FROM stdin;
e047d3b2-47de-4a89-a994-00870caf7ef3	7f8d71f3-2086-4cfe-a6e2-2497c674dcc4	\N	\N	EVENT_PUBLISHED	Registration opened	Event registration is now open	\N	\N	\N	2026-07-20 01:59:33.407345	EVENT_PUBLIC	EVENT_PUBLISHED	EVENT_STATUS	7f8d71f3-2086-4cfe-a6e2-2497c674dcc4	\N	\N	\N	\N	\N	\N	\N	\N	event:7f8d71f3-2086-4cfe-a6e2-2497c674dcc4:published
659c96a3-5916-4c4d-93f8-e476258d054f	66c58f1a-b289-42e1-8d22-83f6031b60a8	\N	\N	EVENT_REGISTRATION_CLOSED	Registration closed	Event registration has closed	\N	\N	\N	2026-07-20 02:08:07.662741	EVENT_PUBLIC	EVENT_REGISTRATION_CLOSED	EVENT_STATUS	66c58f1a-b289-42e1-8d22-83f6031b60a8	\N	\N	\N	\N	\N	\N	\N	\N	event:66c58f1a-b289-42e1-8d22-83f6031b60a8:registration-closed
1833ec1c-1bcd-4cee-abab-505e092dcf5a	66c58f1a-b289-42e1-8d22-83f6031b60a8	d7712ef4-9dc5-4e15-9033-23f52a8e5810	\N	TEAM_DISQUALIFIED	Team disqualified	The team was disqualified	\N	\N	\N	2026-07-20 02:09:11.743954	EVENT_PARTICIPANTS	TEAM_DISQUALIFIED	TEAM	d7712ef4-9dc5-4e15-9033-23f52a8e5810	5c74ef5c-96ae-4810-88f8-1cb9168e8142	\N	\N	\N	\N	\N	\N	\N	team:d7712ef4-9dc5-4e15-9033-23f52a8e5810:disqualified:2026-07-20T23:05:18.538542
0acde45c-6bce-4021-b960-304132dc5e6c	c6484635-5026-4486-943a-8974b2176a3d	7977250b-57f9-4049-8b82-47051309b353	\N	recognition	Hackathon Veteran earned	The team completed qualifying finishes in three distinct hackathon seasons.	\N	\N	active	2026-07-12 17:00:00	EVENT_PARTICIPANTS	TEAM_RECOGNITION_EARNED	TEAM_RECOGNITION	3e82ef34-5b63-406b-bcca-80c2eec280e5	a9a53e66-eb94-42ce-8335-cbe4f1a574df	\N	\N	\N	\N	\N	3e82ef34-5b63-406b-bcca-80c2eec280e5	{"recognitionCode": "HACKATHON_VETERAN", "qualificationCount": 3}	team-profile:1de43813-38b1-4ebe-a7e9-f08d67dffa3b:recognition:HACKATHON_VETERAN:earned
\.


--
-- Data for Name: teams; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.teams (id, track_id, name, status, disqualified_reason, created_at, updated_at, invite_code, team_profile_id, source_team_id, activated_from_profile_at, roster_confirmed_at, roster_confirmed_by) FROM stdin;
2431c70e-5860-4536-9f99-34aabbafa6e7	5ff86b4c-12b5-4701-bc24-c5ab4cea0307	i	active	\N	2026-07-19 10:41:04.92559	2026-07-20 15:41:39.30836	8C664B	2574ca0f-9b7f-4f52-af3a-39fc72077f27	\N	\N	\N	\N
5f12db5b-b46e-440b-98e4-bc6b0b592d74	e18a5239-caf9-4997-8c11-e0bea2ca0f6e	Regression Team 232026	active	\N	2026-07-02 16:20:27.057818	2026-07-20 15:41:39.30836	3C865E	08c16751-ef2e-417b-8384-7216321ea03f	\N	\N	\N	\N
75ff0336-137b-437c-9142-0b6e08afaf76	5ff86b4c-12b5-4701-bc24-c5ab4cea0307	Vô địch	active	\N	2026-07-19 10:26:00.328651	2026-07-20 15:41:39.30836	8B51D9	928af9e2-4817-45a4-a2df-2e5ce27d9c85	\N	\N	\N	\N
aaaa0000-0000-4000-8000-000000000004	aaaa0000-0000-4000-8000-000000000002	DEMO Team	active	\N	2026-07-10 02:49:49.985313	2026-07-20 15:41:39.30836	DEMO2026	06c4fef8-54b0-4113-bbed-8699b1a20d8c	\N	\N	\N	\N
ac4a34dd-3533-443f-ac54-4b2620492d06	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 002	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000002	0eddac4b-d8e6-4fbc-af30-8e5a5868a887	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	e80f247f-d89a-4f20-acb4-85201cf56843
84163cb0-561f-4d35-bbc2-5711274e3760	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 003	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000003	39d4bd27-2551-4a6b-8bdf-805e1d8b5a2d	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	f573bdcf-0392-4bbd-9d86-0f10b5a9d10d
343b705b-dbda-431d-9783-13d6d65090a9	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 004	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000004	67ffb48c-c7b2-45ec-90fa-202d6112636c	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	8033af9a-0e8a-42a6-b8cb-2520757b51e7
4a888f3e-b823-4e48-a89d-78717f81523a	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 005	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000005	7df592b2-a059-4e98-a7f1-20af965bf5a8	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	45c59a91-6d75-4875-9e5b-5fc18a962917
6f9c5467-188a-4e4c-8ad4-2a7385d61e07	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 006	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000006	40e6eb07-5cb0-49d9-a94b-9aa75ee6d5d8	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	b70c1256-ecae-4639-b6ca-c2db0b3f6d2e
69eef567-52af-4cf3-b628-7b187e28bbff	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 007	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000007	fba7ca51-9625-4d6d-9516-a4e8c2db795b	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	85493223-f120-4d65-870c-e350628c21f7
6e028ee2-c138-4374-b935-447baa612216	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 008	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000008	865a9828-ec75-4317-90d3-06f4a92001fa	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	797b66d7-5020-46fb-9daf-115c739fd016
2cadc36f-ebfa-442e-b495-8d5247a44c43	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 009	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000009	06f2aa30-6f82-4347-aa4d-0fb75d782174	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	57db3f95-400e-4e77-bdbc-4a80d8859814
8070dc1e-a71b-4120-8a3d-b330115742d5	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 010	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000010	90336c87-29d3-430b-ad3e-f1a439e9f46c	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	6e84f560-95ea-418b-9b2d-350a0aed795b
f5c72ff9-25fe-48c7-a7e7-2855bb6f3a27	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 011	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000011	79cd02d6-5d34-4754-b42f-b971c8f6144d	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	7dcf36c2-b8c6-411c-b5b3-d8fb6df96e87
f244d75a-e5ec-43f9-91bb-d84bdbd02e93	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 012	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000012	55383a26-a11e-48be-8100-b418d5f07bd2	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	19a9fb26-5dbe-4ef0-a5ea-a32d88f242f8
d205af05-e260-46df-b01e-808307bb624f	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 013	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000013	18875f93-ce72-4904-8b31-98af3c4e2eaa	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	ff9fd982-afbd-4be1-af50-2c19472bf3a4
c4623d20-bbe5-478c-bb98-d64bed04a98d	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 014	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000014	39c58db1-d4ba-48d5-9028-e93f4579b7b5	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	165f75be-0399-4ddf-abcd-c89a3a48386c
84e72391-6e71-44f3-af55-748bbb56e9f5	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 015	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000015	edb0f6b2-e3a1-4edd-ad8d-fdd9d36937bc	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	7bdbc5b9-0310-43d2-9f17-90102bba58db
cd4aa94c-f542-42f4-9f2e-aa82b708d73d	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 016	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000016	5477acfb-d74b-4670-9196-081ce74a191b	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	d8b0f9b4-13da-4c74-81c8-e50434d93793
71a29d76-beea-410d-b759-e016b48dafa5	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 017	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000017	c68cee03-14d9-417f-8ad5-52ade16c6716	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	eae6e74e-ac28-47c2-b38b-155e3e89553c
324abf91-659e-48da-aadc-0fcd28573484	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 018	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000018	09062d7b-67d5-4e51-86bf-d261a24b059a	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0c939f9b-f3f9-4d38-9eec-93e2304b64f3
d511714b-6a95-437d-a294-f48bb4a76552	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 019	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000019	116fad75-f55c-4ef5-9e46-cb95d049e763	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	f225c3a7-7ed2-438d-85e9-a24cc25ca37c
af390c7d-5bb0-4dc9-95a9-1bf8402d9bde	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 020	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000020	28d74ff3-e3a7-4eda-971a-ea5e234b54c0	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	78b6a018-ebb1-4a16-afd8-4f8d0a0d7954
f5a276cf-acd1-40e3-9a88-4ff8c372fd69	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 021	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000021	85b3a045-b2dc-4907-9b81-0baf7b5ad8fd	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	4d922fba-ab0a-485c-9d30-77c8dc0bc3c8
e2ea6c06-bade-4696-910b-25459fe9adf6	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 022	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000022	5547c30a-ec1d-4098-b6a4-c6407cf095a0	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	2107ba26-24e1-4bb5-8afc-6520b220f165
32c65b59-23a6-4911-a7fa-e998637f461e	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 023	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000023	6bdca292-7471-4c7c-ba4d-78f076fa1619	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	9676b1d1-a16a-4286-b60b-24bc2da19ecf
47e60ffb-8926-4ebc-8d98-315cbb3d123c	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 024	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000024	dfc94064-34b9-4879-b807-3bc3c38cf5fd	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	1a426158-18a8-48c9-906b-1c427a1851e9
97c064ad-4d5a-4fc7-9fe7-092b232d0293	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 025	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000025	e8612462-ea7e-4d86-a8ac-4bc79f91c021	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	d0275d3c-02f4-4d50-861a-4cd5eec661b6
5b61fae6-04ce-473d-8efb-dad033a14711	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 026	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000026	eeb266de-c6e0-4ae6-b0ea-a28d881ad538	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	b0e921c4-c0cc-44f8-a26b-f23779ecdd53
4a03b99d-150a-4d11-8da7-177f14d5c9a6	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 027	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000027	df3344a3-2ee5-4875-900b-dcbf51fd4a10	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	f6e0d867-34a9-46a4-843d-4824d7a32598
3a577d0c-bfea-4489-80ad-767fd052fd05	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 028	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000028	05cb91a6-80ca-49bd-83c8-909836b32418	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	72de78e4-ff36-44c8-a924-d831dac3cb95
e0294f9e-1959-4cd1-b294-72bb01640eea	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 029	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000029	d6e8b63f-9ced-4bd0-86d9-fd302a118e19	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	af819e7a-cb7f-4b4f-841b-b31545fbdcdd
6a2bff8b-a2fb-4e88-9cfb-072f801f3c7a	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 030	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000030	c71fa126-98d2-48f2-801f-e03f4ce11745	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	2fca9cfe-9e1f-4a0b-a410-195ecf586967
38eb8b6c-c4b0-402f-b8a9-de5133c53a84	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 031	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000031	a4ee6770-6195-4297-acf8-009c00a7a454	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	f42a9548-746b-4f3f-bb05-824604cbc024
af6709e8-9b63-4536-8f87-020044b8165e	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 032	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000032	03212bd1-3187-424e-a0d7-7b1ab4d70ac9	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	bff69db3-83eb-4132-90ca-6b1318b38cae
bc8b26ac-c9b3-4431-8c9f-c5fa75a20400	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 033	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000033	94ce2fa7-391a-4496-8727-044ebdcbcf4a	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	c51c1507-0f5b-47d5-9f80-3d5baa5ece75
23860630-2338-467d-b98f-18532762faa2	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 034	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000034	8f2951a6-a080-4558-926e-4e220522f3eb	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	d3248a56-9ff8-4e0e-b0ac-e5d7e2806d33
ddad2704-ee2a-4000-87e9-79b3d75ffcf6	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 035	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000035	5dda4a2d-ff36-434e-9612-c8fba3f16ff3	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	8870f78d-1b1f-4315-b374-3f2df2328739
83cf50f6-c524-41f0-9a7b-cd63e0a7b797	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 036	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000036	7501b048-25bc-4044-b705-4053c17e3de4	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	f8990fbb-3848-45ff-8b2e-e215616f7f17
2a876712-2d61-458d-9544-94fffb94b54a	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 037	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000037	2b3984d5-f786-49fa-bd58-d81f6f46c5e2	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	e44d154e-9278-4fc3-a363-a033053d4d47
ddf2f727-ab17-45e3-8667-3d9de522b2f5	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 038	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000038	c5c4003a-d1d3-491d-ae75-1efd8803388b	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	4b24ba59-b6cd-4d9f-a703-76d55d7a9841
6fbd3939-4c39-4a6a-ba22-97f85652fbc1	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 039	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000039	5bca7cc3-1ac7-4ec0-88f9-858a2951e871	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	4668d31d-9d4f-4224-8c57-55f7e1307fb9
71c9068e-865a-4664-a273-ca632a08a90a	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 040	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000040	1e79849a-60c9-4aad-9a26-cb226c1d1f1b	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	721b614b-22a0-4f08-b7e3-996eefdd3673
cfb4feb2-26b0-41ee-a771-0e7100a7203e	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 041	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000041	eb3c0204-85fc-416d-9ce0-c02d67c51748	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0c2f5d44-4ced-4353-a7e2-4d629f617158
8a5db466-e3ec-433f-a781-874d4fcb04b0	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 042	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000042	f0e47356-561b-4acf-bc33-d82943989aae	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	2c693a29-70c6-416f-a4ea-a47135770409
29032de7-54b8-4036-ad26-f63dd34ac354	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 043	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000043	ddefd614-f2cd-42d2-8941-c97d4c883f95	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	355a4b8a-598c-4057-bd9e-14c5e9178cf4
61ff86d0-281c-4b1f-9cd1-f7647850711f	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 044	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000044	b68da9fc-d398-4ddf-a86e-ea465d0b5311	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	29d20518-78f0-41bd-a62a-5e8acbfb7016
0b23000e-b11b-4020-9367-86a0b91f517d	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 045	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000045	95243f36-df18-4152-b9d4-37ece7607f36	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	8b089e4b-c40d-4b33-9c0a-f82676254a8c
335f5a1a-66d5-4d08-b599-98441e5b1655	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 046	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000046	1543f7bf-f816-4659-8cb8-934205171d81	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	6167279d-c6b7-48e4-a24a-1adb7d472f8c
c9be444c-6737-407d-8e7f-31f68b208743	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 047	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000047	7d987a6f-1c9f-4595-9fbb-61cdb171b799	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	12bbd935-08cc-44d5-a435-72a44fef7f57
d2c7f836-def8-4ae8-b768-5fa59f28c21d	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 048	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000048	d14d8334-aabc-4d13-9280-5128346b4aaa	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	596f1fd0-ec69-405a-b02c-3ebdf9c99cd9
8fd110ae-394a-49b0-9d9e-1594d07f3f7c	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 049	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000049	8da36fb6-f70f-4d59-b98a-7a5cfa8bf644	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	b97fffe1-12dd-4dec-9858-16b30312dd52
79fc6452-9e3c-4ceb-992d-43f4cdfbaa5b	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 050	active	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	LT000050	1bdfc693-e7f9-4d4a-9e8e-b421ec2aaed3	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	0d47b41a-b9ce-49cd-bccf-c2d519e7ad0e
d7712ef4-9dc5-4e15-9033-23f52a8e5810	5c74ef5c-96ae-4810-88f8-1cb9168e8142	Load Test Team 001	disqualified	Disqualified by coordinator	2026-07-20 16:05:18.538542	2026-07-20 16:09:11.731865	LT000001	1eb1871e-1266-40d9-855b-d07c7e79b248	\N	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	ac5e1fad-f17e-4e32-9983-82fbb0ef4566
df7a8f14-422d-441c-b5aa-155fed861d8a	0af9c2de-b486-4540-b8bc-5f1d3fa9e796	Veteran Phoenix Team	active	\N	2024-03-21 17:00:00	2024-04-20 17:00:00	VP000001	1de43813-38b1-4ebe-a7e9-f08d67dffa3b	\N	2024-03-21 17:00:00	2024-03-22 17:00:00	7f5de3c3-d202-45c6-a796-b0035e005ef4
5545fb21-255a-4df1-be04-6a9387052429	fe3a7ba6-bdba-4ec0-87c1-7f200dac7bdb	Veteran Phoenix Team	active	\N	2025-09-18 17:00:00	2025-10-18 17:00:00	VP000002	1de43813-38b1-4ebe-a7e9-f08d67dffa3b	\N	2025-09-18 17:00:00	2025-09-19 17:00:00	7f5de3c3-d202-45c6-a796-b0035e005ef4
7977250b-57f9-4049-8b82-47051309b353	a9a53e66-eb94-42ce-8335-cbe4f1a574df	Veteran Phoenix Team	active	\N	2026-06-12 17:00:00	2026-07-12 17:00:00	VP000003	1de43813-38b1-4ebe-a7e9-f08d67dffa3b	\N	2026-06-12 17:00:00	2026-06-13 17:00:00	7f5de3c3-d202-45c6-a796-b0035e005ef4
\.


--
-- Data for Name: tie_break_decisions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tie_break_decisions (id, round_id, team_id, decided_by, decided_at, reason, evidence_url, note) FROM stdin;
\.


--
-- Data for Name: track_judges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.track_judges (id, event_id, track_id, user_id, assigned_at) FROM stdin;
d616d657-663d-4315-9414-8efbd232b3ce	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	2421b5c6-9184-4176-b9b0-a253093b04b3	a1000000-0000-4000-8000-000000000003	2026-07-19 11:32:22.28312
a3a9ddc1-970c-49a6-956e-f79241a98eaa	7f8d71f3-2086-4cfe-a6e2-2497c674dcc4	d3615b83-d5b7-423f-80e5-6337ff5fd408	7aa03131-bf6f-43f1-916d-5c5b0e952c6c	2026-07-20 09:20:02.807696
6740334e-a9f4-405d-b18d-ffd488b1218b	7f8d71f3-2086-4cfe-a6e2-2497c674dcc4	d3615b83-d5b7-423f-80e5-6337ff5fd408	47d30644-2923-4d66-a879-cd83a2041d38	2026-07-20 09:21:41.346614
\.


--
-- Data for Name: track_mentors; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.track_mentors (id, event_id, track_id, user_id, assigned_at) FROM stdin;
a3000000-0000-4000-8000-000000000001	aaaa0000-0000-4000-8000-000000000001	aaaa0000-0000-4000-8000-000000000002	a1000000-0000-4000-8000-000000000002	2026-07-10 02:49:49.985313
89100020-ed88-49df-b342-8eb12ed1ee20	5c133817-ddd1-49ff-ae1b-98d366b950e4	5ff86b4c-12b5-4701-bc24-c5ab4cea0307	7ad640f5-95a3-496b-b2a6-c5a15bcca511	2026-07-19 11:02:45.384208
b3909b33-367a-44ee-bb2b-b8f069a0d863	7f8d71f3-2086-4cfe-a6e2-2497c674dcc4	d3615b83-d5b7-423f-80e5-6337ff5fd408	7aa03131-bf6f-43f1-916d-5c5b0e952c6c	2026-07-20 09:20:02.806192
b5379478-1395-4c20-8ed1-1e6b144ab901	7f8d71f3-2086-4cfe-a6e2-2497c674dcc4	d3615b83-d5b7-423f-80e5-6337ff5fd408	47d30644-2923-4d66-a879-cd83a2041d38	2026-07-20 09:21:41.345618
\.


--
-- Data for Name: tracks; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tracks (id, event_id, name, description, created_at, updated_at, max_teams) FROM stdin;
082553c7-dbed-4cb2-9629-0401e9fd3a7d	9b42520a-bce1-45ac-a165-73aa556d8c82	Regression Track 232026	track smoke	2026-07-02 16:20:26.434699	2026-07-02 16:20:26.434699	\N
118d1506-a833-4a9f-877f-58b3bf4b3772	9b42520a-bce1-45ac-a165-73aa556d8c82	AI	hihi	2026-07-02 17:42:42.759141	2026-07-02 17:42:42.759141	\N
e18a5239-caf9-4997-8c11-e0bea2ca0f6e	9b42520a-bce1-45ac-a165-73aa556d8c82	Track B		2026-07-03 17:19:59.216462	2026-07-03 17:19:59.216462	\N
43e9e14d-0aaa-4d66-a75c-5a8575d386a1	9b42520a-bce1-45ac-a165-73aa556d8c82	E2ETrack97205		2026-07-03 17:19:59.216462	2026-07-03 17:19:59.216462	\N
cf4b4453-06e5-482f-b2a3-6a9a06ef7626	9b42520a-bce1-45ac-a165-73aa556d8c82	E2ETrack799		2026-07-03 17:23:22.841017	2026-07-03 17:23:22.841017	\N
00fbb79f-0891-49d4-b98f-4de4731a7797	9b42520a-bce1-45ac-a165-73aa556d8c82	E2ETrack63357		2026-07-03 17:27:45.361287	2026-07-03 17:27:45.361287	\N
5f8a2671-e573-4efa-b565-0ef8d54f1bcd	9b42520a-bce1-45ac-a165-73aa556d8c82	E2ETrack49308		2026-07-03 17:32:31.315856	2026-07-03 17:32:31.315856	\N
d892f925-3347-4f5e-890b-dc7ca7558e33	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	Track C		2026-07-03 18:05:46.300584	2026-07-03 18:05:46.300584	\N
723f83f1-3b0f-4197-9206-b576562963a7	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	Track A		2026-07-03 18:05:46.300584	2026-07-03 18:05:46.300584	\N
4d95ca60-30fa-4603-8f56-b0290da7829a	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	Track B		2026-07-03 18:05:46.300584	2026-07-03 18:05:46.300584	\N
2421b5c6-9184-4176-b9b0-a253093b04b3	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	General	Default track for registered teams	2026-07-04 01:37:26.358939	2026-07-04 01:37:26.358939	\N
aaaa0000-0000-4000-8000-000000000002	aaaa0000-0000-4000-8000-000000000001	DEMO Track	Seed track for manual testing.	2026-07-10 02:49:49.985313	\N	20
5ff86b4c-12b5-4701-bc24-c5ab4cea0307	5c133817-ddd1-49ff-ae1b-98d366b950e4	General	Default track for registered teams	2026-07-19 09:50:03.271885	2026-07-19 09:50:03.271885	\N
2a932912-a3dd-4fce-b474-23a931c0422b	5c133817-ddd1-49ff-ae1b-98d366b950e4	Track A		2026-07-19 10:57:20.528962	2026-07-19 10:57:20.528962	\N
69dc7792-fbef-4a9a-b01a-8faa37159db9	5c133817-ddd1-49ff-ae1b-98d366b950e4	Track B		2026-07-19 10:57:20.528962	2026-07-19 10:57:20.528962	\N
d3615b83-d5b7-423f-80e5-6337ff5fd408	7f8d71f3-2086-4cfe-a6e2-2497c674dcc4	General	Default track for registered teams	2026-07-20 08:59:33.40234	2026-07-20 08:59:33.40234	\N
5c74ef5c-96ae-4810-88f8-1cb9168e8142	66c58f1a-b289-42e1-8d22-83f6031b60a8	Main Load Test Track	Single track containing all 50 generated teams.	2026-07-20 16:05:18.538542	2026-07-20 16:05:18.538542	50
72d25793-3750-4e35-8a8c-065df0fe7769	66c58f1a-b289-42e1-8d22-83f6031b60a8	Track A		2026-07-20 09:06:09.935566	2026-07-20 09:06:09.935566	10
6e103645-b04c-4d9c-a83b-5b36d46e673a	66c58f1a-b289-42e1-8d22-83f6031b60a8	Track E		2026-07-20 09:06:09.935566	2026-07-20 09:06:09.935566	10
f1b6e4bf-fdce-4a7f-a700-267edfb985eb	66c58f1a-b289-42e1-8d22-83f6031b60a8	Track C		2026-07-20 09:06:09.935566	2026-07-20 09:06:09.935566	10
9ac4329e-65f1-436d-82be-9568d9818f28	66c58f1a-b289-42e1-8d22-83f6031b60a8	Track B		2026-07-20 09:06:09.935566	2026-07-20 09:06:09.935566	10
67c9f413-2503-4b59-a6f9-d22d90e6b222	66c58f1a-b289-42e1-8d22-83f6031b60a8	Track D		2026-07-20 09:06:09.942714	2026-07-20 09:06:09.942714	10
0af9c2de-b486-4540-b8bc-5f1d3fa9e796	1663a859-c386-4ad3-a70e-b402fc5a79c4	Main Track	Veteran qualification track.	2024-02-20 17:00:00	2024-04-20 17:00:00	100
fe3a7ba6-bdba-4ec0-87c1-7f200dac7bdb	e64a6be2-8c67-4f2f-8d61-cbd4693ce942	Main Track	Veteran qualification track.	2025-08-19 17:00:00	2025-10-18 17:00:00	100
a9a53e66-eb94-42ce-8335-cbe4f1a574df	c6484635-5026-4486-943a-8974b2176a3d	Main Track	Veteran qualification track.	2026-05-13 17:00:00	2026-07-12 17:00:00	100
\.


--
-- Data for Name: universities; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.universities (id, name, short_name, country, created_at, updated_at) FROM stdin;
11111111-1111-1111-1111-111111111111	FPT University	FPTU	Vietnam	2026-07-02 14:13:12.122329	\N
0053bd3d-ad1e-4210-81f2-c3cdede05543	HCMUT	\N	Vietnam	2026-07-02 07:29:33.968043	2026-07-02 07:29:33.968043
b43b3920-a3cf-4766-a72a-c8a34a08ba4f	Regression University	\N	Vietnam	2026-07-02 16:20:26.488934	2026-07-02 16:20:26.488934
dc307b88-d085-4d87-a4fd-47a502e5555e	UIT	\N	Vietnam	2026-07-04 04:04:34.156496	2026-07-04 04:04:34.156496
0e2ce1a2-849a-4917-930e-cfaaac9e5342	jjj	\N	Vietnam	2026-07-19 10:40:26.152499	2026-07-19 10:40:26.152499
\.


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_roles (user_id, role_id, assigned_at) FROM stdin;
5b1dada5-bd1a-4f5c-9a57-b4d4e21f7a40	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-02 14:15:32.422374
52b75aa6-317a-44fa-9115-63a40b604265	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-02 14:29:33.952387
b9debf4b-c200-4907-abdf-5ce2208f8eb7	28c09a8e-65bd-44b9-8bf2-fa999efbf8d1	2026-07-02 14:47:36.439192
b9debf4b-c200-4907-abdf-5ce2208f8eb7	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-02 14:47:36.439192
b9debf4b-c200-4907-abdf-5ce2208f8eb7	e9e4426a-4b07-4969-a24d-54e7440cd964	2026-07-02 14:47:36.439192
b9debf4b-c200-4907-abdf-5ce2208f8eb7	ed0ca372-ef82-4f26-bf88-474156fd7d67	2026-07-02 14:47:36.439192
52e07429-90f0-4b2a-821e-77954881d2b1	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-02 23:20:26.487373
450560ce-da8d-43f6-955d-f39d553b77c3	28c09a8e-65bd-44b9-8bf2-fa999efbf8d1	2026-07-03 00:44:10.076935
450560ce-da8d-43f6-955d-f39d553b77c3	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-03 00:44:10.076935
a1000000-0000-4000-8000-000000000001	e9e4426a-4b07-4969-a24d-54e7440cd964	2026-07-10 02:49:49.985313
a1000000-0000-4000-8000-000000000002	28c09a8e-65bd-44b9-8bf2-fa999efbf8d1	2026-07-10 02:49:49.985313
a1000000-0000-4000-8000-000000000003	ed0ca372-ef82-4f26-bf88-474156fd7d67	2026-07-10 02:49:49.985313
a1000000-0000-4000-8000-000000000004	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-10 02:49:49.985313
a1000000-0000-4000-8000-000000000005	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-10 02:49:49.985313
f301a137-0715-41dd-a81a-380035eb808e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-10 02:55:09.773022
7ad640f5-95a3-496b-b2a6-c5a15bcca511	28c09a8e-65bd-44b9-8bf2-fa999efbf8d1	2026-07-19 16:58:42.98234
83bbb9b4-c32e-4b55-aea4-eb3e294f71b1	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-19 17:23:06.93011
ac5dc174-c461-4b2b-aff3-c8a9acd02f8f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-19 17:40:26.149291
4eb1d119-fe2e-48cf-ace8-f295984731f9	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-19 17:47:04.633799
bec002b0-8bfd-4798-bac8-f2cb35e96b48	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-19 17:47:23.699755
e324a57a-9691-4bb8-a39a-e5d2a6d2331a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 15:58:07.940447
ac5e1fad-f17e-4e32-9983-82fbb0ef4566	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
91248f86-5f06-403f-8b56-425a3f978f26	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e739a2a0-fcfa-44cf-ba66-52e0c38ac25c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
f9088069-eefc-434b-a8b0-96bea0b15394	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
966469de-d2a6-49f6-b6f3-9d5f5148f9c0	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e80f247f-d89a-4f20-acb4-85201cf56843	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
8bea3f2e-98b9-4387-a01c-c829c7b5787e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
360a728d-428f-4311-9e98-91de88c3559d	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
51b76d36-4e6a-4333-a27b-7afe2a4f9d1a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
9305157f-ee32-40d8-8b63-894419c8b400	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
f573bdcf-0392-4bbd-9d86-0f10b5a9d10d	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
188c3a51-61ab-44dc-b09d-5f6e984f5de5	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
10b97850-e810-4081-a268-07ca73770c49	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
02b24b64-72c3-4fad-a6db-c2832b7e5356	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b68c9e10-0cbe-421d-8622-8972b934888c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
8033af9a-0e8a-42a6-b8cb-2520757b51e7	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
e041f93f-aedf-4c52-b1f5-dd79b38fb1d3	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
3e322db8-bf88-47aa-8549-55afe5ecf35c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d64a01ff-129d-4d71-b5a0-866209d46273	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d98ebf02-2089-4cc6-8db6-6c27fcd186d3	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
45c59a91-6d75-4875-9e5b-5fc18a962917	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
e5fc740b-69fc-49a3-913d-693b2e0465b3	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
cec83f5b-fa9f-4a44-a215-a64d7e45b86a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
c59eeb36-dac0-44e5-90eb-5e4597af6d3d	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
197a6e30-fc7c-4546-86a5-daddaf02be58	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b70c1256-ecae-4639-b6ca-c2db0b3f6d2e	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
723745d5-7fe8-4ab8-9264-770dcb019a05	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
34104d7c-6650-41d0-bd63-d5ee2c821656	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
98c052f7-664d-475d-bde2-952893559d99	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
ac169d77-6589-4ef2-8845-9f591436e6ca	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
85493223-f120-4d65-870c-e350628c21f7	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
ed67eb39-7111-4e23-b5fc-9945d92a4b25	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
bd12a549-3270-44f5-ab2b-4d4348cf4395	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
1d71f914-3895-4c39-8ea1-de7440110a10	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b5093c46-6842-4911-b12c-55f38f7b7b51	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
797b66d7-5020-46fb-9daf-115c739fd016	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
7bed58e8-143f-42c6-bf26-f2770cd5cb3c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
94a9c5ad-1f58-4a22-af2e-ab8615164d0b	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
72d1095b-b100-4b53-84ad-6bc9f06a57a6	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
c7999bcd-dc53-4827-947d-c0bf3d36dca2	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
57db3f95-400e-4e77-bdbc-4a80d8859814	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
3b70339f-43b3-4c7b-a4bd-bf37421301c2	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
36ece553-1f0f-4d10-9510-7770061db2cf	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
781a9844-1985-422f-a280-267cfb804cbc	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
a54f839c-1f71-4bba-b66e-bb6d15e4bac7	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
6e84f560-95ea-418b-9b2d-350a0aed795b	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
c9641a17-9da5-4a11-89a2-1122b8050d30	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
af796791-12a4-42bb-932d-15b8ed0a05c6	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d46537bc-511f-48a8-bc6a-b267f0a02ee2	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
ed105c49-c897-48d7-bb8e-8ff04711e240	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
7dcf36c2-b8c6-411c-b5b3-d8fb6df96e87	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
96c0d0d9-e176-4029-a058-98ad21ea7f3c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
6b999807-e8dd-42b4-ad4f-0aa0105f7817	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e5621eba-1354-4971-959a-d9cd548200a6	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
17491aa9-a573-4b76-806a-4c8e4ec71852	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
19a9fb26-5dbe-4ef0-a5ea-a32d88f242f8	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
230776cc-0a67-4f94-9c6b-280dbbbfa138	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e7e2fddd-05f1-4218-9476-5f9b96abf556	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
0dc1bd41-2c34-4899-86a5-0369a8c32f8c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
eff7c4b0-a475-4316-8841-f0099267c6ba	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
ff9fd982-afbd-4be1-af50-2c19472bf3a4	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
a5ed3d96-1181-4ecb-8ef5-f98959c4cf97	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
69e2562d-328d-4a87-bf77-aa2a0a9199db	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
194b48ba-d3d5-44da-aae6-f0c6efcb7a24	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
f10781ef-8e93-481a-871b-fe4ea690f49f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
165f75be-0399-4ddf-abcd-c89a3a48386c	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
40e6fee3-129d-47ac-966e-18e289aedb97	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2f5230c2-c9b8-44fa-83ec-05bf6b7b7c01	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d13904ce-4e9c-433c-95d6-2451f07bef18	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
a2d3a0ec-8add-44d2-a936-bd01b5639a7c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
7bdbc5b9-0310-43d2-9f17-90102bba58db	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
100e84d3-b2fb-425c-8b35-3729e689588e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e7363c10-3ed0-4613-a240-319842190baa	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b8aff81f-bfc6-4dd9-b358-8600ea84a6fb	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
72a0e674-b0cc-42ca-9450-554803e841a5	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d8b0f9b4-13da-4c74-81c8-e50434d93793	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
07d45e4f-e584-43ae-857d-9ed885cef57f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
6662ae40-923c-4349-acb7-658ee9f79c54	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
8785a35a-0f05-4394-b8b7-e7084ea3399f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d685fa98-9e1d-45c3-b64d-5988b7302b02	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
eae6e74e-ac28-47c2-b38b-155e3e89553c	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
34e2928d-bed9-4776-8b22-fb363754f3c4	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
860f5d22-d084-44ea-9643-e9a5f5afe187	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
83e82f7a-a5af-4937-b162-fbae703f36f7	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
300872c5-23c3-4929-91e3-42b412da0f8d	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
0c939f9b-f3f9-4d38-9eec-93e2304b64f3	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
d7e1414c-6c45-4349-a8b0-25bbc65d98a9	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2ec4fc87-e231-46db-be35-59089989642a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
02b24689-e5ca-4da4-82f4-f1f0ca679686	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
c95ebe49-da2d-4d5d-98e0-1f3819715093	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
f225c3a7-7ed2-438d-85e9-a24cc25ca37c	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
52ddc965-3aa9-440f-9b89-df69b1e0bd39	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b3d416de-1d9e-4295-9514-4b4d1c85689a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2e75a5c8-0644-4a06-9a5c-e801402d0cfa	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
538b9e6f-9e1a-4902-acc1-4accee1f68e4	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
78b6a018-ebb1-4a16-afd8-4f8d0a0d7954	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
9e691daa-21d5-406e-8c1f-bad5fe4d8e3b	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
0d5752c6-c60b-4f9e-a433-89abf9a5b15b	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
5f40b8b0-a263-48b3-9c40-3f16ab39d3e7	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
6f8ab192-4cf7-47d8-8ac8-d9cb69117253	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
4d922fba-ab0a-485c-9d30-77c8dc0bc3c8	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
62ea75cb-7d26-4f12-8cb1-22a6f3411930	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
ed2efcde-2b9a-4816-910a-e2a31a65fbe4	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
47c6db2a-c336-4eb5-b039-e4fdebd8c353	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
428ab127-3a8c-4274-a905-29709eb4528a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2107ba26-24e1-4bb5-8afc-6520b220f165	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
edb91073-4725-4914-89ec-880803b039e4	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
12b2d080-56dc-4d5f-ad84-35f050ffe46d	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b79e4f5a-246e-416e-9b12-2e00f527e7e8	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
8f7a6ea9-74a9-4900-8ea3-80f27068c57c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
9676b1d1-a16a-4286-b60b-24bc2da19ecf	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
1116a155-8617-44cb-ae61-9e3d11f33191	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
a77cf26d-232a-4afd-a33a-81876f1b079f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e5a48451-ff0a-403b-aaeb-3f8c8bdb999f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
ef628105-8f34-443f-a5d2-74a0d0139728	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
1a426158-18a8-48c9-906b-1c427a1851e9	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
27568529-c5b7-47d9-9dda-d6be88170714	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
1aab72b3-0334-49b8-974c-6b29e657ab14	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
53d6119a-a32f-43b6-9dc9-e28e5eb10866	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2d2c2e2a-4149-421b-9fb5-fb65f429a184	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d0275d3c-02f4-4d50-861a-4cd5eec661b6	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
828d0627-b909-40c4-80e0-5ee455b215fe	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
1a60021a-5cce-48fa-b567-a8b23bf413a7	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e4e96ad5-53c7-423e-af36-ea420e03bdc3	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
7922e855-c5ee-4809-97f8-9f86b7b29bb1	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b0e921c4-c0cc-44f8-a26b-f23779ecdd53	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
8f912c19-2931-4e44-91dc-ccb113a076b9	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2ea5d823-06f8-47c4-8d4a-22702687842e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
aae29548-d234-4514-9d95-2e890cc512fb	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
3fd620cd-4f51-4809-a9df-63416a3605bc	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
f6e0d867-34a9-46a4-843d-4824d7a32598	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
db666de3-de7a-474e-933e-3384f6626cb9	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
52f21c1c-3f76-4167-9a32-03eb688d6fbb	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
eb824860-4827-43ca-af31-8493b402453b	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
c902375c-1f4d-4c50-a53a-f613d7c1958d	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
72de78e4-ff36-44c8-a924-d831dac3cb95	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
225b4686-13f4-4944-994b-48190ad8f968	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
ca8be037-daa0-4e24-8ec5-5884db29ad31	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
3b002876-a2a3-4633-b00d-de84be7b5c43	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d17e4468-10c1-4f3e-8c0a-653cf71b795d	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
af819e7a-cb7f-4b4f-841b-b31545fbdcdd	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
69077bc2-0da5-45a1-8d9a-9d5af2bca03e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
7cb76340-d751-44e1-9167-4c11230557f6	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
332acaa1-7461-4d3e-ac61-74c0ebc57c78	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
1f4048c8-eda6-4241-95a0-1b9d2d1938b6	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2fca9cfe-9e1f-4a0b-a410-195ecf586967	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
4ae33e93-3607-453a-92c2-c56573e11ae0	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
748f4437-85ae-4671-8392-6334aaba4002	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
bb9adeeb-148a-4e19-bc34-61818c51951b	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b6f669d6-6579-43e7-9c07-ea6dfd30d9aa	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
f42a9548-746b-4f3f-bb05-824604cbc024	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
908e5bc2-8a15-476d-807a-a10b71f703f0	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
71dc3b5d-592f-4115-a77e-293f45d0305a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
51f7ccd7-6a89-470f-9015-7fb18c038bca	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
164b6b97-25d0-43f9-b3e6-8e59f5bfc746	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
bff69db3-83eb-4132-90ca-6b1318b38cae	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
862e215c-bfaa-4988-9622-88c47d860caf	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
5a17f6de-5036-45d3-a294-07da5d9eb33e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
5a746fb1-f102-4422-a4cc-25f1657812bf	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
98876449-bbc6-470e-aef6-8ae388718bb5	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
c51c1507-0f5b-47d5-9f80-3d5baa5ece75	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
4daa7be1-45c6-4d32-a0db-a908b0905d8c	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b6193f95-7899-4101-b342-75dc15d6c1fe	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
aca27709-bfe1-4237-968d-86061c4e1a89	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
53ea1d0c-cf63-4633-8df0-a4d8f4ff1a9b	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d3248a56-9ff8-4e0e-b0ac-e5d7e2806d33	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
fcb76f4e-7530-4d25-b0ee-532f89bcd9fd	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
84d107a9-9132-47ed-ab81-ce4f39ac2dbb	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
ace33de1-628b-43d9-a0d2-1430f5e5e350	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
a564543b-d1b3-46a8-85ae-e08353919e2b	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
8870f78d-1b1f-4315-b374-3f2df2328739	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
eae623d2-549b-46fc-9eb5-a616c8a6a143	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
5f3e503b-3db9-4489-a3d2-e124ec1ecf3a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
1cf458dc-5f92-4aed-a579-eccafb920b46	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
8f13e625-daa1-4046-8a2e-7c1d7edd77b9	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
f8990fbb-3848-45ff-8b2e-e215616f7f17	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
c87ad2b9-ab7e-4bed-b366-dee826d86703	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
67028e60-2f00-4973-a1be-89238aae90ca	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b070e04b-de4c-442c-8a90-ca640de9ba32	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
a678073a-7617-47d2-87ce-21f4913918af	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e44d154e-9278-4fc3-a363-a033053d4d47	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
2cfdeb1d-2a10-426f-918d-17c4e93d7a2a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
99993bb1-c074-4e22-882d-c4dd4839c057	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
066abec9-ef07-49f6-ad60-77372b51ed95	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
3acf805e-845e-46b4-b604-5444235146c0	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
4b24ba59-b6cd-4d9f-a703-76d55d7a9841	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
2003a597-f57b-405a-a33b-2564291e1c6e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
4cdf0aae-6be7-46c2-87cb-cfbe7a362553	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b5ce3dac-9d4e-44b1-89a5-b5cc0d5e8591	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
6c6b356a-f644-4f47-a585-51acf05510c0	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
4668d31d-9d4f-4224-8c57-55f7e1307fb9	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
c1bec177-c94c-4c41-9e11-89db28a3a436	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e425358d-0ebf-4a72-b8ff-704e98b9350e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
fb40f875-3425-456d-b947-f332de0caf79	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
9bf93423-27ef-4050-a869-ff8ae7dc7398	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
721b614b-22a0-4f08-b7e3-996eefdd3673	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
5c2deb39-1aee-4382-b611-012943cca8b0	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
201de504-3c7d-46c8-ba89-32113f932876	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
85804d6a-54d2-4d08-9132-e08efc331fce	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
15ce379d-c278-478c-bc3a-2f939c91afbb	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
0c2f5d44-4ced-4353-a7e2-4d629f617158	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
28e5ff84-819b-4514-88a1-ac101fc265c1	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
81654a5f-b3cd-4058-80b9-2eb81bd9a193	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
0dae5854-0971-474b-9ed1-78cc169ac87f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
6dbe25df-9695-4859-a8f6-aa24ec24fbef	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2c693a29-70c6-416f-a4ea-a47135770409	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
8754f344-d08b-4ae9-8db0-416b9611802a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
ab139287-cab6-4f9a-b576-dd0ab0446ef6	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
91b8b4cb-c307-45ee-9670-54a1f49582cd	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
69da9c9a-60f5-48de-978f-aaba04fb89b8	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
355a4b8a-598c-4057-bd9e-14c5e9178cf4	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
43630b9d-b6bf-4d8b-9460-7469fd4180f0	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
7e57fd81-5b7c-4bf2-8272-954417ef0206	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
f55c778a-7188-4676-8f64-02aee4567ce3	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
81f8d309-90df-4dc6-ad0b-95da9ff29c3e	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
29d20518-78f0-41bd-a62a-5e8acbfb7016	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
eab91587-aec2-49fb-8421-3d33cd6a5663	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
790f27b8-b07a-4a89-8722-7e82eed6e662	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
50f96c31-d149-49e3-a238-b51454b2c388	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
3bf95b77-9bc1-4546-b400-5c9442968636	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
8b089e4b-c40d-4b33-9c0a-f82676254a8c	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
7a295313-5833-4738-a7cb-a69c5c38a75a	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
220560c8-51f5-43c6-9808-33ded241fb40	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
d1b9bd2c-a515-4478-83e4-28d9a8d1a836	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
beba5d84-b26a-4f1a-9e80-3bfcafade02f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
6167279d-c6b7-48e4-a24a-1adb7d472f8c	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
15b847fc-b0fb-4ba1-bf7d-072c296436a4	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
df8ff35a-c3c5-4f4a-88a7-12d68bbc7082	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
082481c4-0220-4fe1-b404-26bd0c32f019	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
5491474d-c13e-4dd4-bd03-7e382cd0c4f2	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
12bbd935-08cc-44d5-a435-72a44fef7f57	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
05d7c568-0e47-4ff9-863a-e62df00d2137	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
e567ce9d-86f7-4d5a-813d-d5f3e7cd122d	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
75ab45f8-6c59-4509-bb0a-f6e30d881683	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
1ba988f5-b8d8-4cd3-a538-e3e0ee5aadb9	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
596f1fd0-ec69-405a-b02c-3ebdf9c99cd9	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
65d1696d-c510-4d32-a121-56571026b694	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
c1e69b6d-e76d-40b5-8691-d416adc9d700	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
42975653-b484-4957-9c49-abd8bcf99840	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
c1d031a8-4429-4ff8-812e-aeb8f13400fe	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
b97fffe1-12dd-4dec-9858-16b30312dd52	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
8fcf12ca-575a-4c2f-b8cd-48335f8b4ccc	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
c304a29d-ed17-4f24-8537-afe1e5cee510	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
1692d7b2-14f6-46c4-b92a-1c925ed550a9	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
21d68ab3-3acc-47a9-8c57-3986d83d8961	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
0d47b41a-b9ce-49cd-bccf-c2d519e7ad0e	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:05:18.538542
7b6b68ae-8d59-402d-b616-6879c86b0176	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
9a0f8c96-90f5-4bba-9104-6444a3579814	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
8a2bb866-8704-4cdd-b58b-673d7eeacdfb	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
2091b2ed-d649-4ce5-a935-2445e4238c9f	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:05:18.538542
7f5de3c3-d202-45c6-a796-b0035e005ef4	6f4f98b4-aff1-42df-801b-11f61e93db48	2026-07-20 16:17:20.991673
ff9f5da7-bca5-4a14-a67d-d05e021cfc92	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:17:20.991673
58b50b1a-27f0-4eb5-9e1c-cc524d6fb016	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:17:20.991673
6d6003f6-4b94-4960-888b-5c5cb1d070ba	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:17:20.991673
3639c58e-a515-4fe6-b35b-4af11e42c2cf	5afe0808-f611-48b4-82b6-49383a50efb4	2026-07-20 16:17:20.991673
7aa03131-bf6f-43f1-916d-5c5b0e952c6c	28c09a8e-65bd-44b9-8bf2-fa999efbf8d1	2026-07-20 16:20:02.544121
7aa03131-bf6f-43f1-916d-5c5b0e952c6c	ed0ca372-ef82-4f26-bf88-474156fd7d67	2026-07-20 16:20:02.544121
47d30644-2923-4d66-a879-cd83a2041d38	28c09a8e-65bd-44b9-8bf2-fa999efbf8d1	2026-07-20 16:21:41.092048
47d30644-2923-4d66-a879-cd83a2041d38	ed0ca372-ef82-4f26-bf88-474156fd7d67	2026-07-20 16:21:41.092048
12a39f4b-199c-4d55-963b-2e7bf453ce48	ed0ca372-ef82-4f26-bf88-474156fd7d67	2026-07-20 16:23:46.706784
12a39f4b-199c-4d55-963b-2e7bf453ce48	28c09a8e-65bd-44b9-8bf2-fa999efbf8d1	2026-07-20 16:23:46.706784
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, password_hash, full_name, student_type, student_id, campus_id, is_guest, status, created_at, updated_at, university_id, phone, department, "position", company, expertise, bio, must_change_password, terms_accepted_at, terms_version, privacy_version, security_version) FROM stdin;
5b1dada5-bd1a-4f5c-9a57-b4d4e21f7a40	fpt_test_1782976532@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	FPT Test	fpt	SE999999	22222222-2222-2222-2222-222222222221	f	pending	2026-07-02 07:15:32.700343	2026-07-21 13:05:50.63849	11111111-1111-1111-1111-111111111111	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
52b75aa6-317a-44fa-9115-63a40b604265	external_test_1782977373@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	External Test	external	\N	\N	f	pending	2026-07-02 07:29:34.276928	2026-07-21 13:05:50.63849	0053bd3d-ad1e-4210-81f2-c3cdede05543	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
52e07429-90f0-4b2a-821e-77954881d2b1	reg232026_0@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Reg Member 0	external	\N	\N	f	approved	2026-07-02 16:20:26.734573	2026-07-21 13:05:50.63849	b43b3920-a3cf-4766-a72a-c8a34a08ba4f	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
450560ce-da8d-43f6-955d-f39d553b77c3	reg232026_1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Reg Member 1	external	\N	\N	f	approved	2026-07-02 16:20:27.021084	2026-07-21 13:05:50.63849	b43b3920-a3cf-4766-a72a-c8a34a08ba4f	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b9debf4b-c200-4907-abdf-5ce2208f8eb7	coordinator@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	SmokeDept97972	none	\N	\N	f	approved	2026-07-02 06:48:04.38247	2026-07-21 13:05:50.63849	\N	099996098	E2EDept96098	Coordinator	\N	\N	\N	f	\N	\N	\N	1
a1000000-0000-4000-8000-000000000001	demo.ec@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Demo Coordinator	none	\N	\N	f	approved	2026-07-10 02:49:49.985313	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a1000000-0000-4000-8000-000000000002	demo.mentor@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Demo Mentor	none	\N	\N	f	approved	2026-07-10 02:49:49.985313	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a1000000-0000-4000-8000-000000000004	demo.leader@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Demo Team Leader	fpt	SE100004	\N	f	approved	2026-07-10 02:49:49.985313	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a1000000-0000-4000-8000-000000000005	demo.member@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Demo Team Member	fpt	SE100005	\N	f	approved	2026-07-10 02:49:49.985313	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f301a137-0715-41dd-a81a-380035eb808e	ji@gmail.com	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	ji	fpt	SE490843	22222222-2222-2222-2222-222222222222	f	pending	2026-07-09 19:55:10.072639	2026-07-21 13:05:50.63849	11111111-1111-1111-1111-111111111111	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7ad640f5-95a3-496b-b2a6-c5a15bcca511	triet@gmail.com	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Dinh Minh Triet	none	\N	\N	f	approved	2026-07-19 09:58:43.228442	2026-07-21 13:05:50.63849	\N	\N	\N	\N	FPT	Java expert	expert in java and AI	f	\N	\N	\N	1
83bbb9b4-c32e-4b55-aea4-eb3e294f71b1	gg@gmail.com	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Dinh triet	fpt	SE333333	22222222-2222-2222-2222-222222222221	f	approved	2026-07-19 10:23:07.176089	2026-07-21 13:05:50.63849	11111111-1111-1111-1111-111111111111	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ac5dc174-c461-4b2b-aff3-c8a9acd02f8f	jj@gmail.com	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	ji	external	\N	\N	f	approved	2026-07-19 10:40:26.39612	2026-07-21 13:05:50.63849	0e2ce1a2-849a-4917-930e-cfaaac9e5342	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
4eb1d119-fe2e-48cf-ace8-f295984731f9	a@gmail.com	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	g	fpt	s	22222222-2222-2222-2222-222222222221	f	approved	2026-07-19 10:47:04.887516	2026-07-21 13:05:50.63849	11111111-1111-1111-1111-111111111111	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
bec002b0-8bfd-4798-bac8-f2cb35e96b48	v@gmail.com	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	v	fpt	g5	22222222-2222-2222-2222-222222222225	f	approved	2026-07-19 10:47:23.945004	2026-07-21 13:05:50.63849	11111111-1111-1111-1111-111111111111	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a1000000-0000-4000-8000-000000000003	demo.judge@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Triet	none	\N	\N	f	approved	2026-07-10 02:49:49.985313	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e324a57a-9691-4bb8-a39a-e5d2a6d2331a	cena@gmail.com	$2a$12$.DkgG6F5b1kJBoO.1iZr8erv6mqbx85PCTYgpGfB5wvJ8DimwC4CW	John Cena	fpt	se9090909	22222222-2222-2222-2222-222222222221	f	approved	2026-07-20 08:58:08.187234	2026-07-21 13:05:50.63849	11111111-1111-1111-1111-111111111111	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ac5e1fad-f17e-4e32-9983-82fbb0ef4566	loadtest.team001.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 001 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
91248f86-5f06-403f-8b56-425a3f978f26	loadtest.team001.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 001 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e739a2a0-fcfa-44cf-ba66-52e0c38ac25c	loadtest.team001.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 001 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f9088069-eefc-434b-a8b0-96bea0b15394	loadtest.team001.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 001 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
966469de-d2a6-49f6-b6f3-9d5f5148f9c0	loadtest.team001.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 001 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e80f247f-d89a-4f20-acb4-85201cf56843	loadtest.team002.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 002 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8bea3f2e-98b9-4387-a01c-c829c7b5787e	loadtest.team002.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 002 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
360a728d-428f-4311-9e98-91de88c3559d	loadtest.team002.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 002 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
51b76d36-4e6a-4333-a27b-7afe2a4f9d1a	loadtest.team002.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 002 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
9305157f-ee32-40d8-8b63-894419c8b400	loadtest.team002.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 002 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f573bdcf-0392-4bbd-9d86-0f10b5a9d10d	loadtest.team003.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 003 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7f5de3c3-d202-45c6-a796-b0035e005ef4	veteran.phoenix.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Veteran Phoenix Member 1	none	\N	\N	f	approved	2026-07-20 16:17:20.991673	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ff9f5da7-bca5-4a14-a67d-d05e021cfc92	veteran.phoenix.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Veteran Phoenix Member 2	none	\N	\N	f	approved	2026-07-20 16:17:20.991673	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
188c3a51-61ab-44dc-b09d-5f6e984f5de5	loadtest.team003.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 003 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
10b97850-e810-4081-a268-07ca73770c49	loadtest.team003.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 003 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
02b24b64-72c3-4fad-a6db-c2832b7e5356	loadtest.team003.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 003 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b68c9e10-0cbe-421d-8622-8972b934888c	loadtest.team003.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 003 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8033af9a-0e8a-42a6-b8cb-2520757b51e7	loadtest.team004.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 004 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e041f93f-aedf-4c52-b1f5-dd79b38fb1d3	loadtest.team004.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 004 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
3e322db8-bf88-47aa-8549-55afe5ecf35c	loadtest.team004.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 004 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d64a01ff-129d-4d71-b5a0-866209d46273	loadtest.team004.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 004 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d98ebf02-2089-4cc6-8db6-6c27fcd186d3	loadtest.team004.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 004 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
45c59a91-6d75-4875-9e5b-5fc18a962917	loadtest.team005.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 005 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e5fc740b-69fc-49a3-913d-693b2e0465b3	loadtest.team005.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 005 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
cec83f5b-fa9f-4a44-a215-a64d7e45b86a	loadtest.team005.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 005 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c59eeb36-dac0-44e5-90eb-5e4597af6d3d	loadtest.team005.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 005 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
197a6e30-fc7c-4546-86a5-daddaf02be58	loadtest.team005.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 005 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b70c1256-ecae-4639-b6ca-c2db0b3f6d2e	loadtest.team006.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 006 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
723745d5-7fe8-4ab8-9264-770dcb019a05	loadtest.team006.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 006 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
34104d7c-6650-41d0-bd63-d5ee2c821656	loadtest.team006.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 006 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
98c052f7-664d-475d-bde2-952893559d99	loadtest.team006.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 006 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ac169d77-6589-4ef2-8845-9f591436e6ca	loadtest.team006.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 006 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
85493223-f120-4d65-870c-e350628c21f7	loadtest.team007.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 007 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ed67eb39-7111-4e23-b5fc-9945d92a4b25	loadtest.team007.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 007 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
bd12a549-3270-44f5-ab2b-4d4348cf4395	loadtest.team007.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 007 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1d71f914-3895-4c39-8ea1-de7440110a10	loadtest.team007.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 007 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b5093c46-6842-4911-b12c-55f38f7b7b51	loadtest.team007.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 007 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
797b66d7-5020-46fb-9daf-115c739fd016	loadtest.team008.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 008 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7bed58e8-143f-42c6-bf26-f2770cd5cb3c	loadtest.team008.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 008 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
94a9c5ad-1f58-4a22-af2e-ab8615164d0b	loadtest.team008.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 008 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
72d1095b-b100-4b53-84ad-6bc9f06a57a6	loadtest.team008.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 008 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c7999bcd-dc53-4827-947d-c0bf3d36dca2	loadtest.team008.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 008 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
57db3f95-400e-4e77-bdbc-4a80d8859814	loadtest.team009.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 009 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
3b70339f-43b3-4c7b-a4bd-bf37421301c2	loadtest.team009.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 009 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
36ece553-1f0f-4d10-9510-7770061db2cf	loadtest.team009.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 009 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
781a9844-1985-422f-a280-267cfb804cbc	loadtest.team009.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 009 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a54f839c-1f71-4bba-b66e-bb6d15e4bac7	loadtest.team009.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 009 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
6e84f560-95ea-418b-9b2d-350a0aed795b	loadtest.team010.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 010 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c9641a17-9da5-4a11-89a2-1122b8050d30	loadtest.team010.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 010 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
af796791-12a4-42bb-932d-15b8ed0a05c6	loadtest.team010.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 010 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d46537bc-511f-48a8-bc6a-b267f0a02ee2	loadtest.team010.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 010 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ed105c49-c897-48d7-bb8e-8ff04711e240	loadtest.team010.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 010 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7dcf36c2-b8c6-411c-b5b3-d8fb6df96e87	loadtest.team011.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 011 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
96c0d0d9-e176-4029-a058-98ad21ea7f3c	loadtest.team011.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 011 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
6b999807-e8dd-42b4-ad4f-0aa0105f7817	loadtest.team011.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 011 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e5621eba-1354-4971-959a-d9cd548200a6	loadtest.team011.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 011 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
17491aa9-a573-4b76-806a-4c8e4ec71852	loadtest.team011.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 011 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
19a9fb26-5dbe-4ef0-a5ea-a32d88f242f8	loadtest.team012.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 012 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
230776cc-0a67-4f94-9c6b-280dbbbfa138	loadtest.team012.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 012 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e7e2fddd-05f1-4218-9476-5f9b96abf556	loadtest.team012.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 012 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
0dc1bd41-2c34-4899-86a5-0369a8c32f8c	loadtest.team012.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 012 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
eff7c4b0-a475-4316-8841-f0099267c6ba	loadtest.team012.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 012 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ff9fd982-afbd-4be1-af50-2c19472bf3a4	loadtest.team013.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 013 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a5ed3d96-1181-4ecb-8ef5-f98959c4cf97	loadtest.team013.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 013 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
69e2562d-328d-4a87-bf77-aa2a0a9199db	loadtest.team013.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 013 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
194b48ba-d3d5-44da-aae6-f0c6efcb7a24	loadtest.team013.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 013 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f10781ef-8e93-481a-871b-fe4ea690f49f	loadtest.team013.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 013 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
165f75be-0399-4ddf-abcd-c89a3a48386c	loadtest.team014.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 014 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
40e6fee3-129d-47ac-966e-18e289aedb97	loadtest.team014.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 014 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2f5230c2-c9b8-44fa-83ec-05bf6b7b7c01	loadtest.team014.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 014 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d13904ce-4e9c-433c-95d6-2451f07bef18	loadtest.team014.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 014 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a2d3a0ec-8add-44d2-a936-bd01b5639a7c	loadtest.team014.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 014 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7bdbc5b9-0310-43d2-9f17-90102bba58db	loadtest.team015.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 015 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
100e84d3-b2fb-425c-8b35-3729e689588e	loadtest.team015.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 015 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e7363c10-3ed0-4613-a240-319842190baa	loadtest.team015.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 015 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b8aff81f-bfc6-4dd9-b358-8600ea84a6fb	loadtest.team015.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 015 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
72a0e674-b0cc-42ca-9450-554803e841a5	loadtest.team015.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 015 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d8b0f9b4-13da-4c74-81c8-e50434d93793	loadtest.team016.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 016 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
07d45e4f-e584-43ae-857d-9ed885cef57f	loadtest.team016.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 016 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
6662ae40-923c-4349-acb7-658ee9f79c54	loadtest.team016.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 016 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8785a35a-0f05-4394-b8b7-e7084ea3399f	loadtest.team016.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 016 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d685fa98-9e1d-45c3-b64d-5988b7302b02	loadtest.team016.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 016 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
eae6e74e-ac28-47c2-b38b-155e3e89553c	loadtest.team017.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 017 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
34e2928d-bed9-4776-8b22-fb363754f3c4	loadtest.team017.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 017 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
860f5d22-d084-44ea-9643-e9a5f5afe187	loadtest.team017.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 017 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
83e82f7a-a5af-4937-b162-fbae703f36f7	loadtest.team017.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 017 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
300872c5-23c3-4929-91e3-42b412da0f8d	loadtest.team017.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 017 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
0c939f9b-f3f9-4d38-9eec-93e2304b64f3	loadtest.team018.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 018 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d7e1414c-6c45-4349-a8b0-25bbc65d98a9	loadtest.team018.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 018 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2ec4fc87-e231-46db-be35-59089989642a	loadtest.team018.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 018 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
02b24689-e5ca-4da4-82f4-f1f0ca679686	loadtest.team018.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 018 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c95ebe49-da2d-4d5d-98e0-1f3819715093	loadtest.team018.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 018 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f225c3a7-7ed2-438d-85e9-a24cc25ca37c	loadtest.team019.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 019 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
52ddc965-3aa9-440f-9b89-df69b1e0bd39	loadtest.team019.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 019 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b3d416de-1d9e-4295-9514-4b4d1c85689a	loadtest.team019.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 019 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2e75a5c8-0644-4a06-9a5c-e801402d0cfa	loadtest.team019.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 019 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
538b9e6f-9e1a-4902-acc1-4accee1f68e4	loadtest.team019.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 019 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
78b6a018-ebb1-4a16-afd8-4f8d0a0d7954	loadtest.team020.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 020 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
9e691daa-21d5-406e-8c1f-bad5fe4d8e3b	loadtest.team020.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 020 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
0d5752c6-c60b-4f9e-a433-89abf9a5b15b	loadtest.team020.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 020 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
5f40b8b0-a263-48b3-9c40-3f16ab39d3e7	loadtest.team020.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 020 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
6f8ab192-4cf7-47d8-8ac8-d9cb69117253	loadtest.team020.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 020 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
4d922fba-ab0a-485c-9d30-77c8dc0bc3c8	loadtest.team021.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 021 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
62ea75cb-7d26-4f12-8cb1-22a6f3411930	loadtest.team021.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 021 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ed2efcde-2b9a-4816-910a-e2a31a65fbe4	loadtest.team021.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 021 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
47c6db2a-c336-4eb5-b039-e4fdebd8c353	loadtest.team021.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 021 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
428ab127-3a8c-4274-a905-29709eb4528a	loadtest.team021.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 021 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2107ba26-24e1-4bb5-8afc-6520b220f165	loadtest.team022.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 022 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
edb91073-4725-4914-89ec-880803b039e4	loadtest.team022.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 022 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
12b2d080-56dc-4d5f-ad84-35f050ffe46d	loadtest.team022.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 022 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b79e4f5a-246e-416e-9b12-2e00f527e7e8	loadtest.team022.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 022 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8f7a6ea9-74a9-4900-8ea3-80f27068c57c	loadtest.team022.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 022 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
9676b1d1-a16a-4286-b60b-24bc2da19ecf	loadtest.team023.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 023 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1116a155-8617-44cb-ae61-9e3d11f33191	loadtest.team023.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 023 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a77cf26d-232a-4afd-a33a-81876f1b079f	loadtest.team023.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 023 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e5a48451-ff0a-403b-aaeb-3f8c8bdb999f	loadtest.team023.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 023 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ef628105-8f34-443f-a5d2-74a0d0139728	loadtest.team023.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 023 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1a426158-18a8-48c9-906b-1c427a1851e9	loadtest.team024.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 024 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
27568529-c5b7-47d9-9dda-d6be88170714	loadtest.team024.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 024 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1aab72b3-0334-49b8-974c-6b29e657ab14	loadtest.team024.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 024 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
53d6119a-a32f-43b6-9dc9-e28e5eb10866	loadtest.team024.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 024 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2d2c2e2a-4149-421b-9fb5-fb65f429a184	loadtest.team024.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 024 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d0275d3c-02f4-4d50-861a-4cd5eec661b6	loadtest.team025.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 025 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
828d0627-b909-40c4-80e0-5ee455b215fe	loadtest.team025.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 025 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1a60021a-5cce-48fa-b567-a8b23bf413a7	loadtest.team025.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 025 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e4e96ad5-53c7-423e-af36-ea420e03bdc3	loadtest.team025.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 025 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7922e855-c5ee-4809-97f8-9f86b7b29bb1	loadtest.team025.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 025 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b0e921c4-c0cc-44f8-a26b-f23779ecdd53	loadtest.team026.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 026 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8f912c19-2931-4e44-91dc-ccb113a076b9	loadtest.team026.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 026 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2ea5d823-06f8-47c4-8d4a-22702687842e	loadtest.team026.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 026 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
aae29548-d234-4514-9d95-2e890cc512fb	loadtest.team026.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 026 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
3fd620cd-4f51-4809-a9df-63416a3605bc	loadtest.team026.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 026 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f6e0d867-34a9-46a4-843d-4824d7a32598	loadtest.team027.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 027 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
db666de3-de7a-474e-933e-3384f6626cb9	loadtest.team027.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 027 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
52f21c1c-3f76-4167-9a32-03eb688d6fbb	loadtest.team027.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 027 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
eb824860-4827-43ca-af31-8493b402453b	loadtest.team027.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 027 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c902375c-1f4d-4c50-a53a-f613d7c1958d	loadtest.team027.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 027 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
72de78e4-ff36-44c8-a924-d831dac3cb95	loadtest.team028.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 028 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
225b4686-13f4-4944-994b-48190ad8f968	loadtest.team028.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 028 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ca8be037-daa0-4e24-8ec5-5884db29ad31	loadtest.team028.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 028 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
3b002876-a2a3-4633-b00d-de84be7b5c43	loadtest.team028.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 028 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d17e4468-10c1-4f3e-8c0a-653cf71b795d	loadtest.team028.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 028 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
af819e7a-cb7f-4b4f-841b-b31545fbdcdd	loadtest.team029.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 029 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
69077bc2-0da5-45a1-8d9a-9d5af2bca03e	loadtest.team029.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 029 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7cb76340-d751-44e1-9167-4c11230557f6	loadtest.team029.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 029 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
332acaa1-7461-4d3e-ac61-74c0ebc57c78	loadtest.team029.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 029 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1f4048c8-eda6-4241-95a0-1b9d2d1938b6	loadtest.team029.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 029 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2fca9cfe-9e1f-4a0b-a410-195ecf586967	loadtest.team030.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 030 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
4ae33e93-3607-453a-92c2-c56573e11ae0	loadtest.team030.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 030 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
748f4437-85ae-4671-8392-6334aaba4002	loadtest.team030.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 030 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
bb9adeeb-148a-4e19-bc34-61818c51951b	loadtest.team030.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 030 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b6f669d6-6579-43e7-9c07-ea6dfd30d9aa	loadtest.team030.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 030 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f42a9548-746b-4f3f-bb05-824604cbc024	loadtest.team031.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 031 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
908e5bc2-8a15-476d-807a-a10b71f703f0	loadtest.team031.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 031 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
71dc3b5d-592f-4115-a77e-293f45d0305a	loadtest.team031.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 031 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
51f7ccd7-6a89-470f-9015-7fb18c038bca	loadtest.team031.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 031 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
164b6b97-25d0-43f9-b3e6-8e59f5bfc746	loadtest.team031.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 031 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
bff69db3-83eb-4132-90ca-6b1318b38cae	loadtest.team032.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 032 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
862e215c-bfaa-4988-9622-88c47d860caf	loadtest.team032.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 032 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
5a17f6de-5036-45d3-a294-07da5d9eb33e	loadtest.team032.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 032 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
5a746fb1-f102-4422-a4cc-25f1657812bf	loadtest.team032.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 032 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
98876449-bbc6-470e-aef6-8ae388718bb5	loadtest.team032.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 032 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c51c1507-0f5b-47d5-9f80-3d5baa5ece75	loadtest.team033.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 033 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
4daa7be1-45c6-4d32-a0db-a908b0905d8c	loadtest.team033.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 033 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b6193f95-7899-4101-b342-75dc15d6c1fe	loadtest.team033.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 033 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
aca27709-bfe1-4237-968d-86061c4e1a89	loadtest.team033.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 033 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
53ea1d0c-cf63-4633-8df0-a4d8f4ff1a9b	loadtest.team033.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 033 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d3248a56-9ff8-4e0e-b0ac-e5d7e2806d33	loadtest.team034.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 034 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
fcb76f4e-7530-4d25-b0ee-532f89bcd9fd	loadtest.team034.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 034 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
84d107a9-9132-47ed-ab81-ce4f39ac2dbb	loadtest.team034.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 034 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ace33de1-628b-43d9-a0d2-1430f5e5e350	loadtest.team034.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 034 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a564543b-d1b3-46a8-85ae-e08353919e2b	loadtest.team034.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 034 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8870f78d-1b1f-4315-b374-3f2df2328739	loadtest.team035.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 035 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
eae623d2-549b-46fc-9eb5-a616c8a6a143	loadtest.team035.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 035 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
5f3e503b-3db9-4489-a3d2-e124ec1ecf3a	loadtest.team035.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 035 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1cf458dc-5f92-4aed-a579-eccafb920b46	loadtest.team035.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 035 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8f13e625-daa1-4046-8a2e-7c1d7edd77b9	loadtest.team035.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 035 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f8990fbb-3848-45ff-8b2e-e215616f7f17	loadtest.team036.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 036 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c87ad2b9-ab7e-4bed-b366-dee826d86703	loadtest.team036.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 036 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
67028e60-2f00-4973-a1be-89238aae90ca	loadtest.team036.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 036 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b070e04b-de4c-442c-8a90-ca640de9ba32	loadtest.team036.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 036 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
a678073a-7617-47d2-87ce-21f4913918af	loadtest.team036.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 036 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e44d154e-9278-4fc3-a363-a033053d4d47	loadtest.team037.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 037 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2cfdeb1d-2a10-426f-918d-17c4e93d7a2a	loadtest.team037.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 037 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
99993bb1-c074-4e22-882d-c4dd4839c057	loadtest.team037.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 037 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
066abec9-ef07-49f6-ad60-77372b51ed95	loadtest.team037.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 037 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
3acf805e-845e-46b4-b604-5444235146c0	loadtest.team037.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 037 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
4b24ba59-b6cd-4d9f-a703-76d55d7a9841	loadtest.team038.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 038 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2003a597-f57b-405a-a33b-2564291e1c6e	loadtest.team038.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 038 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
4cdf0aae-6be7-46c2-87cb-cfbe7a362553	loadtest.team038.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 038 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b5ce3dac-9d4e-44b1-89a5-b5cc0d5e8591	loadtest.team038.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 038 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
6c6b356a-f644-4f47-a585-51acf05510c0	loadtest.team038.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 038 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
4668d31d-9d4f-4224-8c57-55f7e1307fb9	loadtest.team039.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 039 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c1bec177-c94c-4c41-9e11-89db28a3a436	loadtest.team039.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 039 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e425358d-0ebf-4a72-b8ff-704e98b9350e	loadtest.team039.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 039 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
fb40f875-3425-456d-b947-f332de0caf79	loadtest.team039.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 039 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
9bf93423-27ef-4050-a869-ff8ae7dc7398	loadtest.team039.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 039 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
721b614b-22a0-4f08-b7e3-996eefdd3673	loadtest.team040.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 040 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
5c2deb39-1aee-4382-b611-012943cca8b0	loadtest.team040.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 040 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
201de504-3c7d-46c8-ba89-32113f932876	loadtest.team040.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 040 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
85804d6a-54d2-4d08-9132-e08efc331fce	loadtest.team040.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 040 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
15ce379d-c278-478c-bc3a-2f939c91afbb	loadtest.team040.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 040 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
0c2f5d44-4ced-4353-a7e2-4d629f617158	loadtest.team041.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 041 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
28e5ff84-819b-4514-88a1-ac101fc265c1	loadtest.team041.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 041 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
81654a5f-b3cd-4058-80b9-2eb81bd9a193	loadtest.team041.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 041 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
0dae5854-0971-474b-9ed1-78cc169ac87f	loadtest.team041.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 041 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
6dbe25df-9695-4859-a8f6-aa24ec24fbef	loadtest.team041.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 041 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2c693a29-70c6-416f-a4ea-a47135770409	loadtest.team042.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 042 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8754f344-d08b-4ae9-8db0-416b9611802a	loadtest.team042.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 042 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
ab139287-cab6-4f9a-b576-dd0ab0446ef6	loadtest.team042.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 042 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
91b8b4cb-c307-45ee-9670-54a1f49582cd	loadtest.team042.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 042 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
69da9c9a-60f5-48de-978f-aaba04fb89b8	loadtest.team042.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 042 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
355a4b8a-598c-4057-bd9e-14c5e9178cf4	loadtest.team043.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 043 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
43630b9d-b6bf-4d8b-9460-7469fd4180f0	loadtest.team043.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 043 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7e57fd81-5b7c-4bf2-8272-954417ef0206	loadtest.team043.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 043 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
f55c778a-7188-4676-8f64-02aee4567ce3	loadtest.team043.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 043 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
81f8d309-90df-4dc6-ad0b-95da9ff29c3e	loadtest.team043.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 043 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
29d20518-78f0-41bd-a62a-5e8acbfb7016	loadtest.team044.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 044 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
eab91587-aec2-49fb-8421-3d33cd6a5663	loadtest.team044.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 044 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
790f27b8-b07a-4a89-8722-7e82eed6e662	loadtest.team044.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 044 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
50f96c31-d149-49e3-a238-b51454b2c388	loadtest.team044.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 044 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
3bf95b77-9bc1-4546-b400-5c9442968636	loadtest.team044.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 044 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8b089e4b-c40d-4b33-9c0a-f82676254a8c	loadtest.team045.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 045 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7a295313-5833-4738-a7cb-a69c5c38a75a	loadtest.team045.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 045 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
220560c8-51f5-43c6-9808-33ded241fb40	loadtest.team045.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 045 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
d1b9bd2c-a515-4478-83e4-28d9a8d1a836	loadtest.team045.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 045 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
beba5d84-b26a-4f1a-9e80-3bfcafade02f	loadtest.team045.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 045 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
6167279d-c6b7-48e4-a24a-1adb7d472f8c	loadtest.team046.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 046 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
15b847fc-b0fb-4ba1-bf7d-072c296436a4	loadtest.team046.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 046 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
df8ff35a-c3c5-4f4a-88a7-12d68bbc7082	loadtest.team046.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 046 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
082481c4-0220-4fe1-b404-26bd0c32f019	loadtest.team046.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 046 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
5491474d-c13e-4dd4-bd03-7e382cd0c4f2	loadtest.team046.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 046 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
12bbd935-08cc-44d5-a435-72a44fef7f57	loadtest.team047.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 047 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
05d7c568-0e47-4ff9-863a-e62df00d2137	loadtest.team047.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 047 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
e567ce9d-86f7-4d5a-813d-d5f3e7cd122d	loadtest.team047.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 047 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
75ab45f8-6c59-4509-bb0a-f6e30d881683	loadtest.team047.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 047 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1ba988f5-b8d8-4cd3-a538-e3e0ee5aadb9	loadtest.team047.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 047 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
596f1fd0-ec69-405a-b02c-3ebdf9c99cd9	loadtest.team048.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 048 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
65d1696d-c510-4d32-a121-56571026b694	loadtest.team048.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 048 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c1e69b6d-e76d-40b5-8691-d416adc9d700	loadtest.team048.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 048 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
42975653-b484-4957-9c49-abd8bcf99840	loadtest.team048.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 048 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c1d031a8-4429-4ff8-812e-aeb8f13400fe	loadtest.team048.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 048 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
b97fffe1-12dd-4dec-9858-16b30312dd52	loadtest.team049.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 049 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8fcf12ca-575a-4c2f-b8cd-48335f8b4ccc	loadtest.team049.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 049 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
c304a29d-ed17-4f24-8537-afe1e5cee510	loadtest.team049.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 049 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
1692d7b2-14f6-46c4-b92a-1c925ed550a9	loadtest.team049.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 049 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
21d68ab3-3acc-47a9-8c57-3986d83d8961	loadtest.team049.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 049 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
0d47b41a-b9ce-49cd-bccf-c2d519e7ad0e	loadtest.team050.member1@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 050 Member 1	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7b6b68ae-8d59-402d-b616-6879c86b0176	loadtest.team050.member2@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 050 Member 2	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
9a0f8c96-90f5-4bba-9104-6444a3579814	loadtest.team050.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 050 Member 3	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
8a2bb866-8704-4cdd-b58b-673d7eeacdfb	loadtest.team050.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 050 Member 4	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
2091b2ed-d649-4ce5-a935-2445e4238c9f	loadtest.team050.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Load Test Team 050 Member 5	none	\N	\N	f	approved	2026-07-20 16:05:18.538542	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
58b50b1a-27f0-4eb5-9e1c-cc524d6fb016	veteran.phoenix.member3@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Veteran Phoenix Member 3	none	\N	\N	f	approved	2026-07-20 16:17:20.991673	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
6d6003f6-4b94-4960-888b-5c5cb1d070ba	veteran.phoenix.member4@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Veteran Phoenix Member 4	none	\N	\N	f	approved	2026-07-20 16:17:20.991673	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
3639c58e-a515-4fe6-b35b-4af11e42c2cf	veteran.phoenix.member5@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Veteran Phoenix Member 5	none	\N	\N	f	approved	2026-07-20 16:17:20.991673	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
7aa03131-bf6f-43f1-916d-5c5b0e952c6c	dinh@gmail.com	$2a$12$bIaDHiXWfsnmvslxZrQdW.sbq1GTtXTKuatUNuW7qLdS6U/18YzI.	Đinh	none	\N	\N	f	approved	2026-07-20 09:20:02.788456	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
47d30644-2923-4d66-a879-cd83a2041d38	hy@gmail.com	$2a$12$1mb/I63/O1mHGdg2Wmu0euzcgP80bWPZnRy.rPKfQk56LbBQd/Bm6	hi	none	\N	\N	f	approved	2026-07-20 09:21:41.332698	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
12a39f4b-199c-4d55-963b-2e7bf453ce48	judge.mentor@seal.local	$2a$12$KlrHds23qslARZvEN..DKOe7gPKFxvbofsfl/GeZEV5qfjoBjLbF6	Judge Mentor Test Account	none	\N	\N	f	approved	2026-07-20 16:23:46.706784	2026-07-21 13:05:50.63849	\N	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	1
\.


--
-- Name: account_activation_tokens account_activation_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation_tokens
    ADD CONSTRAINT account_activation_tokens_pkey PRIMARY KEY (id);


--
-- Name: account_activation_tokens account_activation_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation_tokens
    ADD CONSTRAINT account_activation_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: appeals appeals_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: campuses campuses_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT campuses_pkey PRIMARY KEY (id);


--
-- Name: criteria_templates criteria_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.criteria_templates
    ADD CONSTRAINT criteria_templates_pkey PRIMARY KEY (id);


--
-- Name: event_rules event_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_rules
    ADD CONSTRAINT event_rules_pkey PRIMARY KEY (id);


--
-- Name: event_seed_assignments event_seed_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_pkey PRIMARY KEY (id);


--
-- Name: event_team_finishes event_team_finishes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_pkey PRIMARY KEY (id);


--
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- Name: incident_actions incident_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_pkey PRIMARY KEY (id);


--
-- Name: incident_evidences incident_evidences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_pkey PRIMARY KEY (id);


--
-- Name: incident_reports incident_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_pkey PRIMARY KEY (id);


--
-- Name: logical_round_promotions logical_round_promotions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT logical_round_promotions_pkey PRIMARY KEY (id);


--
-- Name: mentor_feedbacks mentor_feedbacks_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_pkey PRIMARY KEY (id);


--
-- Name: notices notices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: prize_revisions prize_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_pkey PRIMARY KEY (id);


--
-- Name: prizes prizes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_pkey PRIMARY KEY (id);


--
-- Name: revoked_tokens revoked_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_pkey PRIMARY KEY (id);


--
-- Name: revoked_tokens revoked_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: round_criteria round_criteria_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_pkey PRIMARY KEY (id);


--
-- Name: round_definitions round_definitions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_definitions
    ADD CONSTRAINT round_definitions_pkey PRIMARY KEY (id);


--
-- Name: round_judges round_judges_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_pkey PRIMARY KEY (id);


--
-- Name: round_participants round_participants_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_pkey PRIMARY KEY (id);


--
-- Name: round_rankings round_rankings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_pkey PRIMARY KEY (id);


--
-- Name: round_result_version_entries round_result_version_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT round_result_version_entries_pkey PRIMARY KEY (id);


--
-- Name: round_result_versions round_result_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT round_result_versions_pkey PRIMARY KEY (id);


--
-- Name: rounds rounds_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT rounds_pkey PRIMARY KEY (id);


--
-- Name: rule_acceptances rule_acceptances_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_pkey PRIMARY KEY (id);


--
-- Name: scores scores_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_pkey PRIMARY KEY (id);


--
-- Name: submissions submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_pkey PRIMARY KEY (id);


--
-- Name: support_tickets support_tickets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT support_tickets_pkey PRIMARY KEY (id);


--
-- Name: team_chat_messages team_chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_pkey PRIMARY KEY (id);


--
-- Name: team_join_requests team_join_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_pkey PRIMARY KEY (id);


--
-- Name: team_members team_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_pkey PRIMARY KEY (id);


--
-- Name: team_profiles team_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_profiles
    ADD CONSTRAINT team_profiles_pkey PRIMARY KEY (id);


--
-- Name: team_recognitions team_recognitions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_recognitions
    ADD CONSTRAINT team_recognitions_pkey PRIMARY KEY (id);


--
-- Name: team_timeline_events team_timeline_events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_pkey PRIMARY KEY (id);


--
-- Name: teams teams_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);


--
-- Name: tie_break_decisions tie_break_decisions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_pkey PRIMARY KEY (id);


--
-- Name: track_judges track_judges_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_pkey PRIMARY KEY (id);


--
-- Name: track_mentors track_mentors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_pkey PRIMARY KEY (id);


--
-- Name: tracks tracks_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT tracks_pkey PRIMARY KEY (id);


--
-- Name: universities universities_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.universities
    ADD CONSTRAINT universities_name_key UNIQUE (name);


--
-- Name: universities universities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.universities
    ADD CONSTRAINT universities_pkey PRIMARY KEY (id);


--
-- Name: campuses uq_campuses_university_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT uq_campuses_university_name UNIQUE (university_id, name);


--
-- Name: event_seed_assignments uq_event_seed_team_stage; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT uq_event_seed_team_stage UNIQUE (event_id, team_id, competition_stage);


--
-- Name: event_team_finishes uq_event_team_finish_event_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT uq_event_team_finish_event_team UNIQUE (event_id, team_id);


--
-- Name: event_team_finishes uq_event_team_finish_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT uq_event_team_finish_team UNIQUE (team_id);


--
-- Name: logical_round_promotions uq_logical_round_promotions_target_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT uq_logical_round_promotions_target_team UNIQUE (target_logical_round_id, team_id);


--
-- Name: round_result_version_entries uq_result_entry_id_version; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT uq_result_entry_id_version UNIQUE (id, result_version_id);


--
-- Name: round_result_versions uq_result_version_number; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT uq_result_version_number UNIQUE (round_id, version_number);


--
-- Name: round_result_version_entries uq_result_version_rank; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT uq_result_version_rank UNIQUE (result_version_id, rank);


--
-- Name: round_result_version_entries uq_result_version_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT uq_result_version_team UNIQUE (result_version_id, team_id);


--
-- Name: round_criteria uq_round_criteria_round_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT uq_round_criteria_round_name UNIQUE (round_id, name);


--
-- Name: round_definitions uq_round_definitions_event_sequence; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_definitions
    ADD CONSTRAINT uq_round_definitions_event_sequence UNIQUE (event_id, sequence_number);


--
-- Name: round_judges uq_round_judges_round_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT uq_round_judges_round_user UNIQUE (round_id, user_id);


--
-- Name: round_participants uq_round_participants_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT uq_round_participants_round_team UNIQUE (round_id, team_id);


--
-- Name: round_rankings uq_round_rankings_round_rank; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT uq_round_rankings_round_rank UNIQUE (round_id, rank);


--
-- Name: round_rankings uq_round_rankings_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT uq_round_rankings_round_team UNIQUE (round_id, team_id);


--
-- Name: rounds uq_rounds_logical_track; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT uq_rounds_logical_track UNIQUE (logical_round_id, track_id);


--
-- Name: rounds uq_rounds_track_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT uq_rounds_track_name UNIQUE (track_id, name);


--
-- Name: rounds uq_rounds_track_sequence; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT uq_rounds_track_sequence UNIQUE (track_id, sequence_number);


--
-- Name: rule_acceptances uq_rule_acceptances_user_event; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT uq_rule_acceptances_user_event UNIQUE (user_id, event_id);


--
-- Name: scores uq_scores_submission_judge_criterion; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT uq_scores_submission_judge_criterion UNIQUE (submission_id, judge_id, criterion_id);


--
-- Name: submissions uq_submissions_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT uq_submissions_round_team UNIQUE (round_id, team_id);


--
-- Name: team_members uq_team_members_team_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id);


--
-- Name: teams uq_teams_track_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT uq_teams_track_name UNIQUE (track_id, name);


--
-- Name: tie_break_decisions uq_tie_break_decisions_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT uq_tie_break_decisions_round_team UNIQUE (round_id, team_id);


--
-- Name: track_judges uq_track_judges_track_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT uq_track_judges_track_user UNIQUE (track_id, user_id);


--
-- Name: track_mentors uq_track_mentors_track_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT uq_track_mentors_track_user UNIQUE (track_id, user_id);


--
-- Name: tracks uq_tracks_event_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT uq_tracks_event_name UNIQUE (event_id, name);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_activation_expires; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_activation_expires ON public.account_activation_tokens USING btree (expires_at);


--
-- Name: idx_activation_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_activation_user ON public.account_activation_tokens USING btree (user_id);


--
-- Name: idx_appeals_round; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_round ON public.appeals USING btree (round_id);


--
-- Name: idx_appeals_round_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_round_status ON public.appeals USING btree (round_id, status);


--
-- Name: idx_appeals_team; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_team ON public.appeals USING btree (team_id);


--
-- Name: idx_audit_logs_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_action ON public.audit_logs USING btree (action);


--
-- Name: idx_audit_logs_incident_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_incident_id ON public.audit_logs USING btree (incident_id);


--
-- Name: idx_audit_logs_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_team_id ON public.audit_logs USING btree (team_id);


--
-- Name: idx_audit_logs_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_user_id ON public.audit_logs USING btree (user_id);


--
-- Name: idx_event_rules_event; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_rules_event ON public.event_rules USING btree (event_id, visibility);


--
-- Name: idx_event_seed_assignments_event_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_seed_assignments_event_status ON public.event_seed_assignments USING btree (event_id, status);


--
-- Name: idx_event_seed_assignments_profile; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_seed_assignments_profile ON public.event_seed_assignments USING btree (team_profile_id);


--
-- Name: idx_event_seed_assignments_track; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_seed_assignments_track ON public.event_seed_assignments USING btree (track_id);


--
-- Name: idx_event_team_finishes_event; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_team_finishes_event ON public.event_team_finishes USING btree (event_id);


--
-- Name: idx_event_team_finishes_profile; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_team_finishes_profile ON public.event_team_finishes USING btree (team_profile_id);


--
-- Name: idx_event_team_finishes_track_rank; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_team_finishes_track_rank ON public.event_team_finishes USING btree (track_id, final_rank);


--
-- Name: idx_event_team_finishes_version; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_team_finishes_version ON public.event_team_finishes USING btree (result_version_id);


--
-- Name: idx_incident_reports_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_event_id ON public.incident_reports USING btree (event_id);


--
-- Name: idx_incident_reports_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_status ON public.incident_reports USING btree (status);


--
-- Name: idx_incident_reports_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_team_id ON public.incident_reports USING btree (team_id);


--
-- Name: idx_incident_reports_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_type ON public.incident_reports USING btree (type);


--
-- Name: idx_logical_round_promotions_source; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logical_round_promotions_source ON public.logical_round_promotions USING btree (source_logical_round_id);


--
-- Name: idx_logical_round_promotions_source_entry; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logical_round_promotions_source_entry ON public.logical_round_promotions USING btree (source_result_entry_id);


--
-- Name: idx_logical_round_promotions_source_version; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logical_round_promotions_source_version ON public.logical_round_promotions USING btree (source_result_version_id);


--
-- Name: idx_logical_round_promotions_target; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logical_round_promotions_target ON public.logical_round_promotions USING btree (target_logical_round_id);


--
-- Name: idx_notices_author_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_author_id ON public.notices USING btree (author_id);


--
-- Name: idx_notices_target_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_event_id ON public.notices USING btree (target_event_id);


--
-- Name: idx_notices_target_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_role ON public.notices USING btree (target_role);


--
-- Name: idx_notices_target_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_track_id ON public.notices USING btree (target_track_id);


--
-- Name: idx_notifications_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user ON public.notifications USING btree (user_id);


--
-- Name: idx_notifications_user_unread; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user_unread ON public.notifications USING btree (user_id, category) WHERE (read_at IS NULL);


--
-- Name: idx_prize_revisions_prize; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prize_revisions_prize ON public.prize_revisions USING btree (prize_id, changed_at DESC);


--
-- Name: idx_revoked_tokens_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_revoked_tokens_expires_at ON public.revoked_tokens USING btree (expires_at);


--
-- Name: idx_revoked_tokens_token_hash; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_revoked_tokens_token_hash ON public.revoked_tokens USING btree (token_hash);


--
-- Name: idx_round_criteria_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_criteria_round_id ON public.round_criteria USING btree (round_id);


--
-- Name: idx_round_definitions_event_sequence; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_definitions_event_sequence ON public.round_definitions USING btree (event_id, sequence_number);


--
-- Name: idx_round_judges_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_judges_round_id ON public.round_judges USING btree (round_id);


--
-- Name: idx_round_judges_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_judges_user_id ON public.round_judges USING btree (user_id);


--
-- Name: idx_round_participants_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_round_id ON public.round_participants USING btree (round_id);


--
-- Name: idx_round_participants_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_status ON public.round_participants USING btree (status);


--
-- Name: idx_round_participants_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_team_id ON public.round_participants USING btree (team_id);


--
-- Name: idx_round_rankings_rank; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_rank ON public.round_rankings USING btree (rank);


--
-- Name: idx_round_rankings_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_round_id ON public.round_rankings USING btree (round_id);


--
-- Name: idx_round_rankings_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_team_id ON public.round_rankings USING btree (team_id);


--
-- Name: idx_rounds_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_rounds_track_id ON public.rounds USING btree (track_id);


--
-- Name: idx_scores_criterion_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_criterion_id ON public.scores USING btree (criterion_id);


--
-- Name: idx_scores_judge_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_judge_id ON public.scores USING btree (judge_id);


--
-- Name: idx_scores_submission_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_submission_id ON public.scores USING btree (submission_id);


--
-- Name: idx_submissions_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_submissions_round_id ON public.submissions USING btree (round_id);


--
-- Name: idx_submissions_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_submissions_team_id ON public.submissions USING btree (team_id);


--
-- Name: idx_support_tickets_requester_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_support_tickets_requester_id ON public.support_tickets USING btree (requester_id);


--
-- Name: idx_team_chat_messages_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_chat_messages_team_id ON public.team_chat_messages USING btree (team_id);


--
-- Name: idx_team_members_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_members_team_id ON public.team_members USING btree (team_id);


--
-- Name: idx_team_members_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_members_user_id ON public.team_members USING btree (user_id);


--
-- Name: idx_team_recognitions_code_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_recognitions_code_active ON public.team_recognitions USING btree (recognition_code, active);


--
-- Name: idx_team_recognitions_profile; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_recognitions_profile ON public.team_recognitions USING btree (team_profile_id);


--
-- Name: idx_team_timeline_events_event; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event ON public.team_timeline_events USING btree (event_id, occurred_at DESC);


--
-- Name: idx_team_timeline_events_event_round_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event_round_time ON public.team_timeline_events USING btree (event_id, round_id, occurred_at DESC, id DESC);


--
-- Name: idx_team_timeline_events_event_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event_time ON public.team_timeline_events USING btree (event_id, occurred_at DESC, id DESC);


--
-- Name: idx_team_timeline_events_event_track_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event_track_time ON public.team_timeline_events USING btree (event_id, track_id, occurred_at DESC, id DESC);


--
-- Name: idx_team_timeline_events_round_track; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_round_track ON public.team_timeline_events USING btree (round_id, track_id, occurred_at DESC);


--
-- Name: idx_team_timeline_events_scope; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_scope ON public.team_timeline_events USING btree (event_id, visibility_scope, occurred_at DESC);


--
-- Name: idx_team_timeline_events_team; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_team ON public.team_timeline_events USING btree (team_id, occurred_at DESC);


--
-- Name: idx_team_timeline_events_team_time_stable; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_team_time_stable ON public.team_timeline_events USING btree (team_id, occurred_at DESC, id DESC);


--
-- Name: idx_team_timeline_events_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_type ON public.team_timeline_events USING btree (event_id, event_type, occurred_at DESC, id DESC);


--
-- Name: idx_teams_source_team; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_source_team ON public.teams USING btree (source_team_id);


--
-- Name: idx_teams_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_status ON public.teams USING btree (status);


--
-- Name: idx_teams_team_profile; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_team_profile ON public.teams USING btree (team_profile_id);


--
-- Name: idx_teams_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_track_id ON public.teams USING btree (track_id);


--
-- Name: idx_tjr_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tjr_team_id ON public.team_join_requests USING btree (team_id);


--
-- Name: idx_tjr_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tjr_user_id ON public.team_join_requests USING btree (user_id);


--
-- Name: idx_track_judges_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_judges_track_id ON public.track_judges USING btree (track_id);


--
-- Name: idx_track_judges_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_judges_user_id ON public.track_judges USING btree (user_id);


--
-- Name: idx_track_mentors_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_mentors_track_id ON public.track_mentors USING btree (track_id);


--
-- Name: idx_track_mentors_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_mentors_user_id ON public.track_mentors USING btree (user_id);


--
-- Name: idx_tracks_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tracks_event_id ON public.tracks USING btree (event_id);


--
-- Name: idx_users_campus_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_campus_id ON public.users USING btree (campus_id);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- Name: idx_users_university_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_university_id ON public.users USING btree (university_id);


--
-- Name: uq_appeal_team_version; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_appeal_team_version ON public.appeals USING btree (team_id, result_version_id) WHERE (result_version_id IS NOT NULL);


--
-- Name: uq_event_seed_number_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_event_seed_number_active ON public.event_seed_assignments USING btree (event_id, track_id, competition_stage, seed_number) WHERE ((seed_number IS NOT NULL) AND ((status)::text = ANY (ARRAY[('confirmed'::character varying)::text, ('overridden'::character varying)::text])));


--
-- Name: uq_result_version_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_result_version_active ON public.round_result_versions USING btree (round_id) WHERE ((status)::text = 'published'::text);


--
-- Name: uq_round_definitions_event_lower_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_round_definitions_event_lower_name ON public.round_definitions USING btree (event_id, lower((name)::text));


--
-- Name: uq_round_definitions_event_name_sequence; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_round_definitions_event_name_sequence ON public.round_definitions USING btree (event_id, lower((name)::text), sequence_number);


--
-- Name: uq_team_recognition_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_team_recognition_active ON public.team_recognitions USING btree (team_profile_id, recognition_code) WHERE (active = true);


--
-- Name: uq_team_timeline_events_idempotency; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_team_timeline_events_idempotency ON public.team_timeline_events USING btree (idempotency_key) WHERE (idempotency_key IS NOT NULL);


--
-- Name: uq_teams_invite_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_teams_invite_code ON public.teams USING btree (invite_code);


--
-- Name: uq_tjr_team_user_pending; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_tjr_team_user_pending ON public.team_join_requests USING btree (team_id, user_id) WHERE ((status)::text = 'pending'::text);


--
-- Name: campuses trg_campuses_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_campuses_updated_at BEFORE UPDATE ON public.campuses FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: criteria_templates trg_criteria_templates_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_criteria_templates_updated_at BEFORE UPDATE ON public.criteria_templates FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: events trg_events_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_events_updated_at BEFORE UPDATE ON public.events FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: incident_reports trg_incident_reports_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_incident_reports_updated_at BEFORE UPDATE ON public.incident_reports FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: mentor_feedbacks trg_mentor_feedbacks_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_mentor_feedbacks_updated_at BEFORE UPDATE ON public.mentor_feedbacks FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: prizes trg_prizes_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_prizes_updated_at BEFORE UPDATE ON public.prizes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: event_team_finishes trg_reject_event_team_finish_mutation; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_reject_event_team_finish_mutation BEFORE DELETE OR UPDATE ON public.event_team_finishes FOR EACH ROW EXECUTE FUNCTION public.reject_event_team_finish_mutation();


--
-- Name: logical_round_promotions trg_reject_promotion_provenance_mutation; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_reject_promotion_provenance_mutation BEFORE UPDATE ON public.logical_round_promotions FOR EACH ROW EXECUTE FUNCTION public.reject_promotion_provenance_mutation();


--
-- Name: round_criteria trg_round_criteria_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_criteria_updated_at BEFORE UPDATE ON public.round_criteria FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: round_participants trg_round_participants_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_participants_updated_at BEFORE UPDATE ON public.round_participants FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: round_rankings trg_round_rankings_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_rankings_updated_at BEFORE UPDATE ON public.round_rankings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: rounds trg_rounds_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_rounds_updated_at BEFORE UPDATE ON public.rounds FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: scores trg_scores_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_scores_updated_at BEFORE UPDATE ON public.scores FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: submissions trg_submissions_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_submissions_updated_at BEFORE UPDATE ON public.submissions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: teams trg_teams_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_teams_updated_at BEFORE UPDATE ON public.teams FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: tracks trg_tracks_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_tracks_updated_at BEFORE UPDATE ON public.tracks FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: universities trg_universities_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_universities_updated_at BEFORE UPDATE ON public.universities FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: users trg_users_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: event_seed_assignments trg_validate_event_seed_assignment; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_event_seed_assignment BEFORE INSERT OR UPDATE ON public.event_seed_assignments FOR EACH ROW EXECUTE FUNCTION public.validate_event_seed_assignment();


--
-- Name: event_team_finishes trg_validate_event_team_finish; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_event_team_finish BEFORE INSERT ON public.event_team_finishes FOR EACH ROW EXECUTE FUNCTION public.validate_event_team_finish();


--
-- Name: logical_round_promotions trg_validate_logical_round_promotion; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_logical_round_promotion BEFORE INSERT OR UPDATE ON public.logical_round_promotions FOR EACH ROW EXECUTE FUNCTION public.validate_logical_round_promotion();


--
-- Name: logical_round_promotions trg_validate_logical_round_promotion_provenance; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_logical_round_promotion_provenance BEFORE INSERT OR UPDATE ON public.logical_round_promotions FOR EACH ROW EXECUTE FUNCTION public.validate_logical_round_promotion_provenance();


--
-- Name: teams trg_validate_team_profile_registration; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_team_profile_registration BEFORE INSERT OR UPDATE OF team_profile_id, track_id, source_team_id ON public.teams FOR EACH ROW EXECUTE FUNCTION public.validate_team_profile_registration();


--
-- Name: account_activation_tokens account_activation_tokens_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation_tokens
    ADD CONSTRAINT account_activation_tokens_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: account_activation_tokens account_activation_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation_tokens
    ADD CONSTRAINT account_activation_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: appeals appeals_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: appeals appeals_resolved_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_resolved_by_fkey FOREIGN KEY (resolved_by) REFERENCES public.users(id);


--
-- Name: appeals appeals_result_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_result_version_id_fkey FOREIGN KEY (result_version_id) REFERENCES public.round_result_versions(id);


--
-- Name: appeals appeals_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- Name: appeals appeals_submitted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_submitted_by_fkey FOREIGN KEY (submitted_by) REFERENCES public.users(id);


--
-- Name: appeals appeals_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: audit_logs audit_logs_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE SET NULL;


--
-- Name: audit_logs audit_logs_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: campuses campuses_university_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT campuses_university_id_fkey FOREIGN KEY (university_id) REFERENCES public.universities(id) ON DELETE CASCADE;


--
-- Name: event_rules event_rules_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_rules
    ADD CONSTRAINT event_rules_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: event_seed_assignments event_seed_assignments_assigned_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_assigned_by_fkey FOREIGN KEY (assigned_by) REFERENCES public.users(id);


--
-- Name: event_seed_assignments event_seed_assignments_candidate_source_finish_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_candidate_source_finish_id_fkey FOREIGN KEY (candidate_source_finish_id) REFERENCES public.event_team_finishes(id);


--
-- Name: event_seed_assignments event_seed_assignments_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id);


--
-- Name: event_seed_assignments event_seed_assignments_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id);


--
-- Name: event_seed_assignments event_seed_assignments_team_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_team_profile_id_fkey FOREIGN KEY (team_profile_id) REFERENCES public.team_profiles(id);


--
-- Name: event_seed_assignments event_seed_assignments_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id);


--
-- Name: event_team_finishes event_team_finishes_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: event_team_finishes event_team_finishes_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id);


--
-- Name: event_team_finishes event_team_finishes_final_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_final_round_id_fkey FOREIGN KEY (final_round_id) REFERENCES public.rounds(id);


--
-- Name: event_team_finishes event_team_finishes_result_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_result_version_id_fkey FOREIGN KEY (result_version_id) REFERENCES public.round_result_versions(id);


--
-- Name: event_team_finishes event_team_finishes_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id);


--
-- Name: event_team_finishes event_team_finishes_team_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_team_profile_id_fkey FOREIGN KEY (team_profile_id) REFERENCES public.team_profiles(id);


--
-- Name: event_team_finishes event_team_finishes_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id);


--
-- Name: logical_round_promotions fk_promotion_entry_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT fk_promotion_entry_version FOREIGN KEY (source_result_entry_id, source_result_version_id) REFERENCES public.round_result_version_entries(id, result_version_id) ON DELETE RESTRICT;


--
-- Name: logical_round_promotions fk_promotion_source_result_entry; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT fk_promotion_source_result_entry FOREIGN KEY (source_result_entry_id) REFERENCES public.round_result_version_entries(id) ON DELETE RESTRICT;


--
-- Name: logical_round_promotions fk_promotion_source_result_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT fk_promotion_source_result_version FOREIGN KEY (source_result_version_id) REFERENCES public.round_result_versions(id) ON DELETE RESTRICT;


--
-- Name: rounds fk_rounds_logical_round; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT fk_rounds_logical_round FOREIGN KEY (logical_round_id) REFERENCES public.round_definitions(id);


--
-- Name: incident_actions incident_actions_action_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_action_by_fkey FOREIGN KEY (action_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: incident_actions incident_actions_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE CASCADE;


--
-- Name: incident_evidences incident_evidences_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE CASCADE;


--
-- Name: incident_evidences incident_evidences_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: incident_reports incident_reports_assigned_coordinator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_assigned_coordinator_id_fkey FOREIGN KEY (assigned_coordinator_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: incident_reports incident_reports_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: incident_reports incident_reports_reporter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_reporter_id_fkey FOREIGN KEY (reporter_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: incident_reports incident_reports_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- Name: incident_reports incident_reports_submission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_submission_id_fkey FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE SET NULL;


--
-- Name: incident_reports incident_reports_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: incident_reports incident_reports_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- Name: logical_round_promotions logical_round_promotions_source_logical_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT logical_round_promotions_source_logical_round_id_fkey FOREIGN KEY (source_logical_round_id) REFERENCES public.round_definitions(id) ON DELETE RESTRICT;


--
-- Name: logical_round_promotions logical_round_promotions_target_logical_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT logical_round_promotions_target_logical_round_id_fkey FOREIGN KEY (target_logical_round_id) REFERENCES public.round_definitions(id) ON DELETE RESTRICT;


--
-- Name: logical_round_promotions logical_round_promotions_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT logical_round_promotions_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE RESTRICT;


--
-- Name: mentor_feedbacks mentor_feedbacks_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- Name: mentor_feedbacks mentor_feedbacks_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: mentor_feedbacks mentor_feedbacks_track_mentor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_track_mentor_id_fkey FOREIGN KEY (track_mentor_id) REFERENCES public.track_mentors(id) ON DELETE CASCADE;


--
-- Name: notices notices_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: notices notices_target_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_target_event_id_fkey FOREIGN KEY (target_event_id) REFERENCES public.events(id) ON DELETE SET NULL;


--
-- Name: notices notices_target_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_target_track_id_fkey FOREIGN KEY (target_track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: prize_revisions prize_revisions_changed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES public.users(id);


--
-- Name: prize_revisions prize_revisions_new_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_new_team_id_fkey FOREIGN KEY (new_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: prize_revisions prize_revisions_old_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_old_team_id_fkey FOREIGN KEY (old_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: prize_revisions prize_revisions_prize_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_prize_id_fkey FOREIGN KEY (prize_id) REFERENCES public.prizes(id) ON DELETE CASCADE;


--
-- Name: prizes prizes_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: prizes prizes_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: prizes prizes_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- Name: round_criteria round_criteria_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- Name: round_criteria round_criteria_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.criteria_templates(id) ON DELETE SET NULL;


--
-- Name: round_definitions round_definitions_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_definitions
    ADD CONSTRAINT round_definitions_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: round_judges round_judges_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- Name: round_judges round_judges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: round_participants round_participants_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- Name: round_participants round_participants_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: round_rankings round_rankings_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- Name: round_rankings round_rankings_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: round_rankings round_rankings_tie_breaker_criterion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_tie_breaker_criterion_id_fkey FOREIGN KEY (tie_breaker_criterion_id) REFERENCES public.round_criteria(id) ON DELETE SET NULL;


--
-- Name: round_result_version_entries round_result_version_entries_result_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT round_result_version_entries_result_version_id_fkey FOREIGN KEY (result_version_id) REFERENCES public.round_result_versions(id) ON DELETE CASCADE;


--
-- Name: round_result_version_entries round_result_version_entries_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT round_result_version_entries_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id);


--
-- Name: round_result_versions round_result_versions_published_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT round_result_versions_published_by_fkey FOREIGN KEY (published_by) REFERENCES public.users(id);


--
-- Name: round_result_versions round_result_versions_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT round_result_versions_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- Name: round_result_versions round_result_versions_source_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT round_result_versions_source_version_id_fkey FOREIGN KEY (source_version_id) REFERENCES public.round_result_versions(id);


--
-- Name: rounds rounds_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT rounds_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- Name: rule_acceptances rule_acceptances_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: rule_acceptances rule_acceptances_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: scores scores_criterion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_criterion_id_fkey FOREIGN KEY (criterion_id) REFERENCES public.round_criteria(id) ON DELETE CASCADE;


--
-- Name: scores scores_judge_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_judge_id_fkey FOREIGN KEY (judge_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: scores scores_submission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_submission_id_fkey FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE;


--
-- Name: submissions submissions_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- Name: submissions submissions_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: support_tickets support_tickets_requester_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT support_tickets_requester_id_fkey FOREIGN KEY (requester_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: team_chat_messages team_chat_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: team_chat_messages team_chat_messages_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: team_join_requests team_join_requests_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: team_join_requests team_join_requests_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: team_members team_members_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: team_members team_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: team_profiles team_profiles_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_profiles
    ADD CONSTRAINT team_profiles_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: team_recognitions team_recognitions_revoked_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_recognitions
    ADD CONSTRAINT team_recognitions_revoked_by_fkey FOREIGN KEY (revoked_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: team_recognitions team_recognitions_team_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_recognitions
    ADD CONSTRAINT team_recognitions_team_profile_id_fkey FOREIGN KEY (team_profile_id) REFERENCES public.team_profiles(id);


--
-- Name: team_timeline_events team_timeline_events_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: team_timeline_events team_timeline_events_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- Name: team_timeline_events team_timeline_events_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: team_timeline_events team_timeline_events_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- Name: teams teams_roster_confirmed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_roster_confirmed_by_fkey FOREIGN KEY (roster_confirmed_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: teams teams_source_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_source_team_id_fkey FOREIGN KEY (source_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: teams teams_team_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_team_profile_id_fkey FOREIGN KEY (team_profile_id) REFERENCES public.team_profiles(id);


--
-- Name: teams teams_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- Name: tie_break_decisions tie_break_decisions_decided_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_decided_by_fkey FOREIGN KEY (decided_by) REFERENCES public.users(id);


--
-- Name: tie_break_decisions tie_break_decisions_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- Name: tie_break_decisions tie_break_decisions_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- Name: track_judges track_judges_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: track_judges track_judges_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- Name: track_judges track_judges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: track_mentors track_mentors_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: track_mentors track_mentors_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- Name: track_mentors track_mentors_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: tracks tracks_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT tracks_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: users users_campus_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_campus_id_fkey FOREIGN KEY (campus_id) REFERENCES public.campuses(id) ON DELETE SET NULL;


--
-- Name: users users_university_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_university_id_fkey FOREIGN KEY (university_id) REFERENCES public.universities(id) ON DELETE SET NULL;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--
-- PostgreSQL database dump complete
--

\unrestrict c65MpVmowLeGWtftMpEEH5cJgaE30oLYSShBEFbf4khUhK56HSc0Z1syM1AxoDV

