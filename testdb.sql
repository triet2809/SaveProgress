--
-- PostgreSQL database dump
--

\restrict B8YfETNzRNpgJ7zy16qFkwk9cdLDAp9CSoUgJZKn8arVcBN28jl7MVH5utDlSey

-- Dumped from database version 17.10
-- Dumped by pg_dump version 17.10

-- Started on 2026-07-21 15:29:12

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
-- TOC entry 6 (class 2615 OID 46716)
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

--
-- TOC entry 5733 (class 0 OID 0)
-- Dependencies: 6
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS '';


--
-- TOC entry 2 (class 3079 OID 46717)
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- TOC entry 5735 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- TOC entry 938 (class 1247 OID 46755)
-- Name: account_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.account_status AS ENUM (
    'pending',
    'approved',
    'rejected'
);


ALTER TYPE public.account_status OWNER TO postgres;

--
-- TOC entry 941 (class 1247 OID 46762)
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
-- TOC entry 944 (class 1247 OID 46808)
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
-- TOC entry 947 (class 1247 OID 46820)
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
-- TOC entry 950 (class 1247 OID 46834)
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
-- TOC entry 953 (class 1247 OID 46844)
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
-- TOC entry 956 (class 1247 OID 46858)
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
-- TOC entry 959 (class 1247 OID 46868)
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
-- TOC entry 962 (class 1247 OID 46880)
-- Name: student_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.student_type AS ENUM (
    'fpt',
    'external',
    'none'
);


ALTER TYPE public.student_type OWNER TO postgres;

--
-- TOC entry 965 (class 1247 OID 46888)
-- Name: team_member_role; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.team_member_role AS ENUM (
    'leader',
    'member'
);


ALTER TYPE public.team_member_role OWNER TO postgres;

--
-- TOC entry 968 (class 1247 OID 46894)
-- Name: team_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.team_status AS ENUM (
    'active',
    'disqualified'
);


ALTER TYPE public.team_status OWNER TO postgres;

--
-- TOC entry 313 (class 1255 OID 46899)
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
-- TOC entry 320 (class 1255 OID 57611)
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
-- TOC entry 314 (class 1255 OID 46900)
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
-- TOC entry 315 (class 1255 OID 46901)
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
-- TOC entry 316 (class 1255 OID 46902)
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
-- TOC entry 318 (class 1255 OID 57591)
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
-- TOC entry 319 (class 1255 OID 57610)
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
-- TOC entry 317 (class 1255 OID 46903)
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
-- TOC entry 218 (class 1259 OID 46904)
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
-- TOC entry 219 (class 1259 OID 46909)
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
-- TOC entry 220 (class 1259 OID 46918)
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
-- TOC entry 221 (class 1259 OID 46925)
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
-- TOC entry 222 (class 1259 OID 46932)
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
-- TOC entry 223 (class 1259 OID 46940)
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
-- TOC entry 224 (class 1259 OID 46949)
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
-- TOC entry 225 (class 1259 OID 46963)
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
-- TOC entry 226 (class 1259 OID 46971)
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
-- TOC entry 227 (class 1259 OID 46979)
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
-- TOC entry 228 (class 1259 OID 46986)
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
-- TOC entry 229 (class 1259 OID 46993)
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
-- TOC entry 265 (class 1259 OID 57564)
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
-- TOC entry 230 (class 1259 OID 47001)
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
-- TOC entry 231 (class 1259 OID 47008)
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
-- TOC entry 232 (class 1259 OID 47017)
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
-- TOC entry 233 (class 1259 OID 47024)
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
-- TOC entry 234 (class 1259 OID 47031)
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
-- TOC entry 235 (class 1259 OID 47038)
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
-- TOC entry 236 (class 1259 OID 47044)
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    description text
);


ALTER TABLE public.roles OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 47050)
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
-- TOC entry 264 (class 1259 OID 57535)
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
-- TOC entry 238 (class 1259 OID 47059)
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
-- TOC entry 239 (class 1259 OID 47064)
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
-- TOC entry 240 (class 1259 OID 47072)
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
-- TOC entry 241 (class 1259 OID 47081)
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
-- TOC entry 242 (class 1259 OID 47088)
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
-- TOC entry 243 (class 1259 OID 47095)
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
-- TOC entry 244 (class 1259 OID 47104)
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
-- TOC entry 245 (class 1259 OID 47109)
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
-- TOC entry 246 (class 1259 OID 47117)
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
-- TOC entry 247 (class 1259 OID 47126)
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
-- TOC entry 248 (class 1259 OID 47135)
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
-- TOC entry 249 (class 1259 OID 47143)
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
-- TOC entry 250 (class 1259 OID 47152)
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
-- TOC entry 251 (class 1259 OID 47158)
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
-- TOC entry 252 (class 1259 OID 47168)
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
-- TOC entry 253 (class 1259 OID 47180)
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
-- TOC entry 254 (class 1259 OID 47188)
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
-- TOC entry 255 (class 1259 OID 47196)
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
-- TOC entry 256 (class 1259 OID 47203)
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
-- TOC entry 257 (class 1259 OID 47208)
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
-- TOC entry 258 (class 1259 OID 47213)
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
-- TOC entry 259 (class 1259 OID 47220)
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
-- TOC entry 260 (class 1259 OID 47225)
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- TOC entry 261 (class 1259 OID 47229)
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
-- TOC entry 262 (class 1259 OID 47239)
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
-- TOC entry 263 (class 1259 OID 47244)
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
-- TOC entry 5682 (class 0 OID 46904)
-- Dependencies: 218
-- Data for Name: account_activation_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.account_activation_tokens (id, user_id, token_hash, expires_at, used_at, created_at, created_by) FROM stdin;
31000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000009	191946881187f252a45428fc2957f5fe54af5032e15b3c4a7fb9eb89cba7bcd6	2026-07-28 13:55:15.848083	\N	2026-07-21 13:55:15.848083	30000000-0000-4000-8000-000000000001
\.


--
-- TOC entry 5683 (class 0 OID 46909)
-- Dependencies: 219
-- Data for Name: appeals; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.appeals (id, event_id, round_id, team_id, submitted_by, reason, status, response, resolved_by, result_published_at, appeal_deadline, resolved_at, created_at, updated_at, result_version_id, decision, recalculation_required) FROM stdin;
60000000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000007	Please review the technical criterion score.	PENDING	\N	\N	2026-07-19 13:55:15.848083	2026-07-22 13:55:15.848083	\N	2026-07-21 13:55:15.848083	\N	56000000-0000-4000-8000-000000000001	\N	f
\.


--
-- TOC entry 5684 (class 0 OID 46918)
-- Dependencies: 220
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_logs (id, user_id, team_id, incident_id, action, target_type, target_id, old_value, new_value, details, occurred_at) FROM stdin;
81000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	\N	PUBLISH_RESULTS	round_result_version	56000000-0000-4000-8000-000000000001	\N	PUBLISHED	E2E initial result publication	2026-07-21 13:55:15.848083
81000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	61000000-0000-4000-8000-000000000001	PROCESS_INCIDENT	incident_report	61000000-0000-4000-8000-000000000001	reported	under_review	E2E coordinator started review	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5685 (class 0 OID 46925)
-- Dependencies: 221
-- Data for Name: campuses; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.campuses (id, university_id, name, address, city, created_at, updated_at) FROM stdin;
21000000-0000-4000-8000-000000000001	20000000-0000-4000-8000-000000000001	Ho Chi Minh Campus	Thu Duc City	Ho Chi Minh City	2026-07-21 13:55:15.848083	\N
21000000-0000-4000-8000-000000000002	20000000-0000-4000-8000-000000000001	Ha Noi Campus	Thach That	Ha Noi	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5686 (class 0 OID 46932)
-- Dependencies: 222
-- Data for Name: criteria_templates; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.criteria_templates (id, name, description, default_weight, created_at, updated_at) FROM stdin;
46000000-0000-4000-8000-000000000001	Innovation	Novelty and creativity	40.00	2026-07-21 13:55:15.848083	\N
46000000-0000-4000-8000-000000000002	Technical Quality	Architecture and implementation	35.00	2026-07-21 13:55:15.848083	\N
46000000-0000-4000-8000-000000000003	Impact	User and business impact	25.00	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5687 (class 0 OID 46940)
-- Dependencies: 223
-- Data for Name: event_rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_rules (id, event_id, title, content, visibility, display_order, created_at, updated_at) FROM stdin;
41000000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	Eligibility	Each team must have 2-5 eligible members.	PUBLIC	1	2026-07-21 13:55:15.848083	\N
41000000-0000-4000-8000-000000000002	40000000-0000-4000-8000-000000000001	Submission policy	Only the latest submitted version is scored.	PUBLIC	2	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5688 (class 0 OID 46949)
-- Dependencies: 224
-- Data for Name: event_seed_assignments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_seed_assignments (id, event_id, track_id, team_id, team_profile_id, competition_stage, seed_number, seed_tier, candidate_source_finish_id, continuity_count, status, rationale, assigned_by, assigned_at, updated_at, row_version) FROM stdin;
71100000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	50000000-0000-4000-8000-000000000001	final	1	A	71000000-0000-4000-8000-000000000001	1	confirmed	Top qualification team	30000000-0000-4000-8000-000000000001	2026-07-21 13:55:15.848083	\N	0
\.


--
-- TOC entry 5689 (class 0 OID 46963)
-- Dependencies: 225
-- Data for Name: event_team_finishes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_team_finishes (id, event_id, track_id, team_id, team_profile_id, final_round_id, result_version_id, final_rank, final_score, completion_status, completed_at, created_at, created_by, row_version) FROM stdin;
71000000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	50000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000002	56000000-0000-4000-8000-000000000002	1	9.1000	completed	2026-07-21 03:55:15.848083	2026-07-21 13:55:15.848083	30000000-0000-4000-8000-000000000001	0
\.


--
-- TOC entry 5690 (class 0 OID 46971)
-- Dependencies: 226
-- Data for Name: events; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.events (id, title, description, status, created_at, updated_at, term, prize_pool, registration_start, registration_end, event_start, event_end) FROM stdin;
40000000-0000-4000-8000-000000000001	SEAL E2E Hackathon 2026	Published event used by automated E2E tests	ongoing	2026-07-21 13:55:15.848083	\N	Summer 2026	100,000,000 VND	2026-06-21 13:55:15.848083	2026-07-11 13:55:15.848083	2026-07-14 13:55:15.848083	2026-07-28 13:55:15.848083
40000000-0000-4000-8000-000000000002	SEAL Draft Event	Draft event for coordinator tests	draft	2026-07-21 13:55:15.848083	\N	Fall 2026	50,000,000 VND	2026-07-31 13:55:15.848083	2026-08-10 13:55:15.848083	2026-08-15 13:55:15.848083	2026-08-20 13:55:15.848083
ce6713c1-8491-4bf6-a125-13a248a9fb18	Hackathon 26	yy	draft	2026-07-21 07:03:54.500567	2026-07-21 07:03:54.500567	Summer	6777777777	2026-07-21 17:00:00	2026-07-22 17:00:00	2026-07-23 17:00:00	2026-07-24 17:00:00
\.


