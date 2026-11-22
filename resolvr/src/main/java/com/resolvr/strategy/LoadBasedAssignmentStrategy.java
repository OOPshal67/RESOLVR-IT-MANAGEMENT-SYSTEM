package com.resolvr.strategy;

import com.resolvr.dto.AssignmentRecommendation;
import com.resolvr.entity.Incident;
import com.resolvr.entity.Technician;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class LoadBasedAssignmentStrategy implements TechnicianAssignmentStrategy {

    @Override
    public String getStrategyCode() {
        return "LOAD_BASED";
    }

    @Override
    public String getStrategyDisplayName() {
        return "Least Active Workload Balancing Strategy";
    }

    @Override
    public Optional<AssignmentRecommendation> recommendTechnician(Incident incident, List<Technician> candidateTechnicians) {
        if (candidateTechnicians == null || candidateTechnicians.isEmpty()) {
            return Optional.empty();
        }

        return candidateTechnicians.stream()
            .filter(Technician::canAcceptAssignment)
            .min(Comparator.comparingInt(Technician::getCurrentWorkload)
                .thenComparing(Comparator.comparingInt(Technician::getExperienceYears).reversed())
                .thenComparingLong(Technician::getId))
            .map(tech -> {
                double score = Math.max(60.0, 95.0 - (tech.getCurrentWorkload() * 10.0));
                String reason = String.format(
                    "%s selected via Load-Based Strategy — Lowest active workload (%d/%d incidents, %d%% utilization)",
                    tech.getFullName(),
                    tech.getCurrentWorkload(),
                    tech.getMaxWorkload(),
                    tech.getUtilizationPercentage()
                );
                return new AssignmentRecommendation(tech, score, getStrategyCode(), reason);
            });
    }
}
