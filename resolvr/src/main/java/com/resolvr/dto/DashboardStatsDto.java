package com.resolvr.dto;

import jakarta.validation.constraints.*;

public class DashboardStatsDto {
    @PositiveOrZero
    private long totalIncidents;
    @PositiveOrZero
    private long openIncidents;
    @PositiveOrZero
    private long criticalIncidents;
    @PositiveOrZero
    private long highPriorityIncidents;
    @PositiveOrZero
    private long mediumPriorityIncidents;
    @PositiveOrZero
    private long lowPriorityIncidents;
    @PositiveOrZero
    private long resolvedIncidents;
    @PositiveOrZero
    private long closedIncidents;
    @PositiveOrZero
    private long slaViolations;
    @PositiveOrZero
    private long awaitingAssignment;
    @PositiveOrZero
    private long escalatedIncidents;
    @PositiveOrZero @DecimalMax("100.0")
    private double slaComplianceRate;
    @PositiveOrZero
    private double averageResolutionMinutes;

    public long getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(long totalIncidents) { this.totalIncidents = totalIncidents; }
    public long getOpenIncidents() { return openIncidents; }
    public void setOpenIncidents(long openIncidents) { this.openIncidents = openIncidents; }
    public long getCriticalIncidents() { return criticalIncidents; }
    public void setCriticalIncidents(long criticalIncidents) { this.criticalIncidents = criticalIncidents; }
    public long getHighPriorityIncidents() { return highPriorityIncidents; }
    public void setHighPriorityIncidents(long highPriorityIncidents) { this.highPriorityIncidents = highPriorityIncidents; }
    public long getMediumPriorityIncidents() { return mediumPriorityIncidents; }
    public void setMediumPriorityIncidents(long mediumPriorityIncidents) { this.mediumPriorityIncidents = mediumPriorityIncidents; }
    public long getLowPriorityIncidents() { return lowPriorityIncidents; }
    public void setLowPriorityIncidents(long lowPriorityIncidents) { this.lowPriorityIncidents = lowPriorityIncidents; }
    public long getResolvedIncidents() { return resolvedIncidents; }
    public void setResolvedIncidents(long resolvedIncidents) { this.resolvedIncidents = resolvedIncidents; }
    public long getClosedIncidents() { return closedIncidents; }
    public void setClosedIncidents(long closedIncidents) { this.closedIncidents = closedIncidents; }
    public long getSlaViolations() { return slaViolations; }
    public void setSlaViolations(long slaViolations) { this.slaViolations = slaViolations; }
    public long getAwaitingAssignment() { return awaitingAssignment; }
    public void setAwaitingAssignment(long awaitingAssignment) { this.awaitingAssignment = awaitingAssignment; }
    public long getEscalatedIncidents() { return escalatedIncidents; }
    public void setEscalatedIncidents(long escalatedIncidents) { this.escalatedIncidents = escalatedIncidents; }
    public double getSlaComplianceRate() { return slaComplianceRate; }
    public void setSlaComplianceRate(double slaComplianceRate) { this.slaComplianceRate = slaComplianceRate; }
    public double getAverageResolutionMinutes() { return averageResolutionMinutes; }
    public void setAverageResolutionMinutes(double averageResolutionMinutes) { this.averageResolutionMinutes = averageResolutionMinutes; }

    public int getPriorityPercentage(long count) {
        if (totalIncidents <= 0) return 0;
        return (int) Math.round((count * 100.0) / totalIncidents);
    }
}