--
-- TOC entry 5691 (class 0 OID 46979)
-- Dependencies: 227
-- Data for Name: incident_actions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident_actions (id, incident_id, action_by, action_type, target_type, target_id, old_value, new_value, note, created_at) FROM stdin;
61200000-0000-4000-8000-000000000001	61000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000001	require_resubmission	submission	53000000-0000-4000-8000-000000000002	\N	\N	Allow one corrected demo URL before deadline.	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5692 (class 0 OID 46986)
-- Dependencies: 228
-- Data for Name: incident_evidences; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident_evidences (id, incident_id, file_url, external_url, description, uploaded_by, created_at) FROM stdin;
61100000-0000-4000-8000-000000000001	61000000-0000-4000-8000-000000000001	\N	https://example.test/evidence/demo-error	Screenshot and access log	30000000-0000-4000-8000-000000000003	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5693 (class 0 OID 46993)
-- Dependencies: 229
-- Data for Name: incident_reports; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident_reports (id, event_id, track_id, round_id, team_id, submission_id, reporter_id, assigned_coordinator_id, type, status, title, description, created_at, updated_at, resolved_at, severity, category) FROM stdin;
61000000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	53000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000003	30000000-0000-4000-8000-000000000001	technical_issue	under_review	Demo endpoint temporarily unavailable	Judge could not access the demo during the first review attempt.	2026-07-21 13:55:15.848083	\N	\N	medium	submission_access
\.


--
-- TOC entry 5727 (class 0 OID 57564)
-- Dependencies: 265
-- Data for Name: logical_round_promotions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.logical_round_promotions (id, source_logical_round_id, target_logical_round_id, team_id, created_at, source_result_version_id, source_result_entry_id) FROM stdin;
56200000-0000-4000-8000-000000000001	44000000-0000-4000-8000-000000000001	44000000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000001	2026-07-21 13:55:15.848083	56000000-0000-4000-8000-000000000001	56100000-0000-4000-8000-000000000001
\.


--
-- TOC entry 5694 (class 0 OID 47001)
-- Dependencies: 230
-- Data for Name: mentor_feedbacks; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.mentor_feedbacks (id, track_mentor_id, team_id, round_id, content, created_at, updated_at) FROM stdin;
56400000-0000-4000-8000-000000000001	43200000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000002	Focus the final demo on measurable user value.	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5695 (class 0 OID 47008)
-- Dependencies: 231
-- Data for Name: notices; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notices (id, title, content, priority, target_role, target_event_id, target_track_id, author_id, created_at, updated_at, target_team_id) FROM stdin;
63000000-0000-4000-8000-000000000001	Final submission reminder	Final submission closes in 48 hours.	high	team_leader	40000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000001	2026-07-21 13:55:15.848083	2026-07-21 13:55:15.848083	\N
63000000-0000-4000-8000-000000000002	Alpha incident update	Your support ticket is being reviewed.	normal	\N	40000000-0000-4000-8000-000000000001	\N	30000000-0000-4000-8000-000000000001	2026-07-21 13:55:15.848083	2026-07-21 13:55:15.848083	51000000-0000-4000-8000-000000000001
\.


--
-- TOC entry 5696 (class 0 OID 47017)
-- Dependencies: 232
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (id, user_id, type, title, body, category, ref_type, ref_id, read_at, created_at, updated_at) FROM stdin;
63100000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000005	ROUND_PROMOTION	Promoted to final	Alpha Innovators advanced to the final.	competition	round	45000000-0000-4000-8000-000000000002	\N	2026-07-21 13:55:15.848083	\N
63100000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000007	APPEAL_RECEIVED	Appeal received	Your appeal is pending review.	appeal	appeal	60000000-0000-4000-8000-000000000001	2026-07-21 13:55:15.848083	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5697 (class 0 OID 47024)
-- Dependencies: 233
-- Data for Name: prize_revisions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prize_revisions (id, prize_id, action, old_team_id, new_team_id, reason, evidence_note, changed_by, changed_at) FROM stdin;
70100000-0000-4000-8000-000000000001	70000000-0000-4000-8000-000000000001	UPDATE	51000000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000001	Corrected after score verification	Result version 1	30000000-0000-4000-8000-000000000001	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5698 (class 0 OID 47031)
-- Dependencies: 234
-- Data for Name: prizes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prizes (id, event_id, track_id, team_id, name, prize_amount, description, awarded_at, created_at, updated_at) FROM stdin;
70000000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	Best Qualification Project	10000000.00	E2E award record	2026-07-20 13:55:15.848083	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5699 (class 0 OID 47038)
-- Dependencies: 235
-- Data for Name: revoked_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.revoked_tokens (id, token_hash, expires_at, created_at, updated_at) FROM stdin;
32000000-0000-4000-8000-000000000001	feb665f8c99567db2571b858ef029067e9a12246da22fbc43fae961e06f3929e	2026-07-22 13:55:15.848083+07	2026-07-21 13:55:15.848083	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5700 (class 0 OID 47044)
-- Dependencies: 236
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.roles (id, name, description) FROM stdin;
10000000-0000-4000-8000-000000000001	coordinator	E2E coordinator
10000000-0000-4000-8000-000000000002	mentor	E2E mentor
10000000-0000-4000-8000-000000000003	judge	E2E judge
10000000-0000-4000-8000-000000000004	team_leader	E2E team leader
10000000-0000-4000-8000-000000000005	team_member	E2E team member
\.


--
-- TOC entry 5701 (class 0 OID 47050)
-- Dependencies: 237
-- Data for Name: round_criteria; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_criteria (id, round_id, template_id, name, weight, description, created_at, updated_at, status) FROM stdin;
46100000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	46000000-0000-4000-8000-000000000001	Innovation	40.00	Novelty	2026-07-21 13:55:15.848083	\N	active
46100000-0000-4000-8000-000000000002	45000000-0000-4000-8000-000000000001	46000000-0000-4000-8000-000000000002	Technical Quality	35.00	Technical execution	2026-07-21 13:55:15.848083	\N	active
46100000-0000-4000-8000-000000000003	45000000-0000-4000-8000-000000000001	46000000-0000-4000-8000-000000000003	Impact	25.00	Expected impact	2026-07-21 13:55:15.848083	\N	active
\.


--
-- TOC entry 5726 (class 0 OID 57535)
-- Dependencies: 264
-- Data for Name: round_definitions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_definitions (id, event_id, name, sequence_number, created_at, updated_at, is_final, default_top_n_to_promote, lifecycle_state) FROM stdin;
44000000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	Qualification	1	2026-07-21 13:55:15.848083	\N	f	2	APPEAL_WINDOW_OPEN
44000000-0000-4000-8000-000000000002	40000000-0000-4000-8000-000000000001	Final	2	2026-07-21 13:55:15.848083	\N	t	\N	APPEAL_WINDOW_OPEN
\.


--
-- TOC entry 5702 (class 0 OID 47059)
-- Dependencies: 238
-- Data for Name: round_judges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_judges (id, round_id, user_id, assigned_at) FROM stdin;
45100000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000003	2026-07-21 13:55:15.848083
45100000-0000-4000-8000-000000000002	45000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000004	2026-07-21 13:55:15.848083
45100000-0000-4000-8000-000000000003	45000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000003	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5703 (class 0 OID 47064)
-- Dependencies: 239
-- Data for Name: round_participants; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_participants (id, round_id, team_id, status, note, created_at, updated_at) FROM stdin;
52000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	promoted	Rank 1	2026-07-21 13:55:15.848083	\N
52000000-0000-4000-8000-000000000002	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	eliminated	Rank 2 test state	2026-07-21 13:55:15.848083	\N
52000000-0000-4000-8000-000000000003	45000000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000001	active	Promoted to final	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5704 (class 0 OID 47072)
-- Dependencies: 240
-- Data for Name: round_rankings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_rankings (id, round_id, team_id, total_score, rank, status, tie_breaker_criterion_id, tie_breaker_score, tie_breaker_reason, calculated_at, updated_at) FROM stdin;
55000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	8.58	1	promoted	\N	\N	\N	2026-07-19 13:55:15.848083	\N
55000000-0000-4000-8000-000000000002	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	7.33	2	eliminated	\N	\N	\N	2026-07-19 13:55:15.848083	\N
55000000-0000-4000-8000-000000000003	45000000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000001	9.10	1	promoted	\N	\N	\N	2026-07-21 01:55:15.848083	\N
\.


--
-- TOC entry 5705 (class 0 OID 47081)
-- Dependencies: 241
-- Data for Name: round_result_version_entries; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_result_version_entries (id, result_version_id, team_id, rank, total_score, promotion_status, tie_breaker_score, tie_breaker_reason, created_at) FROM stdin;
56100000-0000-4000-8000-000000000001	56000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	1	8.58	promoted	\N	\N	2026-07-21 13:55:15.848083
56100000-0000-4000-8000-000000000002	56000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	2	7.33	eliminated	\N	\N	2026-07-21 13:55:15.848083
56100000-0000-4000-8000-000000000003	56000000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000001	1	9.10	promoted	\N	\N	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5706 (class 0 OID 47088)
-- Dependencies: 242
-- Data for Name: round_result_versions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_result_versions (id, round_id, version_number, status, published_at, appeal_deadline, published_by, source_version_id, reason, created_at) FROM stdin;
56000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	1	published	2026-07-19 13:55:15.848083	2026-07-22 13:55:15.848083	30000000-0000-4000-8000-000000000001	\N	Initial published qualification result	2026-07-21 13:55:15.848083
56000000-0000-4000-8000-000000000002	45000000-0000-4000-8000-000000000002	1	published	2026-07-21 01:55:15.848083	2026-07-23 13:55:15.848083	30000000-0000-4000-8000-000000000001	\N	Published final result	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5707 (class 0 OID 47095)
-- Dependencies: 243
-- Data for Name: rounds; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.rounds (id, track_id, name, sequence_number, submission_deadline, top_n_to_promote, created_at, updated_at, result_published_at, appeal_deadline, lifecycle_state, lifecycle_version, logical_round_id) FROM stdin;
45000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	Qualification - AI	1	2026-07-17 13:55:15.848083	2	2026-07-21 13:55:15.848083	2026-07-21 14:56:37.320263	2026-07-19 13:55:15.848083	2026-07-22 13:55:15.848083	APPEAL_WINDOW_OPEN	1	44000000-0000-4000-8000-000000000001
45000000-0000-4000-8000-000000000002	43000000-0000-4000-8000-000000000001	Final - AI	2	2026-07-20 13:55:15.848083	1	2026-07-21 13:55:15.848083	2026-07-21 14:56:37.320263	2026-07-21 01:55:15.848083	2026-07-23 13:55:15.848083	APPEAL_WINDOW_OPEN	1	44000000-0000-4000-8000-000000000002
\.


