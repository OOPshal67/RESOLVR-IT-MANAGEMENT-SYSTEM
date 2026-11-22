# RESOLVR — Object-Oriented Principles & Design Patterns Catalog

## 1. Summary of Implemented Design Patterns

| Pattern Name | Category | Interface / Base Abstraction | Concrete Implementations in Source Code | Architectural Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Strategy Pattern (Priority)** | Behavioral | `com.resolvr.strategy.PriorityCalculationStrategy` | `WeightedPriorityCalculationStrategy`, `StrictMatrixPriorityStrategy` | Decouples priority scoring algorithms from `IncidentService`; allows runtime switching by Administrators. |
| **Strategy Pattern (Assignment)** | Behavioral | `com.resolvr.strategy.TechnicianAssignmentStrategy` | `SkillBasedAssignmentStrategy`, `LoadBasedAssignmentStrategy`, `RoundRobinAssignmentStrategy` | Encapsulates technician recommendation algorithms (skills+capacity vs. least load vs. rotation) behind a unified contract. |
| **State Pattern** | Behavioral | `com.resolvr.state.IncidentState` | `com.resolvr.state.IncidentStateMachine` (12 registered state handlers) | Enforces valid incident lifecycle transitions and state-entry timestamp side effects (`assignedAt`, `acknowledgedAt`, `resolvedAt`, `closedAt`), throwing `InvalidStateTransitionException` on illegal transitions. |
| **Observer Pattern** | Behavioral | `com.resolvr.observer.IncidentEventObserver` | `AuditLogObserver`, `InAppNotificationObserver`, `SimulatedEmailNotificationObserver` | Decouples incident lifecycle mutations from cross-cutting audit logging and multi-channel notifications via `IncidentEventPublisher`. |
| **Factory Pattern** | Creational | `IncidentFactory`, `NotificationFactory` | `com.resolvr.factory.IncidentFactory`, `com.resolvr.factory.NotificationFactory` | Centralizes complex aggregate initialization (SLA deadline computation, priority reasoning binding, and formatted in-app vs. simulated email payloads). |
| **Repository Pattern** | Architectural | `JpaRepository`, `JpaSpecificationExecutor` | 20 interfaces in `com.resolvr.repository.*` | Abstracts PostgreSQL persistence and dynamic multi-criteria SQL filtering away from domain and service layers. |

## 2. Core OOP Principles Demonstrated

1. **Inheritance (`JOINED` Table Strategy):** `User` (`abstract` base class in `com.resolvr.entity.User`) defines shared identity, authentication, and profile behavior, while `Employee`, `Technician`, and `Administrator` extend `User` and implement the abstract polymorphic method `getRoleIdentifierCode()`.
2. **Encapsulation:** Domain entities protect state invariants through methods such as `Technician.canAcceptAssignment()`, `Technician.incrementWorkload()`, `Technician.decrementWorkload()`, `InformationRequest.recordResponse()`, and `Notification.markAsRead()`.
3. **Polymorphism:** `IncidentService` injects `List<PriorityCalculationStrategy>` and `List<TechnicianAssignmentStrategy>` into a strategy registry `Map<String, ...>` and invokes the active strategy polymorphically without conditional `if/else` chains.
4. **Composition & Aggregation:** `Incident` composes `InvestigationNote`, `InformationRequest`, `Resolution`, and `IncidentFeedback` (`cascade = CascadeType.ALL, orphanRemoval = true`), while `Technician` aggregates `Set<TechnicianSkill>` via `@ManyToMany`.
