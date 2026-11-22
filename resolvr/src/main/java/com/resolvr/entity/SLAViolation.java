package com.resolvr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sla_violations")
public class SLAViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "violation_type", nullable = false, length = 30)
    private String violationType;

    @Column(name = "deadline_timestamp", nullable = false)
    private LocalDateTime deadlineTimestamp;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();

    @Column(name = "overdue_minutes", nullable = false)
    private long overdueMinutes;

    @Column(nullable = false, length = 500)
    private String details;

    public SLAViolation() {
    }

    public SLAViolation(Incident incident, String violationType, LocalDateTime deadlineTimestamp,
                        long overdueMinutes, String details) {
        this.incident = incident;
        this.violationType = violationType;
        this.deadlineTimestamp = deadlineTimestamp;
        this.overdueMinutes = Math.max(0, overdueMinutes);
        this.details = details;
        this.detectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }
    public LocalDateTime getDeadlineTimestamp() { return deadlineTimestamp; }
    public void setDeadlineTimestamp(LocalDateTime deadlineTimestamp) { this.deadlineTimestamp = deadlineTimestamp; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public long getOverdueMinutes() { return overdueMinutes; }
    public void setOverdueMinutes(long overdueMinutes) { this.overdueMinutes = overdueMinutes; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
