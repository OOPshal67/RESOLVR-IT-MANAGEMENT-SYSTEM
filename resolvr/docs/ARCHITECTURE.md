# RESOLVR — System Architecture & UML Diagrams

## 1. Layered Modular Monolithic Architecture

```mermaid
flowchart TB
    subgraph Presentation["Presentation Layer (Spring MVC & Thymeleaf)"]
        UI["21 Thymeleaf Views + 3 Strict Reference Layouts"]
        CTRL["Controllers: Auth, Employee, Technician, Admin, Incident, Notification, ApiStats"]
    end

    subgraph Service["Service & Pattern Layer (Core Business Logic)"]
        IS["IncidentService & StateMachine"]
        US["UserService & CatalogService"]
        STRAT["Strategy Pattern: Priority & Assignment"]
        INT["Intelligent Services: DuplicateDetection, SLARisk, Escalation, Analytics"]
        OBS["Observer Pattern: IncidentEventPublisher"]
    end

    subgraph Domain["Domain Layer (OOP Entities & Enums)"]
        ENT["User -> Employee | Technician | Administrator, Incident, SLAPolicy, Resolution, AuditLog"]
    end

    subgraph Persistence["Repository & Infrastructure Layer"]
        REPO["20 Spring Data JPA Repositories + Specifications"]
        SCHED["SLAScheduler (@Scheduled 60s)"]
        DB[("PostgreSQL Database + Flyway V1/V2")]
    end

    UI --> CTRL
    CTRL --> IS
    CTRL --> US
    CTRL --> INT
    IS --> STRAT
    IS --> OBS
    SCHED --> INT
    IS --> REPO
    US --> REPO
    INT --> REPO
    OBS --> REPO
    REPO --> ENT
    REPO --> DB
```

## 2. UML Use Case Diagram

```mermaid
flowchart LR
    EMP(("Employee / Reporter"))
    TECH(("Technician / Engineer"))
    ADM(("Administrator / IT Manager"))

    subgraph RESOLVR_System["RESOLVR Intelligent ITSM Boundary"]
        UC1(["Register & Authenticate"])
        UC2(["Report Incident & Auto-Classify"])
        UC3(["Track Lifecycle & Respond to Info Requests"])
        UC4(["Verify Resolution, Close & Rate"])
        UC5(["Acknowledge & Investigate Assigned Incidents"])
        UC6(["Record Internal/Public Notes & Resolve"])
        UC7(["Request Reassignment"])
        UC8(["Manage Users, Technicians & Skills"])
        UC9(["Configure Services, Categories, SLA & Strategies"])
        UC10(["Review Duplicate Alerts, Escalations, Audit & Analytics"])
    end

    EMP --> UC1
    EMP --> UC2
    EMP --> UC3
    EMP --> UC4

    TECH --> UC1
    TECH --> UC5
    TECH --> UC6
    TECH --> UC7

    ADM --> UC1
    ADM --> UC8
    ADM --> UC9
    ADM --> UC10
```

## 3. UML Class Diagram (Inheritance, Composition, Aggregation & Strategy Interfaces)

```mermaid
classDiagram
    class User {
        <<abstract>>
        -Long id
        -String username
        -String email
        -String passwordHash
        -String fullName
        -Role role
        -String department
        -boolean active
        +getRoleIdentifierCode()* String
        +updateProfile(String, String, String, String) void
    }

    class Employee {
        -String employeeCode
        -String jobTitle
        -String officeLocation
        +getRoleIdentifierCode() String
    }

    class Technician {
        -String technicianCode
        -String specialization
        -int maxWorkload
        -int currentWorkload
        -boolean available
        -int experienceYears
        -boolean teamLead
        -Set~TechnicianSkill~ skills
        +canAcceptAssignment() boolean
        +incrementWorkload() void
        +decrementWorkload() void
    }

    class Administrator {
        -String adminCode
        -String accessLevel
        -String managementRegion
        +getRoleIdentifierCode() String
    }

    class Incident {
        -Long id
        -String incidentKey
        -String title
        -ImpactLevel impact
        -UrgencyLevel urgency
        -PriorityLevel priority
        -double priorityScore
        -IncidentStatus status
        -LocalDateTime responseDeadline
        -LocalDateTime resolutionDeadline
        -SLARiskLevel slaRiskLevel
        -EscalationLevel escalationLevel
        +isSlaViolated() boolean
        +getRemainingResolutionMinutes() long
    }

    class PriorityCalculationStrategy {
        <<interface>>
        +getStrategyCode() String
        +calculatePriority(ImpactLevel, UrgencyLevel, BusinessCriticality, int, IncidentCategory, ITService) PriorityResult
    }

    class WeightedPriorityCalculationStrategy {
        +calculatePriority(...) PriorityResult
    }

    class StrictMatrixPriorityStrategy {
        +calculatePriority(...) PriorityResult
    }

    class TechnicianAssignmentStrategy {
        <<interface>>
        +getStrategyCode() String
        +recommendTechnician(Incident, List~Technician~) Optional~AssignmentRecommendation~
    }

    class SkillBasedAssignmentStrategy {
        +recommendTechnician(...) Optional~AssignmentRecommendation~
    }

    class LoadBasedAssignmentStrategy {
        +recommendTechnician(...) Optional~AssignmentRecommendation~
    }

    class RoundRobinAssignmentStrategy {
        +recommendTechnician(...) Optional~AssignmentRecommendation~
    }

    User <|-- Employee
    User <|-- Technician
    User <|-- Administrator
    Incident --> "1" Employee : reporter
    Incident --> "0..1" Technician : assignedTechnician
    Incident *-- "0..*" InvestigationNote : composition
    Incident *-- "0..*" InformationRequest : composition
    Incident *-- "0..1" Resolution : composition
    Incident *-- "0..1" IncidentFeedback : composition
    Technician o-- "0..*" TechnicianSkill : aggregation
    PriorityCalculationStrategy <|.. WeightedPriorityCalculationStrategy
    PriorityCalculationStrategy <|.. StrictMatrixPriorityStrategy
    TechnicianAssignmentStrategy <|.. SkillBasedAssignmentStrategy
    TechnicianAssignmentStrategy <|.. LoadBasedAssignmentStrategy
    TechnicianAssignmentStrategy <|.. RoundRobinAssignmentStrategy
```

