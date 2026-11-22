package com.resolvr.service;

import com.resolvr.entity.AuditLog;
import com.resolvr.entity.Incident;
import com.resolvr.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop100ByOrderByActionTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsForIncident(Incident incident) {
        return auditLogRepository.findByIncidentOrderByActionTimestampDesc(incident);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByActionType(String actionType) {
        if (actionType == null || actionType.isBlank() || "ALL".equalsIgnoreCase(actionType)) {
            return getRecentAuditLogs();
        }
        return auditLogRepository.findByActionTypeOrderByActionTimestampDesc(actionType);
    }

    public AuditLog recordSystemAction(String actorUsername, String actorRole, String actionType, String description, String metadata) {
        AuditLog log = new AuditLog(null, "SYSTEM", actorUsername, actorRole, actionType, null, null, description, metadata);
        return auditLogRepository.save(log);
    }
}
