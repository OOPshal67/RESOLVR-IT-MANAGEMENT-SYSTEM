package com.resolvr.service;

import com.resolvr.entity.Incident;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.PriorityLevel;
import com.resolvr.enums.SLARiskLevel;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class SLARiskDetectionService {

    public SLARiskLevel evaluateRiskLevel(Incident incident, LocalDateTime evaluationTime) {
        if (incident.getStatus() == IncidentStatus.RESOLVED ||
            incident.getStatus() == IncidentStatus.VERIFIED ||
            incident.getStatus() == IncidentStatus.CLOSED ||
            incident.getStatus() == IncidentStatus.CANCELLED) {
            return incident.isSlaViolated() ? SLARiskLevel.BREACHED : SLARiskLevel.LOW;
        }

        if (incident.isSlaViolated()) {
            return SLARiskLevel.BREACHED;
        }

        LocalDateTime now = evaluationTime != null ? evaluationTime : LocalDateTime.now();

        if (incident.getResolutionDeadline() != null && now.isAfter(incident.getResolutionDeadline())) {
            return SLARiskLevel.BREACHED;
        }
        if (incident.getFirstResponseAt() == null && incident.getResponseDeadline() != null &&
            now.isAfter(incident.getResponseDeadline())) {
            return SLARiskLevel.BREACHED;
        }

        long totalTargetMinutes = incident.getSlaPolicy() != null
                ? Math.max(1, incident.getSlaPolicy().getResolutionTargetMinutes())
                : 60;
        long remainingMinutes = incident.getResolutionDeadline() != null
                ? Duration.between(now, incident.getResolutionDeadline()).toMinutes()
                : totalTargetMinutes;

        double remainingRatio = (double) remainingMinutes / totalTargetMinutes;
        boolean unacknowledged = incident.getAcknowledgedAt() == null;

        if (remainingRatio <= 0.15 || (incident.getPriority() == PriorityLevel.P1 && unacknowledged && remainingRatio <= 0.60)) {
            return SLARiskLevel.CRITICAL;
        }
        if (remainingRatio <= 0.30 || (unacknowledged && remainingRatio <= 0.50)) {
            return SLARiskLevel.HIGH;
        }
        if (remainingRatio <= 0.55) {
            return SLARiskLevel.MEDIUM;
        }
        return SLARiskLevel.LOW;
    }
}
