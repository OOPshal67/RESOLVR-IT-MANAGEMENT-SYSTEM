package com.resolvr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(name = "assigned_by_username", nullable = false, length = 60)
    private String assignedByUsername;

    @Column(name = "strategy_used", nullable = false, length = 50)
    private String strategyUsed;

    @Column(name = "recommendation_reason", length = 500)
    private String recommendationReason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @Column(name = "reassignment_reason", length = 255)
    private String reassignmentReason;

    public Assignment() {
    }

    public Assignment(Incident incident, Technician technician, String assignedByUsername,
                      String strategyUsed, String recommendationReason) {
        this.incident = incident;
        this.technician = technician;
        this.assignedByUsername = assignedByUsername;
        this.strategyUsed = strategyUsed;
        this.recommendationReason = recommendationReason;
        this.active = true;
        this.assignedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public Technician getTechnician() { return technician; }
    public void setTechnician(Technician technician) { this.technician = technician; }
    public String getAssignedByUsername() { return assignedByUsername; }
    public void setAssignedByUsername(String assignedByUsername) { this.assignedByUsername = assignedByUsername; }
    public String getStrategyUsed() { return strategyUsed; }
    public void setStrategyUsed(String strategyUsed) { this.strategyUsed = strategyUsed; }
    public String getRecommendationReason() { return recommendationReason; }
    public void setRecommendationReason(String recommendationReason) { this.recommendationReason = recommendationReason; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getUnassignedAt() { return unassignedAt; }
    public void setUnassignedAt(LocalDateTime unassignedAt) { this.unassignedAt = unassignedAt; }
    public String getReassignmentReason() { return reassignmentReason; }
    public void setReassignmentReason(String reassignmentReason) { this.reassignmentReason = reassignmentReason; }
}