--
-- TOC entry 5708 (class 0 OID 47104)
-- Dependencies: 244
-- Data for Name: rule_acceptances; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.rule_acceptances (id, user_id, event_id, accepted_at) FROM stdin;
42000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000005	40000000-0000-4000-8000-000000000001	2026-07-06 13:55:15.848083
42000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000007	40000000-0000-4000-8000-000000000001	2026-07-06 13:55:15.848083
\.


--
-- TOC entry 5709 (class 0 OID 47109)
-- Dependencies: 245
-- Data for Name: scores; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.scores (id, submission_id, judge_id, criterion_id, score, weighted_score, criterion_average_score, criterion_variance, criterion_stddev, comment, created_at, updated_at) FROM stdin;
54000000-0000-4000-8000-000000000001	53000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000003	46100000-0000-4000-8000-000000000001	9.00	3.60	8.75	0.1250	0.3536	Strong novelty	2026-07-21 13:55:15.848083	\N
54000000-0000-4000-8000-000000000002	53000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000003	46100000-0000-4000-8000-000000000002	8.50	2.98	8.25	0.1250	0.3536	Solid implementation	2026-07-21 13:55:15.848083	\N
54000000-0000-4000-8000-000000000003	53000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000003	46100000-0000-4000-8000-000000000003	8.00	2.00	8.00	0.0000	0.0000	Clear impact	2026-07-21 13:55:15.848083	\N
54000000-0000-4000-8000-000000000004	53000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000003	46100000-0000-4000-8000-000000000001	7.50	3.00	7.50	0.0000	0.0000	Good concept	2026-07-21 13:55:15.848083	\N
54000000-0000-4000-8000-000000000005	53000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000003	46100000-0000-4000-8000-000000000002	7.00	2.45	7.00	0.0000	0.0000	Needs polish	2026-07-21 13:55:15.848083	\N
54000000-0000-4000-8000-000000000006	53000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000003	46100000-0000-4000-8000-000000000003	7.50	1.88	7.50	0.0000	0.0000	Useful target users	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5710 (class 0 OID 47117)
-- Dependencies: 246
-- Data for Name: submissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.submissions (id, round_id, team_id, repo_url, demo_url, slide_url, report_url, api_metadata, submitted_at, updated_at, project_name, version, review_status, status) FROM stdin;
53000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	https://github.com/example/alpha	https://alpha.example.test	https://example.test/alpha-slides	https://example.test/alpha-report	{"build":"passed","commit":"alpha123"}	2026-07-17 13:55:15.848083	\N	Alpha AI Copilot	1.0.0	approved	submitted
53000000-0000-4000-8000-000000000002	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	https://github.com/example/beta	https://beta.example.test	https://example.test/beta-slides	https://example.test/beta-report	{"build":"passed","commit":"beta123"}	2026-07-17 13:55:15.848083	\N	Beta Learning Assistant	1.0.0	approved	submitted
53000000-0000-4000-8000-000000000003	45000000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000001	https://github.com/example/alpha-final	https://alpha-final.example.test	https://example.test/alpha-final-slides	https://example.test/alpha-final-report	{"build":"passed","commit":"alpha-final-123"}	2026-07-20 13:55:15.848083	\N	Alpha AI Copilot Final	2.0.0	approved	submitted
\.


--
-- TOC entry 5711 (class 0 OID 47126)
-- Dependencies: 247
-- Data for Name: support_tickets; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.support_tickets (id, requester_id, category, priority, subject, description, status, created_at, updated_at) FROM stdin;
62000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000005	technical	high	Cannot upload final slide	Upload returns a 500 error in E2E scenario.	open	2026-07-21 13:55:15.848083	2026-07-21 13:55:15.848083
62000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000007	account	low	Update profile name	Request for profile name correction.	resolved	2026-07-21 13:55:15.848083	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5712 (class 0 OID 47135)
-- Dependencies: 248
-- Data for Name: team_chat_messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_chat_messages (id, team_id, sender_id, message, created_at, updated_at) FROM stdin;
51300000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000005	Welcome to the Alpha E2E team chat.	2026-07-21 13:55:15.848083	2026-07-21 13:55:15.848083
51300000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000006	Submission links are ready.	2026-07-21 13:55:15.848083	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5713 (class 0 OID 47143)
-- Dependencies: 249
-- Data for Name: team_join_requests; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_join_requests (id, team_id, user_id, status, message, created_at, responded_at) FROM stdin;
51200000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000009	pending	Please let me join Alpha.	2026-07-21 13:55:15.848083	\N
51200000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000006	rejected	Cross-team request for rejection flow.	2026-07-21 13:55:15.848083	2026-07-20 13:55:15.848083
\.


--
-- TOC entry 5714 (class 0 OID 47152)
-- Dependencies: 250
-- Data for Name: team_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_members (id, team_id, user_id, role, joined_at) FROM stdin;
51100000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000005	leader	2026-07-21 13:55:15.848083
51100000-0000-4000-8000-000000000002	51000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000006	member	2026-07-21 13:55:15.848083
51100000-0000-4000-8000-000000000003	51000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000007	leader	2026-07-21 13:55:15.848083
51100000-0000-4000-8000-000000000004	51000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000008	member	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5715 (class 0 OID 47158)
-- Dependencies: 251
-- Data for Name: team_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_profiles (id, canonical_name, logo_url, created_by, status, created_at, updated_at, row_version) FROM stdin;
50000000-0000-4000-8000-000000000001	Alpha Innovators	https://example.test/logo-alpha.png	30000000-0000-4000-8000-000000000005	active	2026-07-21 13:55:15.848083	\N	0
50000000-0000-4000-8000-000000000002	Beta Builders	https://example.test/logo-beta.png	30000000-0000-4000-8000-000000000007	active	2026-07-21 13:55:15.848083	\N	0
\.


--
-- TOC entry 5716 (class 0 OID 47168)
-- Dependencies: 252
-- Data for Name: team_recognitions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_recognitions (id, team_profile_id, recognition_code, label, qualification_count, earned_at, active, revoked_at, revoked_by, revoke_reason, created_at, updated_at, row_version) FROM stdin;
71200000-0000-4000-8000-000000000001	50000000-0000-4000-8000-000000000001	FINALIST_2026	2026 Finalist	1	2026-07-20 13:55:15.848083	t	\N	\N	\N	2026-07-21 13:55:15.848083	\N	0
\.


--
-- TOC entry 5717 (class 0 OID 47180)
-- Dependencies: 253
-- Data for Name: team_timeline_events; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_timeline_events (id, event_id, team_id, round_id, type, title, description, score_snapshot, rank_snapshot, status_snapshot, occurred_at, visibility_scope, event_type, source_type, source_id, track_id, submission_id, appeal_id, incident_id, support_ticket_id, seed_id, recognition_id, metadata, idempotency_key) FROM stdin;
80000000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	RESULT_PUBLISHED	Qualification result published	Alpha ranked first.	8.58	1	promoted	2026-07-21 13:55:15.848083	EVENT_PARTICIPANTS	ROUND_RESULT_PUBLISHED	round_result_version	56000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	53000000-0000-4000-8000-000000000001	\N	\N	\N	71100000-0000-4000-8000-000000000001	71200000-0000-4000-8000-000000000001	{"e2e": true}	e2e-result-alpha-v1
80000000-0000-4000-8000-000000000002	40000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	45000000-0000-4000-8000-000000000001	APPEAL_SUBMITTED	Appeal submitted	Beta requested a technical score review.	7.33	2	eliminated	2026-07-21 13:55:15.848083	TEAM_ONLY	APPEAL_SUBMITTED	appeal	60000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	53000000-0000-4000-8000-000000000002	60000000-0000-4000-8000-000000000001	\N	\N	\N	\N	{"e2e": true}	e2e-appeal-beta
\.


--
-- TOC entry 5718 (class 0 OID 47188)
-- Dependencies: 254
-- Data for Name: teams; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.teams (id, track_id, name, status, disqualified_reason, created_at, updated_at, invite_code, team_profile_id, source_team_id, activated_from_profile_at, roster_confirmed_at, roster_confirmed_by) FROM stdin;
51000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	Alpha Innovators	active	\N	2026-07-21 13:55:15.848083	\N	ALPHAE2E	50000000-0000-4000-8000-000000000001	\N	\N	2026-07-09 13:55:15.848083	30000000-0000-4000-8000-000000000005
51000000-0000-4000-8000-000000000002	43000000-0000-4000-8000-000000000001	Beta Builders	active	\N	2026-07-21 13:55:15.848083	\N	BETAE2E1	50000000-0000-4000-8000-000000000002	\N	\N	2026-07-09 13:55:15.848083	30000000-0000-4000-8000-000000000007
\.


--
-- TOC entry 5719 (class 0 OID 47196)
-- Dependencies: 255
-- Data for Name: tie_break_decisions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tie_break_decisions (id, round_id, team_id, decided_by, decided_at, reason, evidence_url, note) FROM stdin;
56300000-0000-4000-8000-000000000001	45000000-0000-4000-8000-000000000001	51000000-0000-4000-8000-000000000002	30000000-0000-4000-8000-000000000001	2026-07-21 13:55:15.848083	Manual test decision for deterministic ordering	https://example.test/tie-break-evidence	E2E-only tie-break record
\.


--
-- TOC entry 5720 (class 0 OID 47203)
-- Dependencies: 256
-- Data for Name: track_judges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.track_judges (id, event_id, track_id, user_id, assigned_at) FROM stdin;
43100000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000003	2026-07-21 13:55:15.848083
43100000-0000-4000-8000-000000000002	40000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000004	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5721 (class 0 OID 47208)
-- Dependencies: 257
-- Data for Name: track_mentors; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.track_mentors (id, event_id, track_id, user_id, assigned_at) FROM stdin;
43200000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	43000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000002	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5722 (class 0 OID 47213)
-- Dependencies: 258
-- Data for Name: tracks; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tracks (id, event_id, name, description, created_at, updated_at, max_teams) FROM stdin;
43000000-0000-4000-8000-000000000001	40000000-0000-4000-8000-000000000001	AI Innovation	AI products and intelligent systems	2026-07-21 13:55:15.848083	\N	50
43000000-0000-4000-8000-000000000002	40000000-0000-4000-8000-000000000001	Green Tech	Sustainability solutions	2026-07-21 13:55:15.848083	\N	50
\.


--
-- TOC entry 5723 (class 0 OID 47220)
-- Dependencies: 259
-- Data for Name: universities; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.universities (id, name, short_name, country, created_at, updated_at) FROM stdin;
20000000-0000-4000-8000-000000000001	FPT University	FPTU	Vietnam	2026-07-21 13:55:15.848083	\N
20000000-0000-4000-8000-000000000002	E2E External University	E2EU	Vietnam	2026-07-21 13:55:15.848083	\N
\.


