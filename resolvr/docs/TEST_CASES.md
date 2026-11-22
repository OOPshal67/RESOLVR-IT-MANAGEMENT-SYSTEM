# RESOLVR — Automated Test Suite & 23 Documented Test Cases

## 1. Test Suite Architecture

All 23 mandatory functional and security requirements are covered by automated JUnit 5 and Mockito unit/service tests under `src/test/java/com/resolvr/`:
- `PriorityAndStrategyTest.java`
- `LifecycleAndSLATest.java`
- `IncidentAndSecurityServiceTest.java`

> **Execution Honesty Note:** Because the cloud generation container environment only has the OpenJDK 17 JRE (`java`) installed without `javac`, `mvn`, or a running PostgreSQL server daemon, all 67 project source files were programmatically validated for Java syntax, package/import integrity, entity-repository-DTO alignment, and route-template consistency, and the complete Maven JUnit 5 test suite is packaged ready to execute via `mvn clean test` in `C:\Users\LENOVO\Desktop\OOP_2\resolvr`.

## 2. Catalog of 23 Automated Test Cases

| Test ID | Test Method & Class | Requirement Tested | Input / Scenario | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-01** | `IncidentAndSecurityServiceTest#testEmployeeRegistration` | Employee Registration & BCrypt | `RegistrationRequest` (`new.employee`, a generated test password) | Assigns `EMP104`, sets `ROLE_EMPLOYEE`, stores BCrypt hash verified via `passwordEncoder.matches()`. |
| **TC-02** | `IncidentAndSecurityServiceTest#testAuthenticationValidAndInvalidLogin` | Successful Login Lookup | `loadUserByUsername("sahith.emp")` | Returns `CustomUserDetails` with matching BCrypt hash and `ROLE_EMPLOYEE` authority. |
| **TC-03** | `IncidentAndSecurityServiceTest#testAuthenticationValidAndInvalidLogin` | Invalid Login Handling | `loadUserByUsername("unknown.user")` | Throws Spring Security `UsernameNotFoundException`. |
| **TC-04** | `IncidentAndSecurityServiceTest#testUnauthorizedAccessToAnotherEmployeesIncident` | Role-Based Access Control | Employee `EMP101` vs `EMP102` accessing `INC1001` | Owner access succeeds; non-owner employee access throws `UnauthorizedOperationException`. |
| **TC-05** | `IncidentAndSecurityServiceTest#testIncidentCreationNotificationsAndAudit` | Incident Creation | `IncidentCreateRequest` on `Payment Gateway` | Creates `Incident`, calculates P1 priority, binds P1 SLA policy, and auto-assigns available technician. |
| **TC-06** | `IncidentAndSecurityServiceTest#testIncidentCreationNotificationsAndAudit` | Incident ID Uniqueness | Sequential incident key generation | Generates unique `INC1001` key checking `incidentRepository.findByIncidentKey`. |
| **TC-07** | `PriorityAndStrategyTest#testPriorityCalculationP1Critical` | Automatic Priority Calculation | `CRITICAL` Impact × `CRITICAL` Urgency × `MISSION_CRITICAL` × `1.25` + `350` users | Computes exact score `88.0` and returns `PriorityLevel.P1` with full reasoning string. |
| **TC-08** | `PriorityAndStrategyTest#testPriorityBoundaryConditions` | Priority Boundary Conditions | Exact boundary inputs for `36.0`, `18.0`, `8.0`, `2.0` | Classifies `>=36.0` as `P1`, `>=18.0` as `P2`, `>=7.0` as `P3`, and `<7.0` as `P4`. |
| **TC-09** | `PriorityAndStrategyTest#testSkillBasedTechnicianAssignment` | Skill-Based Assignment | Two available technicians; one has `SKILL_APPLICATION` | Recommends `Tech Two` with verified `APPLICATION` skill match and higher match score. |
| **TC-10** | `PriorityAndStrategyTest#testLoadBasedTechnicianAssignment` | Load-Based Assignment | Two available technicians with workloads `4/5` and `1/5` | Selects `Light Tech` (`1/5` workload) deterministically. |
| **TC-11** | `PriorityAndStrategyTest#testUnavailableAndMaxWorkloadTechnicianHandling` | Unavailable / Overloaded Tech | Tech 1 `available=false`, Tech 2 `workload=3/3` | All 3 strategies (`SKILL_BASED`, `LOAD_BASED`, `ROUND_ROBIN`) return `Optional.empty()`. |
| **TC-12** | `LifecycleAndSLATest#testSlaDeadlineCalculation` | SLA Deadline Calculation | P1 incident (`5m` response, `30m` resolution) | Sets `responseDeadline = createdAt + 5m` and `resolutionDeadline = createdAt + 30m`. |
| **TC-13** | `LifecycleAndSLATest#testSlaViolationAndRiskDetection` | SLA Risk & Breach Detection | Unacknowledged past response deadline vs. `<15%` time remaining | Returns `SLARiskLevel.BREACHED` when overdue and `SLARiskLevel.CRITICAL` when `<15%` time remains. |
| **TC-14** | `LifecycleAndSLATest#testValidIncidentLifecycleTransitions` | Valid Lifecycle Transitions | `NEW -> CLASSIFIED -> ASSIGNED -> ACKNOWLEDGED -> IN_PROGRESS -> WAITING_FOR_USER -> IN_PROGRESS -> RESOLVED -> VERIFIED -> CLOSED` | All valid state transitions succeed and record state entry timestamps. |
| **TC-15** | `LifecycleAndSLATest#testInvalidLifecycleTransitionThrowsException` | Invalid Lifecycle Transition | `NEW -> CLOSED` and `IN_PROGRESS -> CLOSED` | Throws `InvalidStateTransitionException` preventing premature closure before verification. |
| **TC-16** | `IncidentAndSecurityServiceTest#testIncidentCreationNotificationsAndAudit` | Notification Generation | Incident creation & assignment events | `InAppNotificationObserver` and `SimulatedEmailNotificationObserver` persist notifications. |
| **TC-17** | `IncidentAndSecurityServiceTest#testIncidentCreationNotificationsAndAudit` | Audit Record Creation | Incident creation & assignment events | `AuditLogObserver` persists immutable `AuditLog` entries with actor, previous state, and new state. |
| **TC-18** | `LifecycleAndSLATest#testDuplicateIncidentDetection` | Duplicate Incident Detection | Two `Payment Gateway` 504 timeout incidents | `DuplicateDetectionService` computes Jaccard + service boost score `>= 0.50` and links `INC1001`. |
| **TC-19** | `IncidentAndSecurityServiceTest#testDirectAssignUnavailableTechnicianThrowsException` | Direct Invalid Assignment Guard | Admin attempts to assign `available=false` technician | Throws `TechnicianUnavailableException`. |
| **TC-20** | `IncidentAndSecurityServiceTest#testUnauthorizedAccessToAnotherEmployeesIncident` | Cross-Employee Privacy | Employee `EMP102` requests `EMP101` incident | Blocked at service layer with `UnauthorizedOperationException`. |
| **TC-21** | `IncidentAndSecurityServiceTest#testIncidentResolutionVerificationAndClosure` | Incident Resolution | Technician submits `ResolutionRequest` | Transitions to `RESOLVED`, saves `Resolution`, and decrements technician workload. |
| **TC-22** | `IncidentAndSecurityServiceTest#testIncidentResolutionVerificationAndClosure` | Verification & Closure | Employee submits `FeedbackRequest` (`5` stars) | Transitions `RESOLVED -> VERIFIED -> CLOSED` and persists `IncidentFeedback`. |
| **TC-23** | `LifecycleAndSLATest#testAutomaticEscalation` | Automatic Multi-Tier Escalation | Unacknowledged P1 incident past response target | `EscalationService` elevates incident to `LEVEL_2_TEAM_LEAD` and sets status `ESCALATED`. |

The upgrade adds six unit regressions in HardeningTest and seven PostgreSQL scenarios in AccountWorkflowIT. Run the PostgreSQL profile using the command in the project README.
