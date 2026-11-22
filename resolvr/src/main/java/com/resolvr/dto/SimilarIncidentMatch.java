package com.resolvr.dto;

import com.resolvr.entity.Incident;

import jakarta.validation.constraints.*;

public class SimilarIncidentMatch {
    @NotNull
    private final Incident matchedIncident;
    @PositiveOrZero @DecimalMax("1.0")
    private final double similarityScore;
    @NotBlank
    private final String matchReason;

    public SimilarIncidentMatch(Incident matchedIncident, double similarityScore, String matchReason) {
        this.matchedIncident = matchedIncident;
        this.similarityScore = similarityScore;
        this.matchReason = matchReason;
    }

    public Incident getMatchedIncident() { return matchedIncident; }
    public double getSimilarityScore() { return similarityScore; }
    public int getSimilarityPercentage() { return (int) Math.round(similarityScore * 100.0); }
    public String getMatchReason() { return matchReason; }
}