--
-- TOC entry 5724 (class 0 OID 47225)
-- Dependencies: 260
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_roles (user_id, role_id, assigned_at) FROM stdin;
30000000-0000-4000-8000-000000000001	10000000-0000-4000-8000-000000000001	2026-07-21 13:55:15.848083
30000000-0000-4000-8000-000000000002	10000000-0000-4000-8000-000000000002	2026-07-21 13:55:15.848083
30000000-0000-4000-8000-000000000003	10000000-0000-4000-8000-000000000003	2026-07-21 13:55:15.848083
30000000-0000-4000-8000-000000000004	10000000-0000-4000-8000-000000000003	2026-07-21 13:55:15.848083
30000000-0000-4000-8000-000000000005	10000000-0000-4000-8000-000000000004	2026-07-21 13:55:15.848083
30000000-0000-4000-8000-000000000006	10000000-0000-4000-8000-000000000005	2026-07-21 13:55:15.848083
30000000-0000-4000-8000-000000000007	10000000-0000-4000-8000-000000000004	2026-07-21 13:55:15.848083
30000000-0000-4000-8000-000000000008	10000000-0000-4000-8000-000000000005	2026-07-21 13:55:15.848083
\.


--
-- TOC entry 5725 (class 0 OID 47229)
-- Dependencies: 261
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, password_hash, full_name, student_type, student_id, campus_id, is_guest, status, created_at, updated_at, university_id, phone, department, "position", company, expertise, bio, must_change_password, terms_accepted_at, terms_version, privacy_version, security_version) FROM stdin;
30000000-0000-4000-8000-000000000002	e2e.mentor@seal.local	$2a$10$uHk8l7irdqmEcf43468sv..cLbP328b0wrPq4cDMLSHWoIYIDjpAS	E2E Mentor	none	\N	\N	f	approved	2026-07-21 13:55:15.848083	\N	\N	0900000002	\N	AI Mentor	E2E Labs	AI, Product	Mentor test account	f	2026-07-21 13:55:15.848083	v1	v1	1
30000000-0000-4000-8000-000000000003	e2e.judge1@seal.local	$2a$10$Q/QULZO0Lx1oUnJLNevpken.hirw25YMPmmKpa.82oaI4iXuDxZq.	E2E Judge One	none	\N	\N	f	approved	2026-07-21 13:55:15.848083	\N	\N	0900000003	\N	Senior Engineer	E2E Labs	Backend, Architecture	Judge 1 test account	f	2026-07-21 13:55:15.848083	v1	v1	1
30000000-0000-4000-8000-000000000004	e2e.judge2@seal.local	$2a$10$E0qKr/wc0VQ4aU1EdF/2Be1te21yjmGgSYmtYeFRegTaBdTfgzQES	E2E Judge Two	none	\N	\N	f	approved	2026-07-21 13:55:15.848083	\N	\N	0900000004	\N	Product Manager	E2E Labs	Product, UX	Judge 2 test account	f	2026-07-21 13:55:15.848083	v1	v1	1
30000000-0000-4000-8000-000000000006	e2e.member.alpha@seal.local	$2a$10$Szg10Pa2mlyHJjemXTHnX.jyGcysLxodrPWo.9aUrMMEwlUK79ASa	Alpha Member	fpt	SE100002	21000000-0000-4000-8000-000000000001	f	approved	2026-07-21 13:55:15.848083	\N	20000000-0000-4000-8000-000000000001	0900000006	\N	\N	\N	\N	\N	f	2026-07-21 13:55:15.848083	v1	v1	1
30000000-0000-4000-8000-000000000007	e2e.leader.beta@seal.local	$2a$10$x./3KjrvhNGjdnpWws53J.Tj1UMnsI20JmEVKWXZ2.vgsChWk9aFq	Beta Leader	external	EXT1001	\N	f	approved	2026-07-21 13:55:15.848083	\N	20000000-0000-4000-8000-000000000002	0900000007	\N	\N	\N	\N	\N	f	2026-07-21 13:55:15.848083	v1	v1	1
30000000-0000-4000-8000-000000000008	e2e.member.beta@seal.local	$2a$10$4S1BHCnMPEm5BZPD5D3jmOuMFuBVCJzW9NLxOCJDJXaBNs1AGgx0G	Beta Member	external	EXT1002	\N	f	approved	2026-07-21 13:55:15.848083	\N	20000000-0000-4000-8000-000000000002	0900000008	\N	\N	\N	\N	\N	f	2026-07-21 13:55:15.848083	v1	v1	1
30000000-0000-4000-8000-000000000009	e2e.pending@seal.local	$2a$10$5rZpjfPA4rM1t1fx1dqng.axtzFpaQf.hFCQ8ZzOfpqxOffMUX2/6	Pending Student	fpt	SE100009	21000000-0000-4000-8000-000000000002	f	pending	2026-07-21 13:55:15.848083	\N	20000000-0000-4000-8000-000000000001	0900000009	\N	\N	\N	\N	\N	t	\N	\N	\N	1
30000000-0000-4000-8000-000000000010	e2e.rejected@seal.local	$2a$10$Nxnq3Bh4pbF8qTq5rmVux.FDr37P6bR9JSQTEy3/HYygK18pGoPI2	Rejected Student	fpt	SE100010	21000000-0000-4000-8000-000000000002	f	rejected	2026-07-21 13:55:15.848083	\N	20000000-0000-4000-8000-000000000001	0900000010	\N	\N	\N	\N	\N	f	\N	\N	\N	1
30000000-0000-4000-8000-000000000001	e2e.coordinator@seal.local	$2a$10$S7qxEgI4Nuig8B1hG80ndOgy04PkbLV7QxeG34LB1kTEM/Xg5bUK6	E2E Coordinator	none	\N	\N	f	approved	2026-07-21 13:55:15.848083	2026-07-21 14:02:33.216994	\N	0900000001	SE	Event Coordinator	FPT University	Event operations	Coordinator test account	f	2026-07-21 00:02:33.218481	2026-01	2026-01	2
30000000-0000-4000-8000-000000000005	e2e.leader.alpha@seal.local	$2a$10$4YBqMm3WqaPLsJ9poiId.OCpZMFGs9aVn5hWyzOmpnVzUUaeoPb46	Alpha Leader	fpt	SE100001	21000000-0000-4000-8000-000000000001	f	approved	2026-07-21 13:55:15.848083	2026-07-21 14:05:33.515619	20000000-0000-4000-8000-000000000001	0900000005	\N	\N	\N	\N	\N	f	2026-07-21 00:05:33.516076	2026-01	2026-01	2
\.


--
-- TOC entry 5155 (class 2606 OID 47250)
-- Name: account_activation_tokens account_activation_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation_tokens
    ADD CONSTRAINT account_activation_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 5157 (class 2606 OID 47252)
-- Name: account_activation_tokens account_activation_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation_tokens
    ADD CONSTRAINT account_activation_tokens_token_hash_key UNIQUE (token_hash);


--
-- TOC entry 5161 (class 2606 OID 47254)
-- Name: appeals appeals_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_pkey PRIMARY KEY (id);


--
-- TOC entry 5167 (class 2606 OID 47256)
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 5173 (class 2606 OID 47258)
-- Name: campuses campuses_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT campuses_pkey PRIMARY KEY (id);


--
-- TOC entry 5177 (class 2606 OID 47260)
-- Name: criteria_templates criteria_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.criteria_templates
    ADD CONSTRAINT criteria_templates_pkey PRIMARY KEY (id);


--
-- TOC entry 5179 (class 2606 OID 47262)
-- Name: event_rules event_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_rules
    ADD CONSTRAINT event_rules_pkey PRIMARY KEY (id);


--
-- TOC entry 5182 (class 2606 OID 47264)
-- Name: event_seed_assignments event_seed_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_pkey PRIMARY KEY (id);


--
-- TOC entry 5190 (class 2606 OID 47266)
-- Name: event_team_finishes event_team_finishes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_pkey PRIMARY KEY (id);


--
-- TOC entry 5200 (class 2606 OID 47268)
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- TOC entry 5202 (class 2606 OID 47270)
-- Name: incident_actions incident_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_pkey PRIMARY KEY (id);


--
-- TOC entry 5204 (class 2606 OID 47272)
-- Name: incident_evidences incident_evidences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_pkey PRIMARY KEY (id);


--
-- TOC entry 5210 (class 2606 OID 47274)
-- Name: incident_reports incident_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_pkey PRIMARY KEY (id);


--
-- TOC entry 5396 (class 2606 OID 57571)
-- Name: logical_round_promotions logical_round_promotions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT logical_round_promotions_pkey PRIMARY KEY (id);


--
-- TOC entry 5212 (class 2606 OID 47276)
-- Name: mentor_feedbacks mentor_feedbacks_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_pkey PRIMARY KEY (id);


--
-- TOC entry 5218 (class 2606 OID 47278)
-- Name: notices notices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_pkey PRIMARY KEY (id);


--
-- TOC entry 5222 (class 2606 OID 47280)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 5225 (class 2606 OID 47282)
-- Name: prize_revisions prize_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_pkey PRIMARY KEY (id);


--
-- TOC entry 5227 (class 2606 OID 47284)
-- Name: prizes prizes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_pkey PRIMARY KEY (id);


--
-- TOC entry 5231 (class 2606 OID 47286)
-- Name: revoked_tokens revoked_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 5233 (class 2606 OID 47288)
-- Name: revoked_tokens revoked_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_token_hash_key UNIQUE (token_hash);


--
-- TOC entry 5235 (class 2606 OID 47290)
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- TOC entry 5237 (class 2606 OID 47292)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- TOC entry 5240 (class 2606 OID 47294)
-- Name: round_criteria round_criteria_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_pkey PRIMARY KEY (id);


--
-- TOC entry 5386 (class 2606 OID 57542)
-- Name: round_definitions round_definitions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_definitions
    ADD CONSTRAINT round_definitions_pkey PRIMARY KEY (id);


--
-- TOC entry 5246 (class 2606 OID 47296)
-- Name: round_judges round_judges_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_pkey PRIMARY KEY (id);


--
-- TOC entry 5253 (class 2606 OID 47298)
-- Name: round_participants round_participants_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_pkey PRIMARY KEY (id);


--
-- TOC entry 5260 (class 2606 OID 47300)
-- Name: round_rankings round_rankings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_pkey PRIMARY KEY (id);


--
-- TOC entry 5266 (class 2606 OID 47302)
-- Name: round_result_version_entries round_result_version_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT round_result_version_entries_pkey PRIMARY KEY (id);


--
-- TOC entry 5274 (class 2606 OID 47304)
-- Name: round_result_versions round_result_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT round_result_versions_pkey PRIMARY KEY (id);


--
-- TOC entry 5280 (class 2606 OID 47306)
-- Name: rounds rounds_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT rounds_pkey PRIMARY KEY (id);


--
-- TOC entry 5288 (class 2606 OID 47308)
-- Name: rule_acceptances rule_acceptances_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_pkey PRIMARY KEY (id);


--
-- TOC entry 5295 (class 2606 OID 47310)
-- Name: scores scores_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_pkey PRIMARY KEY (id);


--
-- TOC entry 5301 (class 2606 OID 47312)
-- Name: submissions submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_pkey PRIMARY KEY (id);


--
-- TOC entry 5306 (class 2606 OID 47314)
-- Name: support_tickets support_tickets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT support_tickets_pkey PRIMARY KEY (id);


--
-- TOC entry 5309 (class 2606 OID 47316)
-- Name: team_chat_messages team_chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_pkey PRIMARY KEY (id);


