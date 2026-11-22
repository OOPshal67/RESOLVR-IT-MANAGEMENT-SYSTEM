package com.resolvr.dto;

import com.resolvr.enums.PriorityLevel;

import jakarta.validation.constraints.*;

public class PriorityResult {
    @NotNull
    private final PriorityLevel priorityLevel;
    @PositiveOrZero
    private final double calculatedScore;
    @NotBlank
    private final String reasoning;

    public PriorityResult(PriorityLevel priorityLevel, double calculatedScore, String reasoning) {
        this.priorityLevel = priorityLevel;
        this.calculatedScore = calculatedScore;
        this.reasoning = reasoning;
    }

    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public double getCalculatedScore() { return calculatedScore; }
    public String getReasoning() { return reasoning; }
}
