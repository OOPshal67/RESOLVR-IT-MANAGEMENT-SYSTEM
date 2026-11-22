package com.resolvr;

import com.resolvr.dto.FeedbackRequest;
import com.resolvr.dto.IncidentCreateRequest;
import com.resolvr.dto.RegistrationRequest;
import com.resolvr.dto.ResolutionRequest;
import com.resolvr.entity.*;
import com.resolvr.enums.*;
import com.resolvr.exception.TechnicianUnavailableException;
import com.resolvr.exception.UnauthorizedOperationException;
import com.resolvr.factory.IncidentFactory;
import com.resolvr.FactoryAndObserverMocks;
import com.resolvr.observer.*;
import com.resolvr.repository.*;
import com.resolvr.security.CustomUserDetailsService;
import com.resolvr.service.*;
import com.resolvr.state.IncidentStateMachine;
import com.resolvr.strategy.LoadBasedAssignmentStrategy;
import com.resolvr.strategy.SkillBasedAssignmentStrategy;
import com.resolvr.strategy.WeightedPriorityCalculationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests 1, 2, 3, 4, 5, 6, 16, 17, 19, 20, 21, 22: Registration, Login, Role Authorization,
 * Incident Creation, ID Uniqueness, Notifications, Audit Trail, Unauthorized Incident Access,
 * Incident Resolution, Verification, and Closure.
 */
