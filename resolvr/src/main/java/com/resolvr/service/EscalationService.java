package com.resolvr.service;

import com.resolvr.entity.*;
import com.resolvr.enums.EscalationLevel;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.NotificationType;
import com.resolvr.observer.IncidentDomainEvent;
import com.resolvr.observer.IncidentEventPublisher;
import com.resolvr.repository.AdministratorRepository;
import com.resolvr.repository.EscalationRecordRepository;
import com.resolvr.repository.IncidentRepository;
import com.resolvr.repository.TechnicianRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EscalationService {

    private final EscalationRecordRepository escalationRepository;
    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final AdministratorRepository administratorRepository;
    private final IncidentEventPublisher eventPublisher;

    public EscalationService(EscalationRecordRepository escalationRepository,
                             IncidentRepository incidentRepository,
                             TechnicianRepository technicianRepository,
                             AdministratorRepository administratorRepository,
                             IncidentEventPublisher eventPublisher) {
        this.escalationRepository = escalationRepository;
        this.incidentRepository = incidentRepository;
        this.technicianRepository = technicianRepository;
        this.administratorRepository = administratorRepository;
        this.eventPublisher = eventPublisher;
    }

    public EscalationRecord evaluateAndEscalateIfNeeded(Incident incident, LocalDateTime now) {
        if (!incident.getStatus().isActive()) {
            return null;
        }

        long minutesSinceCreation = Duration.between(incident.getCreatedAt(), now).toMinutes();
        int responseTarget = incident.getSlaPolicy() != null ? incident.getSlaPolicy().getResponseTargetMinutes() : 15;
        boolean unacknowledged = incident.getAcknowledgedAt() == null;

        EscalationLevel targetLevel = EscalationLevel.NONE;
        String targetRole = "";
        String reason = "";

        if (unacknowledged && minutesSinceCreation >= responseTarget * 3L) {
            targetLevel = EscalationLevel.LEVEL_4_IT_MANAGER;
            targetRole = "IT_MANAGER";
            reason = "Critical unacknowledged delay exceeded 3x response SLA (" + minutesSinceCreation + " mins elapsed)";
        } else if (unacknowledged && minutesSinceCreation >= responseTarget * 2L) {
            targetLevel = EscalationLevel.LEVEL_3_ADMIN;
            targetRole = "SYSTEM_ADMINISTRATOR";
            reason = "Unacknowledged incident exceeded 2x response SLA (" + minutesSinceCreation + " mins elapsed)";
        } else if (unacknowledged && minutesSinceCreation > responseTarget) {
            targetLevel = EscalationLevel.LEVEL_2_TEAM_LEAD;
            targetRole = "TEAM_LEAD";
            reason = "Response deadline missed without technician acknowledgment (" + minutesSinceCreation + " mins elapsed)";
        } else if (unacknowledged && minutesSinceCreation >= Math.max(1, (long) (responseTarget * 0.8))) {
            targetLevel = EscalationLevel.LEVEL_1_TECHNICIAN;
            targetRole = "ASSIGNED_TECHNICIAN";
            reason = "Approaching response deadline without acknowledgment";
        }

        if (targetLevel == EscalationLevel.NONE ||
            targetLevel.getLevel() <= incident.getEscalationLevel().getLevel() ||
            escalationRepository.existsByIncidentAndEscalationLevel(incident, targetLevel)) {
            return null;
        }

        return executeEscalation(incident, targetLevel, targetRole, reason, "SLA_SCHEDULER");
    }

    public EscalationRecord manualEscalate(Incident incident, String reason, String actorUsername) {
        EscalationLevel current = incident.getEscalationLevel();
        EscalationLevel next = switch (current) {
            case NONE -> EscalationLevel.LEVEL_1_TECHNICIAN;
            case LEVEL_1_TECHNICIAN -> EscalationLevel.LEVEL_2_TEAM_LEAD;
            case LEVEL_2_TEAM_LEAD -> EscalationLevel.LEVEL_3_ADMIN;
            case LEVEL_3_ADMIN, LEVEL_4_IT_MANAGER -> EscalationLevel.LEVEL_4_IT_MANAGER;
        };
        String role = switch (next) {
            case LEVEL_1_TECHNICIAN -> "ASSIGNED_TECHNICIAN";
            case LEVEL_2_TEAM_LEAD -> "TEAM_LEAD";
            case LEVEL_3_ADMIN -> "SYSTEM_ADMINISTRATOR";
            default -> "IT_MANAGER";
        };
        return executeEscalation(incident, next, role,
                reason != null && !reason.isBlank() ? reason : "Manual escalation triggered by " + actorUsername,
                actorUsername);
    }

    private EscalationRecord executeEscalation(Incident incident, EscalationLevel level,
                                               String targetRole, String reason, String triggeredBy) {
        String prevStatus = incident.getStatus().name();
        incident.setEscalationLevel(level);
        if (level.getLevel() >= EscalationLevel.LEVEL_2_TEAM_LEAD.getLevel() &&
            incident.getStatus() != IncidentStatus.RESOLVED &&
            incident.getStatus() != IncidentStatus.VERIFIED &&
            incident.getStatus() != IncidentStatus.CLOSED) {
            incident.setStatus(IncidentStatus.ESCALATED);
        }
        incident.setUpdatedAt(LocalDateTime.now());
        incidentRepository.save(incident);

        EscalationRecord record = new EscalationRecord(incident, level, targetRole, reason, triggeredBy);
        EscalationRecord saved = escalationRepository.save(record);

        List<User> recipients = new ArrayList<>();
        if (incident.getAssignedTechnician() != null) {
            recipients.add(incident.getAssignedTechnician());
        }
        if (level.getLevel() >= EscalationLevel.LEVEL_2_TEAM_LEAD.getLevel()) {
            recipients.addAll(technicianRepository.findByTeamLeadTrueAndActiveTrue());
        }
        if (level.getLevel() >= EscalationLevel.LEVEL_3_ADMIN.getLevel()) {
            recipients.addAll(administratorRepository.findAll());
        }

        eventPublisher.publish(new IncidentDomainEvent(
            incident,
            triggeredBy,
            "SYSTEM",
            "INCIDENT_ESCALATED",
            prevStatus,
            incident.getStatus().name(),
            "Escalated " + incident.getIncidentKey() + " to " + level.getDescription() + ": " + reason,
            "TargetRole=" + targetRole,
            NotificationType.INCIDENT_ESCALATED,
            recipients
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<EscalationRecord> getRecentEscalations() {
        return escalationRepository.findTop25ByOrderByEscalatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<EscalationRecord> getEscalationsForIncident(Incident incident) {
        return escalationRepository.findByIncidentOrderByEscalatedAtDesc(incident);
    }
}
