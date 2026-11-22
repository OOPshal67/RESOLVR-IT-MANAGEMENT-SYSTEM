package com.resolvr.service;

import com.resolvr.dto.SimilarIncidentMatch;
import com.resolvr.entity.Incident;
import com.resolvr.entity.IncidentRelationship;
import com.resolvr.entity.User;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.RelationshipType;
import com.resolvr.repository.IncidentRelationshipRepository;
import com.resolvr.repository.IncidentRepository;
import com.resolvr.util.TextSimilarityUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DuplicateDetectionService {

    private final IncidentRepository incidentRepository;
    private final IncidentRelationshipRepository relationshipRepository;
    private final AuditService auditService;

    @Value("${resolvr.duplicate.similarity-threshold:0.35}")
    private double similarityThreshold = 0.35;

    public DuplicateDetectionService(IncidentRepository incidentRepository,
                                     IncidentRelationshipRepository relationshipRepository,
                                     AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.relationshipRepository = relationshipRepository;
        this.auditService = auditService;
    }

    public List<SimilarIncidentMatch> detectAndRecordSimilarIncidents(Incident newIncident) {
        List<Incident> candidates = incidentRepository.findByStatusNotIn(
                List.of(IncidentStatus.CLOSED, IncidentStatus.CANCELLED));

        List<SimilarIncidentMatch> matches = new ArrayList<>();
        String combinedNew = newIncident.getTitle() + " " + newIncident.getDescription();

        for (Incident candidate : candidates) {
            if (candidate.getId() != null && candidate.getId().equals(newIncident.getId())) {
                continue;
            }
            if (candidate.getIncidentKey() != null && candidate.getIncidentKey().equals(newIncident.getIncidentKey())) {
                continue;
            }

            String combinedCandidate = candidate.getTitle() + " " + candidate.getDescription();
            double textSim = TextSimilarityUtil.calculateJaccardSimilarity(combinedNew, combinedCandidate);

            double contextBoost = 0.0;
            if (newIncident.getService() != null && candidate.getService() != null &&
                newIncident.getService().getId() != null && newIncident.getService().getId().equals(candidate.getService().getId())) {
                contextBoost += 0.15;
            }
            if (newIncident.getCategory() != null && candidate.getCategory() != null &&
                newIncident.getCategory().getId() != null && newIncident.getCategory().getId().equals(candidate.getCategory().getId())) {
                contextBoost += 0.10;
            }

            double finalScore = Math.min(0.99, textSim + (textSim > 0.12 ? contextBoost : 0.0));
            if (finalScore >= similarityThreshold) {
                RelationshipType relType = finalScore >= 0.55 ? RelationshipType.DUPLICATE : RelationshipType.RELATED;
                String reason = String.format(
                    "%d%% similarity match with %s (%s) on service '%s'",
                    (int) Math.round(finalScore * 100),
                    candidate.getIncidentKey(),
                    relType.name(),
                    candidate.getService() != null ? candidate.getService().getName() : "N/A"
                );
                matches.add(new SimilarIncidentMatch(candidate, finalScore, reason));

                if (newIncident.getId() != null && candidate.getId() != null) {
                    IncidentRelationship relationship = new IncidentRelationship(
                        newIncident, candidate, relType, finalScore, "DUPLICATE_DETECTOR"
                    );
                    relationshipRepository.save(relationship);
                }
            }
        }

        matches.sort(Comparator.comparingDouble(SimilarIncidentMatch::getSimilarityScore).reversed());
        return matches;
    }

    @Transactional(readOnly = true)
    public List<IncidentRelationship> getVisibleRelationships(Incident incident, User viewer) {
        return getRelationshipsForIncident(incident).stream()
                .filter(link -> visible(link.getSourceIncident(), viewer) && visible(link.getTargetIncident(), viewer)).toList();
    }

    private boolean visible(Incident incident, User viewer) {
        return switch (viewer.getRole()) {
            case ROLE_ADMIN -> true;
            case ROLE_EMPLOYEE -> incident.getReporter().getId().equals(viewer.getId());
            case ROLE_TECHNICIAN -> incident.getAssignedTechnician() != null &&
                    incident.getAssignedTechnician().getId().equals(viewer.getId());
        };
    }

    @Transactional(readOnly = true)
    public List<IncidentRelationship> getRelationshipsForIncident(Incident incident) {
        return relationshipRepository.findBySourceIncidentOrTargetIncident(incident, incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentRelationship> getAllDetectedRelationships() {
        return relationshipRepository.findTop20ByOrderBySimilarityScoreDesc();
    }

    public void confirmDuplicateRelationship(Long relationshipId, String actorUsername) {
        IncidentRelationship rel = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("Relationship record not found: " + relationshipId));
        rel.setConfirmed(true);
        rel.setRelationshipType(RelationshipType.DUPLICATE);
        relationshipRepository.save(rel);

        auditService.recordSystemAction(actorUsername, "ROLE_ADMIN", "DUPLICATE_CONFIRMED",
                "Confirmed duplicate relationship between " + rel.getSourceIncident().getIncidentKey() +
                " and " + rel.getTargetIncident().getIncidentKey(),
                "Similarity=" + rel.getSimilarityPercentage() + "%");
    }
}
