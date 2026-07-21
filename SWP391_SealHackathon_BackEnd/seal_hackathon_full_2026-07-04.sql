--
-- PostgreSQL database dump
--

\restrict Z2DjoQ6Yg0122TfIdRwEqkE2b6OyroekmixLFuQosHhP0wuXPLeqBHMMeV6pZDh

-- Dumped from database version 17.10 (Homebrew)
-- Dumped by pg_dump version 17.10 (Homebrew)

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
    'REJECT_INCIDENT'
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

SET default_tablespace = '';

SET default_table_access_method = heap;

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
-- Name: notifications; Type: TABLE; Schema: public; Owner: mac
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


ALTER TABLE public.notifications OWNER TO mac;

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
    CONSTRAINT rounds_sequence_number_check CHECK ((sequence_number > 0)),
    CONSTRAINT rounds_top_n_to_promote_check CHECK ((top_n_to_promote > 0))
);


ALTER TABLE public.rounds OWNER TO postgres;

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
    review_status character varying(50) DEFAULT 'pending'::character varying
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
    CONSTRAINT chk_tjr_status CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'accepted'::character varying, 'rejected'::character varying, 'cancelled'::character varying])::text[])))
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


ALTER TABLE public.teams OWNER TO postgres;

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
    bio text
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
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_logs (id, user_id, team_id, incident_id, action, target_type, target_id, old_value, new_value, details, occurred_at) FROM stdin;
3f4384d4-319a-4e16-aa94-d39607ebda08	b9debf4b-c200-4907-abdf-5ce2208f8eb7	\N	\N	DISQUALIFY_TEAM	team	8b0bb0fc-1857-4b95-964b-92b791c352f6	active	disqualified	Smoke test	2026-07-02 07:59:40.317851
dc96fdbb-4510-4c52-be75-b4cae9ed42dc	b9debf4b-c200-4907-abdf-5ce2208f8eb7	\N	\N	DISQUALIFY_TEAM	team	8b0bb0fc-1857-4b95-964b-92b791c352f6	active	disqualified	tao thich thi t khoa	2026-07-02 08:46:33.735737
643c4fa8-beb8-4b74-b544-9b763987e145	b9debf4b-c200-4907-abdf-5ce2208f8eb7	\N	\N	DISQUALIFY_TEAM	team	8b0bb0fc-1857-4b95-964b-92b791c352f6	active	disqualified	bo m thich ban	2026-07-02 17:43:44.492644
0eb3a454-7687-4374-aec7-302486de8970	b9debf4b-c200-4907-abdf-5ce2208f8eb7	5f12db5b-b46e-440b-98e4-bc6b0b592d74	\N	PROMOTE_TEAM	team	5f12db5b-b46e-440b-98e4-bc6b0b592d74	082553c7-dbed-4cb2-9629-0401e9fd3a7d	118d1506-a833-4a9f-877f-58b3bf4b3772	Moved team to track AI	2026-07-04 01:53:09.197129
cc78e52a-763e-43e3-9550-a9e18025749b	b9debf4b-c200-4907-abdf-5ce2208f8eb7	5f12db5b-b46e-440b-98e4-bc6b0b592d74	\N	PROMOTE_TEAM	team	5f12db5b-b46e-440b-98e4-bc6b0b592d74	118d1506-a833-4a9f-877f-58b3bf4b3772	082553c7-dbed-4cb2-9629-0401e9fd3a7d	Moved team to track Regression Track 232026	2026-07-04 01:53:09.239013
48969fb3-6fc4-400a-b700-670f56bbd5d7	b9debf4b-c200-4907-abdf-5ce2208f8eb7	5f12db5b-b46e-440b-98e4-bc6b0b592d74	\N	PROMOTE_TEAM	team	5f12db5b-b46e-440b-98e4-bc6b0b592d74	082553c7-dbed-4cb2-9629-0401e9fd3a7d	e18a5239-caf9-4997-8c11-e0bea2ca0f6e	Moved team to track Track B	2026-07-04 02:01:38.614675
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
-- Data for Name: events; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.events (id, title, description, status, created_at, updated_at, term, prize_pool, registration_start, registration_end, event_start, event_end) FROM stdin;
9b42520a-bce1-45ac-a165-73aa556d8c82	Regression Event 232026	full regression smoke	draft	2026-07-02 16:20:26.418208	2026-07-02 16:20:26.418208	\N	\N	\N	\N	\N	\N
35c0af3c-aebe-49aa-ab74-c4b5979e498c	Seal 2026	cuộc thi hackathon	draft	2026-07-03 16:43:25.572558	2026-07-03 16:43:25.572558	Summer	200000VND	2026-07-03 17:00:00	2026-07-05 17:00:00	2026-07-07 17:00:00	2026-07-11 17:00:00
32ef48b8-9ac3-4df0-8ad4-52f43f8eb9ae	SMOKE EDITED	tmp	published	2026-07-03 15:00:12.731716	2026-07-03 23:45:26.621204			\N	\N	\N	\N
50bea8c3-e782-4e59-a694-549fe42020bd	LOGIC Event 1783100902713	logic hierarchy test	draft	2026-07-03 17:48:22.71715	2026-07-03 17:48:22.71715	Summer	$1000	2026-07-31 17:00:00	2026-08-04 17:00:00	2026-08-05 17:00:00	2026-08-09 17:00:00
2975fd01-4c7d-451c-9076-3e7118dfdd2e	đá	fasf	draft	2026-07-03 18:05:18.076001	2026-07-03 18:05:18.076001	Spring	3	2026-07-01 17:00:00	2026-07-02 17:00:00	2026-07-08 17:00:00	2026-08-08 17:00:00
81a4656e-8a2d-4152-958d-6f302ec23efe	SMOKE PUB DEL	tmp	ongoing	2026-07-03 15:00:26.054602	2026-07-04 08:37:21.259811			\N	\N	\N	\N
a6ec61f7-2f7e-4d77-86d5-861a62fc060c	kghj	gfh	ongoing	2026-07-03 16:46:07.145649	2026-07-04 08:37:32.055564	Spring	4	2026-07-02 17:00:00	2026-07-03 17:00:00	2026-07-04 17:00:00	2026-07-06 17:00:00
\.


