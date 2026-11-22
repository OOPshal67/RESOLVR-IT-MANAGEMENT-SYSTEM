package com.resolvr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resolutions")
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resolved_by_id", nullable = false)
    private Technician resolvedBy;

    @Column(name = "root_cause", nullable = false, columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "resolution_summary", nullable = false, columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "workaround_applied", columnDefinition = "TEXT")
    private String workaroundApplied;

    @Column(name = "time_spent_minutes", nullable = false)
    private int timeSpentMinutes = 30;

    @Column(name = "resolved_at", nullable = false)
    private LocalDateTime resolvedAt = LocalDateTime.now();

    public Resolution() {
    }

    public Resolution(Incident incident, Technician resolvedBy, String rootCause,
                      String resolutionSummary, String workaroundApplied, int timeSpentMinutes) {
        this.incident = incident;
        this.resolvedBy = resolvedBy;
        this.rootCause = rootCause;
        this.resolutionSummary = resolutionSummary;
        this.workaroundApplied = workaroundApplied;
        this.timeSpentMinutes = Math.max(1, timeSpentMinutes);
        this.resolvedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public Technician getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Technician resolvedBy) { this.resolvedBy = resolvedBy; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
    public String getWorkaroundApplied() { return workaroundApplied; }
    public void setWorkaroundApplied(String workaroundApplied) { this.workaroundApplied = workaroundApplied; }
    public int getTimeSpentMinutes() { return timeSpentMinutes; }
    public void setTimeSpentMinutes(int timeSpentMinutes) { this.timeSpentMinutes = timeSpentMinutes; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
