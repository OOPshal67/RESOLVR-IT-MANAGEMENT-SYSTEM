# RESOLVR — Technology Selection Report

## 1. Justification of Core Stack

| Layer | Selected Technology | Why It Was Selected for RESOLVR | Alternatives Considered & Why Rejected |
| :--- | :--- | :--- | :--- |
| **Programming Language** | **Java 21 (LTS)** | Strong static typing, explicit interface contracts, abstract classes, sealed/record support, and enterprise concurrency ideal for demonstrating OOP principles (`Inheritance`, `Polymorphism`, `Encapsulation`, `Abstraction`). | Python / Node.js — Dynamically typed languages lack compile-time interface enforcement required for rigorous OOP evaluation. |
| **Backend Framework** | **Spring Boot 3.2.5 (Spring MVC, Security, Data JPA, Validation, Scheduler)** | Provides dependency injection (IoC) for polymorphic Strategy and Observer bean registration, declarative `@Transactional` boundaries, `@Scheduled` SLA background jobs, and enterprise security. | Jakarta EE / Raw Servlets — Excessive boilerplate for configuration and database migration management. |
| **Frontend View Engine** | **Thymeleaf + Bootstrap 5 + Chart.js** | Server-side rendering with `thymeleaf-extras-springsecurity6` guarantees role-aware navigation, zero CORS complexity, instant form validation binding, and strict visual replication of reference enterprise layouts. | React / Angular SPA — Splits the project into two separate build pipelines without adding OOP backend value. |
| **Database & Migration** | **PostgreSQL 16 + Flyway** | Production ACID compliance, rich indexing, `JOINED` table inheritance foreign key integrity, and deterministic versioned schema migrations (`V1__init_schema.sql`, `V2__seed_data.sql`). | SQLite / MongoDB — Lack strict relational foreign key constraints or enterprise concurrency required for ITSM audit trails. |
| **Architecture Style** | **Modular Monolith** | Clean separation across 15 functional domains and 5 horizontal layers within a single deployable artifact, avoiding distributed network latency while keeping modules cleanly decoupled. | Microservices — Introduces operational overhead (service discovery, distributed transactions) inappropriate for a single-team academic deployment. |

## 2. How the Stack Supports Object-Oriented Programming (OOP)

1. **Polymorphic Bean Injection:** Spring IoC injects all `PriorityCalculationStrategy`, `TechnicianAssignmentStrategy`, and `IncidentEventObserver` implementations into collections/maps so business services invoke interfaces polymorphically.
2. **JPA Joined Inheritance:** Hibernate maps `User -> Employee, Technician, Administrator` onto normalized PostgreSQL tables (`users`, `employees`, `technicians`, `administrators`) preserving genuine `IS-A` inheritance in both Java and SQL.

## 3. Scalability Roadmap

1. **Database Read Scaling:** PostgreSQL read replicas can serve `AnalyticsService` and `IncidentSearchCriteria` queries while primary nodes handle transactional writes.
2. **Distributed Scheduling:** `SLAScheduler` can be backed by ShedLock so multiple stateless Spring Boot instances can scale horizontally behind a load balancer without duplicate SLA alerts.
