CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_category VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_customer_id ON payments(customer_id);

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    plan_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_subscriptions_customer_id ON subscriptions(customer_id);

CREATE TABLE recovery_cases (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    payment_id BIGINT REFERENCES payments(id),
    subscription_id BIGINT REFERENCES subscriptions(id),
    amount_at_risk NUMERIC(12,2) NOT NULL,
    failure_category VARCHAR(40) NOT NULL,
    risk_level VARCHAR(10),
    recovery_probability DOUBLE PRECISION,
    expected_recovery NUMERIC(12,2),
    recommended_action VARCHAR(30),
    ai_reasoning TEXT,
    confidence_score DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL,
    experiment_group VARCHAR(10),
    retries_used INTEGER NOT NULL DEFAULT 0,
    contacts_used INTEGER NOT NULL DEFAULT 0,
    messages_used INTEGER NOT NULL DEFAULT 0,
    human_escalations_used INTEGER NOT NULL DEFAULT 0,
    recovery_window_deadline TIMESTAMP,
    actual_recovered_amount NUMERIC(12,2),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_recovery_cases_customer_id ON recovery_cases(customer_id);
CREATE INDEX idx_recovery_cases_status ON recovery_cases(status);
CREATE INDEX idx_recovery_cases_experiment_group ON recovery_cases(experiment_group);

CREATE TABLE recovery_actions (
    id BIGSERIAL PRIMARY KEY,
    recovery_case_id BIGINT NOT NULL REFERENCES recovery_cases(id),
    action_type VARCHAR(30) NOT NULL,
    policy_decision VARCHAR(20) NOT NULL,
    execution_result VARCHAR(20),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    executed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_recovery_actions_case_id ON recovery_actions(recovery_case_id);

CREATE TABLE policies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    max_retries INTEGER NOT NULL,
    max_contacts INTEGER NOT NULL,
    max_messages INTEGER NOT NULL,
    max_human_escalations INTEGER NOT NULL,
    max_recovery_window_days INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    recovery_case_id BIGINT REFERENCES recovery_cases(id),
    event_type VARCHAR(40) NOT NULL,
    event_detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_case_id ON audit_logs(recovery_case_id);

CREATE TABLE experiments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_at_risk_revenue NUMERIC(14,2),
    total_expected_recovery NUMERIC(14,2),
    total_actual_recovered NUMERIC(14,2),
    treatment_recovery_rate DOUBLE PRECISION,
    control_recovery_rate DOUBLE PRECISION,
    incremental_lift DOUBLE PRECISION,
    successful_interventions INTEGER,
    failed_interventions INTEGER,
    intervention_count INTEGER,
    cost_per_recovery NUMERIC(12,2),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);