--
-- Data for Name: incident_actions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident_actions (id, incident_id, action_by, action_type, target_type, target_id, old_value, new_value, note, created_at) FROM stdin;
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
62febed3-aae9-44f6-95d5-b88cd452f7b5	9b42520a-bce1-45ac-a165-73aa556d8c82	082553c7-dbed-4cb2-9629-0401e9fd3a7d	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	5f12db5b-b46e-440b-98e4-bc6b0b592d74	cc3658ed-a72b-4b02-8942-a62c87eefc76	b9debf4b-c200-4907-abdf-5ce2208f8eb7	\N	other	reported	Regression Incident	incident smoke	2026-07-02 16:21:52.711207	2026-07-02 16:21:52.711215	\N	\N	\N
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
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: mac
--

COPY public.notifications (id, user_id, type, title, body, category, ref_type, ref_id, read_at, created_at, updated_at) FROM stdin;
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
8521d61b-3025-487f-b577-d8f61a778efd	5799bd8c3be56e17fca17373cb40cd420efb1d8a17216f12183274b3a34bd8e8	2026-07-03 00:52:47+07	2026-07-02 16:52:48.086301	2026-07-02 16:52:48.086301
e5688283-a5f4-4a27-9dec-82d39c8eeb91	7cc94d9a95b1b441380884e04af6accdc486c49d4597a4c69927beb162dfd72c	2026-07-09 23:52:48+07	2026-07-02 16:52:48.096347	2026-07-02 16:52:48.096347
4bba1d1a-166b-4fc3-85e9-5a8849604ada	382080f32a9e30a61e27c2cb6ab0936c5e9e5ddeb9ad3d76bcb688358d0cf63a	2026-07-03 00:56:21+07	2026-07-02 16:56:22.203753	2026-07-02 16:56:22.203753
2d309f25-f972-4f4d-a8c3-4a9a426bb9b4	293903ed6a55b44ed993454e57df75471b0ad9be7b6bcfbca9293c1eddcbc485	2026-07-09 23:56:21+07	2026-07-02 16:56:22.206048	2026-07-02 16:56:22.206048
020b3ab0-5e29-4ecd-84ff-9e83011a3d17	2536ddd12041a7922e9d94d98e9f5183a488b326904b9651f00f17b6bd146059	2026-07-03 00:56:30+07	2026-07-02 16:56:31.419643	2026-07-02 16:56:31.419643
4f89415d-1652-44b3-8b4d-6b01698a8ea3	56f4384cf53f5b39d47edaa480efb70af5354ff9f2f5a270d74f2e45b00b6322	2026-07-09 23:56:30+07	2026-07-02 16:56:31.421647	2026-07-02 16:56:31.421647
ce5a1393-8555-41ca-9471-e75548f34ade	645fd3500e7f40d009554c269f14f4767cdce41ecb3e5bdaa6831617b6075310	2026-07-03 00:56:46+07	2026-07-02 16:56:46.819418	2026-07-02 16:56:46.819418
1d2e00b8-90c0-49ce-b5d3-6c7a791fdae3	0e83cb60000032455b0166ffd0276cfb9d31c0cd8b87f6d40b1bf4fa498f7ae8	2026-07-09 23:56:46+07	2026-07-02 16:56:46.821282	2026-07-02 16:56:46.821282
ef616a9f-3e50-4b5d-a681-3e8d3d18c492	dc57df8936b71a6079fbc289039220a812eab59f94d737f2d5a414f8783adfbb	2026-07-03 00:57:18+07	2026-07-02 16:57:19.103884	2026-07-02 16:57:19.103884
76789efa-2734-464d-8ebd-30a65f19eb38	dc1690977a970ff3798fb4394a35956c11bb7a2325b19023ea9c8ae8017a8de8	2026-07-09 23:57:18+07	2026-07-02 16:57:19.105785	2026-07-02 16:57:19.105785
305cc00d-9175-485e-acba-6d87ded5ae1f	5c4aa3c44510e73f40755b8cbd608fcb27bcd50c2dca95e55e9b966b08f89579	2026-07-03 01:11:36+07	2026-07-02 17:11:37.050476	2026-07-02 17:11:37.050476
9b2f263f-f8bc-4346-a770-ab305a8b8266	78aef62607d6c7047c6ed83defe480b3cdb7dfc1f543ad8dd1871efb80ad3f8b	2026-07-10 00:11:36+07	2026-07-02 17:11:37.053288	2026-07-02 17:11:37.053288
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
\.


