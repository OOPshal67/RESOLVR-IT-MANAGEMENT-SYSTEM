package com.resolvr.observer;

import com.resolvr.entity.AuditLog;
import com.resolvr.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogObserver implements IncidentEventObserver {

    private final AuditLogRepository auditLogRepository;

    public AuditLogObserver(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void onIncidentEvent(IncidentDomainEvent event) {
        AuditLog logEntry = new AuditLog(
            event.getIncident(),
            event.getIncident() != null ? event.getIncident().getIncidentKey() : "SYSTEM",
            event.getActorUsername(),
            event.getActorRole(),
            event.getActionType(),
            event.getPreviousState(),
            event.getNewState(),
            event.getDescription(),
            event.getMetadata()
        );
        auditLogRepository.save(logEntry);
    }
}
