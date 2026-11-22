# RESOLVR

## IT INCIDENT TRACKING AND RESOLUTION SYSTEM

### A Java and Spring Boot service desk for reporting, assigning, and resolving IT incidents.

---

## ABOUT THE PROJECT

RESOLVR helps employees report IT problems and track their progress. Technicians investigate and resolve incidents, while administrators manage assignments and monitor service deadlines.

## KEY FEATURES

- Incident reporting and tracking
- Automatic priority calculation from P1 to P4
- Technician assignment based on skills and workload
- Response and resolution deadline monitoring
- Separate workspaces for employees, technicians, and administrators
- Incident history, audit records, dashboards, and CSV export

## TECHNOLOGY STACK

**Java 21 · Spring Boot 3.2.5 · Spring Security · Spring Data JPA · Thymeleaf · Bootstrap 5 · Chart.js · PostgreSQL 16 · Flyway · Docker Compose**

## RUN THE PROJECT

Install Docker Desktop, open PowerShell in the project folder, and run:

```powershell
$env:DB_PASSWORD = [guid]::NewGuid().ToString("N")
docker compose up --build -d
docker compose logs -f resolvr-app
```

When the application starts, visit http://localhost:8080/login.

To stop the application, press `Ctrl+C`, then run:

```powershell
docker compose down
```

The database is kept in a Docker volume. Use the same `DB_PASSWORD` when restarting with that volume.

## Project Documentation
See the `docs` folder for setup, architecture, database design, API documentation, design patterns, and test cases.

## Run Tests
With Java 21 and Maven installed:

```bash
mvn test
```

To run the PostgreSQL workflow tests with Docker:

```bash
mvn -Ppostgres-tests verify
```

## Team
Group 50 — BITS Pilani, Hyderabad Campus

## Demo Accounts
The development profile seeds sample accounts for local demonstration. These accounts are for local use only. Do not use the development profile or demo credentials for a public or production deployment.
## DEMO ACCOUNTS

| Role | Username | Password |
| :--- | :--- | :--- |
| **Admin** | `admin` | `ResolvrAdmin@2026!` |
| **Technician** | `arjun.tech` | `ResolvrTech@2026!` |
| **Employee** | `Brijesh` | `debbesh` |



