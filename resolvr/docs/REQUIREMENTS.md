# RESOLVR — Problem Analysis & Requirements Specification

## 1. Problem Analysis

### 1.1 Problem Statement
Modern enterprise and university IT departments handle hundreds of heterogeneous service disruptions across payment gateways, campus networks, authentication clusters, cloud storage, and databases. Manual ticket triage leads to inconsistent priority assignment, overloaded technicians, unmonitored Service Level Agreement (SLA) breaches, duplicate work on identical outages, and fragmented audit visibility.

### 1.2 Stakeholders
| Stakeholder | Role in RESOLVR | Primary Objectives |
| :--- | :--- | :--- |
| **Employees / Reporters** | `ROLE_EMPLOYEE` | Report incidents quickly, track real-time lifecycle progress, respond to information requests, verify resolutions, and rate service quality. |
| **Support Engineers / Technicians** | `ROLE_TECHNICIAN` | Receive skill-matched assignments within capacity limits, track SLA countdowns, record internal/public investigation notes, and document root causes. |
| **Administrators / IT Managers** | `ROLE_ADMIN` | Govern users, skills, IT services, categories, SLA policies, and assignment/priority strategies; review duplicate alerts, escalations, audit logs, and analytics. |

### 1.3 Existing Challenges & Proposed Solution
| Challenge in Legacy Helpdesks | RESOLVR Engineered Solution |
| :--- | :--- |
| Arbitrary priority guessing | `PriorityCalculationStrategy` computing deterministic weighted scores from Impact, Urgency, Service Criticality, Category Weight, and Affected Users. |
| Manual or unbalanced dispatching | `TechnicianAssignmentStrategy` offering polymorphic Skill-Based, Load-Based, and Round-Robin strategies respecting availability and max workload. |
| Silent SLA breaches | `SLAScheduler` running every 60 seconds to compute `SLARiskLevel`, send warnings, record `SLAViolation` entries, and trigger multi-tier `EscalationService`. |
| Unlinked duplicate outage reports | `DuplicateDetectionService` normalizing text and computing Jaccard token similarity with service/category boosting. |

### 1.4 Core Assumptions
1. The initial SLA engine uses a **24/7 elapsed-time clock** so SLA warnings, breaches, and escalations can be demonstrated deterministically at any time of day; `SLAPolicy` is structured so business-hour calendars can be plugged in without altering incident entities.
2. Both in-app database notifications and simulated email notifications (`SimulatedEmailNotificationObserver`) run out of the box without requiring external SMTP credentials.

## 2. Functional Requirements Summary

| Module ID | Module Name | Key Functional Capabilities |
| :--- | :--- | :--- |
| **MOD-01** | Authentication & Authorization | BCrypt password hashing, Spring Security role guards, custom role-based login redirect, CSRF protection. |
| **MOD-02** | User & Technician Management | Employee registration, Admin user activation/deactivation, Technician onboarding, skill matrix mapping, availability and max workload controls. |
| **MOD-03** | Incident Management & Classification | Unique `INC1001`+ key generation, 9 extensible categories, 7 configurable IT services with `BusinessCriticality`. |
| **MOD-04** | Priority & SLA Management | Configurable weighted and matrix priority calculation; P1–P4 SLA policies with response and resolution deadlines. |
| **MOD-05** | Intelligent Assignment | Polymorphic Skill-Based, Load-Based, and Round-Robin strategies with recommendation explanation and manual admin override. |
| **MOD-06** | Lifecycle State Machine | Strict State Pattern enforcement across `NEW`, `CLASSIFIED`, `ASSIGNED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `WAITING_FOR_USER`, `ESCALATED`, `RESOLVED`, `VERIFIED`, `CLOSED`, `REOPENED`, `CANCELLED`. |
| **MOD-07** | Notifications & Audit Trail | Observer pattern publishing `IncidentDomainEvent` to `AuditLogObserver`, `InAppNotificationObserver`, and `SimulatedEmailNotificationObserver`. |
| **MOD-08** | Intelligent Automation & Analytics | Jaccard duplicate detection, rule-based SLA risk prediction, 4-level automatic escalation, Chart.js analytics, and CSV report export. |