--
-- TOC entry 5313 (class 2606 OID 47318)
-- Name: team_join_requests team_join_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_pkey PRIMARY KEY (id);


--
-- TOC entry 5318 (class 2606 OID 47320)
-- Name: team_members team_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_pkey PRIMARY KEY (id);


--
-- TOC entry 5322 (class 2606 OID 47322)
-- Name: team_profiles team_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_profiles
    ADD CONSTRAINT team_profiles_pkey PRIMARY KEY (id);


--
-- TOC entry 5326 (class 2606 OID 47324)
-- Name: team_recognitions team_recognitions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_recognitions
    ADD CONSTRAINT team_recognitions_pkey PRIMARY KEY (id);


--
-- TOC entry 5338 (class 2606 OID 47326)
-- Name: team_timeline_events team_timeline_events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_pkey PRIMARY KEY (id);


--
-- TOC entry 5345 (class 2606 OID 47328)
-- Name: teams teams_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);


--
-- TOC entry 5350 (class 2606 OID 47330)
-- Name: tie_break_decisions tie_break_decisions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_pkey PRIMARY KEY (id);


--
-- TOC entry 5356 (class 2606 OID 47332)
-- Name: track_judges track_judges_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_pkey PRIMARY KEY (id);


--
-- TOC entry 5362 (class 2606 OID 47334)
-- Name: track_mentors track_mentors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_pkey PRIMARY KEY (id);


--
-- TOC entry 5367 (class 2606 OID 47336)
-- Name: tracks tracks_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT tracks_pkey PRIMARY KEY (id);


--
-- TOC entry 5371 (class 2606 OID 47338)
-- Name: universities universities_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.universities
    ADD CONSTRAINT universities_name_key UNIQUE (name);


--
-- TOC entry 5373 (class 2606 OID 47340)
-- Name: universities universities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.universities
    ADD CONSTRAINT universities_pkey PRIMARY KEY (id);


--
-- TOC entry 5175 (class 2606 OID 47342)
-- Name: campuses uq_campuses_university_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT uq_campuses_university_name UNIQUE (university_id, name);


--
-- TOC entry 5188 (class 2606 OID 47344)
-- Name: event_seed_assignments uq_event_seed_team_stage; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT uq_event_seed_team_stage UNIQUE (event_id, team_id, competition_stage);


--
-- TOC entry 5196 (class 2606 OID 47346)
-- Name: event_team_finishes uq_event_team_finish_event_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT uq_event_team_finish_event_team UNIQUE (event_id, team_id);


--
-- TOC entry 5198 (class 2606 OID 47348)
-- Name: event_team_finishes uq_event_team_finish_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT uq_event_team_finish_team UNIQUE (team_id);


--
-- TOC entry 5398 (class 2606 OID 57573)
-- Name: logical_round_promotions uq_logical_round_promotions_target_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT uq_logical_round_promotions_target_team UNIQUE (target_logical_round_id, team_id);


--
-- TOC entry 5268 (class 2606 OID 57594)
-- Name: round_result_version_entries uq_result_entry_id_version; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT uq_result_entry_id_version UNIQUE (id, result_version_id);


--
-- TOC entry 5277 (class 2606 OID 47350)
-- Name: round_result_versions uq_result_version_number; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT uq_result_version_number UNIQUE (round_id, version_number);


--
-- TOC entry 5270 (class 2606 OID 47352)
-- Name: round_result_version_entries uq_result_version_rank; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT uq_result_version_rank UNIQUE (result_version_id, rank);


--
-- TOC entry 5272 (class 2606 OID 47354)
-- Name: round_result_version_entries uq_result_version_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT uq_result_version_team UNIQUE (result_version_id, team_id);


--
-- TOC entry 5242 (class 2606 OID 47356)
-- Name: round_criteria uq_round_criteria_round_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT uq_round_criteria_round_name UNIQUE (round_id, name);


--
-- TOC entry 5390 (class 2606 OID 57562)
-- Name: round_definitions uq_round_definitions_event_sequence; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_definitions
    ADD CONSTRAINT uq_round_definitions_event_sequence UNIQUE (event_id, sequence_number);


--
-- TOC entry 5248 (class 2606 OID 47358)
-- Name: round_judges uq_round_judges_round_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT uq_round_judges_round_user UNIQUE (round_id, user_id);


--
-- TOC entry 5255 (class 2606 OID 47360)
-- Name: round_participants uq_round_participants_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT uq_round_participants_round_team UNIQUE (round_id, team_id);


--
-- TOC entry 5262 (class 2606 OID 47362)
-- Name: round_rankings uq_round_rankings_round_rank; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT uq_round_rankings_round_rank UNIQUE (round_id, rank);


--
-- TOC entry 5264 (class 2606 OID 47364)
-- Name: round_rankings uq_round_rankings_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT uq_round_rankings_round_team UNIQUE (round_id, team_id);


--
-- TOC entry 5282 (class 2606 OID 57556)
-- Name: rounds uq_rounds_logical_track; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT uq_rounds_logical_track UNIQUE (logical_round_id, track_id);


--
-- TOC entry 5284 (class 2606 OID 47366)
-- Name: rounds uq_rounds_track_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT uq_rounds_track_name UNIQUE (track_id, name);


--
-- TOC entry 5286 (class 2606 OID 47368)
-- Name: rounds uq_rounds_track_sequence; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT uq_rounds_track_sequence UNIQUE (track_id, sequence_number);


--
-- TOC entry 5290 (class 2606 OID 47370)
-- Name: rule_acceptances uq_rule_acceptances_user_event; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT uq_rule_acceptances_user_event UNIQUE (user_id, event_id);


--
-- TOC entry 5297 (class 2606 OID 47372)
-- Name: scores uq_scores_submission_judge_criterion; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT uq_scores_submission_judge_criterion UNIQUE (submission_id, judge_id, criterion_id);


--
-- TOC entry 5303 (class 2606 OID 47374)
-- Name: submissions uq_submissions_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT uq_submissions_round_team UNIQUE (round_id, team_id);


--
-- TOC entry 5320 (class 2606 OID 47376)
-- Name: team_members uq_team_members_team_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id);


--
-- TOC entry 5348 (class 2606 OID 47378)
-- Name: teams uq_teams_track_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT uq_teams_track_name UNIQUE (track_id, name);


--
-- TOC entry 5352 (class 2606 OID 47380)
-- Name: tie_break_decisions uq_tie_break_decisions_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT uq_tie_break_decisions_round_team UNIQUE (round_id, team_id);


--
-- TOC entry 5358 (class 2606 OID 47382)
-- Name: track_judges uq_track_judges_track_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT uq_track_judges_track_user UNIQUE (track_id, user_id);


--
-- TOC entry 5364 (class 2606 OID 47384)
-- Name: track_mentors uq_track_mentors_track_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT uq_track_mentors_track_user UNIQUE (track_id, user_id);


--
-- TOC entry 5369 (class 2606 OID 47386)
-- Name: tracks uq_tracks_event_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT uq_tracks_event_name UNIQUE (event_id, name);


--
-- TOC entry 5375 (class 2606 OID 47388)
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- TOC entry 5381 (class 2606 OID 47390)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 5383 (class 2606 OID 47392)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 5158 (class 1259 OID 47393)
-- Name: idx_activation_expires; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_activation_expires ON public.account_activation_tokens USING btree (expires_at);


--
-- TOC entry 5159 (class 1259 OID 47394)
-- Name: idx_activation_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_activation_user ON public.account_activation_tokens USING btree (user_id);


--
-- TOC entry 5162 (class 1259 OID 47395)
-- Name: idx_appeals_round; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_round ON public.appeals USING btree (round_id);


--
-- TOC entry 5163 (class 1259 OID 47396)
-- Name: idx_appeals_round_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_round_status ON public.appeals USING btree (round_id, status);


--
-- TOC entry 5164 (class 1259 OID 47397)
-- Name: idx_appeals_team; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_team ON public.appeals USING btree (team_id);


--
-- TOC entry 5168 (class 1259 OID 47398)
-- Name: idx_audit_logs_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_action ON public.audit_logs USING btree (action);


--
-- TOC entry 5169 (class 1259 OID 47399)
-- Name: idx_audit_logs_incident_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_incident_id ON public.audit_logs USING btree (incident_id);


--
-- TOC entry 5170 (class 1259 OID 47400)
-- Name: idx_audit_logs_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_team_id ON public.audit_logs USING btree (team_id);


--
-- TOC entry 5171 (class 1259 OID 47401)
-- Name: idx_audit_logs_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_user_id ON public.audit_logs USING btree (user_id);


--
-- TOC entry 5180 (class 1259 OID 47402)
-- Name: idx_event_rules_event; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_rules_event ON public.event_rules USING btree (event_id, visibility);


--
-- TOC entry 5183 (class 1259 OID 47403)
-- Name: idx_event_seed_assignments_event_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_seed_assignments_event_status ON public.event_seed_assignments USING btree (event_id, status);


--
-- TOC entry 5184 (class 1259 OID 47404)
-- Name: idx_event_seed_assignments_profile; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_seed_assignments_profile ON public.event_seed_assignments USING btree (team_profile_id);


--
-- TOC entry 5185 (class 1259 OID 47405)
-- Name: idx_event_seed_assignments_track; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_seed_assignments_track ON public.event_seed_assignments USING btree (track_id);


--
-- TOC entry 5191 (class 1259 OID 47406)
-- Name: idx_event_team_finishes_event; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_team_finishes_event ON public.event_team_finishes USING btree (event_id);


--
-- TOC entry 5192 (class 1259 OID 47407)
-- Name: idx_event_team_finishes_profile; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_team_finishes_profile ON public.event_team_finishes USING btree (team_profile_id);


--
-- TOC entry 5193 (class 1259 OID 47408)
-- Name: idx_event_team_finishes_track_rank; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_team_finishes_track_rank ON public.event_team_finishes USING btree (track_id, final_rank);


--
-- TOC entry 5194 (class 1259 OID 47409)
-- Name: idx_event_team_finishes_version; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_team_finishes_version ON public.event_team_finishes USING btree (result_version_id);


--
-- TOC entry 5205 (class 1259 OID 47410)
-- Name: idx_incident_reports_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_event_id ON public.incident_reports USING btree (event_id);


--
-- TOC entry 5206 (class 1259 OID 47411)
-- Name: idx_incident_reports_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_status ON public.incident_reports USING btree (status);


--
-- TOC entry 5207 (class 1259 OID 47412)
-- Name: idx_incident_reports_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_team_id ON public.incident_reports USING btree (team_id);


--
-- TOC entry 5208 (class 1259 OID 47413)
-- Name: idx_incident_reports_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_type ON public.incident_reports USING btree (type);


--
-- TOC entry 5391 (class 1259 OID 57589)
-- Name: idx_logical_round_promotions_source; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logical_round_promotions_source ON public.logical_round_promotions USING btree (source_logical_round_id);