@ExtendWith(MockitoExtension.class)
class IncidentAndSecurityServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private AdministratorRepository administratorRepository;
    @Mock private TechnicianSkillRepository skillRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private InvestigationNoteRepository noteRepository;
    @Mock private InformationRequestRepository infoRequestRepository;
    @Mock private ResolutionRepository resolutionRepository;
    @Mock private IncidentFeedbackRepository feedbackRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CatalogService catalogService;
    @Mock private DuplicateDetectionService duplicateDetectionService;

    private final String testPassword = java.util.UUID.randomUUID().toString();
    private PasswordEncoder passwordEncoder;
    private UserService userService;
    private IncidentService incidentService;
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        AuditService auditService = new AuditService(auditLogRepository);
        userService = new UserService(userRepository, employeeRepository, technicianRepository,
                administratorRepository, skillRepository, passwordEncoder, auditService);
        userDetailsService = new CustomUserDetailsService(userRepository);

        AuditLogObserver auditObserver = new AuditLogObserver(auditLogRepository);
        InAppNotificationObserver inAppObserver = new InAppNotificationObserver(
                notificationRepository, new com.resolvr.factory.NotificationFactory());
        SimulatedEmailNotificationObserver emailObserver = new SimulatedEmailNotificationObserver(
                notificationRepository, new com.resolvr.factory.NotificationFactory());
        IncidentEventPublisher eventPublisher = new IncidentEventPublisher(
                List.of(auditObserver, inAppObserver, emailObserver));

        incidentService = new IncidentService(
                incidentRepository, assignmentRepository, noteRepository, infoRequestRepository,
                resolutionRepository, feedbackRepository, technicianRepository, administratorRepository,
                catalogService, duplicateDetectionService, new SLARiskDetectionService(),
                new IncidentFactory(), new IncidentStateMachine(), eventPublisher,
                List.of(new WeightedPriorityCalculationStrategy()),
                List.of(new SkillBasedAssignmentStrategy(), new LoadBasedAssignmentStrategy())
        );
    }

    @Test
    @DisplayName("TC-01: Employee Registration hashes password with BCrypt and generates unique employee code")
    void testEmployeeRegistration() {
        RegistrationRequest req = new RegistrationRequest();
        req.setUsername("new.employee");
        req.setEmail("new.emp@resolvr.io");
        req.setPassword(testPassword);
        req.setFullName("New Employee");
        req.setDepartment("Engineering");

        when(userRepository.existsByUsername("new.employee")).thenReturn(false);
        when(userRepository.existsByEmail("new.emp@resolvr.io")).thenReturn(false);
        when(employeeRepository.nextEmployeeNumber()).thenReturn(104L);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee saved = userService.registerEmployee(req);

        assertEquals("EMP104", saved.getEmployeeCode());
        assertEquals(Role.ROLE_EMPLOYEE, saved.getRole());
        assertNotEquals(testPassword, saved.getPasswordHash());
        assertTrue(passwordEncoder.matches(testPassword, saved.getPasswordHash()));
    }

    @Test
    @DisplayName("TC-02 & TC-03: Authentication UserDetails lookup succeeds for valid user and rejects unknown user")
    void testAuthenticationValidAndInvalidLogin() {
        Employee emp = new Employee("sahith.emp", "sahith@resolvr.io", passwordEncoder.encode(testPassword),
                "Krishna Sahith", "CS", "123", "EMP101", "Student", "Block VK");
        when(userRepository.findByUsername("sahith.emp")).thenReturn(Optional.of(emp));
        when(userRepository.findByUsername("unknown.user")).thenReturn(Optional.empty());

        UserDetails details = userDetailsService.loadUserByUsername("sahith.emp");
        assertEquals("sahith.emp", details.getUsername());
        assertTrue(passwordEncoder.matches(testPassword, details.getPassword()));

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("unknown.user"));
    }

    @Test
    @DisplayName("TC-04 & TC-20: Role-based authorization blocks Employee from accessing another Employee's incident")
    void testUnauthorizedAccessToAnotherEmployeesIncident() {
        Employee owner = new Employee("owner", "o@resolvr.io", "hash", "Owner", "CS", "1", "EMP101", "Dev", "A");
        owner.setId(101L);

        Employee intruder = new Employee("intruder", "i@resolvr.io", "hash", "Intruder", "CS", "2", "EMP102", "Dev", "B");
        intruder.setId(102L);

        Incident incident = new Incident();
        incident.setId(500L);
        incident.setIncidentKey("INC1001");
        incident.setReporter(owner);

        when(incidentRepository.findById(500L)).thenReturn(Optional.of(incident));

        assertThrows(UnauthorizedOperationException.class, () ->
                incidentService.getIncidentAuthorized(500L, intruder));
        assertDoesNotThrow(() ->
                incidentService.getIncidentAuthorized(500L, owner));
    }

    @Test
    @DisplayName("TC-05, TC-06, TC-16, TC-17: Incident Creation generates unique ID, triggers Notifications & Audit Logs")
    void testIncidentCreationNotificationsAndAudit() {
        Employee reporter = new Employee("sahith.emp", "s@resolvr.io", "hash", "Krishna Sahith", "CS", "1", "EMP101", "Dev", "A");
        reporter.setId(1L);

        IncidentCategory cat = new IncidentCategory("APPLICATION", "App", "Desc", 1.25);
        cat.setId(1L);
        ITService srv = new ITService("SRV-PAY", "Payment Gateway", "Desc", BusinessCriticality.MISSION_CRITICAL, "FinTech", "OPERATIONAL");
        srv.setId(1L);
        SLAPolicy sla = new SLAPolicy(PriorityLevel.P1, "P1 SLA", 5, 30, 25);

        Technician tech = new Technician("arjun.tech", "a@resolvr.io", "hash", "Arjun Nair", "SRE", "2", "TECH201", "APPLICATION", 5, 5, true);
        tech.setId(20L);

        when(catalogService.getConfigValue(any(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(catalogService.getCategoryById(1L)).thenReturn(cat);
        when(catalogService.getServiceById(1L)).thenReturn(srv);
        when(catalogService.getSLAPolicyForPriority(PriorityLevel.P1)).thenReturn(sla);
        when(incidentRepository.nextIncidentNumber()).thenReturn(1001L);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            if (i.getId() == null) i.setId(1L);
            return i;
        });
        when(incidentRepository.findById(1L)).thenAnswer(inv -> {
            Incident i = new Incident();
            i.setId(1L);
            i.setIncidentKey("INC1001");
            i.setReporter(reporter);
            i.setCategory(cat);
            i.setService(srv);
            i.setPriority(PriorityLevel.P1);
            i.setStatus(IncidentStatus.ASSIGNED);
            i.setAssignedTechnician(tech);
            i.setSlaPolicy(sla);
            return Optional.of(i);
        });
        when(technicianRepository.findByAvailableTrueAndActiveTrue()).thenReturn(List.of(tech));
        when(technicianRepository.findById(20L)).thenReturn(Optional.of(tech));

        IncidentCreateRequest req = new IncidentCreateRequest();
        req.setTitle("Payment Gateway HTTP 504 Outage");
        req.setDescription("Critical checkout failure across campus payment portal");
        req.setCategoryId(1L);
        req.setServiceId(1L);
        req.setImpact(ImpactLevel.CRITICAL);
        req.setUrgency(UrgencyLevel.CRITICAL);
        req.setAffectedUsers(300);

        Incident created = incidentService.createIncident(req, reporter);

        assertEquals("INC1001", created.getIncidentKey());
        assertEquals(PriorityLevel.P1, created.getPriority());
        assertEquals(IncidentStatus.ASSIGNED, created.getStatus());
        verify(auditLogRepository, atLeast(2)).save(any(AuditLog.class));
        verify(notificationRepository, atLeast(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC-21 & TC-22: Incident Resolution, Employee Verification, and Closure complete full workflow")
    void testIncidentResolutionVerificationAndClosure() {
        Employee reporter = new Employee("sahith.emp", "s@resolvr.io", "hash", "Krishna Sahith", "CS", "1", "EMP101", "Dev", "A");
        reporter.setId(1L);
        Technician tech = new Technician("arjun.tech", "a@resolvr.io", "hash", "Arjun Nair", "SRE", "2", "TECH201", "APPLICATION", 5, 5, true);
        tech.setId(20L);
        tech.setCurrentWorkload(2);

        SLAPolicy sla = new SLAPolicy(PriorityLevel.P1, "P1 SLA", 5, 30, 25);
        Incident incident = new Incident();
        incident.setId(10L);
        incident.setIncidentKey("INC1001");
        incident.setReporter(reporter);
        incident.setAssignedTechnician(tech);
        incident.setPriority(PriorityLevel.P1);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setSlaPolicy(sla);
        incident.setResolutionDeadline( java.time.LocalDateTime.now().plusMinutes(15) );

        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
        when(resolutionRepository.findByIncident(incident)).thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolutionRequest resReq = new ResolutionRequest();
        resReq.setRootCause("Connection pool saturation on payment gateway");
        resReq.setResolutionSummary("Increased HikariCP maxPoolSize and scaled replicas");
        resReq.setTimeSpentMinutes(25);

        Incident resolved = incidentService.resolveIncident(10L, resReq, tech);
        assertEquals(IncidentStatus.RESOLVED, resolved.getStatus());
        assertEquals(1, tech.getCurrentWorkload());
        assertNotNull(resolved.getResolution());

        when(feedbackRepository.findByIncident(incident)).thenReturn(Optional.empty());
        FeedbackRequest fbReq = new FeedbackRequest();
        fbReq.setRating(5);
        fbReq.setComments("Great fix!");
        fbReq.setClosureNotes("Confirmed working on checkout page.");

        Incident closed = incidentService.verifyAndCloseIncident(10L, fbReq, reporter);
        assertEquals(IncidentStatus.CLOSED, closed.getStatus());
        assertNotNull(closed.getVerifiedAt());
        assertNotNull(closed.getClosedAt());
        assertNotNull(closed.getFeedback());
        assertEquals(5, closed.getFeedback().getRating());
    }

    @Test
    @DisplayName("TC-19: Assigning an unavailable technician directly throws TechnicianUnavailableException")
    void testDirectAssignUnavailableTechnicianThrowsException() {
        Incident incident = new Incident();
        incident.setId(99L);
        incident.setIncidentKey("INC1099");
        incident.setStatus(IncidentStatus.CLASSIFIED);

        Technician unavailable = new Technician("tech", "t@resolvr.io", "hash", "Unavailable Tech", "IT", "1", "TECH209", "NET", 5, 2, false);
        unavailable.setId(99L);
        unavailable.setAvailable(false);

        when(incidentRepository.findById(99L)).thenReturn(Optional.of(incident));
        when(technicianRepository.findById(99L)).thenReturn(Optional.of(unavailable));

        assertThrows(TechnicianUnavailableException.class, () ->
                incidentService.assignIncidentToTechnician(99L, 99L, "admin", "MANUAL", "Test"));
    }
}
