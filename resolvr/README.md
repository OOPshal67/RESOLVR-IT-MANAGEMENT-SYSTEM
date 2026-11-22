# RESOLVR

RESOLVR is a web application for tracking IT incidents from the first report to employee verification. Employees report issues and follow their tickets. Technicians investigate and record resolutions. Administrators manage assignments, service targets and incident reporting.

Built by Group 50 for an Object Oriented Programming course.

## Features

- Incident priority calculation and configurable technician assignment
- Similar incident detection and separate response and resolution deadlines
- Role-based workspaces, incident history and audit records
- Administrator dashboards and CSV export

## Run a local demonstration

Install Docker Desktop and open PowerShell in the directory containing this README. These settings start an isolated demonstration on the loopback address. Use a fresh database volume for a fresh demo.

```powershell
$env:DB_PASSWORD = [guid]::NewGuid().ToString("N")
docker compose up --build -d
docker compose logs -f resolvr-app
```

Wait for the application startup message, then open [the sign-in page](http://localhost:8080/login). Press Ctrl+C to leave the log display. For a reusable local database password, copy `.env.example` to `.env`, replace the placeholder and run the Compose commands without setting a new password in PowerShell. The database password must stay the same when reusing the database volume. Keep it in your local environment or an ignored .env file.

For the demo, use these fixed credentials:

| Role | Username | Password |
|---|---|---|
| Administrator | `admin` | `ResolvrAdmin@2026!` |
| Technician | `arjun.tech` | `ResolvrTech@2026!` |

The other sample technician accounts (`priya.tech`, `rohan.tech` and `sneha.tech`) use the same technician password. Sample employee passwords are generated and can be read from `/app/dev-credentials/<username>` inside the running container. The dev profile seeds sample accounts and tickets when the users table is empty. If demo accounts from an earlier version already exist, startup in the dev profile resets those demo accounts to the fixed passwords above and clears their lockout state. Stop the app with `docker compose down`. Named volumes retain the data.

These fixed credentials are for a local demonstration only. Do not use them for a public or production deployment.

## Start with real accounts

For production, explicitly set `SPRING_PROFILES_ACTIVE=default` and `COOKIE_SECURE=true`, then serve the application through HTTPS. Compose binds the application to loopback by default, so a reverse proxy on the host can forward requests to it. PostgreSQL is reachable inside the Compose network. The local dev profile is the default for running from an IDE; it enables demo accounts and must not be used for production.

Set BOOTSTRAP_USERNAME, BOOTSTRAP_EMAIL and BOOTSTRAP_PASSWORD before the first start. Use an unused username. Flyway installs the service catalogue and SLA targets. The startup transaction creates one administrator and records that bootstrap has been used. Remove the bootstrap variables and recreate the app container after the account exists. Further administrator accounts are created under **User accounts → Review requests and create administrators**.

### Upgrading an earlier database

Back up the existing PostgreSQL volume and keep its current database credentials. Start the new image against a copy first. The migration retires credentials for the eight original demo username/email pairs, including pairs whose passwords were changed. Tickets, notes and ownership remain available. If the original demo administrator was the only administrator, set the bootstrap variables with a **different username and email** for the upgrade. Use the new administrator to issue recovery codes to the account owners.

## Environment variables

| Variable | Use |
|---|---|
| DB_PASSWORD | Required PostgreSQL password. Compose passes it to both containers. |
| DB_USERNAME, DB_NAME | Database login and database name. Compose defaults to postgres and resolvr_db. |
| DB_URL | JDBC connection URL when running outside Compose. Compose builds it from DB_NAME. |
| SPRING_PROFILES_ACTIVE | Defaults to dev for local use. Set to default for production. The dev profile enables demo seeding and OpenAPI. |
| COOKIE_SECURE | The dev profile uses false for local HTTP. Set to true for production HTTPS. |
| APP_BIND_ADDRESS | Host address for the app port. Defaults to 127.0.0.1. |
| SERVER_PORT | Application port when running outside Compose. Compose uses 8080. |
| BOOTSTRAP_USERNAME, BOOTSTRAP_EMAIL, BOOTSTRAP_PASSWORD | One-time initial administrator details. Passwords require at least 12 characters and at most 72 UTF-8 bytes. |

## Roles and recovery

| Role | How to get access | What the account can do |
|---|---|---|
| Employee | Create an employee account on the sign-in page. | Report a problem, follow personal tickets, respond and verify closure. |
| Technician | Request access and wait for an administrator to approve it, or have an administrator create the account. | Investigate assigned incidents, add notes, request information and record resolutions. |
| Administrator | An existing administrator creates the account after the first-run bootstrap. | Review applicants, manage users, reassign tickets, change policies and read the audit log. |

The workspace selector checks the role held by the authenticated account. Five failed password checks for a known account within 15 minutes lock sign-in for 15 minutes. For a forgotten password, the service desk verifies the owner’s identity. An administrator opens User accounts, issues a recovery code and gives it to that person privately. The owner enters the code on the reset page within 15 minutes. Issuing another code replaces the previous one. There is no outgoing email integration.

## Priority calculation

Score = impact weight × urgency weight × business criticality weight × category weight + affected-user adjustment. The adjustment is 0 below 5 users, 2 from 5, 4 from 25, 8 from 100 and 12 from 500. A score of at least 36 is P1, at least 18 is P2 and at least 7 is P3. Other scores are P4.

## Tests

With Java 21 and Maven installed, run `mvn test` for the unit suite. With Docker running, use `mvn -Ppostgres-tests verify` for both the unit suite and the PostgreSQL workflow tests. Testcontainers starts a disposable PostgreSQL instance, and Flyway applies all migrations to it. The Docker image build runs the unit suite before packaging the jar.

## Continuous integration

GitHub Actions runs `mvn verify` with Java 21 on pushes and pull requests targeting `main`. The PostgreSQL integration suite is available locally with Docker using the command above.

## Project layout

| Path | Contents |
| --- | --- |
| `src/main/java/com/resolvr/` | Controllers, services, repositories and domain models |
| `src/main/resources/` | Application settings, Flyway migrations and web templates |
| `src/test/` | Unit tests and PostgreSQL workflow tests |
| [`docs/`](docs/README.md) | Architecture, API, database design and test scenarios |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Development and pull request steps |

## Tools used

Java 21, Spring Boot 3.2.5, Spring Security, Spring Data JPA, Thymeleaf, Bootstrap 5, Chart.js, PostgreSQL 16, Flyway and Docker Compose. The tests use JUnit 5, Mockito, MockMvc and Testcontainers.
