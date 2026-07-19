-- PostgreSQL dump converted for GUI Query Tools (e.g. pgAdmin Query Tool).
-- psql-only meta commands were removed and COPY FROM stdin data was converted to INSERT statements.
-- Run this script while connected to the target database.

--
-- PostgreSQL database dump
--


-- Dumped from database version 17.10
-- Dumped by pg_dump version 17.10

-- Started on 2026-07-19 18:46:39

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
-- TOC entry 6 (class 2615 OID 45407)
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


-- Removed for portability: ALTER SCHEMA public OWNER TO postgres;

--
-- TOC entry 5524 (class 0 OID 0)
-- Dependencies: 6
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS '';


--
-- TOC entry 2 (class 3079 OID 45408)
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- TOC entry 5526 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- TOC entry 922 (class 1247 OID 45446)
-- Name: account_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.account_status AS ENUM (
    'pending',
    'approved',
    'rejected'
);


-- Removed for portability: ALTER TYPE public.account_status OWNER TO postgres;

--
-- TOC entry 925 (class 1247 OID 45454)
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
    'REJECT_INCIDENT'
);


-- Removed for portability: ALTER TYPE public.audit_action OWNER TO postgres;

--
-- TOC entry 928 (class 1247 OID 45486)
-- Name: event_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.event_status AS ENUM (
    'draft',
    'published',
    'ongoing',
    'completed',
    'cancelled'
);


-- Removed for portability: ALTER TYPE public.event_status OWNER TO postgres;

--
-- TOC entry 931 (class 1247 OID 45498)
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


-- Removed for portability: ALTER TYPE public.incident_action_type OWNER TO postgres;

--
-- TOC entry 934 (class 1247 OID 45512)
-- Name: incident_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.incident_status AS ENUM (
    'reported',
    'under_review',
    'resolved',
    'rejected'
);


-- Removed for portability: ALTER TYPE public.incident_status OWNER TO postgres;

--
-- TOC entry 937 (class 1247 OID 45522)
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


-- Removed for portability: ALTER TYPE public.incident_type OWNER TO postgres;

--
-- TOC entry 940 (class 1247 OID 45536)
-- Name: promotion_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.promotion_status AS ENUM (
    'pending',
    'promoted',
    'eliminated',
    'disqualified'
);


-- Removed for portability: ALTER TYPE public.promotion_status OWNER TO postgres;

--
-- TOC entry 943 (class 1247 OID 45546)
-- Name: round_participant_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.round_participant_status AS ENUM (
    'pending',
    'active',
    'promoted',
    'eliminated',
    'disqualified'
);


-- Removed for portability: ALTER TYPE public.round_participant_status OWNER TO postgres;

--
-- TOC entry 946 (class 1247 OID 45558)
-- Name: student_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.student_type AS ENUM (
    'fpt',
    'external',
    'none'
);


-- Removed for portability: ALTER TYPE public.student_type OWNER TO postgres;

--
-- TOC entry 949 (class 1247 OID 45566)
-- Name: team_member_role; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.team_member_role AS ENUM (
    'leader',
    'member'
);


-- Removed for portability: ALTER TYPE public.team_member_role OWNER TO postgres;

--
-- TOC entry 952 (class 1247 OID 45572)
-- Name: team_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.team_status AS ENUM (
    'active',
    'disqualified'
);


-- Removed for portability: ALTER TYPE public.team_status OWNER TO postgres;

--
-- TOC entry 293 (class 1255 OID 45577)
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


-- Removed for portability: ALTER FUNCTION public.update_updated_at_column() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 252 (class 1259 OID 46304)
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
    updated_at timestamp without time zone
);


-- Removed for portability: ALTER TABLE public.appeals OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 45578)
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


-- Removed for portability: ALTER TABLE public.audit_logs OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 45585)
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


-- Removed for portability: ALTER TABLE public.campuses OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 45592)
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


-- Removed for portability: ALTER TABLE public.criteria_templates OWNER TO postgres;

--
-- TOC entry 254 (class 1259 OID 46368)
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


-- Removed for portability: ALTER TABLE public.event_rules OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 45600)
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


-- Removed for portability: ALTER TABLE public.events OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 45608)
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


-- Removed for portability: ALTER TABLE public.incident_actions OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 45615)
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


-- Removed for portability: ALTER TABLE public.incident_evidences OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 45622)
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


-- Removed for portability: ALTER TABLE public.incident_reports OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 45630)
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


-- Removed for portability: ALTER TABLE public.mentor_feedbacks OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 45637)
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


-- Removed for portability: ALTER TABLE public.notices OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 45646)
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


-- Removed for portability: ALTER TABLE public.notifications OWNER TO postgres;

--
-- TOC entry 256 (class 1259 OID 46404)
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


-- Removed for portability: ALTER TABLE public.prize_revisions OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 45653)
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


-- Removed for portability: ALTER TABLE public.prizes OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 45660)
-- Name: revoked_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.revoked_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now()
);


-- Removed for portability: ALTER TABLE public.revoked_tokens OWNER TO postgres;

--
-- TOC entry 230 (class 1259 OID 45666)
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    description text
);


-- Removed for portability: ALTER TABLE public.roles OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 45672)
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


-- Removed for portability: ALTER TABLE public.round_criteria OWNER TO postgres;

--
-- TOC entry 232 (class 1259 OID 45681)
-- Name: round_judges; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.round_judges (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    round_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


-- Removed for portability: ALTER TABLE public.round_judges OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 45686)
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


-- Removed for portability: ALTER TABLE public.round_participants OWNER TO postgres;

--
-- TOC entry 234 (class 1259 OID 45694)
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


-- Removed for portability: ALTER TABLE public.round_rankings OWNER TO postgres;

--
-- TOC entry 235 (class 1259 OID 45703)
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
    CONSTRAINT rounds_sequence_number_check CHECK ((sequence_number > 0)),
    CONSTRAINT rounds_top_n_to_promote_check CHECK ((top_n_to_promote > 0))
);


-- Removed for portability: ALTER TABLE public.rounds OWNER TO postgres;

--
-- TOC entry 255 (class 1259 OID 46385)
-- Name: rule_acceptances; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rule_acceptances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    event_id uuid NOT NULL,
    accepted_at timestamp without time zone DEFAULT now() NOT NULL
);


-- Removed for portability: ALTER TABLE public.rule_acceptances OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 45710)
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


-- Removed for portability: ALTER TABLE public.scores OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 45718)
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


-- Removed for portability: ALTER TABLE public.submissions OWNER TO postgres;

--
-- TOC entry 238 (class 1259 OID 45726)
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


-- Removed for portability: ALTER TABLE public.support_tickets OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 45735)
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


-- Removed for portability: ALTER TABLE public.team_chat_messages OWNER TO postgres;

--
-- TOC entry 240 (class 1259 OID 45743)
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


-- Removed for portability: ALTER TABLE public.team_join_requests OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 45752)
-- Name: team_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.team_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    team_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role public.team_member_role DEFAULT 'member'::public.team_member_role NOT NULL,
    joined_at timestamp without time zone DEFAULT now() NOT NULL
);


-- Removed for portability: ALTER TABLE public.team_members OWNER TO postgres;

--
-- TOC entry 251 (class 1259 OID 46278)
-- Name: team_timeline_events; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.team_timeline_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    team_id uuid NOT NULL,
    round_id uuid,
    type character varying(40) NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    score_snapshot numeric(10,2),
    rank_snapshot integer,
    status_snapshot character varying(40),
    occurred_at timestamp without time zone DEFAULT now() NOT NULL
);


-- Removed for portability: ALTER TABLE public.team_timeline_events OWNER TO postgres;

--
-- TOC entry 242 (class 1259 OID 45758)
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
    invite_code character varying(12)
);


-- Removed for portability: ALTER TABLE public.teams OWNER TO postgres;

--
-- TOC entry 253 (class 1259 OID 46342)
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


-- Removed for portability: ALTER TABLE public.tie_break_decisions OWNER TO postgres;

--
-- TOC entry 243 (class 1259 OID 45766)
-- Name: track_judges; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.track_judges (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    track_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


-- Removed for portability: ALTER TABLE public.track_judges OWNER TO postgres;

--
-- TOC entry 244 (class 1259 OID 45771)
-- Name: track_mentors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.track_mentors (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    track_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


-- Removed for portability: ALTER TABLE public.track_mentors OWNER TO postgres;

--
-- TOC entry 245 (class 1259 OID 45776)
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


-- Removed for portability: ALTER TABLE public.tracks OWNER TO postgres;

--
-- TOC entry 246 (class 1259 OID 45783)
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


-- Removed for portability: ALTER TABLE public.universities OWNER TO postgres;

--
-- TOC entry 247 (class 1259 OID 45788)
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL
);


-- Removed for portability: ALTER TABLE public.user_roles OWNER TO postgres;

--
-- TOC entry 248 (class 1259 OID 45792)
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
    bio text
);


-- Removed for portability: ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 249 (class 1259 OID 45802)
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


-- Removed for portability: ALTER VIEW public.view_judge_submissions OWNER TO postgres;

--
-- TOC entry 250 (class 1259 OID 45807)
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


-- Removed for portability: ALTER VIEW public.view_mentor_teams OWNER TO postgres;

--
-- TOC entry 5514 (class 0 OID 46304)
-- Dependencies: 252
-- Data for Name: appeals; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.appeals (0 rows)
-- No rows to insert.



