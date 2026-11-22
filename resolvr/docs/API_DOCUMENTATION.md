# RESOLVR — Web MVC & REST API Documentation

The dev profile enables these OpenAPI routes.
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Spec:** `http://localhost:8080/api-docs`

## 1. Authentication & Account Endpoints (`AuthController`)

| Method | Endpoint | Access Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Public / Authenticated | Redirects authenticated users to their role-specific dashboard (`/admin/dashboard`, `/technician/dashboard`, or `/employee/dashboard`). |
| `GET` | `/login` | Public | Opens the workspace selector and sign-in form. |
| `GET` / `POST` | `/register` | Public | Employee self-service registration with server-side validation and BCrypt password hashing. |
| `GET` / `POST` | `/profile` | Authenticated | View and update personal profile details (`fullName`, `email`, `department`, `phone`). |

## 2. Employee Endpoints (`EmployeeController`)

| Method | Endpoint | Access Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/employee/dashboard` | `ROLE_EMPLOYEE` | Employee personal summary cards and reported incidents table. |
| `GET` / `POST` | `/employee/incidents/new` & `/employee/incidents` | `ROLE_EMPLOYEE` | Create incident form and submission pipeline (auto-priority, SLA binding, duplicate check, smart assignment). |
| `GET` | `/employee/incidents` | `ROLE_EMPLOYEE` | 3-pane incident history, search, and inspector drawer scoped strictly to the logged-in employee. |
| `GET` | `/employee/incidents/{id}` | `ROLE_EMPLOYEE` | Detailed incident view with visual 8-step lifecycle tracker, SLA timeline, and resolution verification form. |
| `POST` | `/employee/incidents/{id}/respond` | `ROLE_EMPLOYEE` | Submit requested information and transition incident from `WAITING_FOR_USER` to `IN_PROGRESS`. |
| `POST` | `/employee/incidents/{id}/verify-close` | `ROLE_EMPLOYEE` | Verify technician resolution, submit 1–5 star rating, and transition incident to `VERIFIED` and `CLOSED`. |

## 3. Technician Endpoints (`TechnicianController`)

| Method | Endpoint | Access Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/technician/dashboard` | `ROLE_TECHNICIAN` | Technician workload capacity, skills bar, SLA risk counts, and assigned incident queue. |
| `GET` | `/technician/incidents` | `ROLE_TECHNICIAN` | 3-pane assigned incidents workspace with category/priority filters and smart resolution checklist. |
| `GET` | `/technician/incidents/{id}` | `ROLE_TECHNICIAN`, `ROLE_ADMIN` | Full ticket investigation workspace with public/internal notes, info requests, resolution composer, and SLA timeline. |
| `POST` | `/technician/incidents/{id}/acknowledge` | `ROLE_TECHNICIAN`, `ROLE_ADMIN` | Acknowledge assigned incident and record `firstResponseAt` timestamp. |
| `POST` | `/technician/incidents/{id}/start` | `ROLE_TECHNICIAN`, `ROLE_ADMIN` | Transition incident to `IN_PROGRESS`. |
| `POST` | `/technician/incidents/{id}/note` | `ROLE_TECHNICIAN`, `ROLE_ADMIN` | Add public reply or internal engineering note. |
| `POST` | `/technician/incidents/{id}/request-info` | `ROLE_TECHNICIAN` | Request clarification from reporter and transition to `WAITING_FOR_USER`. |
| `POST` | `/technician/incidents/{id}/resolve` | `ROLE_TECHNICIAN`, `ROLE_ADMIN` | Record root cause, resolution summary, time spent, and transition to `RESOLVED`. |
| `POST` | `/technician/incidents/{id}/reassignment` | `ROLE_TECHNICIAN` | Request reassignment with reason and release active workload slot. |

## 4. Administrator & REST Endpoints (`AdminController`, `ApiStatsController`)

| Method | Endpoint | Access Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/admin/dashboard` | `ROLE_ADMIN` | SLA Performance Overview, Severity/Priority distribution, Duplicate alerts, Escalations, and Recent incidents. |
| `GET` | `/admin/incidents` | `ROLE_ADMIN` | 3-pane all-incidents management board with smart technician assignment and manual escalation. |
| `POST` | `/admin/incidents/{id}/assign` | `ROLE_ADMIN` | Assign or override technician assignment. |
| `POST` | `/admin/incidents/{id}/escalate` | `ROLE_ADMIN` | Trigger manual multi-tier escalation. |
| `POST` | `/admin/relationships/{id}/confirm` | `ROLE_ADMIN` | Confirm a detected duplicate incident pair. |
| `GET` / `POST` | `/admin/users`, `/admin/technicians`, `/admin/services`, `/admin/categories`, `/admin/sla`, `/admin/strategies` | `ROLE_ADMIN` | Full administrative governance of users, technician skills, IT services, categories, SLA policies, and active Strategy beans. |
| `GET` | `/admin/audit` | `ROLE_ADMIN` | Filterable immutable audit log viewer. |
| `GET` | `/admin/analytics` & `/admin/analytics/export.csv` | `ROLE_ADMIN` | Interactive Chart.js analytics dashboard and downloadable CSV report export. |
| `GET` | `/api/v1/stats` | `ROLE_ADMIN` | Real-time JSON metrics endpoint for dashboard polling. |

## Access and recovery

| Method | Route | Permission | Action |
|---|---|---|---|
| GET, POST | /request-access | Public | Submit a pending technician application. |
| GET, POST | /forgot-password | Public | Exchange an unexpired recovery code for a new password. |
| GET | /admin/access | Admin | View applications and the new administrator form. |
| POST | /admin/access/{id} | Admin | Approve or reject one pending application. |
| POST | /admin/accounts | Admin | Create an administrator. |
| POST | /admin/users/{id}/recovery | Admin | Issue a single-use recovery code. |