--
-- TOC entry 5392 (class 1259 OID 57615)
-- Name: idx_logical_round_promotions_source_entry; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logical_round_promotions_source_entry ON public.logical_round_promotions USING btree (source_result_entry_id);


--
-- TOC entry 5393 (class 1259 OID 57614)
-- Name: idx_logical_round_promotions_source_version; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logical_round_promotions_source_version ON public.logical_round_promotions USING btree (source_result_version_id);


--
-- TOC entry 5394 (class 1259 OID 57590)
-- Name: idx_logical_round_promotions_target; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logical_round_promotions_target ON public.logical_round_promotions USING btree (target_logical_round_id);


--
-- TOC entry 5213 (class 1259 OID 47414)
-- Name: idx_notices_author_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_author_id ON public.notices USING btree (author_id);


--
-- TOC entry 5214 (class 1259 OID 47415)
-- Name: idx_notices_target_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_event_id ON public.notices USING btree (target_event_id);


--
-- TOC entry 5215 (class 1259 OID 47416)
-- Name: idx_notices_target_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_role ON public.notices USING btree (target_role);


--
-- TOC entry 5216 (class 1259 OID 47417)
-- Name: idx_notices_target_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_track_id ON public.notices USING btree (target_track_id);


--
-- TOC entry 5219 (class 1259 OID 47418)
-- Name: idx_notifications_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user ON public.notifications USING btree (user_id);


--
-- TOC entry 5220 (class 1259 OID 47419)
-- Name: idx_notifications_user_unread; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user_unread ON public.notifications USING btree (user_id, category) WHERE (read_at IS NULL);


--
-- TOC entry 5223 (class 1259 OID 47420)
-- Name: idx_prize_revisions_prize; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prize_revisions_prize ON public.prize_revisions USING btree (prize_id, changed_at DESC);


--
-- TOC entry 5228 (class 1259 OID 47421)
-- Name: idx_revoked_tokens_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_revoked_tokens_expires_at ON public.revoked_tokens USING btree (expires_at);


--
-- TOC entry 5229 (class 1259 OID 47422)
-- Name: idx_revoked_tokens_token_hash; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_revoked_tokens_token_hash ON public.revoked_tokens USING btree (token_hash);


--
-- TOC entry 5238 (class 1259 OID 47423)
-- Name: idx_round_criteria_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_criteria_round_id ON public.round_criteria USING btree (round_id);


--
-- TOC entry 5384 (class 1259 OID 57549)
-- Name: idx_round_definitions_event_sequence; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_definitions_event_sequence ON public.round_definitions USING btree (event_id, sequence_number);


--
-- TOC entry 5243 (class 1259 OID 47424)
-- Name: idx_round_judges_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_judges_round_id ON public.round_judges USING btree (round_id);


--
-- TOC entry 5244 (class 1259 OID 47425)
-- Name: idx_round_judges_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_judges_user_id ON public.round_judges USING btree (user_id);


--
-- TOC entry 5249 (class 1259 OID 47426)
-- Name: idx_round_participants_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_round_id ON public.round_participants USING btree (round_id);


--
-- TOC entry 5250 (class 1259 OID 47427)
-- Name: idx_round_participants_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_status ON public.round_participants USING btree (status);


--
-- TOC entry 5251 (class 1259 OID 47428)
-- Name: idx_round_participants_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_team_id ON public.round_participants USING btree (team_id);


--
-- TOC entry 5256 (class 1259 OID 47429)
-- Name: idx_round_rankings_rank; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_rank ON public.round_rankings USING btree (rank);


--
-- TOC entry 5257 (class 1259 OID 47430)
-- Name: idx_round_rankings_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_round_id ON public.round_rankings USING btree (round_id);


--
-- TOC entry 5258 (class 1259 OID 47431)
-- Name: idx_round_rankings_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_team_id ON public.round_rankings USING btree (team_id);


--
-- TOC entry 5278 (class 1259 OID 47432)
-- Name: idx_rounds_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_rounds_track_id ON public.rounds USING btree (track_id);


--
-- TOC entry 5291 (class 1259 OID 47433)
-- Name: idx_scores_criterion_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_criterion_id ON public.scores USING btree (criterion_id);


--
-- TOC entry 5292 (class 1259 OID 47434)
-- Name: idx_scores_judge_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_judge_id ON public.scores USING btree (judge_id);


--
-- TOC entry 5293 (class 1259 OID 47435)
-- Name: idx_scores_submission_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_submission_id ON public.scores USING btree (submission_id);


--
-- TOC entry 5298 (class 1259 OID 47436)
-- Name: idx_submissions_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_submissions_round_id ON public.submissions USING btree (round_id);


--
-- TOC entry 5299 (class 1259 OID 47437)
-- Name: idx_submissions_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_submissions_team_id ON public.submissions USING btree (team_id);


--
-- TOC entry 5304 (class 1259 OID 47438)
-- Name: idx_support_tickets_requester_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_support_tickets_requester_id ON public.support_tickets USING btree (requester_id);


--
-- TOC entry 5307 (class 1259 OID 47439)
-- Name: idx_team_chat_messages_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_chat_messages_team_id ON public.team_chat_messages USING btree (team_id);


--
-- TOC entry 5315 (class 1259 OID 47440)
-- Name: idx_team_members_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_members_team_id ON public.team_members USING btree (team_id);


--
-- TOC entry 5316 (class 1259 OID 47441)
-- Name: idx_team_members_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_members_user_id ON public.team_members USING btree (user_id);


--
-- TOC entry 5323 (class 1259 OID 47442)
-- Name: idx_team_recognitions_code_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_recognitions_code_active ON public.team_recognitions USING btree (recognition_code, active);


--
-- TOC entry 5324 (class 1259 OID 47443)
-- Name: idx_team_recognitions_profile; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_recognitions_profile ON public.team_recognitions USING btree (team_profile_id);


--
-- TOC entry 5328 (class 1259 OID 47444)
-- Name: idx_team_timeline_events_event; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event ON public.team_timeline_events USING btree (event_id, occurred_at DESC);


--
-- TOC entry 5329 (class 1259 OID 47445)
-- Name: idx_team_timeline_events_event_round_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event_round_time ON public.team_timeline_events USING btree (event_id, round_id, occurred_at DESC, id DESC);


--
-- TOC entry 5330 (class 1259 OID 47446)
-- Name: idx_team_timeline_events_event_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event_time ON public.team_timeline_events USING btree (event_id, occurred_at DESC, id DESC);


--
-- TOC entry 5331 (class 1259 OID 47447)
-- Name: idx_team_timeline_events_event_track_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event_track_time ON public.team_timeline_events USING btree (event_id, track_id, occurred_at DESC, id DESC);


--
-- TOC entry 5332 (class 1259 OID 47448)
-- Name: idx_team_timeline_events_round_track; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_round_track ON public.team_timeline_events USING btree (round_id, track_id, occurred_at DESC);


--
-- TOC entry 5333 (class 1259 OID 47449)
-- Name: idx_team_timeline_events_scope; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_scope ON public.team_timeline_events USING btree (event_id, visibility_scope, occurred_at DESC);


--
-- TOC entry 5334 (class 1259 OID 47450)
-- Name: idx_team_timeline_events_team; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_team ON public.team_timeline_events USING btree (team_id, occurred_at DESC);


--
-- TOC entry 5335 (class 1259 OID 47451)
-- Name: idx_team_timeline_events_team_time_stable; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_team_time_stable ON public.team_timeline_events USING btree (team_id, occurred_at DESC, id DESC);


--
-- TOC entry 5336 (class 1259 OID 47452)
-- Name: idx_team_timeline_events_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_type ON public.team_timeline_events USING btree (event_id, event_type, occurred_at DESC, id DESC);


--
-- TOC entry 5340 (class 1259 OID 47453)
-- Name: idx_teams_source_team; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_source_team ON public.teams USING btree (source_team_id);


--
-- TOC entry 5341 (class 1259 OID 47454)
-- Name: idx_teams_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_status ON public.teams USING btree (status);


--
-- TOC entry 5342 (class 1259 OID 47455)
-- Name: idx_teams_team_profile; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_team_profile ON public.teams USING btree (team_profile_id);


--
-- TOC entry 5343 (class 1259 OID 47456)
-- Name: idx_teams_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_track_id ON public.teams USING btree (track_id);


--
-- TOC entry 5310 (class 1259 OID 47457)
-- Name: idx_tjr_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tjr_team_id ON public.team_join_requests USING btree (team_id);


--
-- TOC entry 5311 (class 1259 OID 47458)
-- Name: idx_tjr_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tjr_user_id ON public.team_join_requests USING btree (user_id);


--
-- TOC entry 5353 (class 1259 OID 47459)
-- Name: idx_track_judges_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_judges_track_id ON public.track_judges USING btree (track_id);


--
-- TOC entry 5354 (class 1259 OID 47460)
-- Name: idx_track_judges_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_judges_user_id ON public.track_judges USING btree (user_id);


--
-- TOC entry 5359 (class 1259 OID 47461)
-- Name: idx_track_mentors_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_mentors_track_id ON public.track_mentors USING btree (track_id);


--
-- TOC entry 5360 (class 1259 OID 47462)
-- Name: idx_track_mentors_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_mentors_user_id ON public.track_mentors USING btree (user_id);


--
-- TOC entry 5365 (class 1259 OID 47463)
-- Name: idx_tracks_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tracks_event_id ON public.tracks USING btree (event_id);


--
-- TOC entry 5376 (class 1259 OID 47464)
-- Name: idx_users_campus_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_campus_id ON public.users USING btree (campus_id);


--
-- TOC entry 5377 (class 1259 OID 47465)
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- TOC entry 5378 (class 1259 OID 47466)
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- TOC entry 5379 (class 1259 OID 47467)
-- Name: idx_users_university_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_university_id ON public.users USING btree (university_id);


--
-- TOC entry 5165 (class 1259 OID 47468)
-- Name: uq_appeal_team_version; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_appeal_team_version ON public.appeals USING btree (team_id, result_version_id) WHERE (result_version_id IS NOT NULL);


--
-- TOC entry 5186 (class 1259 OID 47469)
-- Name: uq_event_seed_number_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_event_seed_number_active ON public.event_seed_assignments USING btree (event_id, track_id, competition_stage, seed_number) WHERE ((seed_number IS NOT NULL) AND ((status)::text = ANY (ARRAY[('confirmed'::character varying)::text, ('overridden'::character varying)::text])));


--
-- TOC entry 5275 (class 1259 OID 47470)
-- Name: uq_result_version_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_result_version_active ON public.round_result_versions USING btree (round_id) WHERE ((status)::text = 'published'::text);


--
-- TOC entry 5387 (class 1259 OID 57563)
-- Name: uq_round_definitions_event_lower_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_round_definitions_event_lower_name ON public.round_definitions USING btree (event_id, lower((name)::text));


--
-- TOC entry 5388 (class 1259 OID 57548)
-- Name: uq_round_definitions_event_name_sequence; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_round_definitions_event_name_sequence ON public.round_definitions USING btree (event_id, lower((name)::text), sequence_number);


