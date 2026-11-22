package com.resolvr.strategy;

import com.resolvr.dto.AssignmentRecommendation;
import com.resolvr.entity.Incident;
import com.resolvr.entity.Technician;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class SkillBasedAssignmentStrategy implements TechnicianAssignmentStrategy {

    @Override
    public String getStrategyCode() {
        return "SKILL_BASED";
    }

    @Override
    public String getStrategyDisplayName() {
        return "Intelligent Skill & Capacity Matching Strategy";
    }

    @Override
    public Optional<AssignmentRecommendation> recommendTechnician(Incident incident, List<Technician> candidateTechnicians) {
        if (candidateTechnicians == null || candidateTechnicians.isEmpty()) {
            return Optional.empty();
        }

        String categoryCode = incident.getCategory() != null ? incident.getCategory().getCode() : "";
        String serviceCode = incident.getService() != null ? incident.getService().getServiceCode() : "";

        return candidateTechnicians.stream()
            .filter(Technician::canAcceptAssignment)
            .map(tech -> {
                boolean hasCategorySkill = tech.hasSkillForCategory(categoryCode);
                boolean hasServiceSpecialization = tech.getSpecialization() != null &&
                        (tech.getSpecialization().toUpperCase().contains(categoryCode) ||
                         tech.getSpecialization().toUpperCase().contains(serviceCode));

                double skillScore = hasCategorySkill ? 50.0 : (hasServiceSpecialization ? 35.0 : 10.0);
                double capacityScore = (1.0 - ((double) tech.getCurrentWorkload() / Math.max(1, tech.getMaxWorkload()))) * 30.0;
                double expScore = Math.min(20.0, tech.getExperienceYears() * 3.0);
                double totalScore = Math.round((skillScore + capacityScore + expScore) * 10.0) / 10.0;

                String reason = String.format(
                    "%s recommended via Skill-Based Strategy (Match Score: %.1f/100) — %s, Active Workload: %d/%d, Experience: %d yrs",
                    tech.getFullName(),
                    totalScore,
                    hasCategorySkill ? "Verified " + categoryCode + " skill match" : "General IT support capability",
                    tech.getCurrentWorkload(),
                    tech.getMaxWorkload(),
                    tech.getExperienceYears()
                );

                return new AssignmentRecommendation(tech, totalScore, getStrategyCode(), reason);
            })
            .max(Comparator.comparingDouble(AssignmentRecommendation::getMatchScore)
                .thenComparingInt(rec -> -rec.getTechnician().getCurrentWorkload())
                .thenComparingLong(rec -> -rec.getTechnician().getId()));
    }
}