--
-- Data for Name: round_judges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.round_judges (id, round_id, user_id, assigned_at) FROM stdin;
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
-- Data for Name: rounds; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.rounds (id, track_id, name, sequence_number, submission_deadline, top_n_to_promote, created_at, updated_at) FROM stdin;
46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	082553c7-dbed-4cb2-9629-0401e9fd3a7d	Regression Round 232026	1	2026-07-09 16:20:26	3	2026-07-02 16:20:26.454913	2026-07-02 16:20:26.454913
876945ae-d236-4b69-89d4-2a515c55d9f7	082553c7-dbed-4cb2-9629-0401e9fd3a7d	round 2	13	2026-07-04 17:43:00	5	2026-07-02 17:43:13.774367	2026-07-02 17:43:13.774367
27451535-d487-4d47-a015-230c2f53236b	082553c7-dbed-4cb2-9629-0401e9fd3a7d	E2ERound5363	99407	2026-08-31 22:00:00	5	2026-07-03 17:23:27.425048	2026-07-03 17:23:27.425048
a206b707-43b9-4d52-98ab-2fb58ea85627	082553c7-dbed-4cb2-9629-0401e9fd3a7d	E2ERound67889	99669	2026-08-31 22:00:00	5	2026-07-03 17:27:49.925515	2026-07-03 17:27:49.925515
f00c85fe-3caa-4602-a6a4-ed0ddadbc4e4	082553c7-dbed-4cb2-9629-0401e9fd3a7d	E2ERound53846	99955	2026-08-31 22:00:00	5	2026-07-03 17:32:35.902038	2026-07-03 17:32:35.902038
78964a1c-111d-48cb-9dc6-6ef80a554dab	4c316b7d-9963-467b-87c7-e70baa547992	LOGICRound2713	21902	2026-09-01 05:00:00	3	2026-07-03 17:48:26.200791	2026-07-03 17:48:26.200791
47325e63-2e70-4281-909a-dcf8d45c9542	d892f925-3347-4f5e-890b-dc7ca7558e33	1	9	2026-07-04 18:37:00	3	2026-07-04 01:37:59.20713	2026-07-04 01:37:59.20713
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
\.


