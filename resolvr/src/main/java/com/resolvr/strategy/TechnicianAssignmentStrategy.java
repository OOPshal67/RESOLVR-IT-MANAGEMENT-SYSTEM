package com.resolvr.strategy;

import com.resolvr.dto.AssignmentRecommendation;
import com.resolvr.entity.Incident;
import com.resolvr.entity.Technician;

import java.util.List;
import java.util.Optional;

public interface TechnicianAssignmentStrategy {
    String getStrategyCode();
    String getStrategyDisplayName();
    Optional<AssignmentRecommendation> recommendTechnician(Incident incident, List<Technician> candidateTechnicians);
}
