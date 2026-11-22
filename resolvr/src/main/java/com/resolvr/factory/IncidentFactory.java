package com.resolvr.factory;

import com.resolvr.dto.IncidentCreateRequest;
import com.resolvr.dto.PriorityResult;
import com.resolvr.entity.*;
import com.resolvr.enums.BusinessCriticality;
import com.resolvr.enums.EscalationLevel;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.SLARiskLevel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Factory Pattern implementation for controlled creation and initialization of Incident aggregates.
 */
@Component
public class IncidentFactory {

    public Incident createIncident(String incidentKey,
                                   IncidentCreateRequest request,
                                   Employee reporter,
                                   IncidentCategory category,
                                   ITService service,
                                   PriorityResult priorityResult,
                                   SLAPolicy slaPolicy) {
        LocalDateTime now = LocalDateTime.now();
        BusinessCriticality criticality = service.getBusinessCriticality();

        Incident incident = new Incident();
        incident.setIncidentKey(incidentKey);
        incident.setTitle(request.getTitle().trim());
        incident.setDescription(request.getDescription().trim());
        incident.setReporter(reporter);
        incident.setCategory(category);
        incident.setService(service);
        incident.setImpact(request.getImpact());
        incident.setUrgency(request.getUrgency());
        incident.setBusinessCriticality(criticality);
        incident.setAffectedUsers(Math.max(1, request.getAffectedUsers()));
        incident.setPriority(priorityResult.getPriorityLevel());
        incident.setPriorityScore(priorityResult.getCalculatedScore());
        incident.setPriorityReasoning(priorityResult.getReasoning());
        incident.setStatus(IncidentStatus.CLASSIFIED);
        incident.setSlaPolicy(slaPolicy);
        incident.setCreatedAt(now);
        incident.setUpdatedAt(now);
        incident.setResponseDeadline(now.plusMinutes(slaPolicy.getResponseTargetMinutes()));
        incident.setResolutionDeadline(now.plusMinutes(slaPolicy.getResolutionTargetMinutes()));
        incident.setSlaRiskLevel(SLARiskLevel.LOW);
        incident.setEscalationLevel(EscalationLevel.NONE);
        incident.setAttachmentName(request.getAttachmentName());
        return incident;
    }
}
