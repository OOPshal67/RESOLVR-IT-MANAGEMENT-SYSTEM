CREATE SEQUENCE incident_number;
SELECT setval('incident_number', greatest(1017, coalesce(max(substring(incident_key from '[0-9]+$')::bigint), 0) + 1), false) FROM incidents WHERE incident_key ~ '^INC[0-9]+$';
CREATE SEQUENCE employee_number;
SELECT setval('employee_number', greatest(104, coalesce(max(substring(employee_code from '[0-9]+$')::bigint), 0) + 1), false) FROM employees WHERE employee_code ~ '^EMP[0-9]+$';
CREATE SEQUENCE technician_number;
SELECT setval('technician_number', greatest(205, coalesce(max(substring(technician_code from '[0-9]+$')::bigint), 0) + 1), false) FROM technicians WHERE technician_code ~ '^TECH[0-9]+$';
CREATE TABLE access_requests (
 id bigserial PRIMARY KEY, username varchar(60) NOT NULL, email varchar(120) NOT NULL,
 full_name varchar(120) NOT NULL, department varchar(80) NOT NULL, password_hash varchar(255),
 status varchar(10) NOT NULL CHECK(status IN ('PENDING','APPROVED','REJECTED')),
 requested_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX pending_username ON access_requests(username) WHERE status='PENDING';
CREATE UNIQUE INDEX pending_email ON access_requests(email) WHERE status='PENDING';
CREATE TABLE first_admin (id integer PRIMARY KEY CHECK (id=1));
ALTER TABLE users ADD COLUMN failures integer NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN failed_at timestamptz;
ALTER TABLE users ADD COLUMN locked_until timestamptz;
ALTER TABLE users ADD COLUMN reset_digest varchar(64);
ALTER TABLE users ADD COLUMN reset_until timestamptz;
ALTER TABLE users ADD COLUMN revision bigint NOT NULL DEFAULT 0;
ALTER TABLE incidents ADD COLUMN revision bigint NOT NULL DEFAULT 0;

-- Retire the original demonstration credentials when upgrading a populated database.
WITH retired AS (
 UPDATE users SET password_hash='!retired' WHERE (username,email) IN (
 ('admin','admin@resolvr.io'),('arjun.tech','arjun.nair@resolvr.io'),('priya.tech','priya.sharma@resolvr.io'),
 ('rohan.tech','rohan.verma@resolvr.io'),('sneha.tech','sneha.reddy@resolvr.io'),
 ('sahith.emp','sahith.aggala@resolvr.io'),('ananya.emp','ananya.menon@resolvr.io'),
 ('karthik.emp','karthik.iyer@resolvr.io')) RETURNING username
)
INSERT INTO audit_logs(actor_username,actor_role,action_type,description)
 SELECT 'SYSTEM','SYSTEM','LEGACY_CREDENTIAL_RETIRED',username FROM retired;
INSERT INTO first_admin SELECT 1 WHERE EXISTS
 (SELECT 1 FROM users WHERE role='ROLE_ADMIN' AND password_hash<>'!retired');

ALTER TABLE audit_logs ALTER COLUMN description TYPE text;
ALTER TABLE audit_logs ALTER COLUMN metadata TYPE text;
CREATE UNIQUE INDEX active_recovery_codes ON users(reset_digest) WHERE reset_digest IS NOT NULL;
