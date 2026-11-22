package com.resolvr.scheduler;

import com.resolvr.entity.Incident;
import com.resolvr.entity.SLAViolation;
import com.resolvr.entity.User;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.NotificationType;
import com.resolvr.enums.SLARiskLevel;
import com.resolvr.observer.IncidentDomainEvent;
import com.resolvr.observer.IncidentEventPublisher;
import com.resolvr.repository.AdministratorRepository;
import com.resolvr.repository.IncidentRepository;
import com.resolvr.repository.SLAViolationRepository;
import com.resolvr.service.EscalationService;
import com.resolvr.service.SLARiskDetectionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SLAScheduler {

    private static final Logger log = LoggerFactory.getLogger(SLAScheduler.class);

    private final IncidentRepository incidentRepository;
    private final SLAViolationRepository slaViolationRepository;
    private final AdministratorRepository administratorRepository;
    private final SLARiskDetectionService slaRiskDetectionService;
    private final EscalationService escalationService;
    private final IncidentEventPublisher eventPublisher;

    public SLAScheduler(IncidentRepository incidentRepository,
                        SLAViolationRepository slaViolationRepository,
                        AdministratorRepository administratorRepository,
                        SLARiskDetectionService slaRiskDetectionService,
                        EscalationService escalationService,
                        IncidentEventPublisher eventPublisher) {
        this.incidentRepository = incidentRepository;
        this.slaViolationRepository = slaViolationRepository;
        this.administratorRepository = administratorRepository;
        this.slaRiskDetectionService = slaRiskDetectionService;
        this.escalationService = escalationService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${resolvr.sla.scheduler-interval-ms:60000}")
    @Transactional
    public void monitorActiveIncidentsSla() {
        List<Incident> activeIncidents = incidentRepository.findByStatusNotIn(
                List.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, IncidentStatus.CANCELLED));

        LocalDateTime now = LocalDateTime.now();

        for (Incident incident : activeIncidents) {
            boolean modified = false;

            // 1. Evaluate SLA Risk Level
            SLARiskLevel newRisk = slaRiskDetectionService.evaluateRiskLevel(incident, now);
            if (incident.getSlaRiskLevel() != newRisk) {
                incident.setSlaRiskLevel(newRisk);
                modified = true;
            }

            // 2. Check Approaching Deadline Warning (prevent duplicate warnings via slaWarningSent flag)
            if (!incident.isSlaWarningSent() && (newRisk == SLARiskLevel.HIGH || newRisk == SLARiskLevel.CRITICAL)) {
                incident.setSlaWarningSent(true);
                modified = true;

                List<User> recipients = new ArrayList<>(administratorRepository.findAll());
                if (incident.getAssignedTechnician() != null) {
                    recipients.add(incident.getAssignedTechnician());
                }

                eventPublisher.publish(new IncidentDomainEvent(
                    incident,
                    "SLA_MONITOR",
                    "SYSTEM",
                    "SLA_WARNING",
                    incident.getStatus().name(),
                    incident.getStatus().name(),
                    "SLA Warning: Incident " + incident.getIncidentKey() + " is at " + newRisk.getLabel() +
                    " (" + incident.getFormattedRemainingResolutionTime() + ")",
                    "Deadline=" + incident.getResolutionDeadline(),
                    NotificationType.SLA_WARNING,
                    recipients
                ));
            }

            // 3. Detect Missed First-Response SLA Deadline
            if (incident.getFirstResponseAt() == null && incident.getResponseDeadline() != null &&
                now.isAfter(incident.getResponseDeadline()) && !incident.isResponseSlaViolated()) {
                incident.setResponseSlaViolated(true);
                incident.setSlaRiskLevel(SLARiskLevel.BREACHED);
                modified = true;

                if (!slaViolationRepository.existsByIncidentAndViolationType(incident, "RESPONSE_SLA_BREACH")) {
                    long overdue = Math.max(1, Duration.between(incident.getResponseDeadline(), now).toMinutes());
                    SLAViolation violation = new SLAViolation(
                        incident,
                        "RESPONSE_SLA_BREACH",
                        incident.getResponseDeadline(),
                        overdue,
                        "First response target missed by " + overdue + " minutes for priority " + incident.getPriority()
                    );
                    slaViolationRepository.save(violation);

                    publishViolationEvent(incident, "RESPONSE_SLA_BREACH", violation.getDetails());
                }
            }

            // 4. Detect Missed Resolution SLA Deadline
            if (incident.getResolutionDeadline() != null && now.isAfter(incident.getResolutionDeadline()) &&
                !incident.isResolutionSlaViolated()) {
                incident.setResolutionSlaViolated(true);
                incident.setSlaRiskLevel(SLARiskLevel.BREACHED);
                modified = true;

                if (!slaViolationRepository.existsByIncidentAndViolationType(incident, "RESOLUTION_SLA_BREACH")) {
                    long overdue = Math.max(1, Duration.between(incident.getResolutionDeadline(), now).toMinutes());
                    SLAViolation violation = new SLAViolation(
                        incident,
                        "RESOLUTION_SLA_BREACH",
                        incident.getResolutionDeadline(),
                        overdue,
                        "Resolution SLA target missed by " + overdue + " minutes for priority " + incident.getPriority()
                    );
                    slaViolationRepository.save(violation);

                    publishViolationEvent(incident, "RESOLUTION_SLA_BREACH", violation.getDetails());
                }
            }

            if (modified) {
                incident.setUpdatedAt(now);
                incidentRepository.save(incident);
            }

            // 5. Evaluate Automatic Multi-Tier Escalation
            escalationService.evaluateAndEscalateIfNeeded(incident, now);
        }
    }

    private void publishViolationEvent(Incident incident, String breachType, String details) {
        List<User> recipients = new ArrayList<>(administratorRepository.findAll());
        if (incident.getAssignedTechnician() != null) {
            recipients.add(incident.getAssignedTechnician());
        }
        eventPublisher.publish(new IncidentDomainEvent(
            incident,
            "SLA_MONITOR",
            "SYSTEM",
            "SLA_VIOLATION_DETECTED",
            incident.getStatus().name(),
            incident.getStatus().name(),
            "SLA Violation [" + breachType + "] on " + incident.getIncidentKey() + ": " + details,
            breachType,
            NotificationType.SLA_VIOLATED,
            recipients
        ));
        log.warn("SLA Violation recorded for {}: {}", incident.getIncidentKey(), details);
    }
}
