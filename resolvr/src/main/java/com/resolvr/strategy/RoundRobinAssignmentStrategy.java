package com.resolvr.strategy;

import com.resolvr.dto.AssignmentRecommendation;
import com.resolvr.entity.Incident;
import com.resolvr.entity.Technician;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinAssignmentStrategy implements TechnicianAssignmentStrategy {

    private final AtomicInteger rotationIndex = new AtomicInteger(0);

    @Override
    public String getStrategyCode() {
        return "ROUND_ROBIN";
    }

    @Override
    public String getStrategyDisplayName() {
        return "Sequential Round-Robin Queue Strategy";
    }

    @Override
    public Optional<AssignmentRecommendation> recommendTechnician(Incident incident, List<Technician> candidateTechnicians) {
        if (candidateTechnicians == null || candidateTechnicians.isEmpty()) {
            return Optional.empty();
        }

        List<Technician> eligible = candidateTechnicians.stream()
                .filter(Technician::canAcceptAssignment)
                .sorted(Comparator.comparingLong(x -> x.getId() != null ? x.getId() : 0L))
                .toList();

        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        int idx = Math.floorMod(rotationIndex.getAndIncrement(), eligible.size());
        Technician chosen = eligible.get(idx);

        String reason = String.format(
            "%s selected via Round-Robin rotation slot #%d (Workload: %d/%d)",
            chosen.getFullName(), idx + 1, chosen.getCurrentWorkload(), chosen.getMaxWorkload()
        );
        return Optional.of(new AssignmentRecommendation(chosen, 82.0, getStrategyCode(), reason));
    }
}