--
-- TOC entry 5482 (class 0 OID 45578)
-- Dependencies: 218
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.audit_logs (8 rows)
INSERT INTO public.audit_logs (id, user_id, team_id, incident_id, action, target_type, target_id, old_value, new_value, details, occurred_at) VALUES
    (E'3f4384d4-319a-4e16-aa94-d39607ebda08', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', NULL, NULL, E'DISQUALIFY_TEAM', E'team', E'8b0bb0fc-1857-4b95-964b-92b791c352f6', E'active', E'disqualified', E'Smoke test', E'2026-07-02 07:59:40.317851'),
    (E'dc96fdbb-4510-4c52-be75-b4cae9ed42dc', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', NULL, NULL, E'DISQUALIFY_TEAM', E'team', E'8b0bb0fc-1857-4b95-964b-92b791c352f6', E'active', E'disqualified', E'tao thich thi t khoa', E'2026-07-02 08:46:33.735737'),
    (E'643c4fa8-beb8-4b74-b544-9b763987e145', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', NULL, NULL, E'DISQUALIFY_TEAM', E'team', E'8b0bb0fc-1857-4b95-964b-92b791c352f6', E'active', E'disqualified', E'bo m thich ban', E'2026-07-02 17:43:44.492644'),
    (E'0eb3a454-7687-4374-aec7-302486de8970', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', NULL, E'PROMOTE_TEAM', E'team', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'118d1506-a833-4a9f-877f-58b3bf4b3772', E'Moved team to track AI', E'2026-07-04 01:53:09.197129'),
    (E'cc78e52a-763e-43e3-9550-a9e18025749b', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', NULL, E'PROMOTE_TEAM', E'team', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'118d1506-a833-4a9f-877f-58b3bf4b3772', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'Moved team to track Regression Track 232026', E'2026-07-04 01:53:09.239013'),
    (E'48969fb3-6fc4-400a-b700-670f56bbd5d7', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', NULL, E'PROMOTE_TEAM', E'team', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'e18a5239-caf9-4997-8c11-e0bea2ca0f6e', E'Moved team to track Track B', E'2026-07-04 02:01:38.614675'),
    (E'0752ac16-a226-46fc-aa0a-c035b5dc06d0', E'a1000000-0000-4000-8000-000000000001', E'75ff0336-137b-437c-9142-0b6e08afaf76', NULL, E'DISQUALIFY_TEAM', E'team', E'75ff0336-137b-437c-9142-0b6e08afaf76', E'active', E'disqualified', E'Không đủ thành viên khi đóng đăng ký (2/3)', E'2026-07-19 10:54:17.446095'),
    (E'889903eb-5299-4668-be36-0170611a8380', E'a1000000-0000-4000-8000-000000000001', E'2431c70e-5860-4536-9f99-34aabbafa6e7', NULL, E'DISQUALIFY_TEAM', E'team', E'2431c70e-5860-4536-9f99-34aabbafa6e7', E'active', E'disqualified', E'Không đủ thành viên khi đóng đăng ký (2/3)', E'2026-07-19 10:54:17.449602');



--
-- TOC entry 5483 (class 0 OID 45585)
-- Dependencies: 219
-- Data for Name: campuses; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.campuses (5 rows)
INSERT INTO public.campuses (id, university_id, name, address, city, created_at, updated_at) VALUES
    (E'22222222-2222-2222-2222-222222222221', E'11111111-1111-1111-1111-111111111111', E'FPT University Ho Chi Minh City', NULL, E'Ho Chi Minh City', E'2026-07-02 14:13:12.1265', NULL),
    (E'22222222-2222-2222-2222-222222222222', E'11111111-1111-1111-1111-111111111111', E'FPT University Ha Noi', NULL, E'Ha Noi', E'2026-07-02 14:13:12.1265', NULL),
    (E'22222222-2222-2222-2222-222222222223', E'11111111-1111-1111-1111-111111111111', E'FPT University Da Nang', NULL, E'Da Nang', E'2026-07-02 14:13:12.1265', NULL),
    (E'22222222-2222-2222-2222-222222222224', E'11111111-1111-1111-1111-111111111111', E'FPT University Can Tho', NULL, E'Can Tho', E'2026-07-02 14:13:12.1265', NULL),
    (E'22222222-2222-2222-2222-222222222225', E'11111111-1111-1111-1111-111111111111', E'FPT University Quy Nhon', NULL, E'Quy Nhon', E'2026-07-02 14:13:12.1265', NULL);



--
-- TOC entry 5484 (class 0 OID 45592)
-- Dependencies: 220
-- Data for Name: criteria_templates; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.criteria_templates (0 rows)
-- No rows to insert.



--
-- TOC entry 5516 (class 0 OID 46368)
-- Dependencies: 254
-- Data for Name: event_rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.event_rules (0 rows)
-- No rows to insert.



--
-- TOC entry 5485 (class 0 OID 45600)
-- Dependencies: 221
-- Data for Name: events; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.events (6 rows)
INSERT INTO public.events (id, title, description, status, created_at, updated_at, term, prize_pool, registration_start, registration_end, event_start, event_end) VALUES
    (E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'Regression Event 232026', E'full regression smoke', E'draft', E'2026-07-02 16:20:26.418208', E'2026-07-02 16:20:26.418208', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'32ef48b8-9ac3-4df0-8ad4-52f43f8eb9ae', E'SMOKE EDITED', E'tmp', E'published', E'2026-07-03 15:00:12.731716', E'2026-07-03 23:45:26.621204', E'', E'', NULL, NULL, NULL, NULL),
    (E'81a4656e-8a2d-4152-958d-6f302ec23efe', E'SMOKE PUB DEL', E'tmp', E'ongoing', E'2026-07-03 15:00:26.054602', E'2026-07-04 08:37:21.259811', E'', E'', NULL, NULL, NULL, NULL),
    (E'a6ec61f7-2f7e-4d77-86d5-861a62fc060c', E'kghj', E'gfh', E'ongoing', E'2026-07-03 16:46:07.145649', E'2026-07-04 08:37:32.055564', E'Spring', E'4', E'2026-07-02 17:00:00', E'2026-07-03 17:00:00', E'2026-07-04 17:00:00', E'2026-07-06 17:00:00'),
    (E'aaaa0000-0000-4000-8000-000000000001', E'DEMO Event (test accounts)', E'Seed event for manual feature testing.', E'ongoing', E'2026-07-10 02:49:49.985313', NULL, E'Summer 2026', E'10.000.000 VND', E'2026-07-03 02:49:49.985313', E'2026-07-17 02:49:49.985313', E'2026-07-09 02:49:49.985313', E'2026-08-09 02:49:49.985313'),
    (E'5c133817-ddd1-49ff-ae1b-98d366b950e4', E'Hackathon 2026', E'AI in Testing topic', E'ongoing', E'2026-07-19 09:49:55.385525', E'2026-07-19 17:54:17.436292', E'Summer', E'60000', E'2026-07-19 17:00:00', E'2026-07-21 17:00:00', E'2026-07-22 17:00:00', E'2026-07-29 17:00:00');



--
-- TOC entry 5486 (class 0 OID 45608)
-- Dependencies: 222
-- Data for Name: incident_actions; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.incident_actions (4 rows)
INSERT INTO public.incident_actions (id, incident_id, action_by, action_type, target_type, target_id, old_value, new_value, note, created_at) VALUES
    (E'5e81b5c6-0f2a-4f3b-bf12-b76ce060ebc7', E'62febed3-aae9-44f6-95d5-b88cd452f7b5', E'a1000000-0000-4000-8000-000000000001', E'other', NULL, NULL, E'reported', E'under_review', E'Decision: Under Review', E'2026-07-19 10:19:58.929878'),
    (E'2f4bdc28-e964-45e4-86a7-57c117839764', E'62febed3-aae9-44f6-95d5-b88cd452f7b5', E'a1000000-0000-4000-8000-000000000001', E'other', NULL, NULL, E'under_review', E'under_review', E'Decision: Under Review', E'2026-07-19 10:20:00.602819'),
    (E'6ee38e6c-3b2b-47fc-aa91-7b5e6710e114', E'62febed3-aae9-44f6-95d5-b88cd452f7b5', E'a1000000-0000-4000-8000-000000000001', E'other', NULL, NULL, E'under_review', E'resolved', E'Decision: Resolved', E'2026-07-19 10:20:01.78565'),
    (E'ac6683b1-1f11-41a8-ae53-fbe577cf22b3', E'62febed3-aae9-44f6-95d5-b88cd452f7b5', E'a1000000-0000-4000-8000-000000000001', E'other', NULL, NULL, E'resolved', E'under_review', E'Decision: Under Review', E'2026-07-19 10:20:02.98408');



--
-- TOC entry 5487 (class 0 OID 45615)
-- Dependencies: 223
-- Data for Name: incident_evidences; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.incident_evidences (0 rows)
-- No rows to insert.



--
-- TOC entry 5488 (class 0 OID 45622)
-- Dependencies: 224
-- Data for Name: incident_reports; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.incident_reports (1 rows)
INSERT INTO public.incident_reports (id, event_id, track_id, round_id, team_id, submission_id, reporter_id, assigned_coordinator_id, type, status, title, description, created_at, updated_at, resolved_at, severity, category) VALUES
    (E'62febed3-aae9-44f6-95d5-b88cd452f7b5', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'cc3658ed-a72b-4b02-8942-a62c87eefc76', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', NULL, E'other', E'under_review', E'Regression Incident', E'incident smoke', E'2026-07-02 16:21:52.711207', E'2026-07-19 17:20:02.978824', E'2026-07-19 10:20:01.782664', NULL, NULL);



--
-- TOC entry 5489 (class 0 OID 45630)
-- Dependencies: 225
-- Data for Name: mentor_feedbacks; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.mentor_feedbacks (0 rows)
-- No rows to insert.



--
-- TOC entry 5490 (class 0 OID 45637)
-- Dependencies: 226
-- Data for Name: notices; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.notices (7 rows)
INSERT INTO public.notices (id, title, content, priority, target_role, target_event_id, target_track_id, author_id, created_at, updated_at, target_team_id) VALUES
    (E'09870d52-2805-4279-b23b-23669e3fc5e6', E'Smoke notice', E'Notice endpoint smoke', E'normal', E'judge', NULL, NULL, E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'2026-07-02 16:13:55.004049', E'2026-07-02 16:13:55.004049', NULL),
    (E'284bcc67-2640-4ab2-99a8-247827290585', E'Regression Notice', E'full regression notice', E'normal', E'judge', NULL, NULL, E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'2026-07-02 16:21:09.857379', E'2026-07-02 16:21:09.857379', NULL),
    (E'9e601cd6-9492-4ea5-b971-4647869e6f0a', E'Team notice smoke', E'team notice smoke', E'normal', E'team_member', NULL, NULL, E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'2026-07-02 16:45:43.297306', E'2026-07-02 16:45:43.297306', NULL),
    (E'57fdae09-f8f8-4d8b-b241-2a6ade4cfa3d', E'Mentor notice smoke', E'mentor notice smoke', E'normal', E'mentor', NULL, NULL, E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'2026-07-02 16:45:43.309762', E'2026-07-02 16:45:43.309762', NULL),
    (E'a2010f6a-1dca-4fbf-a842-03731fb9eead', E'probe', E'probe body', E'normal', E'all', NULL, NULL, E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'2026-07-03 13:59:58.965111', E'2026-07-03 13:59:58.965111', NULL),
    (E'14aa0dcc-3ba0-415b-a886-cf46624ba41d', E'probe2', E'probe body', E'high', E'judge', NULL, NULL, E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'2026-07-03 13:59:58.978187', E'2026-07-03 13:59:58.978187', NULL),
    (E'4ab6abfb-fc85-45a4-ac1c-86d3061a94cc', E'probe notice', E'probe', E'normal', E'team_member', NULL, NULL, E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'2026-07-03 14:00:32.447079', E'2026-07-03 14:00:32.447079', NULL);



--
-- TOC entry 5491 (class 0 OID 45646)
-- Dependencies: 227
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.notifications (0 rows)
-- No rows to insert.



--
-- TOC entry 5518 (class 0 OID 46404)
-- Dependencies: 256
-- Data for Name: prize_revisions; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.prize_revisions (0 rows)
-- No rows to insert.



--
-- TOC entry 5492 (class 0 OID 45653)
-- Dependencies: 228
-- Data for Name: prizes; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.prizes (1 rows)
INSERT INTO public.prizes (id, event_id, track_id, team_id, name, prize_amount, description, awarded_at, created_at, updated_at) VALUES
    (E'4399f90a-cf9c-4b5c-b508-c8c5a18231e7', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', NULL, E'Regression Prize', NULL, E'smoke', NULL, E'2026-07-02 16:21:09.846969', E'2026-07-04 08:28:17.95402');



--
-- TOC entry 5493 (class 0 OID 45660)
-- Dependencies: 229
-- Data for Name: revoked_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.revoked_tokens (12 rows)
INSERT INTO public.revoked_tokens (id, token_hash, expires_at, created_at, updated_at) VALUES
    (E'8521d61b-3025-487f-b577-d8f61a778efd', E'5799bd8c3be56e17fca17373cb40cd420efb1d8a17216f12183274b3a34bd8e8', E'2026-07-03 00:52:47+07', E'2026-07-02 16:52:48.086301', E'2026-07-02 16:52:48.086301'),
    (E'e5688283-a5f4-4a27-9dec-82d39c8eeb91', E'7cc94d9a95b1b441380884e04af6accdc486c49d4597a4c69927beb162dfd72c', E'2026-07-09 23:52:48+07', E'2026-07-02 16:52:48.096347', E'2026-07-02 16:52:48.096347'),
    (E'4bba1d1a-166b-4fc3-85e9-5a8849604ada', E'382080f32a9e30a61e27c2cb6ab0936c5e9e5ddeb9ad3d76bcb688358d0cf63a', E'2026-07-03 00:56:21+07', E'2026-07-02 16:56:22.203753', E'2026-07-02 16:56:22.203753'),
    (E'2d309f25-f972-4f4d-a8c3-4a9a426bb9b4', E'293903ed6a55b44ed993454e57df75471b0ad9be7b6bcfbca9293c1eddcbc485', E'2026-07-09 23:56:21+07', E'2026-07-02 16:56:22.206048', E'2026-07-02 16:56:22.206048'),
    (E'020b3ab0-5e29-4ecd-84ff-9e83011a3d17', E'2536ddd12041a7922e9d94d98e9f5183a488b326904b9651f00f17b6bd146059', E'2026-07-03 00:56:30+07', E'2026-07-02 16:56:31.419643', E'2026-07-02 16:56:31.419643'),
    (E'4f89415d-1652-44b3-8b4d-6b01698a8ea3', E'56f4384cf53f5b39d47edaa480efb70af5354ff9f2f5a270d74f2e45b00b6322', E'2026-07-09 23:56:30+07', E'2026-07-02 16:56:31.421647', E'2026-07-02 16:56:31.421647'),
    (E'ce5a1393-8555-41ca-9471-e75548f34ade', E'645fd3500e7f40d009554c269f14f4767cdce41ecb3e5bdaa6831617b6075310', E'2026-07-03 00:56:46+07', E'2026-07-02 16:56:46.819418', E'2026-07-02 16:56:46.819418'),
    (E'1d2e00b8-90c0-49ce-b5d3-6c7a791fdae3', E'0e83cb60000032455b0166ffd0276cfb9d31c0cd8b87f6d40b1bf4fa498f7ae8', E'2026-07-09 23:56:46+07', E'2026-07-02 16:56:46.821282', E'2026-07-02 16:56:46.821282'),
    (E'ef616a9f-3e50-4b5d-a681-3e8d3d18c492', E'dc57df8936b71a6079fbc289039220a812eab59f94d737f2d5a414f8783adfbb', E'2026-07-03 00:57:18+07', E'2026-07-02 16:57:19.103884', E'2026-07-02 16:57:19.103884'),
    (E'76789efa-2734-464d-8ebd-30a65f19eb38', E'dc1690977a970ff3798fb4394a35956c11bb7a2325b19023ea9c8ae8017a8de8', E'2026-07-09 23:57:18+07', E'2026-07-02 16:57:19.105785', E'2026-07-02 16:57:19.105785'),
    (E'305cc00d-9175-485e-acba-6d87ded5ae1f', E'5c4aa3c44510e73f40755b8cbd608fcb27bcd50c2dca95e55e9b966b08f89579', E'2026-07-03 01:11:36+07', E'2026-07-02 17:11:37.050476', E'2026-07-02 17:11:37.050476'),
    (E'9b2f263f-f8bc-4346-a770-ab305a8b8266', E'78aef62607d6c7047c6ed83defe480b3cdb7dfc1f543ad8dd1871efb80ad3f8b', E'2026-07-10 00:11:36+07', E'2026-07-02 17:11:37.053288', E'2026-07-02 17:11:37.053288');



--
-- TOC entry 5494 (class 0 OID 45666)
-- Dependencies: 230
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.roles (5 rows)
INSERT INTO public.roles (id, name, description) VALUES
    (E'e9e4426a-4b07-4969-a24d-54e7440cd964', E'coordinator', E'Event Coordinator from SE Department or PDP Staff'),
    (E'6f4f98b4-aff1-42df-801b-11f61e93db48', E'team_leader', E'Leader of a hackathon team'),
    (E'5afe0808-f611-48b4-82b6-49383a50efb4', E'team_member', E'Member of a hackathon team'),
    (E'28c09a8e-65bd-44b9-8bf2-fa999efbf8d1', E'mentor', E'Mentor assigned to a track'),
    (E'ed0ca372-ef82-4f26-bf88-474156fd7d67', E'judge', E'Judge assigned to a round');



--
-- TOC entry 5495 (class 0 OID 45672)
-- Dependencies: 231
-- Data for Name: round_criteria; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.round_criteria (7 rows)
INSERT INTO public.round_criteria (id, round_id, template_id, name, weight, description, created_at, updated_at, status) VALUES
    (E'99dc7e5c-2b3e-48a4-9e3b-657c8ae7a89a', E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', NULL, E'Regression Criterion', E'1.00', E'smoke', E'2026-07-02 16:20:26.473488', E'2026-07-02 16:20:26.473503', E'active'),
    (E'553cda26-eb47-4f54-a04b-8397d3bc5c55', E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', NULL, E'E2ECrit6279', E'25.00', E'', E'2026-07-03 17:20:08.319536', E'2026-07-03 17:20:08.319559', E'active'),
    (E'73038e4b-d91d-4b89-93c1-5c7d1bb37531', E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', NULL, E'E2ECrit9937', E'25.00', E'', E'2026-07-03 17:23:31.957485', E'2026-07-03 17:23:31.957522', E'active'),
    (E'9f38f646-492c-41bf-b4a5-1a5f784b572e', E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', NULL, E'E2ECrit72446', E'25.00', E'', E'2026-07-03 17:27:54.460143', E'2026-07-03 17:27:54.46017', E'active'),
    (E'aba75b80-95d5-41e9-8a73-a5bfe93aeefe', E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', NULL, E'E2ECrit58423', E'30.00', E'', E'2026-07-03 17:32:40.434234', E'2026-07-04 00:48:44.686425', E'active'),
    (E'92f2cea9-3c0d-4e84-a104-170a6fb11203', E'1a0cef4f-3aae-4e78-8f75-09dd77767eac', NULL, E'1', E'20.00', E'', E'2026-07-19 11:33:59.483943', E'2026-07-19 11:33:59.483943', E'active'),
    (E'0e99cdab-6e34-4d3a-a295-0a9d9e538ae1', E'1a0cef4f-3aae-4e78-8f75-09dd77767eac', NULL, E'2', E'80.00', E'', E'2026-07-19 11:34:05.379852', E'2026-07-19 11:34:05.379852', E'active');



--
-- TOC entry 5496 (class 0 OID 45681)
-- Dependencies: 232
-- Data for Name: round_judges; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.round_judges (2 rows)
INSERT INTO public.round_judges (id, round_id, user_id, assigned_at) VALUES
    (E'a3000000-0000-4000-8000-000000000003', E'aaaa0000-0000-4000-8000-000000000003', E'a1000000-0000-4000-8000-000000000003', E'2026-07-10 02:49:49.985313'),
    (E'e390f5f2-b066-4dc1-bf61-61f2873f8926', E'1a0cef4f-3aae-4e78-8f75-09dd77767eac', E'a1000000-0000-4000-8000-000000000003', E'2026-07-19 11:33:03.019129');



--
-- TOC entry 5497 (class 0 OID 45686)
-- Dependencies: 233
-- Data for Name: round_participants; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.round_participants (0 rows)
-- No rows to insert.



--
-- TOC entry 5498 (class 0 OID 45694)
-- Dependencies: 234
-- Data for Name: round_rankings; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.round_rankings (1 rows)
INSERT INTO public.round_rankings (id, round_id, team_id, total_score, rank, status, tie_breaker_criterion_id, tie_breaker_score, tie_breaker_reason, calculated_at, updated_at) VALUES
    (E'47fd33bb-9d6c-48a4-b0cf-03b15ecd3b54', E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'8080.00', E'1', E'pending', NULL, NULL, E'Ranked by total weighted score; team name used for deterministic ordering on ties', E'2026-07-03 17:48:38.274602', E'2026-07-03 17:48:38.274616');



--
-- TOC entry 5499 (class 0 OID 45703)
-- Dependencies: 235
-- Data for Name: rounds; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.rounds (8 rows)
INSERT INTO public.rounds (id, track_id, name, sequence_number, submission_deadline, top_n_to_promote, created_at, updated_at, result_published_at, appeal_deadline) VALUES
    (E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'Regression Round 232026', E'1', E'2026-07-09 16:20:26', E'3', E'2026-07-02 16:20:26.454913', E'2026-07-02 16:20:26.454913', NULL, NULL),
    (E'876945ae-d236-4b69-89d4-2a515c55d9f7', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'round 2', E'13', E'2026-07-04 17:43:00', E'5', E'2026-07-02 17:43:13.774367', E'2026-07-02 17:43:13.774367', NULL, NULL),
    (E'27451535-d487-4d47-a015-230c2f53236b', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'E2ERound5363', E'99407', E'2026-08-31 22:00:00', E'5', E'2026-07-03 17:23:27.425048', E'2026-07-03 17:23:27.425048', NULL, NULL),
    (E'a206b707-43b9-4d52-98ab-2fb58ea85627', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'E2ERound67889', E'99669', E'2026-08-31 22:00:00', E'5', E'2026-07-03 17:27:49.925515', E'2026-07-03 17:27:49.925515', NULL, NULL),
    (E'f00c85fe-3caa-4602-a6a4-ed0ddadbc4e4', E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'E2ERound53846', E'99955', E'2026-08-31 22:00:00', E'5', E'2026-07-03 17:32:35.902038', E'2026-07-03 17:32:35.902038', NULL, NULL),
    (E'47325e63-2e70-4281-909a-dcf8d45c9542', E'd892f925-3347-4f5e-890b-dc7ca7558e33', E'1', E'9', E'2026-07-04 18:37:00', E'3', E'2026-07-04 01:37:59.20713', E'2026-07-04 01:37:59.20713', NULL, NULL),
    (E'aaaa0000-0000-4000-8000-000000000003', E'aaaa0000-0000-4000-8000-000000000002', E'DEMO Round 1', E'1', E'2026-07-24 02:49:49.985313', E'3', E'2026-07-10 02:49:49.985313', NULL, NULL, NULL),
    (E'1a0cef4f-3aae-4e78-8f75-09dd77767eac', E'5ff86b4c-12b5-4701-bc24-c5ab4cea0307', E'round 1', E'1', E'2026-07-20 03:56:00', E'2', E'2026-07-19 10:56:40.785183', E'2026-07-19 10:56:40.785183', NULL, NULL);



--
-- TOC entry 5517 (class 0 OID 46385)
-- Dependencies: 255
-- Data for Name: rule_acceptances; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.rule_acceptances (0 rows)
-- No rows to insert.



--
-- TOC entry 5500 (class 0 OID 45710)
-- Dependencies: 236
-- Data for Name: scores; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.scores (7 rows)
INSERT INTO public.scores (id, submission_id, judge_id, criterion_id, score, weighted_score, criterion_average_score, criterion_variance, criterion_stddev, comment, created_at, updated_at) VALUES
    (E'29172851-ebef-4a9b-bb93-8ce07f825aa6', E'cc3658ed-a72b-4b02-8942-a62c87eefc76', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'99dc7e5c-2b3e-48a4-9e3b-657c8ae7a89a', E'80.00', E'80.00', NULL, NULL, NULL, E'logic weight test', E'2026-07-02 16:21:09.811477', E'2026-07-04 00:48:34.734182'),
    (E'791c554c-f7b6-4c54-a3fb-e62a5ca07e40', E'cc3658ed-a72b-4b02-8942-a62c87eefc76', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'553cda26-eb47-4f54-a04b-8397d3bc5c55', E'80.00', E'2000.00', NULL, NULL, NULL, E'logic weight test', E'2026-07-03 17:20:39.304769', E'2026-07-04 00:48:34.752666'),
    (E'c0fba8d9-244c-4b90-ae3f-44679b487ee7', E'cc3658ed-a72b-4b02-8942-a62c87eefc76', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'73038e4b-d91d-4b89-93c1-5c7d1bb37531', E'80.00', E'2000.00', NULL, NULL, NULL, E'logic weight test', E'2026-07-03 17:24:15.578017', E'2026-07-04 00:48:34.768731'),
    (E'bfa27de4-4ba4-4e4c-afdd-8c4757749557', E'cc3658ed-a72b-4b02-8942-a62c87eefc76', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'9f38f646-492c-41bf-b4a5-1a5f784b572e', E'80.00', E'2000.00', NULL, NULL, NULL, E'logic weight test', E'2026-07-03 17:28:39.043044', E'2026-07-04 00:48:34.781078'),
    (E'ababb520-ab3f-4c20-b576-e4183aac5b72', E'cc3658ed-a72b-4b02-8942-a62c87eefc76', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'aba75b80-95d5-41e9-8a73-a5bfe93aeefe', E'80.00', E'2000.00', NULL, NULL, NULL, E'logic weight test', E'2026-07-03 17:33:24.44238', E'2026-07-04 00:48:34.792798'),
    (E'bba07086-348c-4a43-89a7-287f70928c05', E'69966311-3bbb-4165-b266-cf794ef8b99e', E'a1000000-0000-4000-8000-000000000003', E'92f2cea9-3c0d-4e84-a104-170a6fb11203', E'100.00', E'2000.00', NULL, NULL, NULL, NULL, E'2026-07-19 11:34:14.5742', E'2026-07-19 11:34:14.5742'),
    (E'956c35e9-29d0-4b7b-8f5e-39f8243b539a', E'69966311-3bbb-4165-b266-cf794ef8b99e', E'a1000000-0000-4000-8000-000000000003', E'0e99cdab-6e34-4d3a-a295-0a9d9e538ae1', E'100.00', E'8000.00', NULL, NULL, NULL, NULL, E'2026-07-19 11:34:14.591691', E'2026-07-19 11:34:14.591691');



--
-- TOC entry 5501 (class 0 OID 45718)
-- Dependencies: 237
-- Data for Name: submissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.submissions (2 rows)
INSERT INTO public.submissions (id, round_id, team_id, repo_url, demo_url, slide_url, report_url, api_metadata, submitted_at, updated_at, project_name, version, review_status, status) VALUES
    (E'cc3658ed-a72b-4b02-8942-a62c87eefc76', E'46dc99d9-691f-4bbb-bd1e-dc28f48e2c33', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'https://github.com/e2e/tail-64618', E'https://demo.example.com', E'https://example.com/slide', E'https://example.com/report', E'e2e-deep notes', E'2026-07-02 16:20:27.090629', E'2026-07-10 02:49:49.985313', NULL, NULL, E'pending', E'submitted'),
    (E'69966311-3bbb-4165-b266-cf794ef8b99e', E'1a0cef4f-3aae-4e78-8f75-09dd77767eac', E'75ff0336-137b-437c-9142-0b6e08afaf76', E'http://localhost:5173/team/submissions', E'http://localhost:5173/team/submissions', E'http://localhost:5173/team/submissions', E'http://localhost:5173/team/submissions', E'ád', E'2026-07-19 11:02:25.043825', E'2026-07-19 11:02:25.043825', NULL, NULL, NULL, E'draft');



--
-- TOC entry 5502 (class 0 OID 45726)
-- Dependencies: 238
-- Data for Name: support_tickets; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.support_tickets (2 rows)
INSERT INTO public.support_tickets (id, requester_id, category, priority, subject, description, status, created_at, updated_at) VALUES
    (E'e8b1bb75-2563-4201-908a-ae20a5726793', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'technical', E'low', E'Smoke ticket', E'support smoke', E'open', E'2026-07-02 17:11:24.132142', E'2026-07-02 17:11:24.132142'),
    (E'5afe23da-3b18-40d3-8fea-afae1e601334', E'83bbb9b4-c32e-4b55-aea4-eb3e294f71b1', E'technical', E'high', E'j', E'h', E'open', E'2026-07-19 10:27:49.236532', E'2026-07-19 10:27:49.236532');



--
-- TOC entry 5503 (class 0 OID 45735)
-- Dependencies: 239
-- Data for Name: team_chat_messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.team_chat_messages (3 rows)
INSERT INTO public.team_chat_messages (id, team_id, sender_id, message, created_at, updated_at) VALUES
    (E'0e90e5bb-38df-48b2-88b2-d320c28213fa', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'chat smoke', E'2026-07-02 17:11:24.094925', E'2026-07-02 17:11:24.094925'),
    (E'ca651c29-72d4-4223-a38f-861c2908576c', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'e2e-deep hello 49193', E'2026-07-03 17:20:49.236438', E'2026-07-03 17:20:49.236438'),
    (E'ddb3a80a-cca0-4294-9d12-75c16d0960c8', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'tail hello 54914', E'2026-07-03 17:40:54.941669', E'2026-07-03 17:40:54.941669');



--
-- TOC entry 5504 (class 0 OID 45743)
-- Dependencies: 240
-- Data for Name: team_join_requests; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.team_join_requests (1 rows)
INSERT INTO public.team_join_requests (id, team_id, user_id, status, message, created_at, responded_at) VALUES
    (E'abe8b077-9be8-44be-874f-d2b8636a630f', E'75ff0336-137b-437c-9142-0b6e08afaf76', E'4eb1d119-fe2e-48cf-ace8-f295984731f9', E'accepted', NULL, E'2026-07-19 10:48:40.509653', E'2026-07-19 10:52:33.877931');



--
-- TOC entry 5505 (class 0 OID 45752)
-- Dependencies: 241
-- Data for Name: team_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.team_members (9 rows)
INSERT INTO public.team_members (id, team_id, user_id, role, joined_at) VALUES
    (E'd5efffe7-e1ca-453d-b636-a88a53702951', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'leader', E'2026-07-02 16:20:27.063579'),
    (E'41950771-417b-4571-874f-9296829607bb', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'52e07429-90f0-4b2a-821e-77954881d2b1', E'member', E'2026-07-02 16:20:27.065781'),
    (E'5bf82639-b24b-459b-80cf-1437ea74baba', E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'450560ce-da8d-43f6-955d-f39d553b77c3', E'member', E'2026-07-02 16:20:27.067951'),
    (E'a2000000-0000-4000-8000-000000000001', E'aaaa0000-0000-4000-8000-000000000004', E'a1000000-0000-4000-8000-000000000004', E'leader', E'2026-07-10 02:49:49.985313'),
    (E'a2000000-0000-4000-8000-000000000002', E'aaaa0000-0000-4000-8000-000000000004', E'a1000000-0000-4000-8000-000000000005', E'member', E'2026-07-10 02:49:49.985313'),
    (E'205e4811-c3f9-4299-be8e-34674ac9b05c', E'75ff0336-137b-437c-9142-0b6e08afaf76', E'83bbb9b4-c32e-4b55-aea4-eb3e294f71b1', E'leader', E'2026-07-19 10:26:00.338753'),
    (E'0f274899-3521-4d2f-9fa2-4b321cf74b34', E'2431c70e-5860-4536-9f99-34aabbafa6e7', E'ac5dc174-c461-4b2b-aff3-c8a9acd02f8f', E'leader', E'2026-07-19 10:41:04.931057'),
    (E'074f33f0-3567-4cb4-bb0a-e323c9d4bfeb', E'2431c70e-5860-4536-9f99-34aabbafa6e7', E'bec002b0-8bfd-4798-bac8-f2cb35e96b48', E'member', E'2026-07-19 10:48:58.337775'),
    (E'eb33bb42-2045-4764-96d9-bf06b734715f', E'75ff0336-137b-437c-9142-0b6e08afaf76', E'4eb1d119-fe2e-48cf-ace8-f295984731f9', E'member', E'2026-07-19 10:52:33.878965');



--
-- TOC entry 5513 (class 0 OID 46278)
-- Dependencies: 251
-- Data for Name: team_timeline_events; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.team_timeline_events (0 rows)
-- No rows to insert.



--
-- TOC entry 5506 (class 0 OID 45758)
-- Dependencies: 242
-- Data for Name: teams; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.teams (4 rows)
INSERT INTO public.teams (id, track_id, name, status, disqualified_reason, created_at, updated_at, invite_code) VALUES
    (E'5f12db5b-b46e-440b-98e4-bc6b0b592d74', E'e18a5239-caf9-4997-8c11-e0bea2ca0f6e', E'Regression Team 232026', E'active', NULL, E'2026-07-02 16:20:27.057818', E'2026-07-04 11:26:57.04209', E'3C865E'),
    (E'aaaa0000-0000-4000-8000-000000000004', E'aaaa0000-0000-4000-8000-000000000002', E'DEMO Team', E'active', NULL, E'2026-07-10 02:49:49.985313', NULL, E'DEMO2026'),
    (E'75ff0336-137b-437c-9142-0b6e08afaf76', E'5ff86b4c-12b5-4701-bc24-c5ab4cea0307', E'Vô địch', E'active', NULL, E'2026-07-19 10:26:00.328651', E'2026-07-19 18:01:16.078247', E'8B51D9'),
    (E'2431c70e-5860-4536-9f99-34aabbafa6e7', E'5ff86b4c-12b5-4701-bc24-c5ab4cea0307', E'i', E'active', NULL, E'2026-07-19 10:41:04.92559', E'2026-07-19 18:01:19.766555', E'8C664B');



--
-- TOC entry 5515 (class 0 OID 46342)
-- Dependencies: 253
-- Data for Name: tie_break_decisions; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.tie_break_decisions (0 rows)
-- No rows to insert.



--
-- TOC entry 5507 (class 0 OID 45766)
-- Dependencies: 243
-- Data for Name: track_judges; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.track_judges (1 rows)
INSERT INTO public.track_judges (id, event_id, track_id, user_id, assigned_at) VALUES
    (E'd616d657-663d-4315-9414-8efbd232b3ce', E'a6ec61f7-2f7e-4d77-86d5-861a62fc060c', E'2421b5c6-9184-4176-b9b0-a253093b04b3', E'a1000000-0000-4000-8000-000000000003', E'2026-07-19 11:32:22.28312');



--
-- TOC entry 5508 (class 0 OID 45771)
-- Dependencies: 244
-- Data for Name: track_mentors; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.track_mentors (2 rows)
INSERT INTO public.track_mentors (id, event_id, track_id, user_id, assigned_at) VALUES
    (E'a3000000-0000-4000-8000-000000000001', E'aaaa0000-0000-4000-8000-000000000001', E'aaaa0000-0000-4000-8000-000000000002', E'a1000000-0000-4000-8000-000000000002', E'2026-07-10 02:49:49.985313'),
    (E'89100020-ed88-49df-b342-8eb12ed1ee20', E'5c133817-ddd1-49ff-ae1b-98d366b950e4', E'5ff86b4c-12b5-4701-bc24-c5ab4cea0307', E'7ad640f5-95a3-496b-b2a6-c5a15bcca511', E'2026-07-19 11:02:45.384208');



--
-- TOC entry 5509 (class 0 OID 45776)
-- Dependencies: 245
-- Data for Name: tracks; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.tracks (15 rows)
INSERT INTO public.tracks (id, event_id, name, description, created_at, updated_at, max_teams) VALUES
    (E'082553c7-dbed-4cb2-9629-0401e9fd3a7d', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'Regression Track 232026', E'track smoke', E'2026-07-02 16:20:26.434699', E'2026-07-02 16:20:26.434699', NULL),
    (E'118d1506-a833-4a9f-877f-58b3bf4b3772', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'AI', E'hihi', E'2026-07-02 17:42:42.759141', E'2026-07-02 17:42:42.759141', NULL),
    (E'e18a5239-caf9-4997-8c11-e0bea2ca0f6e', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'Track B', E'', E'2026-07-03 17:19:59.216462', E'2026-07-03 17:19:59.216462', NULL),
    (E'43e9e14d-0aaa-4d66-a75c-5a8575d386a1', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'E2ETrack97205', E'', E'2026-07-03 17:19:59.216462', E'2026-07-03 17:19:59.216462', NULL),
    (E'cf4b4453-06e5-482f-b2a3-6a9a06ef7626', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'E2ETrack799', E'', E'2026-07-03 17:23:22.841017', E'2026-07-03 17:23:22.841017', NULL),
    (E'00fbb79f-0891-49d4-b98f-4de4731a7797', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'E2ETrack63357', E'', E'2026-07-03 17:27:45.361287', E'2026-07-03 17:27:45.361287', NULL),
    (E'5f8a2671-e573-4efa-b565-0ef8d54f1bcd', E'9b42520a-bce1-45ac-a165-73aa556d8c82', E'E2ETrack49308', E'', E'2026-07-03 17:32:31.315856', E'2026-07-03 17:32:31.315856', NULL),
    (E'd892f925-3347-4f5e-890b-dc7ca7558e33', E'a6ec61f7-2f7e-4d77-86d5-861a62fc060c', E'Track C', E'', E'2026-07-03 18:05:46.300584', E'2026-07-03 18:05:46.300584', NULL),
    (E'723f83f1-3b0f-4197-9206-b576562963a7', E'a6ec61f7-2f7e-4d77-86d5-861a62fc060c', E'Track A', E'', E'2026-07-03 18:05:46.300584', E'2026-07-03 18:05:46.300584', NULL),
    (E'4d95ca60-30fa-4603-8f56-b0290da7829a', E'a6ec61f7-2f7e-4d77-86d5-861a62fc060c', E'Track B', E'', E'2026-07-03 18:05:46.300584', E'2026-07-03 18:05:46.300584', NULL),
    (E'2421b5c6-9184-4176-b9b0-a253093b04b3', E'a6ec61f7-2f7e-4d77-86d5-861a62fc060c', E'General', E'Default track for registered teams', E'2026-07-04 01:37:26.358939', E'2026-07-04 01:37:26.358939', NULL),
    (E'aaaa0000-0000-4000-8000-000000000002', E'aaaa0000-0000-4000-8000-000000000001', E'DEMO Track', E'Seed track for manual testing.', E'2026-07-10 02:49:49.985313', NULL, E'20'),
    (E'5ff86b4c-12b5-4701-bc24-c5ab4cea0307', E'5c133817-ddd1-49ff-ae1b-98d366b950e4', E'General', E'Default track for registered teams', E'2026-07-19 09:50:03.271885', E'2026-07-19 09:50:03.271885', NULL),
    (E'2a932912-a3dd-4fce-b474-23a931c0422b', E'5c133817-ddd1-49ff-ae1b-98d366b950e4', E'Track A', E'', E'2026-07-19 10:57:20.528962', E'2026-07-19 10:57:20.528962', NULL),
    (E'69dc7792-fbef-4a9a-b01a-8faa37159db9', E'5c133817-ddd1-49ff-ae1b-98d366b950e4', E'Track B', E'', E'2026-07-19 10:57:20.528962', E'2026-07-19 10:57:20.528962', NULL);



--
-- TOC entry 5510 (class 0 OID 45783)
-- Dependencies: 246
-- Data for Name: universities; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.universities (5 rows)
INSERT INTO public.universities (id, name, short_name, country, created_at, updated_at) VALUES
    (E'11111111-1111-1111-1111-111111111111', E'FPT University', E'FPTU', E'Vietnam', E'2026-07-02 14:13:12.122329', NULL),
    (E'0053bd3d-ad1e-4210-81f2-c3cdede05543', E'HCMUT', NULL, E'Vietnam', E'2026-07-02 07:29:33.968043', E'2026-07-02 07:29:33.968043'),
    (E'b43b3920-a3cf-4766-a72a-c8a34a08ba4f', E'Regression University', NULL, E'Vietnam', E'2026-07-02 16:20:26.488934', E'2026-07-02 16:20:26.488934'),
    (E'dc307b88-d085-4d87-a4fd-47a502e5555e', E'UIT', NULL, E'Vietnam', E'2026-07-04 04:04:34.156496', E'2026-07-04 04:04:34.156496'),
    (E'0e2ce1a2-849a-4917-930e-cfaaac9e5342', E'jjj', NULL, E'Vietnam', E'2026-07-19 10:40:26.152499', E'2026-07-19 10:40:26.152499');



--
-- TOC entry 5511 (class 0 OID 45788)
-- Dependencies: 247
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.user_roles (20 rows)
INSERT INTO public.user_roles (user_id, role_id, assigned_at) VALUES
    (E'5b1dada5-bd1a-4f5c-9a57-b4d4e21f7a40', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-02 14:15:32.422374'),
    (E'52b75aa6-317a-44fa-9115-63a40b604265', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-02 14:29:33.952387'),
    (E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'28c09a8e-65bd-44b9-8bf2-fa999efbf8d1', E'2026-07-02 14:47:36.439192'),
    (E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-02 14:47:36.439192'),
    (E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'e9e4426a-4b07-4969-a24d-54e7440cd964', E'2026-07-02 14:47:36.439192'),
    (E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'ed0ca372-ef82-4f26-bf88-474156fd7d67', E'2026-07-02 14:47:36.439192'),
    (E'52e07429-90f0-4b2a-821e-77954881d2b1', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-02 23:20:26.487373'),
    (E'450560ce-da8d-43f6-955d-f39d553b77c3', E'28c09a8e-65bd-44b9-8bf2-fa999efbf8d1', E'2026-07-03 00:44:10.076935'),
    (E'450560ce-da8d-43f6-955d-f39d553b77c3', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-03 00:44:10.076935'),
    (E'a1000000-0000-4000-8000-000000000001', E'e9e4426a-4b07-4969-a24d-54e7440cd964', E'2026-07-10 02:49:49.985313'),
    (E'a1000000-0000-4000-8000-000000000002', E'28c09a8e-65bd-44b9-8bf2-fa999efbf8d1', E'2026-07-10 02:49:49.985313'),
    (E'a1000000-0000-4000-8000-000000000003', E'ed0ca372-ef82-4f26-bf88-474156fd7d67', E'2026-07-10 02:49:49.985313'),
    (E'a1000000-0000-4000-8000-000000000004', E'6f4f98b4-aff1-42df-801b-11f61e93db48', E'2026-07-10 02:49:49.985313'),
    (E'a1000000-0000-4000-8000-000000000005', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-10 02:49:49.985313'),
    (E'f301a137-0715-41dd-a81a-380035eb808e', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-10 02:55:09.773022'),
    (E'7ad640f5-95a3-496b-b2a6-c5a15bcca511', E'28c09a8e-65bd-44b9-8bf2-fa999efbf8d1', E'2026-07-19 16:58:42.98234'),
    (E'83bbb9b4-c32e-4b55-aea4-eb3e294f71b1', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-19 17:23:06.93011'),
    (E'ac5dc174-c461-4b2b-aff3-c8a9acd02f8f', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-19 17:40:26.149291'),
    (E'4eb1d119-fe2e-48cf-ace8-f295984731f9', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-19 17:47:04.633799'),
    (E'bec002b0-8bfd-4798-bac8-f2cb35e96b48', E'5afe0808-f611-48b4-82b6-49383a50efb4', E'2026-07-19 17:47:23.699755');



--
-- TOC entry 5512 (class 0 OID 45792)
-- Dependencies: 248
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- Converted from COPY: public.users (16 rows)
INSERT INTO public.users (id, email, password_hash, full_name, student_type, student_id, campus_id, is_guest, status, created_at, updated_at, university_id, phone, department, "position", company, expertise, bio) VALUES
    (E'5b1dada5-bd1a-4f5c-9a57-b4d4e21f7a40', E'fpt_test_1782976532@seal.local', E'$2a$12$4UHjTacjDTNNfJhmJo5YteEJVi/z.AmbGxjPNOqM0kOCLOnsYpYSW', E'FPT Test', E'fpt', E'SE999999', E'22222222-2222-2222-2222-222222222221', E'f', E'pending', E'2026-07-02 07:15:32.700343', E'2026-07-02 14:26:13.478042', E'11111111-1111-1111-1111-111111111111', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'52b75aa6-317a-44fa-9115-63a40b604265', E'external_test_1782977373@seal.local', E'$2a$12$xPd8CK03SDeRQ3SxBXK7Ie7Y1.dhfUTgHFW7vFH5Nhk4UkTOe7jHq', E'External Test', E'external', NULL, NULL, E'f', E'pending', E'2026-07-02 07:29:34.276928', E'2026-07-02 07:29:34.276928', E'0053bd3d-ad1e-4210-81f2-c3cdede05543', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'52e07429-90f0-4b2a-821e-77954881d2b1', E'reg232026_0@seal.local', E'$2a$12$nSYzrf6pV/OsqjXuusFX2eSOJIOeg6XmE/7.XkOJTbH7sQeEAKddW', E'Reg Member 0', E'external', NULL, NULL, E'f', E'approved', E'2026-07-02 16:20:26.734573', E'2026-07-02 23:20:26.751487', E'b43b3920-a3cf-4766-a72a-c8a34a08ba4f', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'450560ce-da8d-43f6-955d-f39d553b77c3', E'reg232026_1@seal.local', E'$2a$12$hQysaABwYCsqMrVFNOIVPeTa3kTXbmS5qWNdTjl/3A8wGMO4scdv.', E'Reg Member 1', E'external', NULL, NULL, E'f', E'approved', E'2026-07-02 16:20:27.021084', E'2026-07-03 00:44:10.076935', E'b43b3920-a3cf-4766-a72a-c8a34a08ba4f', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'b9debf4b-c200-4907-abdf-5ce2208f8eb7', E'coordinator@seal.local', E'$2a$12$NzoScH178DBAczXea4Pl5OYYVjwjCE952.Jx4qWGJVhqw77aszGZK', E'SmokeDept97972', E'none', NULL, NULL, E'f', E'approved', E'2026-07-02 06:48:04.38247', E'2026-07-04 00:33:16.172055', NULL, E'099996098', E'E2EDept96098', E'Coordinator', NULL, NULL, NULL),
    (E'a1000000-0000-4000-8000-000000000001', E'demo.ec@seal.local', E'$2a$12$lbyENCECafzqrU3En/KFHuazqdU/NWwLVtlME.hV5reCdFxuBd45S', E'Demo Coordinator', E'none', NULL, NULL, E'f', E'approved', E'2026-07-10 02:49:49.985313', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (E'a1000000-0000-4000-8000-000000000002', E'demo.mentor@seal.local', E'$2a$12$lbyENCECafzqrU3En/KFHuazqdU/NWwLVtlME.hV5reCdFxuBd45S', E'Demo Mentor', E'none', NULL, NULL, E'f', E'approved', E'2026-07-10 02:49:49.985313', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (E'a1000000-0000-4000-8000-000000000004', E'demo.leader@seal.local', E'$2a$12$lbyENCECafzqrU3En/KFHuazqdU/NWwLVtlME.hV5reCdFxuBd45S', E'Demo Team Leader', E'fpt', E'SE100004', NULL, E'f', E'approved', E'2026-07-10 02:49:49.985313', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (E'a1000000-0000-4000-8000-000000000005', E'demo.member@seal.local', E'$2a$12$lbyENCECafzqrU3En/KFHuazqdU/NWwLVtlME.hV5reCdFxuBd45S', E'Demo Team Member', E'fpt', E'SE100005', NULL, E'f', E'approved', E'2026-07-10 02:49:49.985313', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (E'f301a137-0715-41dd-a81a-380035eb808e', E'ji@gmail.com', E'$2a$12$EmOqrdjGz9kpVeK3WCq4yOAZxa3Q7ksTHA3SDZYUUfrn/KkRQuyIe', E'ji', E'fpt', E'SE490843', E'22222222-2222-2222-2222-222222222222', E'f', E'pending', E'2026-07-09 19:55:10.072639', E'2026-07-09 19:55:10.072639', E'11111111-1111-1111-1111-111111111111', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'7ad640f5-95a3-496b-b2a6-c5a15bcca511', E'triet@gmail.com', E'$2a$12$XEdedp6RspLnoSECUSQtm.oO07JNoDfSAuXQhlJnfG69Q7CmlJXWq', E'Dinh Minh Triet', E'none', NULL, NULL, E'f', E'approved', E'2026-07-19 09:58:43.228442', E'2026-07-19 09:58:43.228442', NULL, NULL, NULL, NULL, E'FPT', E'Java expert', E'expert in java and AI'),
    (E'83bbb9b4-c32e-4b55-aea4-eb3e294f71b1', E'gg@gmail.com', E'$2a$12$rH0h4fZjOiuE4E2iJpG9Z.HB/XKx7ycl9HXoLF8j8Y99LA1dwwCJG', E'Dinh triet', E'fpt', E'SE333333', E'22222222-2222-2222-2222-222222222221', E'f', E'approved', E'2026-07-19 10:23:07.176089', E'2026-07-19 17:25:22.595925', E'11111111-1111-1111-1111-111111111111', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'ac5dc174-c461-4b2b-aff3-c8a9acd02f8f', E'jj@gmail.com', E'$2a$12$XjbWb.H/58e7wnHp6Zafj.NYBPSDM5k5mBRKWjnEiHRyZC5yXOpTy', E'ji', E'external', NULL, NULL, E'f', E'approved', E'2026-07-19 10:40:26.39612', E'2026-07-19 17:40:40.17378', E'0e2ce1a2-849a-4917-930e-cfaaac9e5342', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'4eb1d119-fe2e-48cf-ace8-f295984731f9', E'a@gmail.com', E'$2a$12$O22GyCA7seOCmY6MwxSVzek2BOYRtzENrq.HghiuVohtCAeP5zBxS', E'g', E'fpt', E's', E'22222222-2222-2222-2222-222222222221', E'f', E'approved', E'2026-07-19 10:47:04.887516', E'2026-07-19 17:48:00.896282', E'11111111-1111-1111-1111-111111111111', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'bec002b0-8bfd-4798-bac8-f2cb35e96b48', E'v@gmail.com', E'$2a$12$Iyr6oNoZ2czmfSevpb63buOWRMv/phtYLac4ss8xhRJhTTD.7.QE2', E'v', E'fpt', E'g5', E'22222222-2222-2222-2222-222222222225', E'f', E'approved', E'2026-07-19 10:47:23.945004', E'2026-07-19 17:48:01.52001', E'11111111-1111-1111-1111-111111111111', NULL, NULL, NULL, NULL, NULL, NULL),
    (E'a1000000-0000-4000-8000-000000000003', E'demo.judge@seal.local', E'$2a$12$lbyENCECafzqrU3En/KFHuazqdU/NWwLVtlME.hV5reCdFxuBd45S', E'Triet', E'none', NULL, NULL, E'f', E'approved', E'2026-07-10 02:49:49.985313', E'2026-07-19 18:31:50.502446', NULL, NULL, NULL, NULL, NULL, NULL, NULL);



--
-- TOC entry 5224 (class 2606 OID 46313)
-- Name: appeals appeals_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_pkey PRIMARY KEY (id);


--
-- TOC entry 5066 (class 2606 OID 45813)
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 5072 (class 2606 OID 45815)
-- Name: campuses campuses_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT campuses_pkey PRIMARY KEY (id);


--
-- TOC entry 5076 (class 2606 OID 45817)
-- Name: criteria_templates criteria_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.criteria_templates
    ADD CONSTRAINT criteria_templates_pkey PRIMARY KEY (id);


--
-- TOC entry 5233 (class 2606 OID 46378)
-- Name: event_rules event_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_rules
    ADD CONSTRAINT event_rules_pkey PRIMARY KEY (id);


--
-- TOC entry 5078 (class 2606 OID 45819)
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- TOC entry 5080 (class 2606 OID 45821)
-- Name: incident_actions incident_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_pkey PRIMARY KEY (id);


--
-- TOC entry 5082 (class 2606 OID 45823)
-- Name: incident_evidences incident_evidences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_pkey PRIMARY KEY (id);


--
-- TOC entry 5088 (class 2606 OID 45825)
-- Name: incident_reports incident_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_pkey PRIMARY KEY (id);


--
-- TOC entry 5090 (class 2606 OID 45827)
-- Name: mentor_feedbacks mentor_feedbacks_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_pkey PRIMARY KEY (id);


--
-- TOC entry 5096 (class 2606 OID 45829)
-- Name: notices notices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_pkey PRIMARY KEY (id);


--
-- TOC entry 5100 (class 2606 OID 45831)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 5241 (class 2606 OID 46412)
-- Name: prize_revisions prize_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_pkey PRIMARY KEY (id);


--
-- TOC entry 5102 (class 2606 OID 45833)
-- Name: prizes prizes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_pkey PRIMARY KEY (id);


--
-- TOC entry 5106 (class 2606 OID 45835)
-- Name: revoked_tokens revoked_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 5108 (class 2606 OID 45837)
-- Name: revoked_tokens revoked_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_token_hash_key UNIQUE (token_hash);


--
-- TOC entry 5110 (class 2606 OID 45839)
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- TOC entry 5112 (class 2606 OID 45841)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- TOC entry 5115 (class 2606 OID 45843)
-- Name: round_criteria round_criteria_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_pkey PRIMARY KEY (id);


--
-- TOC entry 5121 (class 2606 OID 45845)
-- Name: round_judges round_judges_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_pkey PRIMARY KEY (id);


--
-- TOC entry 5128 (class 2606 OID 45847)
-- Name: round_participants round_participants_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_pkey PRIMARY KEY (id);


--
-- TOC entry 5135 (class 2606 OID 45849)
-- Name: round_rankings round_rankings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_pkey PRIMARY KEY (id);


--
-- TOC entry 5142 (class 2606 OID 45851)
-- Name: rounds rounds_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT rounds_pkey PRIMARY KEY (id);


--
-- TOC entry 5236 (class 2606 OID 46391)
-- Name: rule_acceptances rule_acceptances_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_pkey PRIMARY KEY (id);


--
-- TOC entry 5151 (class 2606 OID 45853)
-- Name: scores scores_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_pkey PRIMARY KEY (id);


--
-- TOC entry 5157 (class 2606 OID 45855)
-- Name: submissions submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_pkey PRIMARY KEY (id);


--
-- TOC entry 5162 (class 2606 OID 45857)
-- Name: support_tickets support_tickets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT support_tickets_pkey PRIMARY KEY (id);


--
-- TOC entry 5165 (class 2606 OID 45859)
-- Name: team_chat_messages team_chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_pkey PRIMARY KEY (id);


--
-- TOC entry 5169 (class 2606 OID 45861)
-- Name: team_join_requests team_join_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_pkey PRIMARY KEY (id);


--
-- TOC entry 5174 (class 2606 OID 45863)
-- Name: team_members team_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_pkey PRIMARY KEY (id);


--
-- TOC entry 5222 (class 2606 OID 46286)
-- Name: team_timeline_events team_timeline_events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_pkey PRIMARY KEY (id);


--
-- TOC entry 5180 (class 2606 OID 45865)
-- Name: teams teams_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);


--
-- TOC entry 5229 (class 2606 OID 46350)
-- Name: tie_break_decisions tie_break_decisions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_pkey PRIMARY KEY (id);


--
-- TOC entry 5187 (class 2606 OID 45867)
-- Name: track_judges track_judges_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_pkey PRIMARY KEY (id);


--
-- TOC entry 5195 (class 2606 OID 45869)
-- Name: track_mentors track_mentors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_pkey PRIMARY KEY (id);


--
-- TOC entry 5202 (class 2606 OID 45871)
-- Name: tracks tracks_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT tracks_pkey PRIMARY KEY (id);


--
-- TOC entry 5206 (class 2606 OID 45873)
-- Name: universities universities_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.universities
    ADD CONSTRAINT universities_name_key UNIQUE (name);


--
-- TOC entry 5208 (class 2606 OID 45875)
-- Name: universities universities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.universities
    ADD CONSTRAINT universities_pkey PRIMARY KEY (id);


--
-- TOC entry 5074 (class 2606 OID 45877)
-- Name: campuses uq_campuses_university_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT uq_campuses_university_name UNIQUE (university_id, name);


--
-- TOC entry 5117 (class 2606 OID 45879)
-- Name: round_criteria uq_round_criteria_round_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT uq_round_criteria_round_name UNIQUE (round_id, name);


--
-- TOC entry 5123 (class 2606 OID 45881)
-- Name: round_judges uq_round_judges_round_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT uq_round_judges_round_user UNIQUE (round_id, user_id);


--
-- TOC entry 5130 (class 2606 OID 45883)
-- Name: round_participants uq_round_participants_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT uq_round_participants_round_team UNIQUE (round_id, team_id);


--
-- TOC entry 5137 (class 2606 OID 45885)
-- Name: round_rankings uq_round_rankings_round_rank; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT uq_round_rankings_round_rank UNIQUE (round_id, rank);


--
-- TOC entry 5139 (class 2606 OID 45887)
-- Name: round_rankings uq_round_rankings_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT uq_round_rankings_round_team UNIQUE (round_id, team_id);


--
-- TOC entry 5144 (class 2606 OID 45889)
-- Name: rounds uq_rounds_track_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT uq_rounds_track_name UNIQUE (track_id, name);


--
-- TOC entry 5146 (class 2606 OID 45891)
-- Name: rounds uq_rounds_track_sequence; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT uq_rounds_track_sequence UNIQUE (track_id, sequence_number);


--
-- TOC entry 5238 (class 2606 OID 46393)
-- Name: rule_acceptances uq_rule_acceptances_user_event; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT uq_rule_acceptances_user_event UNIQUE (user_id, event_id);


--
-- TOC entry 5153 (class 2606 OID 45893)
-- Name: scores uq_scores_submission_judge_criterion; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT uq_scores_submission_judge_criterion UNIQUE (submission_id, judge_id, criterion_id);


--
-- TOC entry 5159 (class 2606 OID 45895)
-- Name: submissions uq_submissions_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT uq_submissions_round_team UNIQUE (round_id, team_id);


--
-- TOC entry 5176 (class 2606 OID 45897)
-- Name: team_members uq_team_members_team_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id);


--
-- TOC entry 5183 (class 2606 OID 45899)
-- Name: teams uq_teams_track_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT uq_teams_track_name UNIQUE (track_id, name);


--
-- TOC entry 5231 (class 2606 OID 46352)
-- Name: tie_break_decisions uq_tie_break_decisions_round_team; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT uq_tie_break_decisions_round_team UNIQUE (round_id, team_id);


--
-- TOC entry 5189 (class 2606 OID 45901)
-- Name: track_judges uq_track_judges_event_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT uq_track_judges_event_user UNIQUE (event_id, user_id);


--
-- TOC entry 5191 (class 2606 OID 45903)
-- Name: track_judges uq_track_judges_track_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT uq_track_judges_track_user UNIQUE (track_id, user_id);


--
-- TOC entry 5197 (class 2606 OID 45905)
-- Name: track_mentors uq_track_mentors_event_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT uq_track_mentors_event_user UNIQUE (event_id, user_id);


--
-- TOC entry 5199 (class 2606 OID 45907)
-- Name: track_mentors uq_track_mentors_track_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT uq_track_mentors_track_user UNIQUE (track_id, user_id);


--
-- TOC entry 5204 (class 2606 OID 45909)
-- Name: tracks uq_tracks_event_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT uq_tracks_event_name UNIQUE (event_id, name);


--
-- TOC entry 5210 (class 2606 OID 45911)
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- TOC entry 5216 (class 2606 OID 45913)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 5218 (class 2606 OID 45915)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 5225 (class 1259 OID 46339)
-- Name: idx_appeals_round; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_round ON public.appeals USING btree (round_id);


--
-- TOC entry 5226 (class 1259 OID 46341)
-- Name: idx_appeals_round_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_round_status ON public.appeals USING btree (round_id, status);


--
-- TOC entry 5227 (class 1259 OID 46340)
-- Name: idx_appeals_team; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_appeals_team ON public.appeals USING btree (team_id);


--
-- TOC entry 5067 (class 1259 OID 45916)
-- Name: idx_audit_logs_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_action ON public.audit_logs USING btree (action);


--
-- TOC entry 5068 (class 1259 OID 45917)
-- Name: idx_audit_logs_incident_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_incident_id ON public.audit_logs USING btree (incident_id);


--
-- TOC entry 5069 (class 1259 OID 45918)
-- Name: idx_audit_logs_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_team_id ON public.audit_logs USING btree (team_id);


--
-- TOC entry 5070 (class 1259 OID 45919)
-- Name: idx_audit_logs_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_user_id ON public.audit_logs USING btree (user_id);


--
-- TOC entry 5234 (class 1259 OID 46384)
-- Name: idx_event_rules_event; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_event_rules_event ON public.event_rules USING btree (event_id, visibility);


--
-- TOC entry 5083 (class 1259 OID 45920)
-- Name: idx_incident_reports_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_event_id ON public.incident_reports USING btree (event_id);


--
-- TOC entry 5084 (class 1259 OID 45921)
-- Name: idx_incident_reports_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_status ON public.incident_reports USING btree (status);


--
-- TOC entry 5085 (class 1259 OID 45922)
-- Name: idx_incident_reports_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_team_id ON public.incident_reports USING btree (team_id);


--
-- TOC entry 5086 (class 1259 OID 45923)
-- Name: idx_incident_reports_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_reports_type ON public.incident_reports USING btree (type);


--
-- TOC entry 5091 (class 1259 OID 45924)
-- Name: idx_notices_author_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_author_id ON public.notices USING btree (author_id);


--
-- TOC entry 5092 (class 1259 OID 45925)
-- Name: idx_notices_target_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_event_id ON public.notices USING btree (target_event_id);


--
-- TOC entry 5093 (class 1259 OID 45926)
-- Name: idx_notices_target_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_role ON public.notices USING btree (target_role);


--
-- TOC entry 5094 (class 1259 OID 45927)
-- Name: idx_notices_target_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notices_target_track_id ON public.notices USING btree (target_track_id);


--
-- TOC entry 5097 (class 1259 OID 45928)
-- Name: idx_notifications_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user ON public.notifications USING btree (user_id);


--
-- TOC entry 5098 (class 1259 OID 45929)
-- Name: idx_notifications_user_unread; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user_unread ON public.notifications USING btree (user_id, category) WHERE (read_at IS NULL);


--
-- TOC entry 5239 (class 1259 OID 46433)
-- Name: idx_prize_revisions_prize; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prize_revisions_prize ON public.prize_revisions USING btree (prize_id, changed_at DESC);


--
-- TOC entry 5103 (class 1259 OID 45930)
-- Name: idx_revoked_tokens_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_revoked_tokens_expires_at ON public.revoked_tokens USING btree (expires_at);


--
-- TOC entry 5104 (class 1259 OID 45931)
-- Name: idx_revoked_tokens_token_hash; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_revoked_tokens_token_hash ON public.revoked_tokens USING btree (token_hash);


--
-- TOC entry 5113 (class 1259 OID 45932)
-- Name: idx_round_criteria_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_criteria_round_id ON public.round_criteria USING btree (round_id);


--
-- TOC entry 5118 (class 1259 OID 45933)
-- Name: idx_round_judges_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_judges_round_id ON public.round_judges USING btree (round_id);


--
-- TOC entry 5119 (class 1259 OID 45934)
-- Name: idx_round_judges_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_judges_user_id ON public.round_judges USING btree (user_id);


--
-- TOC entry 5124 (class 1259 OID 45935)
-- Name: idx_round_participants_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_round_id ON public.round_participants USING btree (round_id);


--
-- TOC entry 5125 (class 1259 OID 45936)
-- Name: idx_round_participants_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_status ON public.round_participants USING btree (status);


--
-- TOC entry 5126 (class 1259 OID 45937)
-- Name: idx_round_participants_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_participants_team_id ON public.round_participants USING btree (team_id);


--
-- TOC entry 5131 (class 1259 OID 45938)
-- Name: idx_round_rankings_rank; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_rank ON public.round_rankings USING btree (rank);


--
-- TOC entry 5132 (class 1259 OID 45939)
-- Name: idx_round_rankings_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_round_id ON public.round_rankings USING btree (round_id);


--
-- TOC entry 5133 (class 1259 OID 45940)
-- Name: idx_round_rankings_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_round_rankings_team_id ON public.round_rankings USING btree (team_id);


--
-- TOC entry 5140 (class 1259 OID 45941)
-- Name: idx_rounds_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_rounds_track_id ON public.rounds USING btree (track_id);


--
-- TOC entry 5147 (class 1259 OID 45942)
-- Name: idx_scores_criterion_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_criterion_id ON public.scores USING btree (criterion_id);


--
-- TOC entry 5148 (class 1259 OID 45943)
-- Name: idx_scores_judge_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_judge_id ON public.scores USING btree (judge_id);


--
-- TOC entry 5149 (class 1259 OID 45944)
-- Name: idx_scores_submission_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scores_submission_id ON public.scores USING btree (submission_id);


--
-- TOC entry 5154 (class 1259 OID 45945)
-- Name: idx_submissions_round_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_submissions_round_id ON public.submissions USING btree (round_id);


--
-- TOC entry 5155 (class 1259 OID 45946)
-- Name: idx_submissions_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_submissions_team_id ON public.submissions USING btree (team_id);


--
-- TOC entry 5160 (class 1259 OID 45947)
-- Name: idx_support_tickets_requester_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_support_tickets_requester_id ON public.support_tickets USING btree (requester_id);


--
-- TOC entry 5163 (class 1259 OID 45948)
-- Name: idx_team_chat_messages_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_chat_messages_team_id ON public.team_chat_messages USING btree (team_id);


--
-- TOC entry 5171 (class 1259 OID 45949)
-- Name: idx_team_members_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_members_team_id ON public.team_members USING btree (team_id);


--
-- TOC entry 5172 (class 1259 OID 45950)
-- Name: idx_team_members_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_members_user_id ON public.team_members USING btree (user_id);


--
-- TOC entry 5219 (class 1259 OID 46303)
-- Name: idx_team_timeline_events_event; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_event ON public.team_timeline_events USING btree (event_id, occurred_at DESC);


--
-- TOC entry 5220 (class 1259 OID 46302)
-- Name: idx_team_timeline_events_team; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_team_timeline_events_team ON public.team_timeline_events USING btree (team_id, occurred_at DESC);


--
-- TOC entry 5177 (class 1259 OID 45951)
-- Name: idx_teams_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_status ON public.teams USING btree (status);


--
-- TOC entry 5178 (class 1259 OID 45952)
-- Name: idx_teams_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_track_id ON public.teams USING btree (track_id);


--
-- TOC entry 5166 (class 1259 OID 45953)
-- Name: idx_tjr_team_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tjr_team_id ON public.team_join_requests USING btree (team_id);


--
-- TOC entry 5167 (class 1259 OID 45954)
-- Name: idx_tjr_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tjr_user_id ON public.team_join_requests USING btree (user_id);


--
-- TOC entry 5184 (class 1259 OID 45955)
-- Name: idx_track_judges_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_judges_track_id ON public.track_judges USING btree (track_id);


--
-- TOC entry 5185 (class 1259 OID 45956)
-- Name: idx_track_judges_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_judges_user_id ON public.track_judges USING btree (user_id);


--
-- TOC entry 5192 (class 1259 OID 45957)
-- Name: idx_track_mentors_track_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_mentors_track_id ON public.track_mentors USING btree (track_id);


--
-- TOC entry 5193 (class 1259 OID 45958)
-- Name: idx_track_mentors_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_track_mentors_user_id ON public.track_mentors USING btree (user_id);


--
-- TOC entry 5200 (class 1259 OID 45959)
-- Name: idx_tracks_event_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tracks_event_id ON public.tracks USING btree (event_id);


--
-- TOC entry 5211 (class 1259 OID 45960)
-- Name: idx_users_campus_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_campus_id ON public.users USING btree (campus_id);


--
-- TOC entry 5212 (class 1259 OID 45961)
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- TOC entry 5213 (class 1259 OID 45962)
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- TOC entry 5214 (class 1259 OID 45963)
-- Name: idx_users_university_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_university_id ON public.users USING btree (university_id);


--
-- TOC entry 5181 (class 1259 OID 45964)
-- Name: uq_teams_invite_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_teams_invite_code ON public.teams USING btree (invite_code);


--
-- TOC entry 5170 (class 1259 OID 45965)
-- Name: uq_tjr_team_user_pending; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_tjr_team_user_pending ON public.team_join_requests USING btree (team_id, user_id) WHERE ((status)::text = 'pending'::text);


--
-- TOC entry 5319 (class 2620 OID 45966)
-- Name: campuses trg_campuses_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_campuses_updated_at BEFORE UPDATE ON public.campuses FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5320 (class 2620 OID 45967)
-- Name: criteria_templates trg_criteria_templates_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_criteria_templates_updated_at BEFORE UPDATE ON public.criteria_templates FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5321 (class 2620 OID 45968)
-- Name: events trg_events_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_events_updated_at BEFORE UPDATE ON public.events FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5322 (class 2620 OID 45969)
-- Name: incident_reports trg_incident_reports_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_incident_reports_updated_at BEFORE UPDATE ON public.incident_reports FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5323 (class 2620 OID 45970)
-- Name: mentor_feedbacks trg_mentor_feedbacks_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_mentor_feedbacks_updated_at BEFORE UPDATE ON public.mentor_feedbacks FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5324 (class 2620 OID 45971)
-- Name: prizes trg_prizes_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_prizes_updated_at BEFORE UPDATE ON public.prizes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5325 (class 2620 OID 45972)
-- Name: round_criteria trg_round_criteria_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_criteria_updated_at BEFORE UPDATE ON public.round_criteria FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5326 (class 2620 OID 45973)
-- Name: round_participants trg_round_participants_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_participants_updated_at BEFORE UPDATE ON public.round_participants FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5327 (class 2620 OID 45974)
-- Name: round_rankings trg_round_rankings_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_round_rankings_updated_at BEFORE UPDATE ON public.round_rankings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5328 (class 2620 OID 45975)
-- Name: rounds trg_rounds_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_rounds_updated_at BEFORE UPDATE ON public.rounds FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5329 (class 2620 OID 45976)
-- Name: scores trg_scores_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_scores_updated_at BEFORE UPDATE ON public.scores FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5330 (class 2620 OID 45977)
-- Name: submissions trg_submissions_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_submissions_updated_at BEFORE UPDATE ON public.submissions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5331 (class 2620 OID 45978)
-- Name: teams trg_teams_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_teams_updated_at BEFORE UPDATE ON public.teams FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5332 (class 2620 OID 45979)
-- Name: tracks trg_tracks_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_tracks_updated_at BEFORE UPDATE ON public.tracks FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5333 (class 2620 OID 45980)
-- Name: universities trg_universities_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_universities_updated_at BEFORE UPDATE ON public.universities FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5334 (class 2620 OID 45981)
-- Name: users trg_users_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5304 (class 2606 OID 46314)
-- Name: appeals appeals_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5305 (class 2606 OID 46334)
-- Name: appeals appeals_resolved_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_resolved_by_fkey FOREIGN KEY (resolved_by) REFERENCES public.users(id);


--
-- TOC entry 5306 (class 2606 OID 46319)
-- Name: appeals appeals_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5307 (class 2606 OID 46329)
-- Name: appeals appeals_submitted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_submitted_by_fkey FOREIGN KEY (submitted_by) REFERENCES public.users(id);


--
-- TOC entry 5308 (class 2606 OID 46324)
-- Name: appeals appeals_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5242 (class 2606 OID 45982)
-- Name: audit_logs audit_logs_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE SET NULL;


--
-- TOC entry 5243 (class 2606 OID 45987)
-- Name: audit_logs audit_logs_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5244 (class 2606 OID 45992)
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5245 (class 2606 OID 45997)
-- Name: campuses campuses_university_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.campuses
    ADD CONSTRAINT campuses_university_id_fkey FOREIGN KEY (university_id) REFERENCES public.universities(id) ON DELETE CASCADE;


--
-- TOC entry 5312 (class 2606 OID 46379)
-- Name: event_rules event_rules_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_rules
    ADD CONSTRAINT event_rules_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5246 (class 2606 OID 46002)
-- Name: incident_actions incident_actions_action_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_action_by_fkey FOREIGN KEY (action_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5247 (class 2606 OID 46007)
-- Name: incident_actions incident_actions_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_actions
    ADD CONSTRAINT incident_actions_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE CASCADE;


--
-- TOC entry 5248 (class 2606 OID 46012)
-- Name: incident_evidences incident_evidences_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incident_reports(id) ON DELETE CASCADE;


--
-- TOC entry 5249 (class 2606 OID 46017)
-- Name: incident_evidences incident_evidences_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_evidences
    ADD CONSTRAINT incident_evidences_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5250 (class 2606 OID 46022)
-- Name: incident_reports incident_reports_assigned_coordinator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_assigned_coordinator_id_fkey FOREIGN KEY (assigned_coordinator_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 5251 (class 2606 OID 46027)
-- Name: incident_reports incident_reports_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5252 (class 2606 OID 46032)
-- Name: incident_reports incident_reports_reporter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_reporter_id_fkey FOREIGN KEY (reporter_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5253 (class 2606 OID 46037)
-- Name: incident_reports incident_reports_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- TOC entry 5254 (class 2606 OID 46042)
-- Name: incident_reports incident_reports_submission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_submission_id_fkey FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE SET NULL;


--
-- TOC entry 5255 (class 2606 OID 46047)
-- Name: incident_reports incident_reports_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5256 (class 2606 OID 46052)
-- Name: incident_reports incident_reports_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_reports
    ADD CONSTRAINT incident_reports_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- TOC entry 5257 (class 2606 OID 46057)
-- Name: mentor_feedbacks mentor_feedbacks_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- TOC entry 5258 (class 2606 OID 46062)
-- Name: mentor_feedbacks mentor_feedbacks_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5259 (class 2606 OID 46067)
-- Name: mentor_feedbacks mentor_feedbacks_track_mentor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mentor_feedbacks
    ADD CONSTRAINT mentor_feedbacks_track_mentor_id_fkey FOREIGN KEY (track_mentor_id) REFERENCES public.track_mentors(id) ON DELETE CASCADE;


--
-- TOC entry 5260 (class 2606 OID 46072)
-- Name: notices notices_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5261 (class 2606 OID 46077)
-- Name: notices notices_target_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_target_event_id_fkey FOREIGN KEY (target_event_id) REFERENCES public.events(id) ON DELETE SET NULL;


--
-- TOC entry 5262 (class 2606 OID 46082)
-- Name: notices notices_target_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_target_track_id_fkey FOREIGN KEY (target_track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- TOC entry 5263 (class 2606 OID 46087)
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5315 (class 2606 OID 46428)
-- Name: prize_revisions prize_revisions_changed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES public.users(id);


--
-- TOC entry 5316 (class 2606 OID 46423)
-- Name: prize_revisions prize_revisions_new_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_new_team_id_fkey FOREIGN KEY (new_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5317 (class 2606 OID 46418)
-- Name: prize_revisions prize_revisions_old_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_old_team_id_fkey FOREIGN KEY (old_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5318 (class 2606 OID 46413)
-- Name: prize_revisions prize_revisions_prize_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prize_revisions
    ADD CONSTRAINT prize_revisions_prize_id_fkey FOREIGN KEY (prize_id) REFERENCES public.prizes(id) ON DELETE CASCADE;


--
-- TOC entry 5264 (class 2606 OID 46092)
-- Name: prizes prizes_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5265 (class 2606 OID 46097)
-- Name: prizes prizes_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- TOC entry 5266 (class 2606 OID 46102)
-- Name: prizes prizes_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prizes
    ADD CONSTRAINT prizes_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;


--
-- TOC entry 5267 (class 2606 OID 46107)
-- Name: round_criteria round_criteria_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5268 (class 2606 OID 46112)
-- Name: round_criteria round_criteria_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT round_criteria_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.criteria_templates(id) ON DELETE SET NULL;


--
-- TOC entry 5269 (class 2606 OID 46117)
-- Name: round_judges round_judges_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5270 (class 2606 OID 46122)
-- Name: round_judges round_judges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_judges
    ADD CONSTRAINT round_judges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5271 (class 2606 OID 46127)
-- Name: round_participants round_participants_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5272 (class 2606 OID 46132)
-- Name: round_participants round_participants_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_participants
    ADD CONSTRAINT round_participants_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5273 (class 2606 OID 46137)
-- Name: round_rankings round_rankings_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5274 (class 2606 OID 46142)
-- Name: round_rankings round_rankings_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5275 (class 2606 OID 46147)
-- Name: round_rankings round_rankings_tie_breaker_criterion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_rankings
    ADD CONSTRAINT round_rankings_tie_breaker_criterion_id_fkey FOREIGN KEY (tie_breaker_criterion_id) REFERENCES public.round_criteria(id) ON DELETE SET NULL;


--
-- TOC entry 5276 (class 2606 OID 46152)
-- Name: rounds rounds_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT rounds_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- TOC entry 5313 (class 2606 OID 46399)
-- Name: rule_acceptances rule_acceptances_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5314 (class 2606 OID 46394)
-- Name: rule_acceptances rule_acceptances_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_acceptances
    ADD CONSTRAINT rule_acceptances_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5277 (class 2606 OID 46157)
-- Name: scores scores_criterion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_criterion_id_fkey FOREIGN KEY (criterion_id) REFERENCES public.round_criteria(id) ON DELETE CASCADE;


--
-- TOC entry 5278 (class 2606 OID 46162)
-- Name: scores scores_judge_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_judge_id_fkey FOREIGN KEY (judge_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5279 (class 2606 OID 46167)
-- Name: scores scores_submission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scores
    ADD CONSTRAINT scores_submission_id_fkey FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE;


--
-- TOC entry 5280 (class 2606 OID 46172)
-- Name: submissions submissions_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5281 (class 2606 OID 46177)
-- Name: submissions submissions_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.submissions
    ADD CONSTRAINT submissions_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5282 (class 2606 OID 46182)
-- Name: support_tickets support_tickets_requester_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT support_tickets_requester_id_fkey FOREIGN KEY (requester_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5283 (class 2606 OID 46187)
-- Name: team_chat_messages team_chat_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5284 (class 2606 OID 46192)
-- Name: team_chat_messages team_chat_messages_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_chat_messages
    ADD CONSTRAINT team_chat_messages_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5285 (class 2606 OID 46197)
-- Name: team_join_requests team_join_requests_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5286 (class 2606 OID 46202)
-- Name: team_join_requests team_join_requests_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_join_requests
    ADD CONSTRAINT team_join_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5287 (class 2606 OID 46207)
-- Name: team_members team_members_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5288 (class 2606 OID 46212)
-- Name: team_members team_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_members
    ADD CONSTRAINT team_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5301 (class 2606 OID 46287)
-- Name: team_timeline_events team_timeline_events_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5302 (class 2606 OID 46297)
-- Name: team_timeline_events team_timeline_events_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE SET NULL;


--
-- TOC entry 5303 (class 2606 OID 46292)
-- Name: team_timeline_events team_timeline_events_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.team_timeline_events
    ADD CONSTRAINT team_timeline_events_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5289 (class 2606 OID 46217)
-- Name: teams teams_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- TOC entry 5309 (class 2606 OID 46363)
-- Name: tie_break_decisions tie_break_decisions_decided_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_decided_by_fkey FOREIGN KEY (decided_by) REFERENCES public.users(id);


--
-- TOC entry 5310 (class 2606 OID 46353)
-- Name: tie_break_decisions tie_break_decisions_round_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_round_id_fkey FOREIGN KEY (round_id) REFERENCES public.rounds(id) ON DELETE CASCADE;


--
-- TOC entry 5311 (class 2606 OID 46358)
-- Name: tie_break_decisions tie_break_decisions_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tie_break_decisions
    ADD CONSTRAINT tie_break_decisions_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE CASCADE;


--
-- TOC entry 5290 (class 2606 OID 46222)
-- Name: track_judges track_judges_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5291 (class 2606 OID 46227)
-- Name: track_judges track_judges_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- TOC entry 5292 (class 2606 OID 46232)
-- Name: track_judges track_judges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT track_judges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5293 (class 2606 OID 46237)
-- Name: track_mentors track_mentors_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5294 (class 2606 OID 46242)
-- Name: track_mentors track_mentors_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


--
-- TOC entry 5295 (class 2606 OID 46247)
-- Name: track_mentors track_mentors_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT track_mentors_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5296 (class 2606 OID 46252)
-- Name: tracks tracks_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tracks
    ADD CONSTRAINT tracks_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;


--
-- TOC entry 5297 (class 2606 OID 46257)
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- TOC entry 5298 (class 2606 OID 46262)
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5299 (class 2606 OID 46267)
-- Name: users users_campus_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_campus_id_fkey FOREIGN KEY (campus_id) REFERENCES public.campuses(id) ON DELETE SET NULL;


--
-- TOC entry 5300 (class 2606 OID 46272)
-- Name: users users_university_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_university_id_fkey FOREIGN KEY (university_id) REFERENCES public.universities(id) ON DELETE SET NULL;


--
-- TOC entry 5525 (class 0 OID 0)
-- Dependencies: 6
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


-- Completed on 2026-07-19 18:46:39

--
-- PostgreSQL database dump complete
--


