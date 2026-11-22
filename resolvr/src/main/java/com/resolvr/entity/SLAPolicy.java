package com.resolvr.entity;

import com.resolvr.enums.PriorityLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sla_policies")
public class SLAPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, unique = true, length = 15)
    private PriorityLevel priorityLevel;

    @Column(name = "policy_name", nullable = false, length = 80)
    private String policyName;

    @Column(name = "response_target_minutes", nullable = false)
    private int responseTargetMinutes;

    @Column(name = "resolution_target_minutes", nullable = false)
    private int resolutionTargetMinutes;

    @Column(name = "warning_threshold_percent", nullable = false)
    private int warningThresholdPercent = 25;

    @Column(name = "escalation_enabled", nullable = false)
    private boolean escalationEnabled = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public SLAPolicy() {
    }

    public SLAPolicy(PriorityLevel priorityLevel, String policyName, int responseTargetMinutes,
                     int resolutionTargetMinutes, int warningThresholdPercent) {
        this.priorityLevel = priorityLevel;
        this.policyName = policyName;
        this.responseTargetMinutes = responseTargetMinutes;
        this.resolutionTargetMinutes = resolutionTargetMinutes;
        this.warningThresholdPercent = warningThresholdPercent;
        this.escalationEnabled = true;
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public String getFormattedResponseTarget() {
        if (responseTargetMinutes < 60) {
            return responseTargetMinutes + " mins";
        }
        return (responseTargetMinutes / 60) + "h " + (responseTargetMinutes % 60 > 0 ? (responseTargetMinutes % 60) + "m" : "");
    }

    public String getFormattedResolutionTarget() {
        if (resolutionTargetMinutes < 60) {
            return resolutionTargetMinutes + " mins";
        }
        return (resolutionTargetMinutes / 60) + " hours";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public int getResponseTargetMinutes() { return responseTargetMinutes; }
    public void setResponseTargetMinutes(int responseTargetMinutes) { this.responseTargetMinutes = responseTargetMinutes; }
    public int getResolutionTargetMinutes() { return resolutionTargetMinutes; }
    public void setResolutionTargetMinutes(int resolutionTargetMinutes) { this.resolutionTargetMinutes = resolutionTargetMinutes; }
    public int getWarningThresholdPercent() { return warningThresholdPercent; }
    public void setWarningThresholdPercent(int warningThresholdPercent) { this.warningThresholdPercent = warningThresholdPercent; }
    public boolean isEscalationEnabled() { return escalationEnabled; }
    public void setEscalationEnabled(boolean escalationEnabled) { this.escalationEnabled = escalationEnabled; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
