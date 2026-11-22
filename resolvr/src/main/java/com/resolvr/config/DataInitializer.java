package com.resolvr.config;

import com.resolvr.entity.*;
import com.resolvr.enums.*;
import com.resolvr.repository.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Profile("dev")
@Order(1)
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEMO_ADMIN_PASSWORD = "ResolvrAdmin@2026!";
    private static final String DEMO_TECHNICIAN_PASSWORD = "ResolvrTech@2026!";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final TechnicianRepository technicianRepository;
    private final AdministratorRepository administratorRepository;
    private final TechnicianSkillRepository skillRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final IncidentRepository incidentRepository;
    private final AssignmentRepository assignmentRepository;
    private final InvestigationNoteRepository noteRepository;
    private final InformationRequestRepository infoRequestRepository;
    private final ResolutionRepository resolutionRepository;
    private final IncidentFeedbackRepository feedbackRepository;
    private final IncidentRelationshipRepository relationshipRepository;
    private final SLAViolationRepository slaViolationRepository;
    private final EscalationRecordRepository escalationRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApplicationConfigurationRepository configRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public DataInitializer(UserRepository userRepository,
                           EmployeeRepository employeeRepository,
                           TechnicianRepository technicianRepository,
                           AdministratorRepository administratorRepository,
                           TechnicianSkillRepository skillRepository,
                           IncidentCategoryRepository categoryRepository,
                           ITServiceRepository serviceRepository,
                           SLAPolicyRepository slaPolicyRepository,
                           IncidentRepository incidentRepository,
                           AssignmentRepository assignmentRepository,
                           InvestigationNoteRepository noteRepository,
                           InformationRequestRepository infoRequestRepository,
                           ResolutionRepository resolutionRepository,
                           IncidentFeedbackRepository feedbackRepository,
                           IncidentRelationshipRepository relationshipRepository,
                           SLAViolationRepository slaViolationRepository,
                           EscalationRecordRepository escalationRepository,
                           NotificationRepository notificationRepository,
                           AuditLogRepository auditLogRepository,
                           ApplicationConfigurationRepository configRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.technicianRepository = technicianRepository;
        this.administratorRepository = administratorRepository;
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.incidentRepository = incidentRepository;
        this.assignmentRepository = assignmentRepository;
        this.noteRepository = noteRepository;
        this.infoRequestRepository = infoRequestRepository;
        this.resolutionRepository = resolutionRepository;
        this.feedbackRepository = feedbackRepository;
        this.relationshipRepository = relationshipRepository;
        this.slaViolationRepository = slaViolationRepository;
        this.escalationRepository = escalationRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.configRepository = configRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            refreshExistingDemoCredentials();
            log.info("RESOLVR database already seeded ({} users, {} incidents). Skipping sample data initialization.",
                    userRepository.count(), incidentRepository.count());
            return;
        }

        log.info("Seeding RESOLVR enterprise demonstration dataset...");

        saveConfigIfAbsent("ACTIVE_ASSIGNMENT_STRATEGY", "SKILL_BASED", "Active Technician Assignment Strategy");
        saveConfigIfAbsent("ACTIVE_PRIORITY_STRATEGY", "WEIGHTED_SCORE", "Active Priority Calculation Strategy");
        saveConfigIfAbsent("AUTOMATIC_ESCALATION_ENABLED", "TRUE", "Automatic Multi-Tier Escalation Enabled");

        SLAPolicy slaP1 = slaPolicyRepository.findByPriorityLevel(PriorityLevel.P1).orElseThrow();
        SLAPolicy slaP2 = slaPolicyRepository.findByPriorityLevel(PriorityLevel.P2).orElseThrow();
        SLAPolicy slaP3 = slaPolicyRepository.findByPriorityLevel(PriorityLevel.P3).orElseThrow();
        SLAPolicy slaP4 = slaPolicyRepository.findByPriorityLevel(PriorityLevel.P4).orElseThrow();

        IncidentCategory catHardware = categoryRepository.findByCode("HARDWARE").orElseThrow();
        IncidentCategory catSoftware = categoryRepository.findByCode("SOFTWARE").orElseThrow();
        IncidentCategory catNetwork = categoryRepository.findByCode("NETWORK").orElseThrow();
        IncidentCategory catDatabase = categoryRepository.findByCode("DATABASE").orElseThrow();
        IncidentCategory catCloud = categoryRepository.findByCode("CLOUD").orElseThrow();
        IncidentCategory catSecurity = categoryRepository.findByCode("SECURITY").orElseThrow();
        IncidentCategory catAuth = categoryRepository.findByCode("AUTHENTICATION").orElseThrow();
        IncidentCategory catApp = categoryRepository.findByCode("APPLICATION").orElseThrow();
        IncidentCategory catStorage = categoryRepository.findByCode("STORAGE").orElseThrow();

        ITService srvPayment = serviceRepository.findByServiceCode("SRV-PAY").orElseThrow();
        ITService srvWifi = serviceRepository.findByServiceCode("SRV-WIFI").orElseThrow();
        ITService srvAuth = serviceRepository.findByServiceCode("SRV-AUTH").orElseThrow();
        ITService srvCloud = serviceRepository.findByServiceCode("SRV-CLOUD").orElseThrow();
        ITService srvDb = serviceRepository.findByServiceCode("SRV-DB").orElseThrow();
        ITService srvEmail = serviceRepository.findByServiceCode("SRV-MAIL").orElseThrow();
        ITService srvLms = serviceRepository.findByServiceCode("SRV-LMS").orElseThrow();

        TechnicianSkill skNet = skillRepository.findBySkillCode("SKILL_NETWORK").orElseThrow();
        TechnicianSkill skDb = skillRepository.findBySkillCode("SKILL_DATABASE").orElseThrow();
        TechnicianSkill skApp = skillRepository.findBySkillCode("SKILL_APPLICATION").orElseThrow();
        TechnicianSkill skSec = skillRepository.findBySkillCode("SKILL_SECURITY").orElseThrow();
        TechnicianSkill skAuth = skillRepository.findBySkillCode("SKILL_AUTH").orElseThrow();
        TechnicianSkill skCloud = skillRepository.findBySkillCode("SKILL_CLOUD").orElseThrow();
        TechnicianSkill skHw = skillRepository.findBySkillCode("SKILL_HARDWARE").orElseThrow();
        TechnicianSkill skSw = skillRepository.findBySkillCode("SKILL_SOFTWARE").orElseThrow();
        TechnicianSkill skStor = skillRepository.findBySkillCode("SKILL_STORAGE").orElseThrow();

        Administrator admin = new Administrator(
            "admin", "admin@resolvr.io", demoPassword("admin"),
            "Vikramaditya Rao", "IT Governance & Service Management", "+91-40-6630-1001",
            "ADM001", "FULL_SYSTEM", "GLOBAL_ENTERPRISE"
        );
        administratorRepository.save(admin);

        Technician tech1 = new Technician(
            "arjun.tech", "arjun.nair@resolvr.io", demoPassword("arjun.tech"),
            "Arjun Nair", "Application & FinTech SRE", "+91-40-6630-2001",
            "TECH201", "APPLICATION, DATABASE, PAYMENT", 6, 6, true
        );
        tech1.setSkills(Set.of(skApp, skDb));
        tech1.setCurrentWorkload(3);
        technicianRepository.save(tech1);

        Technician tech2 = new Technician(
            "priya.tech", "priya.sharma@resolvr.io", demoPassword("priya.tech"),
            "Priya Sharma", "Network & Cloud Infrastructure", "+91-40-6630-2002",
            "TECH202", "NETWORK, CLOUD, STORAGE", 6, 4, false
        );
        tech2.setSkills(Set.of(skNet, skCloud, skStor));
        tech2.setCurrentWorkload(2);
        technicianRepository.save(tech2);

        Technician tech3 = new Technician(
            "rohan.tech", "rohan.verma@resolvr.io", demoPassword("rohan.tech"),
            "Rohan Verma", "Cybersecurity & Identity IAM", "+91-40-6630-2003",
            "TECH203", "SECURITY, AUTHENTICATION", 5, 5, true
        );
        tech3.setSkills(Set.of(skSec, skAuth));
        tech3.setCurrentWorkload(2);
        technicianRepository.save(tech3);

        Technician tech4 = new Technician(
            "sneha.tech", "sneha.reddy@resolvr.io", demoPassword("sneha.tech"),
            "Sneha Reddy", "Workplace Endpoint & Software Support", "+91-40-6630-2004",
            "TECH204", "HARDWARE, SOFTWARE", 5, 3, false
        );
        tech4.setSkills(Set.of(skHw, skSw));
        tech4.setCurrentWorkload(2);
        technicianRepository.save(tech4);

        Employee emp1 = new Employee(
            "sahith.emp", "sahith.aggala@resolvr.io", demoPassword("sahith.emp"),
            "Krishna Sahith", "Computer Science & Engineering", "+91-40-6630-3001",
            "EMP101", "Systems Research Associate", "Academic Block C - Room 204"
        );
        employeeRepository.save(emp1);

        Employee emp2 = new Employee(
            "ananya.emp", "ananya.menon@resolvr.io", demoPassword("ananya.emp"),
            "Ananya Menon", "Finance & Billing Operations", "+91-40-6630-3002",
            "EMP102", "Senior Financial Analyst", "Admin Wing - Floor 2"
        );
        employeeRepository.save(emp2);

        Employee emp3 = new Employee(
            "karthik.emp", "karthik.iyer@resolvr.io", demoPassword("karthik.emp"),
            "Karthik Iyer", "Digital Humanities & Academic Affairs", "+91-40-6630-3003",
            "EMP103", "Program Coordinator", "Faculty Block B - Room 112"
        );
        employeeRepository.save(emp3);

        LocalDateTime now = LocalDateTime.now();

        Incident inc1 = buildIncident("INC1001",
            "Payment Gateway checkout failing with HTTP 504 Gateway Timeout",
            "Multiple students and finance staff are unable to complete semester tuition and fee transactions on the Payment Gateway. Checkout requests hang for 30 seconds and return HTTP 504 timeout from the upstream bank connector.",
            emp2, catApp, srvPayment, ImpactLevel.CRITICAL, UrgencyLevel.CRITICAL, 350,
            PriorityLevel.P1, 88.0, "Weighted Score 88.0 [Impact(CRITICAL=4) × Urgency(CRITICAL=4) × Criticality(MISSION_CRITICAL=4) × CatWeight(1.25) + UserAdj(+8.0 for 350 users)] => P1 - Critical",
            IncidentStatus.IN_PROGRESS, tech1, slaP1,
            now.minusMinutes(22), now.minusMinutes(21), now.minusMinutes(18),
            SLARiskLevel.HIGH, EscalationLevel.NONE);

        Incident inc2 = buildIncident("INC1002",
            "Duplicate charge error and timeout on Payment Gateway portal",
            "Users attempting fee payment on the Payment Gateway experience a gateway timeout on checkout, and retrying triggers a duplicate transaction lock warning.",
            emp1, catApp, srvPayment, ImpactLevel.CRITICAL, UrgencyLevel.HIGH, 180,
            PriorityLevel.P1, 68.0, "Weighted Score 68.0 [Impact(CRITICAL=4) × Urgency(HIGH=3) × Criticality(MISSION_CRITICAL=4) × CatWeight(1.25) + UserAdj(+8.0 for 180 users)] => P1 - Critical",
            IncidentStatus.ESCALATED, tech1, slaP1,
            now.minusMinutes(38), now.minusMinutes(37), null,
            SLARiskLevel.BREACHED, EscalationLevel.LEVEL_2_TEAM_LEAD);
        inc2.setResponseSlaViolated(true);
        inc2.setResolutionSlaViolated(true);
        inc2.setSlaWarningSent(true);
        incidentRepository.save(inc2);

        Incident inc3 = buildIncident("INC1003",
            "Primary PostgreSQL Database Server read replica lag exceeding 45 seconds",
            "Reporting queries and LMS gradebook lookups are returning stale records due to WAL replay delay on the secondary PostgreSQL node.",
            emp1, catDatabase, srvDb, ImpactLevel.HIGH, UrgencyLevel.HIGH, 120,
            PriorityLevel.P1, 54.8, "Weighted Score 54.8 [Impact(HIGH=3) × Urgency(HIGH=3) × Criticality(MISSION_CRITICAL=4) × CatWeight(1.30) + UserAdj(+8.0 for 120 users)] => P1 - Critical",
            IncidentStatus.ACKNOWLEDGED, tech1, slaP1,
            now.minusMinutes(12), now.minusMinutes(11), now.minusMinutes(9),
            SLARiskLevel.MEDIUM, EscalationLevel.NONE);

        Incident inc4 = buildIncident("INC1004",
            "SSO Authentication System MFA token validation rejecting valid TOTP codes",
            "Faculty and students logging into the central SSO portal receive Invalid MFA Code errors despite synchronized authenticator clocks.",
            emp3, catAuth, srvAuth, ImpactLevel.HIGH, UrgencyLevel.HIGH, 210,
            PriorityLevel.P1, 54.8, "Weighted Score 54.8 [Impact(HIGH=3) × Urgency(HIGH=3) × Criticality(MISSION_CRITICAL=4) × CatWeight(1.30) + UserAdj(+8.0 for 210 users)] => P1 - Critical",
            IncidentStatus.IN_PROGRESS, tech3, slaP1,
            now.minusMinutes(25), now.minusMinutes(24), now.minusMinutes(21),
            SLARiskLevel.HIGH, EscalationLevel.NONE);

        Incident inc5 = buildIncident("INC1005",
            "Campus Wi-Fi packet loss and intermittent disconnects in Hostel Block VK",
            "Students in Hostel Block VK and Library Annex experience 40% packet loss and frequent WPA3 Enterprise re-authentication drops.",
            emp1, catNetwork, srvWifi, ImpactLevel.HIGH, UrgencyLevel.MEDIUM, 85,
            PriorityLevel.P2, 25.6, "Weighted Score 25.6 [Impact(HIGH=3) × Urgency(MEDIUM=2) × Criticality(HIGH=3) × CatWeight(1.20) + UserAdj(+4.0 for 85 users)] => P2 - High",
            IncidentStatus.IN_PROGRESS, tech2, slaP2,
            now.minusMinutes(65), now.minusMinutes(62), now.minusMinutes(55),
            SLARiskLevel.MEDIUM, EscalationLevel.NONE);

        Incident inc6 = buildIncident("INC1006",
            "Wireless access point unreachable in Hostel Block VK corridor",
            "Campus Wi-Fi signal is dropping repeatedly in Hostel Block VK rooms 001-020 with high latency during evening peak hours.",
            emp3, catNetwork, srvWifi, ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, 45,
            PriorityLevel.P2, 18.4, "Weighted Score 18.4 [Impact(MEDIUM=2) × Urgency(MEDIUM=2) × Criticality(HIGH=3) × CatWeight(1.20) + UserAdj(+4.0 for 45 users)] => P2 - High",
            IncidentStatus.ASSIGNED, tech2, slaP2,
            now.minusMinutes(10), now.minusMinutes(9), null,
            SLARiskLevel.LOW, EscalationLevel.NONE);

        Incident inc7 = buildIncident("INC1007",
            "Firewall IDS alert on suspicious outbound TLS traffic from lab subnet",
            "Automated security sensor flagged unusual outbound connections on port 8443 originating from Computer Lab 3.",
            emp1, catSecurity, srvAuth, ImpactLevel.MEDIUM, UrgencyLevel.HIGH, 15,
            PriorityLevel.P2, 35.6, "Weighted Score 35.6 [Impact(MEDIUM=2) × Urgency(HIGH=3) × Criticality(MISSION_CRITICAL=4) × CatWeight(1.40) + UserAdj(+2.0 for 15 users)] => P2 - High",
            IncidentStatus.WAITING_FOR_USER, tech3, slaP2,
            now.minusMinutes(90), now.minusMinutes(85), now.minusMinutes(78),
            SLARiskLevel.MEDIUM, EscalationLevel.NONE);

        Incident inc8 = buildIncident("INC1008",
            "Learning Management System lecture video upload failing for files over 250MB",
            "Course instructors cannot upload recorded HD lecture videos to the LMS repository; upload stops at 99% with HTTP 413 Payload Too Large.",
            emp3, catCloud, srvLms, ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, 32,
            PriorityLevel.P2, 19.0, "Weighted Score 19.0 [Impact(MEDIUM=2) × Urgency(MEDIUM=2) × Criticality(HIGH=3) × CatWeight(1.25) + UserAdj(+4.0 for 32 users)] => P2 - High",
            IncidentStatus.RESOLVED, tech2, slaP2,
            now.minusHours(4), now.minusHours(4).plusMinutes(4), now.minusHours(4).plusMinutes(9),
            SLARiskLevel.LOW, EscalationLevel.NONE);
        inc8.setResolvedAt(now.minusHours(2).minusMinutes(20));
        incidentRepository.save(inc8);

        Incident inc9 = buildIncident("INC1009",
            "Cloud Storage shared research bucket quota synchronization delay",
            "Research teams expanding their project storage allocation do not see updated bucket quotas reflected in the client mount.",
            emp1, catStorage, srvCloud, ImpactLevel.MEDIUM, UrgencyLevel.LOW, 12,
            PriorityLevel.P3, 8.6, "Weighted Score 8.6 [Impact(MEDIUM=2) × Urgency(LOW=1) × Criticality(HIGH=3) × CatWeight(1.10) + UserAdj(+2.0 for 12 users)] => P3 - Medium",
            IncidentStatus.IN_PROGRESS, tech4, slaP3,
            now.minusHours(3), now.minusHours(3).plusMinutes(10), now.minusHours(2).minusMinutes(40),
            SLARiskLevel.LOW, EscalationLevel.NONE);

        Incident inc10 = buildIncident("INC1010",
            "IntelliJ IDEA Ultimate educational license server renewal error on lab PCs",
            "JetBrains floating license server in Software Lab 2 displays certificate validation warning on startup.",
            emp1, catSoftware, srvLms, ImpactLevel.LOW, UrgencyLevel.MEDIUM, 18,
            PriorityLevel.P3, 8.0, "Weighted Score 8.0 [Impact(LOW=1) × Urgency(MEDIUM=2) × Criticality(HIGH=3) × CatWeight(1.00) + UserAdj(+2.0 for 18 users)] => P3 - Medium",
            IncidentStatus.ASSIGNED, tech4, slaP3,
            now.minusMinutes(40), now.minusMinutes(38), null,
            SLARiskLevel.LOW, EscalationLevel.NONE);

        Incident inc11 = buildIncident("INC1011",
            "Departmental Email Service distribution list delayed delivery",
            "Circular announcements sent to the all-faculty mailing list take 25 minutes to arrive in recipient inboxes.",
            emp2, catApp, srvEmail, ImpactLevel.MEDIUM, UrgencyLevel.LOW, 40,
            PriorityLevel.P3, 9.0, "Weighted Score 9.0 [Impact(MEDIUM=2) × Urgency(LOW=1) × Criticality(MEDIUM=2) × CatWeight(1.25) + UserAdj(+4.0 for 40 users)] => P3 - Medium",
            IncidentStatus.RESOLVED, tech1, slaP3,
            now.minusHours(9), now.minusHours(9).plusMinutes(12), now.minusHours(8).minusMinutes(45),
            SLARiskLevel.LOW, EscalationLevel.NONE);
        inc11.setResolvedAt(now.minusHours(5));
        incidentRepository.save(inc11);

        Incident inc12 = buildIncident("INC1012",
            "Docking station dual-monitor HDMI port flickering in Finance Office",
            "Secondary external monitor connected via USB-C hub flickers intermittently when opening large spreadsheets.",
            emp2, catHardware, srvEmail, ImpactLevel.LOW, UrgencyLevel.LOW, 1,
            PriorityLevel.P4, 2.0, "Weighted Score 2.0 [Impact(LOW=1) × Urgency(LOW=1) × Criticality(MEDIUM=2) × CatWeight(1.00) + UserAdj(+0.0 for 1 user)] => P4 - Low",
            IncidentStatus.VERIFIED, tech4, slaP4,
            now.minusHours(18), now.minusHours(17).minusMinutes(40), now.minusHours(17),
            SLARiskLevel.LOW, EscalationLevel.NONE);
        inc12.setResolvedAt(now.minusHours(12));
        inc12.setVerifiedAt(now.minusHours(10));
        incidentRepository.save(inc12);

        Incident inc13 = buildIncident("INC1013",
            "Password reset email link expiring prematurely on Authentication System",
            "Self-service password recovery links generated for external guest lecturers report token expired after 2 minutes.",
            emp3, catAuth, srvAuth, ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, 8,
            PriorityLevel.P2, 22.8, "Weighted Score 22.8 [Impact(MEDIUM=2) × Urgency(MEDIUM=2) × Criticality(MISSION_CRITICAL=4) × CatWeight(1.30) + UserAdj(+2.0 for 8 users)] => P2 - High",
            IncidentStatus.CLOSED, tech3, slaP2,
            now.minusDays(2), now.minusDays(2).plusMinutes(3), now.minusDays(2).plusMinutes(8),
            SLARiskLevel.LOW, EscalationLevel.NONE);
        inc13.setResolvedAt(now.minusDays(2).plusMinutes(75));
        inc13.setVerifiedAt(now.minusDays(2).plusMinutes(110));
        inc13.setClosedAt(now.minusDays(2).plusMinutes(115));
        inc13.setClosureNotes("NTP drift on auth worker node fixed and verified by reporter.");
        incidentRepository.save(inc13);

        Incident inc14 = buildIncident("INC1014",
            "Automated nightly backup verification warning on Cloud Storage archive",
            "Nightly snapshot checksum job took 14 minutes longer than baseline window due to large dataset ingestion.",
            emp1, catStorage, srvCloud, ImpactLevel.LOW, UrgencyLevel.LOW, 3,
            PriorityLevel.P4, 3.3, "Weighted Score 3.3 [Impact(LOW=1) × Urgency(LOW=1) × Criticality(HIGH=3) × CatWeight(1.10) + UserAdj(+0.0 for 3 users)] => P4 - Low",
            IncidentStatus.CLOSED, tech2, slaP4,
            now.minusDays(3), now.minusDays(3).plusMinutes(20), now.minusDays(3).plusMinutes(35),
            SLARiskLevel.LOW, EscalationLevel.NONE);
        inc14.setResolvedAt(now.minusDays(3).plusHours(3));
        inc14.setVerifiedAt(now.minusDays(3).plusHours(5));
        inc14.setClosedAt(now.minusDays(3).plusHours(5));
        inc14.setClosureNotes("Archive window expanded and verified.");
        incidentRepository.save(inc14);

        Incident inc15 = buildIncident("INC1015",
            "Workstation RAM upgrade and thermal paste maintenance in Research Lab",
            "Developer workstation experiences high fan noise during local container builds.",
            emp1, catHardware, srvLms, ImpactLevel.LOW, UrgencyLevel.LOW, 1,
            PriorityLevel.P4, 3.0, "Weighted Score 3.0 [Impact(LOW=1) × Urgency(LOW=1) × Criticality(HIGH=3) × CatWeight(1.00) + UserAdj(+0.0 for 1 user)] => P4 - Low",
            IncidentStatus.CLOSED, tech4, slaP4,
            now.minusDays(4), now.minusDays(4).plusMinutes(30), now.minusDays(4).plusMinutes(50),
            SLARiskLevel.LOW, EscalationLevel.NONE);
        inc15.setResolvedAt(now.minusDays(4).plusHours(4));
        inc15.setVerifiedAt(now.minusDays(4).plusHours(6));
        inc15.setClosedAt(now.minusDays(4).plusHours(6));
        inc15.setClosureNotes("Completed hardware servicing and thermal diagnostics.");
        incidentRepository.save(inc15);

        Incident inc16 = buildIncident("INC1016",
            "SSL certificate renewal reminder for staging LMS subdomain",
            "Staging environment TLS certificate is scheduled for rotation within the next 14 days.",
            emp3, catSecurity, srvLms, ImpactLevel.LOW, UrgencyLevel.LOW, 2,
            PriorityLevel.P4, 4.2, "Weighted Score 4.2 [Impact(LOW=1) × Urgency(LOW=1) × Criticality(HIGH=3) × CatWeight(1.40) + UserAdj(+0.0 for 2 users)] => P4 - Low",
            IncidentStatus.CLASSIFIED, null, slaP4,
            now.minusMinutes(15), null, null,
            SLARiskLevel.LOW, EscalationLevel.NONE);

        noteRepository.save(new InvestigationNote(inc1, tech1,
            "Inspected upstream HikariCP connection pool on Payment Gateway service; observed 98% active socket exhaustion on bank webhook callbacks.", true));
        noteRepository.save(new InvestigationNote(inc1, tech1,
            "We have identified connection pool saturation on the payment processor node and are scaling out the gateway replicas while draining stuck transactions.", false));
        noteRepository.save(new InvestigationNote(inc3, tech1,
            "Analysing pg_stat_replication on primary DB node; long-running analytical export query was holding a shared lock.", true));
        noteRepository.save(new InvestigationNote(inc4, tech3,
            "Checked chrony NTP synchronization across SSO cluster nodes; Node-2 had a 34-second clock skew affecting TOTP window validation.", false));
        noteRepository.save(new InvestigationNote(inc5, tech2,
            "Reviewed Cisco Catalyst WLC telemetry for Hostel Block VK; AP-VK-03 radio channel utilization spiked due to 2.4GHz interference.", false));

        infoRequestRepository.save(new InformationRequest(inc7, tech3,
            "Please confirm the workstation hostname or MAC address in Computer Lab 3 where the port 8443 research client was executed so we can whitelist or isolate the container."));

        resolutionRepository.save(new Resolution(inc8, tech2,
            "Nginx ingress proxy client_max_body_size was capped at 250m on the LMS media upload route.",
            "Updated ingress controller annotation client-body-buffer-size and client_max_body_size to 2048m and reloaded workers with zero downtime.",
            "Split video files or upload via Cloud Storage link.", 42));

        resolutionRepository.save(new Resolution(inc11, tech1,
            "Postfix outbound relay queue concurrency limit was throttled during bulk attachment blasts.",
            "Increased relay worker concurrency and enabled asynchronous recipient batching.",
            "None required.", 55));

        resolutionRepository.save(new Resolution(inc12, tech4,
            "DisplayPort MST firmware on USB-C dock required update for 60Hz dual 4K output.",
            "Flashed dock firmware v3.14 and replaced HDMI 2.0 cable.",
            "Connect primary monitor directly to laptop HDMI port.", 35));

        resolutionRepository.save(new Resolution(inc13, tech3,
            "Redis TTL key for guest password reset tokens was configured in seconds instead of minutes (120s instead of 120m).",
            "Patched token expiration TTL configuration to 3600 seconds and deployed hotfix v2.4.2.",
            "Admin manual password reset.", 48));

        resolutionRepository.save(new Resolution(inc14, tech2,
            "Concurrent dataset upload contended with snapshot I/O bandwidth.",
            "Rescheduled incremental ZFS snapshot job to 03:30 AM IST and enabled parallel checksum threads.",
            "None.", 60));

        resolutionRepository.save(new Resolution(inc15, tech4,
            "Dust accumulation in heatsink fins and dried thermal compound.",
            "Cleaned cooling assembly, applied Arctic MX-6 thermal compound, and upgraded workstation memory to 32GB DDR5.",
            "None.", 45));

        feedbackRepository.save(new IncidentFeedback(inc13, emp3, 5, "Fast root cause diagnosis and permanent fix on the SSO token expiry!"));
        feedbackRepository.save(new IncidentFeedback(inc14, emp1, 5, "Nightly backups have been running smoothly since the window adjustment."));
        feedbackRepository.save(new IncidentFeedback(inc15, emp1, 5, "Workstation runs completely silent and compiles twice as fast now."));

        IncidentRelationship rel1 = new IncidentRelationship(inc2, inc1, RelationshipType.DUPLICATE, 0.78, "DUPLICATE_DETECTOR");
        rel1.setConfirmed(true);
        relationshipRepository.save(rel1);

        IncidentRelationship rel2 = new IncidentRelationship(inc6, inc5, RelationshipType.DUPLICATE, 0.69, "DUPLICATE_DETECTOR");
        rel2.setConfirmed(false);
        relationshipRepository.save(rel2);

        IncidentRelationship rel3 = new IncidentRelationship(inc13, inc4, RelationshipType.RELATED, 0.44, "DUPLICATE_DETECTOR");
        rel3.setConfirmed(true);
        relationshipRepository.save(rel3);

        slaViolationRepository.save(new SLAViolation(
            inc2, "RESPONSE_SLA_BREACH", inc2.getResponseDeadline(), 33,
            "First response target (5 mins) missed on P1 Payment Gateway incident INC1002."
        ));
        slaViolationRepository.save(new SLAViolation(
            inc2, "RESOLUTION_SLA_BREACH", inc2.getResolutionDeadline(), 8,
            "Resolution target (30 mins) breached on P1 Payment Gateway incident INC1002."
        ));

        escalationRepository.save(new EscalationRecord(
            inc2, EscalationLevel.LEVEL_1_TECHNICIAN, "ASSIGNED_TECHNICIAN",
            "Approaching response SLA deadline without technician acknowledgment", "SLA_SCHEDULER"
        ));
        escalationRepository.save(new EscalationRecord(
            inc2, EscalationLevel.LEVEL_2_TEAM_LEAD, "TEAM_LEAD",
            "Response SLA breached on critical P1 Payment Gateway incident INC1002 — escalated to Team Lead", "SLA_SCHEDULER"
        ));

        notificationRepository.save(new Notification(
            admin, inc2, NotificationType.INCIDENT_ESCALATED, NotificationChannel.IN_APP,
            "[INC1002] Critical P1 Incident Escalated to Team Lead",
            "Incident INC1002 (Payment Gateway duplicate charge/timeout) breached response SLA and was escalated to Level 2."
        ));
        notificationRepository.save(new Notification(
            admin, inc2, NotificationType.SLA_VIOLATED, NotificationChannel.SIMULATED_EMAIL,
            "RESOLVR Alert — INC1002: SLA_VIOLATION_DETECTED",
            "[SIMULATED EMAIL TO: Vikramaditya Rao <admin@resolvr.io>] Subject: SLA Breach on INC1002 | Resolution SLA target missed for P1 incident."
        ));
        notificationRepository.save(new Notification(
            tech1, inc1, NotificationType.INCIDENT_ASSIGNED, NotificationChannel.IN_APP,
            "[INC1001] P1 Critical Incident Assigned",
            "You have been assigned to INC1001 (Payment Gateway checkout failing with HTTP 504) via Skill-Based Strategy."
        ));
        notificationRepository.save(new Notification(
            emp1, inc7, NotificationType.INFO_REQUESTED, NotificationChannel.IN_APP,
            "[INC1007] Technician Requested Additional Information",
            "Technician Rohan Verma requested workstation hostname details for INC1007."
        ));
        notificationRepository.save(new Notification(
            emp3, inc8, NotificationType.INCIDENT_RESOLVED, NotificationChannel.IN_APP,
            "[INC1008] Incident Resolved — Verification Requested",
            "Technician Priya Sharma resolved INC1008 (LMS video upload failure). Please verify and close the ticket."
        ));

        auditLogRepository.save(new AuditLog(inc1, "INC1001", "ananya.emp", "ROLE_EMPLOYEE",
            "INCIDENT_CREATED", "NEW", "CLASSIFIED", "Incident INC1001 reported for Payment Gateway", "Priority calculated as P1 (Score 88.0)"));
        auditLogRepository.save(new AuditLog(inc1, "INC1001", "SYSTEM_AUTO_ASSIGN", "SYSTEM",
            "INCIDENT_ASSIGNED", "CLASSIFIED", "ASSIGNED", "Assigned INC1001 to Arjun Nair (TECH201) via Skill-Based Strategy", "Match Score: 92.0"));
        auditLogRepository.save(new AuditLog(inc1, "INC1001", "arjun.tech", "ROLE_TECHNICIAN",
            "INCIDENT_ACKNOWLEDGED", "ASSIGNED", "ACKNOWLEDGED", "Technician Arjun Nair acknowledged INC1001 within 4 minutes (SLA Compliant)", null));
        auditLogRepository.save(new AuditLog(inc1, "INC1001", "arjun.tech", "ROLE_TECHNICIAN",
            "INVESTIGATION_STARTED", "ACKNOWLEDGED", "IN_PROGRESS", "Active investigation started on Payment Gateway connection pool", null));
        auditLogRepository.save(new AuditLog(inc2, "INC1002", "SLA_MONITOR", "SYSTEM",
            "SLA_VIOLATION_DETECTED", "ASSIGNED", "ESCALATED", "SLA Breach detected on INC1002 and escalated to Level 2 Team Lead", "Overdue by 8 mins"));
        auditLogRepository.save(new AuditLog(inc8, "INC1008", "priya.tech", "ROLE_TECHNICIAN",
            "INCIDENT_RESOLVED", "IN_PROGRESS", "RESOLVED", "Resolved INC1008 by increasing Nginx ingress upload limit to 2048MB", "Time spent: 42m"));
        auditLogRepository.save(new AuditLog(inc13, "INC1013", "karthik.emp", "ROLE_EMPLOYEE",
            "INCIDENT_CLOSED", "VERIFIED", "CLOSED", "Incident INC1013 verified and closed with 5/5 rating", "SLA Compliant"));

        log.info("RESOLVR demonstration dataset initialized successfully with 8 users and 16 incidents.");
    }

    private void refreshExistingDemoCredentials() {
        refreshDemoCredential("admin", "ROLE_ADMIN", DEMO_ADMIN_PASSWORD);
        for (String username : Set.of("arjun.tech", "priya.tech", "rohan.tech", "sneha.tech")) {
            refreshDemoCredential(username, "ROLE_TECHNICIAN", DEMO_TECHNICIAN_PASSWORD);
        }
    }

    private void refreshDemoCredential(String username, String role, String password) {
        jdbc.update("""
                update users
                   set password_hash=?, active=true, failures=0, failed_at=null, locked_until=null,
                       reset_digest=null, reset_until=null, updated_at=now()
                 where username=? and role=?
                """, passwordEncoder.encode(password), username, role);
    }

    private String demoPassword(String username) {
        if ("admin".equals(username)) {
            return passwordEncoder.encode(DEMO_ADMIN_PASSWORD);
        }
        if (Set.of("arjun.tech", "priya.tech", "rohan.tech", "sneha.tech").contains(username)) {
            return passwordEncoder.encode(DEMO_TECHNICIAN_PASSWORD);
        }
        String secret = UUID.randomUUID().toString();
        var file = Path.of(System.getProperty("resolvr.demo.dir", "/app/dev-credentials"), username);
        try {
            Files.createDirectories(file.getParent());
            var temporary = Files.createTempFile(file.getParent(), ".credential-", ".tmp", PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rw-------")));
            Files.writeString(temporary, secret);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot write the development credential for " + username, ex);
        }
        auditLogRepository.save(new AuditLog(null, "SYSTEM", "SYSTEM", "SYSTEM", "DEMO_ACCOUNT_CREATED",
                null, null, "Created development account " + username, null));
        return passwordEncoder.encode(secret);
    }

    private void saveConfigIfAbsent(String key, String value, String desc) {
        if (configRepository.findByConfigKey(key).isEmpty()) {
            configRepository.save(new ApplicationConfiguration(key, value, desc, "SYSTEM"));
        }
    }

    private Incident buildIncident(String key, String title, String description,
                                   Employee reporter, IncidentCategory category, ITService service,
                                   ImpactLevel impact, UrgencyLevel urgency, int affectedUsers,
                                   PriorityLevel priority, double score, String reasoning,
                                   IncidentStatus status, Technician assignee, SLAPolicy slaPolicy,
                                   LocalDateTime createdAt, LocalDateTime assignedAt, LocalDateTime ackAt,
                                   SLARiskLevel riskLevel, EscalationLevel escalationLevel) {
        Incident inc = new Incident();
        inc.setIncidentKey(key);
        inc.setTitle(title);
        inc.setDescription(description);
        inc.setReporter(reporter);
        inc.setCategory(category);
        inc.setService(service);
        inc.setImpact(impact);
        inc.setUrgency(urgency);
        inc.setBusinessCriticality(service.getBusinessCriticality());
        inc.setAffectedUsers(affectedUsers);
        inc.setPriority(priority);
        inc.setPriorityScore(score);
        inc.setPriorityReasoning(reasoning);
        inc.setStatus(status);
        inc.setAssignedTechnician(assignee);
        inc.setSlaPolicy(slaPolicy);
        inc.setCreatedAt(createdAt);
        inc.setUpdatedAt(LocalDateTime.now());
        inc.setAssignedAt(assignedAt);
        inc.setAcknowledgedAt(ackAt);
        inc.setFirstResponseAt(ackAt);
        inc.setResponseDeadline(createdAt.plusMinutes(slaPolicy.getResponseTargetMinutes()));
        inc.setResolutionDeadline(createdAt.plusMinutes(slaPolicy.getResolutionTargetMinutes()));
        inc.setSlaRiskLevel(riskLevel);
        inc.setEscalationLevel(escalationLevel);
        if (assignee != null) {
            inc.setRecommendedTechnicianReason(
                assignee.getFullName() + " matched via Skill-Based Strategy (" + category.getCode() + " specialization)"
            );
        } else {
            inc.setRecommendedTechnicianReason("Queued for automatic or manual technician assignment.");
        }
        return incidentRepository.save(inc);
    }
}