--
-- TOC entry 5327 (class 1259 OID 47471)
-- Name: uq_team_recognition_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_team_recognition_active ON public.team_recognitions USING btree (team_profile_id, recognition_code) WHERE (active = true);


--
-- TOC entry 5339 (class 1259 OID 47472)
-- Name: uq_team_timeline_events_idempotency; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_team_timeline_events_idempotency ON public.team_timeline_events USING btree (idempotency_key) WHERE (idempotency_key IS NOT NULL);


--
-- TOC entry 5346 (class 1259 OID 47473)
-- Name: uq_teams_invite_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_teams_invite_code ON public.teams USING btree (invite_code);


--
-- TOC entry 5314 (class 1259 OID 47474)
-- Name: uq_tjr_team_user_pending; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_tjr_team_user_pending ON public.team_join_requests USING btree (team_id, user_id) WHERE ((status)::text = 'pending'::text);


--
-- TOC entry 5512 (class 2620 OID 47475)
-- Name: campuses trg_campuses_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_campuses_updated_at BEFORE UPDATE ON public.campuses FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5513 (class 2620 OID 47476)
-- Name: criteria_templates trg_criteria_templates_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_criteria_templates_updated_at BEFORE UPDATE ON public.criteria_templates FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5517 (class 2620 OID 47477)
-- Name: events trg_events_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_events_updated_at BEFORE UPDATE ON public.events FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5518 (class 2620 OID 47478)
-- Name: incident_reports trg_incident_reports_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_incident_reports_updated_at BEFORE UPDATE ON public.incident_reports FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5519 (class 2620 OID 47479)
-- Name: mentor_feedbacks trg_mentor_feedbacks_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_mentor_feedbacks_updated_at BEFORE UPDATE ON public.mentor_feedbacks FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5520 (class 2620 OID 47480)
-- Name: prizes trg_prizes_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_prizes_updated_at BEFORE UPDATE ON public.prizes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5515 (class 2620 OID 47481)
-- Name: event_team_finishes trg_reject_event_team_finish_mutation; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_reject_event_team_finish_mutation BEFORE DELETE OR UPDATE ON public.event_team_finishes FOR EACH ROW EXECUTE FUNCTION public.reject_event_team_finish_mutation();


--
-- TOC entry 5532 (class 2620 OID 57613)
-- Name: logical_round_promotions trg_reject_promotion_provenance_mutation; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_reject_promotion_provenance_mutation BEFORE UPDATE ON public.logical_round_promotions FOR EACH ROW EXECUTE FUNCTION public.reject_promotion_provenance_mutation();


--
-- TOC entry 5521 (class 2620 OID 47482)
-- Name: round_criteria trg_round_criteria_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_criteria_updated_at BEFORE UPDATE ON public.round_criteria FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5522 (class 2620 OID 47483)
-- Name: round_participants trg_round_participants_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_participants_updated_at BEFORE UPDATE ON public.round_participants FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5523 (class 2620 OID 47484)
-- Name: round_rankings trg_round_rankings_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_rankings_updated_at BEFORE UPDATE ON public.round_rankings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5524 (class 2620 OID 47485)
-- Name: rounds trg_rounds_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_rounds_updated_at BEFORE UPDATE ON public.rounds FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5525 (class 2620 OID 47486)
-- Name: scores trg_scores_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_scores_updated_at BEFORE UPDATE ON public.scores FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5526 (class 2620 OID 47487)
-- Name: submissions trg_submissions_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_submissions_updated_at BEFORE UPDATE ON public.submissions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5527 (class 2620 OID 47488)
-- Name: teams trg_teams_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_teams_updated_at BEFORE UPDATE ON public.teams FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5529 (class 2620 OID 47489)
-- Name: tracks trg_tracks_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_tracks_updated_at BEFORE UPDATE ON public.tracks FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5530 (class 2620 OID 47490)
-- Name: universities trg_universities_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_universities_updated_at BEFORE UPDATE ON public.universities FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5531 (class 2620 OID 47491)
-- Name: users trg_users_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5514 (class 2620 OID 47492)
-- Name: event_seed_assignments trg_validate_event_seed_assignment; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_event_seed_assignment BEFORE INSERT OR UPDATE ON public.event_seed_assignments FOR EACH ROW EXECUTE FUNCTION public.validate_event_seed_assignment();


--
-- TOC entry 5516 (class 2620 OID 47493)
-- Name: event_team_finishes trg_validate_event_team_finish; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_event_team_finish BEFORE INSERT ON public.event_team_finishes FOR EACH ROW EXECUTE FUNCTION public.validate_event_team_finish();


--
-- TOC entry 5533 (class 2620 OID 57592)
-- Name: logical_round_promotions trg_validate_logical_round_promotion; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_logical_round_promotion BEFORE INSERT OR UPDATE ON public.logical_round_promotions FOR EACH ROW EXECUTE FUNCTION public.validate_logical_round_promotion();


--
-- TOC entry 5534 (class 2620 OID 57612)
-- Name: logical_round_promotions trg_validate_logical_round_promotion_provenance; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_logical_round_promotion_provenance BEFORE INSERT OR UPDATE ON public.logical_round_promotions FOR EACH ROW EXECUTE FUNCTION public.validate_logical_round_promotion_provenance();


--
-- TOC entry 5528 (class 2620 OID 47494)
-- Name: teams trg_validate_team_profile_registration; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_team_profile_registration BEFORE INSERT OR UPDATE OF team_profile_id, track_id, source_team_id ON public.teams FOR EACH ROW EXECUTE FUNCTION public.validate_team_profile_registration();


--
-- TOC entry 5399 (class 2606 OID 47495)
-- Name: account_activation_tokens account_activation_tokens_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation_tokens
    ADD CONSTRAINT account_activation_tokens_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5400 (class 2606 OID 47500)
-- Name: account_activation_tokens account_activation_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation_tokens
    ADD CONSTRAINT account_activation_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5401 (class 2606 OID 47505)
-- Name: appeals appeals_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5402 (class 2606 OID 47510)
-- Name: appeals appeals_resolved_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_resolved_by_fkey FOREIGN KEY (resolved_by) REFERENCES public.users(id);


--
-- TOC entry 5403 (class 2606 OID 47515)
-- Name: appeals appeals_result_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_result_version_id_fkey FOREIGN KEY (result_version_id) REFERENCES public.round_result_versions(id);


--
-- TOC entry 5404 (class 2606 OID 47520)
-- Name: appeals appeals_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5405 (class 2606 OID 47525)
-- Name: appeals appeals_submitted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_submitted_by_fkey FOREIGN KEY (submitted_by) REFERENCES public.users(id);


--
-- TOC entry 5406 (class 2606 OID 47530)
-- Name: appeals appeals_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5407 (class 2606 OID 47535)
-- Name: audit_logs audit_logs_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE SET NULL;


--
-- TOC entry 5408 (class 2606 OID 47540)
-- Name: audit_logs audit_logs_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5409 (class 2606 OID 47545)
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5410 (class 2606 OID 47550)
-- Name: campuses campuses_university_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT campuses_university_id_fkey FOREIGN KEY (university_id) REFERENCES public.universities(id) ON DELETE CASCADE;


--
-- TOC entry 5411 (class 2606 OID 47555)
-- Name: event_rules event_rules_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_rules
    ADD CONSTRAINT event_rules_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5412 (class 2606 OID 47560)
-- Name: event_seed_assignments event_seed_assignments_assigned_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_assigned_by_fkey FOREIGN KEY (assigned_by) REFERENCES public.users(id);


--
-- TOC entry 5413 (class 2606 OID 47565)
-- Name: event_seed_assignments event_seed_assignments_candidate_source_finish_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_candidate_source_finish_id_fkey FOREIGN KEY (candidate_source_finish_id) REFERENCES public.event_team_finishes(id);


--
-- TOC entry 5414 (class 2606 OID 47570)
-- Name: event_seed_assignments event_seed_assignments_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id);


--
-- TOC entry 5415 (class 2606 OID 47575)
-- Name: event_seed_assignments event_seed_assignments_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id);


--
-- TOC entry 5416 (class 2606 OID 47580)
-- Name: event_seed_assignments event_seed_assignments_team_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_team_profile_id_fkey FOREIGN KEY (team_profile_id) REFERENCES public.team_profiles(id);


--
-- TOC entry 5417 (class 2606 OID 47585)
-- Name: event_seed_assignments event_seed_assignments_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_seed_assignments
    ADD CONSTRAINT event_seed_assignments_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id);


--
-- TOC entry 5418 (class 2606 OID 47590)
-- Name: event_team_finishes event_team_finishes_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5419 (class 2606 OID 47595)
-- Name: event_team_finishes event_team_finishes_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id);


--
-- TOC entry 5420 (class 2606 OID 47600)
-- Name: event_team_finishes event_team_finishes_final_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_final_round_id_fkey FOREIGN KEY (final_round_id) REFERENCES public.rounds(id);


--
-- TOC entry 5421 (class 2606 OID 47605)
-- Name: event_team_finishes event_team_finishes_result_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_result_version_id_fkey FOREIGN KEY (result_version_id) REFERENCES public.round_result_versions(id);


--
-- TOC entry 5422 (class 2606 OID 47610)
-- Name: event_team_finishes event_team_finishes_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id);


--
-- TOC entry 5423 (class 2606 OID 47615)
-- Name: event_team_finishes event_team_finishes_team_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_team_profile_id_fkey FOREIGN KEY (team_profile_id) REFERENCES public.team_profiles(id);


--
-- TOC entry 5424 (class 2606 OID 47620)
-- Name: event_team_finishes event_team_finishes_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_team_finishes
    ADD CONSTRAINT event_team_finishes_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id);


--
-- TOC entry 5506 (class 2606 OID 57605)
-- Name: logical_round_promotions fk_promotion_entry_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT fk_promotion_entry_version FOREIGN KEY (source_result_entry_id, source_result_version_id) REFERENCES public.round_result_version_entries(id, result_version_id) ON DELETE RESTRICT;


--
-- TOC entry 5507 (class 2606 OID 57600)
-- Name: logical_round_promotions fk_promotion_source_result_entry; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT fk_promotion_source_result_entry FOREIGN KEY (source_result_entry_id) REFERENCES public.round_result_version_entries(id) ON DELETE RESTRICT;


--
-- TOC entry 5508 (class 2606 OID 57595)
-- Name: logical_round_promotions fk_promotion_source_result_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT fk_promotion_source_result_version FOREIGN KEY (source_result_version_id) REFERENCES public.round_result_versions(id) ON DELETE RESTRICT;


--
-- TOC entry 5464 (class 2606 OID 57550)
-- Name: rounds fk_rounds_logical_round; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT fk_rounds_logical_round FOREIGN KEY (logical_round_id) REFERENCES public.round_definitions(id);


