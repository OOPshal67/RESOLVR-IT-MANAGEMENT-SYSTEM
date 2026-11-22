package com.resolvr.dto;

import com.resolvr.entity.Technician;

import jakarta.validation.constraints.*;

public class AssignmentRecommendation {
    @NotNull
    private final Technician technician;
    @PositiveOrZero @DecimalMax("100.0")
    private final double matchScore;
    @NotBlank
    private final String strategyName;
    @NotBlank
    private final String reason;

    public AssignmentRecommendation(Technician technician, double matchScore, String strategyName, String reason) {
        this.technician = technician;
        this.matchScore = matchScore;
        this.strategyName = strategyName;
        this.reason = reason;
    }

    public Technician getTechnician() { return technician; }
    public double getMatchScore() { return matchScore; }
    public int getConfidencePercentage() { return (int) Math.min(99, Math.max(50, Math.round(matchScore))); }
    public String getStrategyName() { return strategyName; }
    public String getReason() { return reason; }
}