--
-- Data for Name: submissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.submissions (id, round_id, team_id, repo_url, demo_url, slide_url, report_url, api_metadata, submitted_at, updated_at, project_name, version, review_status) FROM stdin;
cc3658ed-a72b-4b02-8942-a62c87eefc76	46dc99d9-691f-4bbb-bd1e-dc28f48e2c33	5f12db5b-b46e-440b-98e4-bc6b0b592d74	https://github.com/e2e/tail-64618	https://demo.example.com	https://example.com/slide	https://example.com/report	e2e-deep notes	2026-07-02 16:20:27.090629	2026-07-04 00:39:25.556005	\N	\N	pending
\.


--
-- Data for Name: support_tickets; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.support_tickets (id, requester_id, category, priority, subject, description, status, created_at, updated_at) FROM stdin;
e8b1bb75-2563-4201-908a-ae20a5726793	b9debf4b-c200-4907-abdf-5ce2208f8eb7	technical	low	Smoke ticket	support smoke	open	2026-07-02 17:11:24.132142	2026-07-02 17:11:24.132142
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
\.


--
-- Data for Name: team_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.team_members (id, team_id, user_id, role, joined_at) FROM stdin;
d5efffe7-e1ca-453d-b636-a88a53702951	5f12db5b-b46e-440b-98e4-bc6b0b592d74	b9debf4b-c200-4907-abdf-5ce2208f8eb7	leader	2026-07-02 16:20:27.063579
41950771-417b-4571-874f-9296829607bb	5f12db5b-b46e-440b-98e4-bc6b0b592d74	52e07429-90f0-4b2a-821e-77954881d2b1	member	2026-07-02 16:20:27.065781
5bf82639-b24b-459b-80cf-1437ea74baba	5f12db5b-b46e-440b-98e4-bc6b0b592d74	450560ce-da8d-43f6-955d-f39d553b77c3	member	2026-07-02 16:20:27.067951
\.


--
-- Data for Name: teams; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.teams (id, track_id, name, status, disqualified_reason, created_at, updated_at, invite_code) FROM stdin;
5f12db5b-b46e-440b-98e4-bc6b0b592d74	e18a5239-caf9-4997-8c11-e0bea2ca0f6e	Regression Team 232026	active	\N	2026-07-02 16:20:27.057818	2026-07-04 11:26:57.04209	3C865E
\.


--
-- Data for Name: track_judges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.track_judges (id, event_id, track_id, user_id, assigned_at) FROM stdin;
\.


--
-- Data for Name: track_mentors; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.track_mentors (id, event_id, track_id, user_id, assigned_at) FROM stdin;
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
4c316b7d-9963-467b-87c7-e70baa547992	50bea8c3-e782-4e59-a694-549fe42020bd	LOGICTrack2713	logic track	2026-07-03 17:48:24.324472	2026-07-03 17:48:24.324472	\N
d892f925-3347-4f5e-890b-dc7ca7558e33	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	Track C		2026-07-03 18:05:46.300584	2026-07-03 18:05:46.300584	\N
723f83f1-3b0f-4197-9206-b576562963a7	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	Track A		2026-07-03 18:05:46.300584	2026-07-03 18:05:46.300584	\N
4d95ca60-30fa-4603-8f56-b0290da7829a	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	Track B		2026-07-03 18:05:46.300584	2026-07-03 18:05:46.300584	\N
2421b5c6-9184-4176-b9b0-a253093b04b3	a6ec61f7-2f7e-4d77-86d5-861a62fc060c	General	Default track for registered teams	2026-07-04 01:37:26.358939	2026-07-04 01:37:26.358939	\N
\.


