-- ============================================================================
-- RESOLVR — Flyway Migration V1: Core PostgreSQL Schema
-- ============================================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    department VARCHAR(80) NOT NULL,
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

CREATE TABLE employees (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    job_title VARCHAR(80) NOT NULL,
    office_location VARCHAR(80) NOT NULL
);

CREATE TABLE technicians (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    technician_code VARCHAR(30) NOT NULL UNIQUE,
    specialization VARCHAR(80) NOT NULL,
    max_workload INTEGER NOT NULL DEFAULT 5,
    current_workload INTEGER NOT NULL DEFAULT 0,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    experience_years INTEGER NOT NULL DEFAULT 1,
    team_lead BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_technicians_available ON technicians(available);

CREATE TABLE administrators (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    admin_code VARCHAR(30) NOT NULL UNIQUE,
    access_level VARCHAR(40) NOT NULL DEFAULT 'FULL_SYSTEM',
    management_region VARCHAR(60) NOT NULL DEFAULT 'GLOBAL'
);

CREATE TABLE technician_skills (
    id BIGSERIAL PRIMARY KEY,
    skill_code VARCHAR(40) NOT NULL UNIQUE,
    skill_name VARCHAR(80) NOT NULL,
    category_code VARCHAR(40) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE technician_skill_mapping (
    technician_id BIGINT NOT NULL REFERENCES technicians(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES technician_skills(id) ON DELETE CASCADE,
    PRIMARY KEY (technician_id, skill_id)
);

CREATE TABLE incident_categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    default_weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE it_services (
    id BIGSERIAL PRIMARY KEY,
    service_code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    business_criticality VARCHAR(30) NOT NULL,
    owner_department VARCHAR(80) NOT NULL,
    operational_status VARCHAR(30) NOT NULL DEFAULT 'OPERATIONAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sla_policies (
    id BIGSERIAL PRIMARY KEY,
    priority_level VARCHAR(15) NOT NULL UNIQUE,
    policy_name VARCHAR(80) NOT NULL,
    response_target_minutes INTEGER NOT NULL,
    resolution_target_minutes INTEGER NOT NULL,
    warning_threshold_percent INTEGER NOT NULL DEFAULT 25,
    escalation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    incident_key VARCHAR(30) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    reporter_id BIGINT NOT NULL REFERENCES employees(id),
    category_id BIGINT NOT NULL REFERENCES incident_categories(id),
    service_id BIGINT NOT NULL REFERENCES it_services(id),
    impact VARCHAR(20) NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    business_criticality VARCHAR(30) NOT NULL,
    affected_users INTEGER NOT NULL DEFAULT 1,
    priority VARCHAR(15) NOT NULL,
    priority_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    priority_reasoning VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    assigned_technician_id BIGINT REFERENCES technicians(id),
    sla_policy_id BIGINT NOT NULL REFERENCES sla_policies(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP,
    acknowledged_at TIMESTAMP,
    first_response_at TIMESTAMP,
    response_deadline TIMESTAMP NOT NULL,
    resolution_deadline TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    verified_at TIMESTAMP,
    closed_at TIMESTAMP,
    response_sla_violated BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_sla_violated BOOLEAN NOT NULL DEFAULT FALSE,
    sla_warning_sent BOOLEAN NOT NULL DEFAULT FALSE,
    sla_risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    escalation_level VARCHAR(35) NOT NULL DEFAULT 'NONE',
    attachment_name VARCHAR(255),
    closure_notes TEXT,
    recommended_technician_reason VARCHAR(500)
);

CREATE INDEX idx_incidents_key ON incidents(incident_key);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_priority ON incidents(priority);
CREATE INDEX idx_incidents_reporter ON incidents(reporter_id);
CREATE INDEX idx_incidents_technician ON incidents(assigned_technician_id);
CREATE INDEX idx_incidents_category ON incidents(category_id);
CREATE INDEX idx_incidents_service ON incidents(service_id);
CREATE INDEX idx_incidents_created_at ON incidents(created_at);

CREATE TABLE assignments (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    technician_id BIGINT NOT NULL REFERENCES technicians(id),
    assigned_by_username VARCHAR(60) NOT NULL,
    strategy_used VARCHAR(50) NOT NULL,
    recommendation_reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMP,
    reassignment_reason VARCHAR(255)
);

CREATE TABLE investigation_notes (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id),
    note_content TEXT NOT NULL,
    internal_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE information_requests (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    technician_id BIGINT NOT NULL REFERENCES technicians(id),
    request_message TEXT NOT NULL,
    employee_response TEXT,
    responded BOOLEAN NOT NULL DEFAULT FALSE,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP
);

CREATE TABLE resolutions (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL UNIQUE REFERENCES incidents(id) ON DELETE CASCADE,
    resolved_by_id BIGINT NOT NULL REFERENCES technicians(id),
    root_cause TEXT NOT NULL,
    resolution_summary TEXT NOT NULL,
    workaround_applied TEXT,
    time_spent_minutes INTEGER NOT NULL DEFAULT 30,
    resolved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incident_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL UNIQUE REFERENCES incidents(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incident_relationships (
    id BIGSERIAL PRIMARY KEY,
    source_incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    target_incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    relationship_type VARCHAR(30) NOT NULL,
    similarity_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    detected_by VARCHAR(60) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sla_violations (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    violation_type VARCHAR(30) NOT NULL,
    deadline_timestamp TIMESTAMP NOT NULL,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    overdue_minutes BIGINT NOT NULL DEFAULT 0,
    details VARCHAR(500) NOT NULL
);

CREATE TABLE escalation_records (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    escalation_level VARCHAR(35) NOT NULL,
    escalated_to_role VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    triggered_by VARCHAR(60) NOT NULL DEFAULT 'SLA_SCHEDULER',
    escalated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
    notification_type VARCHAR(40) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, read_status);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT REFERENCES incidents(id) ON DELETE SET NULL,
    incident_key VARCHAR(30),
    actor_username VARCHAR(60) NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    previous_state VARCHAR(50),
    new_state VARCHAR(50),
    description VARCHAR(500) NOT NULL,
    metadata VARCHAR(500),
    action_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_incident ON audit_logs(incident_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(action_timestamp DESC);

CREATE TABLE application_configurations (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(80) NOT NULL UNIQUE,
    config_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_by VARCHAR(60) NOT NULL DEFAULT 'SYSTEM',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