--
-- TOC entry 5425 (class 2606 OID 47625)
-- Name: incident_actions incident_actions_action_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_action_by_fkey FOREIGN KEY (action_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5426 (class 2606 OID 47630)
-- Name: incident_actions incident_actions_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE CASCADE;


--
-- TOC entry 5427 (class 2606 OID 47635)
-- Name: incident_evidences incident_evidences_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE CASCADE;


--
-- TOC entry 5428 (class 2606 OID 47640)
-- Name: incident_evidences incident_evidences_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5429 (class 2606 OID 47645)
-- Name: incident_reports incident_reports_assigned_coordinator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_assigned_coordinator_id_fkey FOREIGN KEY (assigned_coordinator_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5430 (class 2606 OID 47650)
-- Name: incident_reports incident_reports_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5431 (class 2606 OID 47655)
-- Name: incident_reports incident_reports_reporter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_reporter_id_fkey FOREIGN KEY (reporter_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5432 (class 2606 OID 47660)
-- Name: incident_reports incident_reports_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- TOC entry 5433 (class 2606 OID 47665)
-- Name: incident_reports incident_reports_submission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_submission_id_fkey FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE SET NULL;


--
-- TOC entry 5434 (class 2606 OID 47670)
-- Name: incident_reports incident_reports_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5435 (class 2606 OID 47675)
-- Name: incident_reports incident_reports_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- TOC entry 5509 (class 2606 OID 57574)
-- Name: logical_round_promotions logical_round_promotions_source_logical_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT logical_round_promotions_source_logical_round_id_fkey FOREIGN KEY (source_logical_round_id) REFERENCES public.round_definitions(id) ON DELETE RESTRICT;


--
-- TOC entry 5510 (class 2606 OID 57579)
-- Name: logical_round_promotions logical_round_promotions_target_logical_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT logical_round_promotions_target_logical_round_id_fkey FOREIGN KEY (target_logical_round_id) REFERENCES public.round_definitions(id) ON DELETE RESTRICT;


--
-- TOC entry 5511 (class 2606 OID 57584)
-- Name: logical_round_promotions logical_round_promotions_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logical_round_promotions
    ADD CONSTRAINT logical_round_promotions_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE RESTRICT;


--
-- TOC entry 5436 (class 2606 OID 47680)
-- Name: mentor_feedbacks mentor_feedbacks_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- TOC entry 5437 (class 2606 OID 47685)
-- Name: mentor_feedbacks mentor_feedbacks_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5438 (class 2606 OID 47690)
-- Name: mentor_feedbacks mentor_feedbacks_track_mentor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_track_mentor_id_fkey FOREIGN KEY (track_mentor_id) REFERENCES public.track_mentors(id) ON DELETE CASCADE;


--
-- TOC entry 5439 (class 2606 OID 47695)
-- Name: notices notices_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5440 (class 2606 OID 47700)
-- Name: notices notices_target_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_target_event_id_fkey FOREIGN KEY (target_event_id) REFERENCES public.events(id) ON DELETE SET NULL;


--
-- TOC entry 5441 (class 2606 OID 47705)
-- Name: notices notices_target_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_target_track_id_fkey FOREIGN KEY (target_track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- TOC entry 5442 (class 2606 OID 47710)
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5443 (class 2606 OID 47715)
-- Name: prize_revisions prize_revisions_changed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES public.users(id);


--
-- TOC entry 5444 (class 2606 OID 47720)
-- Name: prize_revisions prize_revisions_new_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_new_team_id_fkey FOREIGN KEY (new_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5445 (class 2606 OID 47725)
-- Name: prize_revisions prize_revisions_old_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_old_team_id_fkey FOREIGN KEY (old_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5446 (class 2606 OID 47730)
-- Name: prize_revisions prize_revisions_prize_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_prize_id_fkey FOREIGN KEY (prize_id) REFERENCES public.prizes(id) ON DELETE CASCADE;


--
-- TOC entry 5447 (class 2606 OID 47735)
-- Name: prizes prizes_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5448 (class 2606 OID 47740)
-- Name: prizes prizes_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5449 (class 2606 OID 47745)
-- Name: prizes prizes_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- TOC entry 5450 (class 2606 OID 47750)
-- Name: round_criteria round_criteria_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5451 (class 2606 OID 47755)
-- Name: round_criteria round_criteria_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.criteria_templates(id) ON DELETE SET NULL;


--
-- TOC entry 5505 (class 2606 OID 57543)
-- Name: round_definitions round_definitions_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_definitions
    ADD CONSTRAINT round_definitions_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5452 (class 2606 OID 47760)
-- Name: round_judges round_judges_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5453 (class 2606 OID 47765)
-- Name: round_judges round_judges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5454 (class 2606 OID 47770)
-- Name: round_participants round_participants_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5455 (class 2606 OID 47775)
-- Name: round_participants round_participants_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5456 (class 2606 OID 47780)
-- Name: round_rankings round_rankings_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5457 (class 2606 OID 47785)
-- Name: round_rankings round_rankings_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5458 (class 2606 OID 47790)
-- Name: round_rankings round_rankings_tie_breaker_criterion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_tie_breaker_criterion_id_fkey FOREIGN KEY (tie_breaker_criterion_id) REFERENCES public.round_criteria(id) ON DELETE SET NULL;


--
-- TOC entry 5459 (class 2606 OID 47795)
-- Name: round_result_version_entries round_result_version_entries_result_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT round_result_version_entries_result_version_id_fkey FOREIGN KEY (result_version_id) REFERENCES public.round_result_versions(id) ON DELETE CASCADE;


--
-- TOC entry 5460 (class 2606 OID 47800)
-- Name: round_result_version_entries round_result_version_entries_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_version_entries
    ADD CONSTRAINT round_result_version_entries_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id);


--
-- TOC entry 5461 (class 2606 OID 47805)
-- Name: round_result_versions round_result_versions_published_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT round_result_versions_published_by_fkey FOREIGN KEY (published_by) REFERENCES public.users(id);


--
-- TOC entry 5462 (class 2606 OID 47810)
-- Name: round_result_versions round_result_versions_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT round_result_versions_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5463 (class 2606 OID 47815)
-- Name: round_result_versions round_result_versions_source_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_result_versions
    ADD CONSTRAINT round_result_versions_source_version_id_fkey FOREIGN KEY (source_version_id) REFERENCES public.round_result_versions(id);


--
-- TOC entry 5465 (class 2606 OID 47820)
-- Name: rounds rounds_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT rounds_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- TOC entry 5466 (class 2606 OID 47825)
-- Name: rule_acceptances rule_acceptances_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5467 (class 2606 OID 47830)
-- Name: rule_acceptances rule_acceptances_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5468 (class 2606 OID 47835)
-- Name: scores scores_criterion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_criterion_id_fkey FOREIGN KEY (criterion_id) REFERENCES public.round_criteria(id) ON DELETE CASCADE;


--
-- TOC entry 5469 (class 2606 OID 47840)
-- Name: scores scores_judge_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_judge_id_fkey FOREIGN KEY (judge_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5470 (class 2606 OID 47845)
-- Name: scores scores_submission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_submission_id_fkey FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE;


--
-- TOC entry 5471 (class 2606 OID 47850)
-- Name: submissions submissions_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5472 (class 2606 OID 47855)
-- Name: submissions submissions_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5473 (class 2606 OID 47860)
-- Name: support_tickets support_tickets_requester_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT support_tickets_requester_id_fkey FOREIGN KEY (requester_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5474 (class 2606 OID 47865)
-- Name: team_chat_messages team_chat_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5475 (class 2606 OID 47870)
-- Name: team_chat_messages team_chat_messages_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5476 (class 2606 OID 47875)
-- Name: team_join_requests team_join_requests_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5477 (class 2606 OID 47880)
-- Name: team_join_requests team_join_requests_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5478 (class 2606 OID 47885)
-- Name: team_members team_members_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5479 (class 2606 OID 47890)
-- Name: team_members team_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5480 (class 2606 OID 47895)
-- Name: team_profiles team_profiles_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_profiles
    ADD CONSTRAINT team_profiles_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5481 (class 2606 OID 47900)
-- Name: team_recognitions team_recognitions_revoked_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_recognitions
    ADD CONSTRAINT team_recognitions_revoked_by_fkey FOREIGN KEY (revoked_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5482 (class 2606 OID 47905)
-- Name: team_recognitions team_recognitions_team_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_recognitions
    ADD CONSTRAINT team_recognitions_team_profile_id_fkey FOREIGN KEY (team_profile_id) REFERENCES public.team_profiles(id);


--
-- TOC entry 5483 (class 2606 OID 47910)
-- Name: team_timeline_events team_timeline_events_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5484 (class 2606 OID 47915)
-- Name: team_timeline_events team_timeline_events_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- TOC entry 5485 (class 2606 OID 47920)
-- Name: team_timeline_events team_timeline_events_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5486 (class 2606 OID 47925)
-- Name: team_timeline_events team_timeline_events_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- TOC entry 5487 (class 2606 OID 47930)
-- Name: teams teams_roster_confirmed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_roster_confirmed_by_fkey FOREIGN KEY (roster_confirmed_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5488 (class 2606 OID 47935)
-- Name: teams teams_source_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_source_team_id_fkey FOREIGN KEY (source_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5489 (class 2606 OID 47940)
-- Name: teams teams_team_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_team_profile_id_fkey FOREIGN KEY (team_profile_id) REFERENCES public.team_profiles(id);


--
-- TOC entry 5490 (class 2606 OID 47945)
-- Name: teams teams_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- TOC entry 5491 (class 2606 OID 47950)
-- Name: tie_break_decisions tie_break_decisions_decided_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_decided_by_fkey FOREIGN KEY (decided_by) REFERENCES public.users(id);


--
-- TOC entry 5492 (class 2606 OID 47955)
-- Name: tie_break_decisions tie_break_decisions_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5493 (class 2606 OID 47960)
-- Name: tie_break_decisions tie_break_decisions_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5494 (class 2606 OID 47965)
-- Name: track_judges track_judges_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5495 (class 2606 OID 47970)
-- Name: track_judges track_judges_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- TOC entry 5496 (class 2606 OID 47975)
-- Name: track_judges track_judges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5497 (class 2606 OID 47980)
-- Name: track_mentors track_mentors_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5498 (class 2606 OID 47985)
-- Name: track_mentors track_mentors_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- TOC entry 5499 (class 2606 OID 47990)
-- Name: track_mentors track_mentors_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5500 (class 2606 OID 47995)
-- Name: tracks tracks_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT tracks_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5501 (class 2606 OID 48000)
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- TOC entry 5502 (class 2606 OID 48005)
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5503 (class 2606 OID 48010)
-- Name: users users_campus_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_campus_id_fkey FOREIGN KEY (campus_id) REFERENCES public.campuses(id) ON DELETE SET NULL;


--
-- TOC entry 5504 (class 2606 OID 48015)
-- Name: users users_university_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_university_id_fkey FOREIGN KEY (university_id) REFERENCES public.universities(id) ON DELETE SET NULL;


--
-- TOC entry 5734 (class 0 OID 0)
-- Dependencies: 6
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


-- Completed on 2026-07-21 15:29:12

--
-- PostgreSQL database dump complete
--

\unrestrict B8YfETNzRNpgJ7zy16qFkwk9cdLDAp9CSoUgJZKn8arVcBN28jl7MVH5utDlSey