--
-- Data for Name: universities; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.universities (id, name, short_name, country, created_at, updated_at) FROM stdin;
11111111-1111-1111-1111-111111111111	FPT University	FPTU	Vietnam	2026-07-02 14:13:12.122329	\N
0053bd3d-ad1e-4210-81f2-c3cdede05543	HCMUT	\N	Vietnam	2026-07-02 07:29:33.968043	2026-07-02 07:29:33.968043
b43b3920-a3cf-4766-a72a-c8a34a08ba4f	Regression University	\N	Vietnam	2026-07-02 16:20:26.488934	2026-07-02 16:20:26.488934
dc307b88-d085-4d87-a4fd-47a502e5555e	UIT	\N	Vietnam	2026-07-04 04:04:34.156496	2026-07-04 04:04:34.156496
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
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, password_hash, full_name, student_type, student_id, campus_id, is_guest, status, created_at, updated_at, university_id, phone, department, "position", company, expertise, bio) FROM stdin;
5b1dada5-bd1a-4f5c-9a57-b4d4e21f7a40	fpt_test_1782976532@seal.local	$2a$12$4UHjTacjDTNNfJhmJo5YteEJVi/z.AmbGxjPNOqM0kOCLOnsYpYSW	FPT Test	fpt	SE999999	22222222-2222-2222-2222-222222222221	f	pending	2026-07-02 07:15:32.700343	2026-07-02 14:26:13.478042	11111111-1111-1111-1111-111111111111	\N	\N	\N	\N	\N	\N
52b75aa6-317a-44fa-9115-63a40b604265	external_test_1782977373@seal.local	$2a$12$xPd8CK03SDeRQ3SxBXK7Ie7Y1.dhfUTgHFW7vFH5Nhk4UkTOe7jHq	External Test	external	\N	\N	f	pending	2026-07-02 07:29:34.276928	2026-07-02 07:29:34.276928	0053bd3d-ad1e-4210-81f2-c3cdede05543	\N	\N	\N	\N	\N	\N
52e07429-90f0-4b2a-821e-77954881d2b1	reg232026_0@seal.local	$2a$12$nSYzrf6pV/OsqjXuusFX2eSOJIOeg6XmE/7.XkOJTbH7sQeEAKddW	Reg Member 0	external	\N	\N	f	approved	2026-07-02 16:20:26.734573	2026-07-02 23:20:26.751487	b43b3920-a3cf-4766-a72a-c8a34a08ba4f	\N	\N	\N	\N	\N	\N
450560ce-da8d-43f6-955d-f39d553b77c3	reg232026_1@seal.local	$2a$12$hQysaABwYCsqMrVFNOIVPeTa3kTXbmS5qWNdTjl/3A8wGMO4scdv.	Reg Member 1	external	\N	\N	f	approved	2026-07-02 16:20:27.021084	2026-07-03 00:44:10.076935	b43b3920-a3cf-4766-a72a-c8a34a08ba4f	\N	\N	\N	\N	\N	\N
b9debf4b-c200-4907-abdf-5ce2208f8eb7	coordinator@seal.local	$2a$12$NzoScH178DBAczXea4Pl5OYYVjwjCE952.Jx4qWGJVhqw77aszGZK	SmokeDept97972	none	\N	\N	f	approved	2026-07-02 06:48:04.38247	2026-07-04 00:33:16.172055	\N	099996098	E2EDept96098	Coordinator	\N	\N	\N
\.


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
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: mac
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


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
-- Name: rounds rounds_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT rounds_pkey PRIMARY KEY (id);


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
-- Name: teams teams_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);


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
-- Name: round_criteria uq_round_criteria_round_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.round_criteria
    ADD CONSTRAINT uq_round_criteria_round_name UNIQUE (round_id, name);


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
-- Name: track_judges uq_track_judges_event_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT uq_track_judges_event_user UNIQUE (event_id, user_id);


--
-- Name: track_judges uq_track_judges_track_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_judges
    ADD CONSTRAINT uq_track_judges_track_user UNIQUE (track_id, user_id);


--
-- Name: track_mentors uq_track_mentors_event_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.track_mentors
    ADD CONSTRAINT uq_track_mentors_event_user UNIQUE (event_id, user_id);


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
-- Name: idx_notifications_user; Type: INDEX; Schema: public; Owner: mac
--

CREATE INDEX idx_notifications_user ON public.notifications USING btree (user_id);


--
-- Name: idx_notifications_user_unread; Type: INDEX; Schema: public; Owner: mac
--

CREATE INDEX idx_notifications_user_unread ON public.notifications USING btree (user_id, category) WHERE (read_at IS NULL);


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
-- Name: idx_teams_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_teams_status ON public.teams USING btree (status);


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
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: mac
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


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
-- Name: rounds rounds_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rounds
    ADD CONSTRAINT rounds_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


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
-- Name: teams teams_track_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_track_id_fkey FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE CASCADE;


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
-- PostgreSQL database dump complete
--

\unrestrict Z2DjoQ6Yg0122TfIdRwEqkE2b6OyroekmixLFuQosHhP0wuXPLeqBHMMeV6pZDh