## 4. Sequence Diagram — Workflow 1: Incident Creation, Priority, Duplicate Check & Smart Assignment

```mermaid
sequenceDiagram
    actor Emp as Employee
    participant Ctrl as EmployeeController
    participant Svc as IncidentService
    participant PStrat as PriorityCalculationStrategy
    participant Fac as IncidentFactory
    participant Dup as DuplicateDetectionService
    participant AStrat as TechnicianAssignmentStrategy
    participant Pub as IncidentEventPublisher
    participant DB as PostgreSQL

    Emp->>Ctrl: POST /employee/incidents (IncidentCreateRequest)
    Ctrl->>Svc: createIncident(request, reporter)
    Svc->>PStrat: calculatePriority(impact, urgency, criticality, users, cat, srv)
    PStrat-->>Svc: PriorityResult(P1, 88.0, reasoning)
    Svc->>Fac: createIncident(INC1001, request, reporter, cat, srv, priorityResult, slaPolicy)
    Fac-->>Svc: Incident(status=CLASSIFIED, deadlines)
    Svc->>DB: save(incident)
    Svc->>Dup: detectAndRecordSimilarIncidents(incident)
    Dup->>DB: save(IncidentRelationship)
    Svc->>AStrat: recommendTechnician(incident, availableTechnicians)
    AStrat-->>Svc: AssignmentRecommendation(Arjun Nair, score=92.0)
    Svc->>Svc: assignIncidentToTechnician(INC1001, Arjun Nair)
    Svc->>Pub: publish(IncidentDomainEvent)
    Pub->>DB: save(AuditLog, InAppNotification, SimulatedEmailNotification)
    Svc-->>Ctrl: Incident(INC1001, status=ASSIGNED)
    Ctrl-->>Emp: Redirect /employee/incidents/{id}
```

## 5. Sequence Diagram — Workflow 2: Technician Investigation, Resolution & Employee Verification/Closure

```mermaid
sequenceDiagram
    actor Tech as Technician
    actor Emp as Employee
    participant TCtrl as TechnicianController
    participant ECtrl as EmployeeController
    participant Svc as IncidentService
    participant SM as IncidentStateMachine
    participant Pub as IncidentEventPublisher
    participant DB as PostgreSQL

    Tech->>TCtrl: POST /technician/incidents/{id}/acknowledge
    TCtrl->>Svc: acknowledgeIncident(id, tech)
    Svc->>SM: transition(incident, ACKNOWLEDGED)
    Svc->>Pub: publish(INCIDENT_ACKNOWLEDGED)
    Tech->>TCtrl: POST /technician/incidents/{id}/resolve (ResolutionRequest)
    TCtrl->>Svc: resolveIncident(id, request, tech)
    Svc->>SM: transition(incident, RESOLVED)
    Svc->>DB: save(Resolution), decrementWorkload(tech)
    Svc->>Pub: publish(INCIDENT_RESOLVED)
    Emp->>ECtrl: POST /employee/incidents/{id}/verify-close (FeedbackRequest)
    ECtrl->>Svc: verifyAndCloseIncident(id, feedback, emp)
    Svc->>SM: transition(incident, VERIFIED -> CLOSED)
    Svc->>DB: save(IncidentFeedback, Incident)
    Svc->>Pub: publish(INCIDENT_VERIFIED, INCIDENT_CLOSED)
```
