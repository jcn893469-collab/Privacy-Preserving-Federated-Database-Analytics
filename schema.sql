--
-- PostgreSQL database dump
--

\restrict 6uKIqKc8tkweU950QRyN71FXeMtxdPkS3fGjRv8I77VH9RhYqZfJaIcEmlglU55

-- Dumped from database version 18.6 (4e5fd08)
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
-- Name: fn_audit_contribution_change(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_audit_contribution_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_db_name VARCHAR(150);
    v_owner_id BIGINT;
    v_owner_email VARCHAR(255);
    v_action VARCHAR(100);
    v_details TEXT;
    v_entity_id BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_action := 'CONTRIBUTION_DELETED';
        v_details := CONCAT('Deleted contribution from database ID: ', OLD.database_id);
        v_entity_id := OLD.id;
        INSERT INTO audit_log (
            user_id, user_email, action, entity_type, entity_id, details, status
        ) VALUES (
            NULL, 'SYSTEM', v_action, 'ANALYTICS_CONTRIBUTION', v_entity_id,
            v_details, 'SUCCESS'
        );
        RETURN OLD;
    END IF;

    SELECT fd.organization_name, u.id, u.email
    INTO v_db_name, v_owner_id, v_owner_email
    FROM federated_database fd
    LEFT JOIN app_user u ON fd.owner_user_id = u.id
    WHERE fd.id = NEW.database_id;

    v_entity_id := NEW.id;
    IF TG_OP = 'INSERT' THEN
        v_action := 'CONTRIBUTION_SUBMITTED';
        v_details := CONCAT(
            'Org: ', v_db_name, ', Records: ', NEW.record_count,
            ', DP Epsilon: ', NEW.epsilon, ', Noise Sigma: ', NEW.noise_sigma
        );
    ELSE
        v_action := 'CONTRIBUTION_UPDATED';
        v_details := CONCAT(
            'Org: ', v_db_name, ', Records: ', OLD.record_count, ' -> ', NEW.record_count,
            ', DP Epsilon: ', OLD.epsilon, ' -> ', NEW.epsilon
        );
    END IF;

    INSERT INTO audit_log (
        user_id, user_email, action, entity_type, entity_id, details, status
    ) VALUES (
        v_owner_id, v_owner_email, v_action, 'ANALYTICS_CONTRIBUTION',
        v_entity_id, v_details, 'SUCCESS'
    );
    RETURN NEW;
END;
$$;


--
-- Name: fn_calculate_federated_statistics(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_calculate_federated_statistics(p_database_id bigint DEFAULT NULL::bigint) RETURNS TABLE(p_participating_orgs bigint, p_contribution_count bigint, p_total_records bigint, p_aggregate_noisy_sum numeric, p_global_mean numeric, p_avg_records numeric, p_min_records integer, p_max_records integer)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF p_database_id IS NOT NULL THEN
        RETURN QUERY
        SELECT 
            COUNT(DISTINCT ac.database_id) AS p_participating_orgs,
            COUNT(ac.id) AS p_contribution_count,
            COALESCE(SUM(ac.record_count), 0)::BIGINT AS p_total_records,
            COALESCE(SUM(ac.noisy_sum), 0.0000)::NUMERIC AS p_aggregate_noisy_sum,
            CASE 
                WHEN COALESCE(SUM(ac.record_count), 0) > 0 
                THEN ROUND(CAST(SUM(ac.noisy_sum) / SUM(ac.record_count) AS NUMERIC), 4)
                ELSE 0.0000 
            END AS p_global_mean,
            COALESCE(AVG(ac.record_count), 0.0)::NUMERIC AS p_avg_records,
            COALESCE(MIN(ac.record_count), 0)::INTEGER AS p_min_records,
            COALESCE(MAX(ac.record_count), 0)::INTEGER AS p_max_records
        FROM analytics_contribution ac
        WHERE ac.database_id = p_database_id;
    ELSE
        RETURN QUERY
        SELECT 
            COUNT(DISTINCT ac.database_id) AS p_participating_orgs,
            COUNT(ac.id) AS p_contribution_count,
            COALESCE(SUM(ac.record_count), 0)::BIGINT AS p_total_records,
            COALESCE(SUM(ac.noisy_sum), 0.0000)::NUMERIC AS p_aggregate_noisy_sum,
            CASE 
                WHEN COALESCE(SUM(ac.record_count), 0) > 0 
                THEN ROUND(CAST(SUM(ac.noisy_sum) / SUM(ac.record_count) AS NUMERIC), 4)
                ELSE 0.0000 
            END AS p_global_mean,
            COALESCE(AVG(ac.record_count), 0.0)::NUMERIC AS p_avg_records,
            COALESCE(MIN(ac.record_count), 0)::INTEGER AS p_min_records,
            COALESCE(MAX(ac.record_count), 0)::INTEGER AS p_max_records
        FROM analytics_contribution ac;
    END IF;
END;
$$;


--
-- Name: sp_generate_organization_analytics_summary(bigint, jsonb); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_generate_organization_analytics_summary(IN p_database_id bigint, INOUT p_summary jsonb DEFAULT NULL::jsonb)
    LANGUAGE plpgsql
    AS $$
BEGIN
    SELECT jsonb_build_object(
        'databaseId', fd.id,
        'organizationName', fd.organization_name,
        'dbIdentifier', fd.db_identifier,
        'active', fd.is_active,
        'ownerEmail', u.email,
        'contributionCount', COUNT(ac.id),
        'totalRecordsProtected', COALESCE(SUM(ac.record_count), 0),
        'totalNoisySum', COALESCE(SUM(ac.noisy_sum), 0.0000),
        'avgRecordsPerContribution', COALESCE(AVG(ac.record_count), 0.0000),
        'avgAnomalyRate', COALESCE(AVG(ac.anomaly_rate), 0.0000),
        'privacyEpsilonBudget', fd.privacy_epsilon_budget,
        'privacyEpsilonSpent', fd.privacy_epsilon_spent,
        'privacyBudgetRemaining', fd.privacy_epsilon_budget - fd.privacy_epsilon_spent,
        'lastContributionAt', MAX(ac.submitted_at)
    )
    INTO p_summary
    FROM federated_database fd
    JOIN app_user u ON u.id = fd.owner_user_id
    LEFT JOIN analytics_contribution ac ON ac.database_id = fd.id
    WHERE fd.id = p_database_id
    GROUP BY fd.id, u.email;

    IF NOT FOUND THEN
        p_summary := NULL;
    END IF;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: analytics_contribution; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.analytics_contribution (
    id bigint NOT NULL,
    database_id bigint NOT NULL,
    record_count integer NOT NULL,
    noisy_sum numeric(18,4) NOT NULL,
    epsilon numeric(10,4) NOT NULL,
    submitted_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    delta_value numeric(20,15) DEFAULT 0.000010000000000 NOT NULL,
    mechanism character varying(40) DEFAULT 'GAUSSIAN'::character varying NOT NULL,
    clipping_lower numeric(18,4),
    clipping_upper numeric(18,4),
    sensitivity numeric(18,4),
    noise_sigma numeric(18,8),
    noise_scale numeric(18,8),
    rdp_epsilon_cost numeric(10,4),
    anomaly_count integer,
    anomaly_rate numeric(10,6),
    CONSTRAINT chk_analytics_contribution_epsilon CHECK ((epsilon > (0)::numeric)),
    CONSTRAINT chk_analytics_contribution_mechanism CHECK (((mechanism)::text = 'GAUSSIAN'::text)),
    CONSTRAINT chk_analytics_contribution_record_count CHECK ((record_count > 0))
);


--
-- Name: analytics_contribution_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.analytics_contribution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: analytics_contribution_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.analytics_contribution_id_seq OWNED BY public.analytics_contribution.id;


--
-- Name: app_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_user (
    id bigint NOT NULL,
    full_name character varying(150) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(30) DEFAULT 'RESEARCHER'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    token_version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_app_user_role CHECK (((role)::text = ANY (ARRAY[('ADMIN'::character varying)::text, ('DATA_OWNER'::character varying)::text, ('RESEARCHER'::character varying)::text]))),
    CONSTRAINT chk_app_user_token_version CHECK ((token_version >= 0))
);


--
-- Name: app_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.app_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: app_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.app_user_id_seq OWNED BY public.app_user.id;


--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    id bigint NOT NULL,
    user_id bigint,
    user_email character varying(255),
    action character varying(100) NOT NULL,
    entity_type character varying(80) NOT NULL,
    entity_id bigint,
    details text,
    ip_address character varying(64),
    status character varying(30) DEFAULT 'SUCCESS'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: audit_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.audit_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: audit_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.audit_log_id_seq OWNED BY public.audit_log.id;


--
-- Name: dataset_column_schema; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dataset_column_schema (
    id bigint NOT NULL,
    dataset_id bigint NOT NULL,
    column_name character varying(100) NOT NULL,
    data_type character varying(50) DEFAULT 'NUMERIC'::character varying NOT NULL,
    clipping_lower numeric(18,4) DEFAULT 0.0000 NOT NULL,
    clipping_upper numeric(18,4) DEFAULT 100.0000 NOT NULL,
    sensitivity numeric(18,4) DEFAULT 100.0000 NOT NULL,
    is_target_metric boolean DEFAULT true NOT NULL,
    CONSTRAINT chk_column_clipping_bounds CHECK ((clipping_upper > clipping_lower)),
    CONSTRAINT chk_column_sensitivity CHECK ((sensitivity > (0)::numeric))
);


--
-- Name: dataset_column_schema_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dataset_column_schema_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dataset_column_schema_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dataset_column_schema_id_seq OWNED BY public.dataset_column_schema.id;


--
-- Name: dataset_metadata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dataset_metadata (
    id bigint NOT NULL,
    database_id bigint NOT NULL,
    dataset_name character varying(150) NOT NULL,
    description text,
    schema_version character varying(30) DEFAULT '1.0'::character varying NOT NULL,
    total_records integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_dataset_total_records CHECK ((total_records >= 0))
);


--
-- Name: dataset_metadata_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dataset_metadata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dataset_metadata_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dataset_metadata_id_seq OWNED BY public.dataset_metadata.id;


--
-- Name: federated_database; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.federated_database (
    id bigint NOT NULL,
    organization_name character varying(150) NOT NULL,
    db_identifier character varying(100) NOT NULL,
    description text,
    owner_user_id bigint NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    registered_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    privacy_epsilon_budget numeric(10,4) DEFAULT 10.0000 NOT NULL,
    privacy_delta_budget numeric(20,15) DEFAULT 0.000010000000000 NOT NULL,
    privacy_epsilon_spent numeric(10,4) DEFAULT 0.0000 NOT NULL,
    rdp_accountant_state text,
    CONSTRAINT chk_federated_database_delta_budget CHECK (((privacy_delta_budget > (0)::numeric) AND (privacy_delta_budget < (1)::numeric))),
    CONSTRAINT chk_federated_database_epsilon_budget CHECK ((privacy_epsilon_budget > (0)::numeric))
);


--
-- Name: federated_database_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.federated_database_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: federated_database_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.federated_database_id_seq OWNED BY public.federated_database.id;


--
-- Name: federated_result; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.federated_result (
    id bigint NOT NULL,
    run_identifier character varying(100) NOT NULL,
    computation_type character varying(50) DEFAULT 'FEDERATED_MEAN'::character varying NOT NULL,
    target_metric character varying(100) DEFAULT 'overall'::character varying NOT NULL,
    participating_org_count integer NOT NULL,
    total_contribution_count integer NOT NULL,
    protected_total_records integer NOT NULL,
    protected_aggregate_sum numeric(18,4) NOT NULL,
    global_protected_mean numeric(18,6) NOT NULL,
    privacy_mechanism character varying(50) DEFAULT 'GAUSSIAN + RDP'::character varying NOT NULL,
    executed_by_user_id bigint,
    executed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_run_orgs_nonneg CHECK ((participating_org_count >= 0)),
    CONSTRAINT chk_run_records_nonneg CHECK ((protected_total_records >= 0))
);


--
-- Name: federated_result_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.federated_result_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: federated_result_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.federated_result_id_seq OWNED BY public.federated_result.id;


--
-- Name: federated_run_participant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.federated_run_participant (
    id bigint NOT NULL,
    run_id bigint NOT NULL,
    contribution_id bigint NOT NULL,
    database_id bigint NOT NULL,
    organization_name character varying(150) NOT NULL,
    record_count integer NOT NULL,
    noisy_sum numeric(18,4) NOT NULL,
    contributed_at timestamp without time zone NOT NULL
);


--
-- Name: federated_run_participant_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.federated_run_participant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: federated_run_participant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.federated_run_participant_id_seq OWNED BY public.federated_run_participant.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: privacy_budget_ledger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.privacy_budget_ledger (
    id bigint NOT NULL,
    database_id bigint NOT NULL,
    contribution_id bigint NOT NULL,
    pre_epsilon_spent numeric(10,4) NOT NULL,
    round_epsilon_cost numeric(10,4) NOT NULL,
    post_epsilon_spent numeric(10,4) NOT NULL,
    noise_scale numeric(18,8) NOT NULL,
    noise_sigma numeric(18,8) NOT NULL,
    evaluated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_ledger_cost_positive CHECK ((round_epsilon_cost >= (0)::numeric)),
    CONSTRAINT chk_ledger_post_spent CHECK ((post_epsilon_spent >= pre_epsilon_spent))
);


--
-- Name: privacy_budget_ledger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.privacy_budget_ledger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: privacy_budget_ledger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.privacy_budget_ledger_id_seq OWNED BY public.privacy_budget_ledger.id;


--
-- Name: v_federated_analytics_overview; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_federated_analytics_overview AS
 SELECT count(DISTINCT fd.id) AS total_registered_organizations,
    count(DISTINCT
        CASE
            WHEN (ac.id IS NOT NULL) THEN fd.id
            ELSE NULL::bigint
        END) AS participating_organizations,
    count(ac.id) AS total_contributions,
    COALESCE(sum(ac.record_count), (0)::bigint) AS total_protected_records,
    COALESCE(sum(ac.noisy_sum), 0.0000) AS global_noisy_sum,
        CASE
            WHEN (COALESCE(sum(ac.record_count), (0)::bigint) > 0) THEN round((COALESCE(sum(ac.noisy_sum), 0.0000) / (sum(ac.record_count))::numeric), 4)
            ELSE 0.0000
        END AS global_protected_mean,
    COALESCE(avg(ac.record_count), 0.0) AS avg_record_count,
    COALESCE(min(ac.record_count), 0) AS min_record_count,
    COALESCE(max(ac.record_count), 0) AS max_record_count,
    COALESCE(avg(ac.anomaly_rate), 0.0) AS system_avg_anomaly_rate,
    COALESCE(sum(fd.privacy_epsilon_spent), 0.0000) AS total_privacy_budget_spent
   FROM (public.federated_database fd
     LEFT JOIN public.analytics_contribution ac ON ((fd.id = ac.database_id)));


--
-- Name: v_organization_contribution_summary; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_organization_contribution_summary AS
 SELECT fd.id AS database_id,
    fd.organization_name,
    fd.db_identifier,
    fd.is_active,
    u.email AS owner_email,
    u.full_name AS owner_name,
    count(ac.id) AS contribution_count,
    COALESCE(sum(ac.record_count), (0)::bigint) AS total_records_protected,
    COALESCE(sum(ac.noisy_sum), 0.0000) AS total_noisy_sum,
    COALESCE(avg(ac.record_count), 0.0000) AS avg_records_per_contribution,
    COALESCE(avg(ac.anomaly_rate), 0.0000) AS avg_anomaly_rate,
    COALESCE(sum(ac.anomaly_count), (0)::bigint) AS total_anomalies_flagged,
    fd.privacy_epsilon_budget,
    fd.privacy_epsilon_spent,
    (fd.privacy_epsilon_budget - fd.privacy_epsilon_spent) AS privacy_budget_remaining,
    fd.registered_at,
    max(ac.submitted_at) AS last_contribution_at
   FROM ((public.federated_database fd
     JOIN public.app_user u ON ((fd.owner_user_id = u.id)))
     LEFT JOIN public.analytics_contribution ac ON ((fd.id = ac.database_id)))
  GROUP BY fd.id, fd.organization_name, fd.db_identifier, fd.is_active, u.email, u.full_name, fd.privacy_epsilon_budget, fd.privacy_epsilon_spent, fd.registered_at;


--
-- Name: analytics_contribution id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.analytics_contribution ALTER COLUMN id SET DEFAULT nextval('public.analytics_contribution_id_seq'::regclass);


--
-- Name: app_user id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user ALTER COLUMN id SET DEFAULT nextval('public.app_user_id_seq'::regclass);


--
-- Name: audit_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log ALTER COLUMN id SET DEFAULT nextval('public.audit_log_id_seq'::regclass);


--
-- Name: dataset_column_schema id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dataset_column_schema ALTER COLUMN id SET DEFAULT nextval('public.dataset_column_schema_id_seq'::regclass);


--
-- Name: dataset_metadata id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dataset_metadata ALTER COLUMN id SET DEFAULT nextval('public.dataset_metadata_id_seq'::regclass);


--
-- Name: federated_database id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_database ALTER COLUMN id SET DEFAULT nextval('public.federated_database_id_seq'::regclass);


--
-- Name: federated_result id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_result ALTER COLUMN id SET DEFAULT nextval('public.federated_result_id_seq'::regclass);


--
-- Name: federated_run_participant id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_run_participant ALTER COLUMN id SET DEFAULT nextval('public.federated_run_participant_id_seq'::regclass);


--
-- Name: privacy_budget_ledger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.privacy_budget_ledger ALTER COLUMN id SET DEFAULT nextval('public.privacy_budget_ledger_id_seq'::regclass);


--
-- Name: analytics_contribution analytics_contribution_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.analytics_contribution
    ADD CONSTRAINT analytics_contribution_pkey PRIMARY KEY (id);


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- Name: dataset_column_schema dataset_column_schema_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dataset_column_schema
    ADD CONSTRAINT dataset_column_schema_pkey PRIMARY KEY (id);


--
-- Name: dataset_metadata dataset_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dataset_metadata
    ADD CONSTRAINT dataset_metadata_pkey PRIMARY KEY (id);


--
-- Name: federated_database federated_database_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_database
    ADD CONSTRAINT federated_database_pkey PRIMARY KEY (id);


--
-- Name: federated_result federated_result_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_result
    ADD CONSTRAINT federated_result_pkey PRIMARY KEY (id);


--
-- Name: federated_run_participant federated_run_participant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_run_participant
    ADD CONSTRAINT federated_run_participant_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: privacy_budget_ledger privacy_budget_ledger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.privacy_budget_ledger
    ADD CONSTRAINT privacy_budget_ledger_pkey PRIMARY KEY (id);


--
-- Name: app_user uq_app_user_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT uq_app_user_email UNIQUE (email);


--
-- Name: dataset_column_schema uq_column_per_dataset; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dataset_column_schema
    ADD CONSTRAINT uq_column_per_dataset UNIQUE (dataset_id, column_name);


--
-- Name: dataset_metadata uq_dataset_name_per_database; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dataset_metadata
    ADD CONSTRAINT uq_dataset_name_per_database UNIQUE (database_id, dataset_name);


--
-- Name: federated_database uq_federated_database_identifier; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_database
    ADD CONSTRAINT uq_federated_database_identifier UNIQUE (db_identifier);


--
-- Name: federated_result uq_federated_run_identifier; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_result
    ADD CONSTRAINT uq_federated_run_identifier UNIQUE (run_identifier);


--
-- Name: federated_run_participant uq_run_contribution; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_run_participant
    ADD CONSTRAINT uq_run_contribution UNIQUE (run_id, contribution_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_analytics_contribution_database_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_analytics_contribution_database_id ON public.analytics_contribution USING btree (database_id);


--
-- Name: idx_app_user_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_app_user_email ON public.app_user USING btree (email);


--
-- Name: idx_audit_log_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_action ON public.audit_log USING btree (action);


--
-- Name: idx_audit_log_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_created_at ON public.audit_log USING btree (created_at DESC);


--
-- Name: idx_audit_log_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_entity ON public.audit_log USING btree (entity_type, entity_id);


--
-- Name: idx_audit_log_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_user ON public.audit_log USING btree (user_id);


--
-- Name: idx_dataset_column_dataset; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dataset_column_dataset ON public.dataset_column_schema USING btree (dataset_id);


--
-- Name: idx_dataset_metadata_database; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dataset_metadata_database ON public.dataset_metadata USING btree (database_id);


--
-- Name: idx_federated_database_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_federated_database_owner ON public.federated_database USING btree (owner_user_id);


--
-- Name: idx_federated_result_executed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_federated_result_executed_at ON public.federated_result USING btree (executed_at);


--
-- Name: idx_participant_contribution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_participant_contribution ON public.federated_run_participant USING btree (contribution_id);


--
-- Name: idx_participant_run; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_participant_run ON public.federated_run_participant USING btree (run_id);


--
-- Name: idx_privacy_ledger_contribution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_privacy_ledger_contribution ON public.privacy_budget_ledger USING btree (contribution_id);


--
-- Name: idx_privacy_ledger_database; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_privacy_ledger_database ON public.privacy_budget_ledger USING btree (database_id);


--
-- Name: idx_privacy_ledger_evaluated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_privacy_ledger_evaluated_at ON public.privacy_budget_ledger USING btree (evaluated_at);


--
-- Name: analytics_contribution trg_analytics_contribution_audit; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_analytics_contribution_audit AFTER INSERT OR DELETE OR UPDATE ON public.analytics_contribution FOR EACH ROW EXECUTE FUNCTION public.fn_audit_contribution_change();


--
-- Name: analytics_contribution fk_analytics_contribution_database; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.analytics_contribution
    ADD CONSTRAINT fk_analytics_contribution_database FOREIGN KEY (database_id) REFERENCES public.federated_database(id) ON DELETE RESTRICT;


--
-- Name: audit_log fk_audit_log_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- Name: dataset_column_schema fk_dataset_column_dataset; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dataset_column_schema
    ADD CONSTRAINT fk_dataset_column_dataset FOREIGN KEY (dataset_id) REFERENCES public.dataset_metadata(id) ON DELETE CASCADE;


--
-- Name: dataset_metadata fk_dataset_metadata_database; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dataset_metadata
    ADD CONSTRAINT fk_dataset_metadata_database FOREIGN KEY (database_id) REFERENCES public.federated_database(id) ON DELETE CASCADE;


--
-- Name: federated_database fk_federated_database_owner; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_database
    ADD CONSTRAINT fk_federated_database_owner FOREIGN KEY (owner_user_id) REFERENCES public.app_user(id) ON DELETE RESTRICT;


--
-- Name: federated_result fk_federated_result_executor; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_result
    ADD CONSTRAINT fk_federated_result_executor FOREIGN KEY (executed_by_user_id) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- Name: privacy_budget_ledger fk_ledger_contribution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.privacy_budget_ledger
    ADD CONSTRAINT fk_ledger_contribution FOREIGN KEY (contribution_id) REFERENCES public.analytics_contribution(id) ON DELETE CASCADE;


--
-- Name: privacy_budget_ledger fk_ledger_database; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.privacy_budget_ledger
    ADD CONSTRAINT fk_ledger_database FOREIGN KEY (database_id) REFERENCES public.federated_database(id) ON DELETE CASCADE;


--
-- Name: federated_run_participant fk_participant_contribution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_run_participant
    ADD CONSTRAINT fk_participant_contribution FOREIGN KEY (contribution_id) REFERENCES public.analytics_contribution(id) ON DELETE CASCADE;


--
-- Name: federated_run_participant fk_participant_database; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_run_participant
    ADD CONSTRAINT fk_participant_database FOREIGN KEY (database_id) REFERENCES public.federated_database(id) ON DELETE CASCADE;


--
-- Name: federated_run_participant fk_participant_run; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_run_participant
    ADD CONSTRAINT fk_participant_run FOREIGN KEY (run_id) REFERENCES public.federated_result(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict 6uKIqKc8tkweU950QRyN71FXeMtxdPkS3fGjRv8I77VH9RhYqZfJaIcEmlglU55

