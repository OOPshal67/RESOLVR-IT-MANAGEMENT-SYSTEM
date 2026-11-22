-- ============================================================================
-- RESOLVR — Flyway Migration V2: Default System Configurations
-- Complete relational seed data with BCrypt-hashed accounts and 16 realistic
-- incidents is idempotently initialized via DataInitializer on startup.
-- ============================================================================

INSERT INTO application_configurations (config_key, config_value, description, updated_by)
VALUES
    ('ACTIVE_ASSIGNMENT_STRATEGY', 'SKILL_BASED', 'Active Technician Assignment Strategy (SKILL_BASED, LOAD_BASED, ROUND_ROBIN)', 'SYSTEM'),
    ('ACTIVE_PRIORITY_STRATEGY', 'WEIGHTED_SCORE', 'Active Priority Calculation Strategy (WEIGHTED_SCORE, ITIL_MATRIX)', 'SYSTEM'),
    ('AUTOMATIC_ESCALATION_ENABLED', 'TRUE', 'Enable Automatic Multi-Tier Incident Escalation', 'SYSTEM')
ON CONFLICT (config_key) DO NOTHING;
