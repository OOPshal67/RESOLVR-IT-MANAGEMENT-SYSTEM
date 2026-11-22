package com.resolvr;

import com.resolvr.dto.IncidentCreateRequest;
import com.resolvr.dto.PriorityResult;
import com.resolvr.dto.SimilarIncidentMatch;
import com.resolvr.entity.*;
import com.resolvr.enums.*;
import com.resolvr.exception.InvalidStateTransitionException;
import com.resolvr.factory.IncidentFactory;
import com.resolvr.observer.IncidentEventPublisher;
import com.resolvr.repository.*;
import com.resolvr.service.AuditService;
import com.resolvr.service.DuplicateDetectionService;
import com.resolvr.service.EscalationService;
import com.resolvr.service.SLARiskDetectionService;
import com.resolvr.state.IncidentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests 12, 13, 14, 15, 18, 23: SLA Deadline Calculation, SLA Risk & Violation Detection,
 * Valid & Invalid Lifecycle Transitions, Duplicate Incident Detection, and Automatic Escalation.
 */
@ExtendWith(MockitoExtension.class)
class LifecycleAndSLATest {

    private IncidentStateMachine stateMachine;
    private IncidentFactory incidentFactory;
    private SLARiskDetectionService slaRiskService;

    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentRelationshipRepository relationshipRepository;
    @Mock private EscalationRecordRepository escalationRepository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private AdministratorRepository administratorRepository;
    @Mock private AuditService auditService;

    @BeforeEach
    void setUp() {
        stateMachine = new IncidentStateMachine();
        incidentFactory = new IncidentFactory();
        slaRiskService = new SLARiskDetectionService();
    }

    @Test
    @DisplayName("TC-12: SLA Deadline Calculation sets exact response and resolution targets on creation")
    void testSlaDeadlineCalculation() {
        Employee emp = new Employee("emp", "e@resolvr.io", "hash", "Emp", "CS", "111", "EMP101", "Dev", "Block A");
        IncidentCategory cat = new IncidentCategory("APPLICATION", "App", "Desc", 1.25);
        ITService srv = new ITService("SRV-PAY", "Payment Gateway", "Desc", BusinessCriticality.MISSION_CRITICAL, "FinTech", "OPERATIONAL");
        SLAPolicy policy = new SLAPolicy(PriorityLevel.P1, "P1 SLA", 5, 30, 25);
        PriorityResult pr = new PriorityResult(PriorityLevel.P1, 88.0, "P1 Score");

        IncidentCreateRequest req = new IncidentCreateRequest();
        req.setTitle("Payment Gateway Down");
        req.setDescription("Complete checkout outage on payment portal");
        req.setImpact(ImpactLevel.CRITICAL);
        req.setUrgency(UrgencyLevel.CRITICAL);
        req.setAffectedUsers(200);

        Incident inc = incidentFactory.createIncident("INC1001", req, emp, cat, srv, pr, policy);

        assertNotNull(inc.getResponseDeadline());
        assertNotNull(inc.getResolutionDeadline());
        assertEquals(inc.getCreatedAt().plusMinutes(5), inc.getResponseDeadline());
        assertEquals(inc.getCreatedAt().plusMinutes(30), inc.getResolutionDeadline());
    }

    @Test
    @DisplayName("TC-13: SLA Risk Detection identifies BREACHED and CRITICAL risk levels accurately")
    void testSlaViolationAndRiskDetection() {
        SLAPolicy p1Policy = new SLAPolicy(PriorityLevel.P1, "P1 SLA", 5, 30, 25);
        Incident incident = new Incident();
        incident.setPriority(PriorityLevel.P1);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setSlaPolicy(p1Policy);
        LocalDateTime now = LocalDateTime.now();
        incident.setCreatedAt(now.minusMinutes(26));
        incident.setResponseDeadline(now.minusMinutes(21));
        incident.setResolutionDeadline(now.plusMinutes(4));

        // Response deadline already passed without acknowledgment -> BREACHED
        SLARiskLevel breachedRisk = slaRiskService.evaluateRiskLevel(incident, now);
        assertEquals(SLARiskLevel.BREACHED, breachedRisk);

        // Acknowledged on time, 4 mins remaining out of 30 mins (13.3% left) -> CRITICAL
        incident.setFirstResponseAt(now.minusMinutes(24));
        incident.setAcknowledgedAt(now.minusMinutes(24));
        SLARiskLevel criticalRisk = slaRiskService.evaluateRiskLevel(incident, now);
        assertEquals(SLARiskLevel.CRITICAL, criticalRisk);
    }

