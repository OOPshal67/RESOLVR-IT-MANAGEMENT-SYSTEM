package com.resolvr.entity;

import com.resolvr.enums.EscalationLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalation_records")
public class EscalationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", nullable = false, length = 35)
    private EscalationLevel escalationLevel;

    @Column(name = "escalated_to_role", nullable = false, length = 40)
    private String escalatedToRole;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "triggered_by", nullable = false, length = 60)
    private String triggeredBy = "SLA_SCHEDULER";

    @Column(name = "escalated_at", nullable = false)
    private LocalDateTime escalatedAt = LocalDateTime.now();

    public EscalationRecord() {
    }

    public EscalationRecord(Incident incident, EscalationLevel escalationLevel,
                            String escalatedToRole, String reason, String triggeredBy) {
        this.incident = incident;
        this.escalationLevel = escalationLevel;
        this.escalatedToRole = escalatedToRole;
        this.reason = reason;
        this.triggeredBy = triggeredBy;
        this.escalatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public EscalationLevel getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(EscalationLevel escalationLevel) { this.escalationLevel = escalationLevel; }
    public String getEscalatedToRole() { return escalatedToRole; }
    public void setEscalatedToRole(String escalatedToRole) { this.escalatedToRole = escalatedToRole; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
}