    @Test
    @DisplayName("TC-14: Valid Incident Lifecycle Transitions succeed from NEW through CLOSED")
    void testValidIncidentLifecycleTransitions() {
        Incident inc = new Incident();
        inc.setIncidentKey("INC1001");
        inc.setStatus(IncidentStatus.NEW);

        stateMachine.transition(inc, IncidentStatus.CLASSIFIED);
        assertEquals(IncidentStatus.CLASSIFIED, inc.getStatus());

        stateMachine.transition(inc, IncidentStatus.ASSIGNED);
        assertEquals(IncidentStatus.ASSIGNED, inc.getStatus());
        assertNotNull(inc.getAssignedAt());

        stateMachine.transition(inc, IncidentStatus.ACKNOWLEDGED);
        assertEquals(IncidentStatus.ACKNOWLEDGED, inc.getStatus());
        assertNotNull(inc.getAcknowledgedAt());

        stateMachine.transition(inc, IncidentStatus.IN_PROGRESS);
        assertEquals(IncidentStatus.IN_PROGRESS, inc.getStatus());

        stateMachine.transition(inc, IncidentStatus.WAITING_FOR_USER);
        assertEquals(IncidentStatus.WAITING_FOR_USER, inc.getStatus());

        stateMachine.transition(inc, IncidentStatus.IN_PROGRESS);
        stateMachine.transition(inc, IncidentStatus.RESOLVED);
        assertEquals(IncidentStatus.RESOLVED, inc.getStatus());
        assertNotNull(inc.getResolvedAt());

        stateMachine.transition(inc, IncidentStatus.VERIFIED);
        assertEquals(IncidentStatus.VERIFIED, inc.getStatus());

        stateMachine.transition(inc, IncidentStatus.CLOSED);
        assertEquals(IncidentStatus.CLOSED, inc.getStatus());
        assertNotNull(inc.getClosedAt());
    }

    @Test
    @DisplayName("TC-15: Invalid Lifecycle Transition (NEW -> CLOSED or IN_PROGRESS -> CLOSED) throws InvalidStateTransitionException")
    void testInvalidLifecycleTransitionThrowsException() {
        Incident inc = new Incident();
        inc.setIncidentKey("INC1005");
        inc.setStatus(IncidentStatus.NEW);

        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transition(inc, IncidentStatus.CLOSED));

        inc.setStatus(IncidentStatus.IN_PROGRESS);
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transition(inc, IncidentStatus.CLOSED));
    }

    @Test
    @DisplayName("TC-18: Duplicate Incident Detection identifies similar active incidents via Jaccard Similarity")
    void testDuplicateIncidentDetection() {
        DuplicateDetectionService dupService = new DuplicateDetectionService(
                incidentRepository, relationshipRepository, auditService);

        IncidentCategory cat = new IncidentCategory("APPLICATION", "App", "Desc", 1.25);
        cat.setId(1L);
        ITService srv = new ITService("SRV-PAY", "Payment Gateway", "Desc", BusinessCriticality.MISSION_CRITICAL, "FinTech", "OPERATIONAL");
        srv.setId(1L);

        Incident existing = new Incident();
        existing.setId(10L);
        existing.setIncidentKey("INC1001");
        existing.setTitle("Payment Gateway checkout failing with HTTP 504 timeout");
        existing.setDescription("Users cannot complete tuition fee payment on the Payment Gateway due to 504 gateway timeout.");
        existing.setCategory(cat);
        existing.setService(srv);
        existing.setStatus(IncidentStatus.IN_PROGRESS);

        Incident incoming = new Incident();
        incoming.setId(11L);
        incoming.setIncidentKey("INC1002");
        incoming.setTitle("Payment Gateway fee checkout HTTP 504 timeout error");
        incoming.setDescription("Students attempting tuition fee payment on Payment Gateway receive 504 gateway timeout.");
        incoming.setCategory(cat);
        incoming.setService(srv);
        incoming.setStatus(IncidentStatus.CLASSIFIED);

        when(incidentRepository.findByStatusNotIn(any())).thenReturn(List.of(existing));

        List<SimilarIncidentMatch> matches = dupService.detectAndRecordSimilarIncidents(incoming);
        assertFalse(matches.isEmpty());
        assertEquals("INC1001", matches.get(0).getMatchedIncident().getIncidentKey());
        assertTrue(matches.get(0).getSimilarityScore() >= 0.50);
    }

    @Test
    @DisplayName("TC-23: Automatic Escalation elevates unacknowledged critical incident to Level 2 Team Lead")
    void testAutomaticEscalation() {
        IncidentEventPublisher publisher = new IncidentEventPublisher(List.of());
        EscalationService escalationService = new EscalationService(
                escalationRepository, incidentRepository, technicianRepository, administratorRepository, publisher);

        SLAPolicy p1Policy = new SLAPolicy(PriorityLevel.P1, "P1 SLA", 5, 30, 25);
        Incident inc = new Incident();
        inc.setId(1L);
        inc.setIncidentKey("INC1002");
        inc.setPriority(PriorityLevel.P1);
        inc.setStatus(IncidentStatus.ASSIGNED);
        inc.setSlaPolicy(p1Policy);
        inc.setCreatedAt(LocalDateTime.now().minusMinutes(7));
        inc.setEscalationLevel(EscalationLevel.NONE);

        when(escalationRepository.existsByIncidentAndEscalationLevel(inc, EscalationLevel.LEVEL_2_TEAM_LEAD)).thenReturn(false);
        when(escalationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EscalationRecord record = escalationService.evaluateAndEscalateIfNeeded(inc, LocalDateTime.now());
        assertNotNull(record);
        assertEquals(EscalationLevel.LEVEL_2_TEAM_LEAD, record.getEscalationLevel());
        assertEquals(IncidentStatus.ESCALATED, inc.getStatus());
    }
}
