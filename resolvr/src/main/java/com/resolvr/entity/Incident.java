package com.resolvr.entity;

import com.resolvr.enums.*;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
public class Incident {

    @Version
    @Column(nullable = false)
    private long revision;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_key", nullable = false, unique = true, length = 30)
    private String incidentKey;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Employee reporter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private IncidentCategory category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", nullable = false)
    private ITService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImpactLevel impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UrgencyLevel urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_criticality", nullable = false, length = 30)
    private BusinessCriticality businessCriticality;

    @Column(name = "affected_users", nullable = false)
    private int affectedUsers = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PriorityLevel priority;

    @Column(name = "priority_score", nullable = false)
    private double priorityScore;

    @Column(name = "priority_reasoning", length = 500)
    private String priorityReasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.NEW;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sla_policy_id", nullable = false)
    private SLAPolicy slaPolicy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "response_deadline", nullable = false)
    private LocalDateTime responseDeadline;

    @Column(name = "resolution_deadline", nullable = false)
    private LocalDateTime resolutionDeadline;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "response_sla_violated", nullable = false)
    private boolean responseSlaViolated = false;

    @Column(name = "resolution_sla_violated", nullable = false)
    private boolean resolutionSlaViolated = false;

    @Column(name = "sla_warning_sent", nullable = false)
    private boolean slaWarningSent = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "sla_risk_level", nullable = false, length = 20)
    private SLARiskLevel slaRiskLevel = SLARiskLevel.LOW;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", nullable = false, length = 35)
    private EscalationLevel escalationLevel = EscalationLevel.NONE;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "closure_notes", columnDefinition = "TEXT")
    private String closureNotes;

    @Column(name = "recommended_technician_reason", length = 500)
    private String recommendedTechnicianReason;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<InvestigationNote> investigationNotes = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("requestedAt DESC")
    private List<InformationRequest> informationRequests = new ArrayList<>();

    @OneToOne(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private Resolution resolution;

    @OneToOne(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private IncidentFeedback feedback;

    public Incident() {
    }

    public boolean isSlaViolated() {
        return responseSlaViolated || resolutionSlaViolated;
    }

    public long getRemainingResolutionMinutes() {
        if (resolutionDeadline == null) return 0;
        LocalDateTime reference = resolvedAt != null ? resolvedAt : LocalDateTime.now();
        return Duration.between(reference, resolutionDeadline).toMinutes();
    }

    public long getElapsedMinutes() {
        LocalDateTime end = resolvedAt != null ? resolvedAt : LocalDateTime.now();
        return createdAt == null ? 0 : Math.max(0, Duration.between(createdAt, end).toMinutes());
    }

    public String getFormattedAge() {
        long mins = getElapsedMinutes();
        if (mins < 60) return mins + "m";
        long hours = mins / 60;
        long rem = mins % 60;
        if (hours < 24) return hours + "h " + rem + "m";
        return (hours / 24) + "d " + (hours % 24) + "h";
    }

    public String getFormattedRemainingResolutionTime() {
        if (status == IncidentStatus.RESOLVED || status == IncidentStatus.VERIFIED || status == IncidentStatus.CLOSED) {
            return resolutionSlaViolated ? "Resolved (Breached)" : "Met SLA Target";
        }
        long mins = getRemainingResolutionMinutes();
        if (mins < 0) {
            return "Overdue by " + Math.abs(mins) + "m";
        }
        long hours = mins / 60;
        long remMins = mins % 60;
        return String.format("%02dh:%02dm remaining", hours, remMins);
    }

    public int getSlaProgressPercentage() {
        if (slaPolicy == null || slaPolicy.getResolutionTargetMinutes() <= 0) return 0;
        long elapsed = getElapsedMinutes();
        int pct = (int) Math.round((elapsed * 100.0) / slaPolicy.getResolutionTargetMinutes());
        return Math.clamp(pct, 0, 100);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIncidentKey() { return incidentKey; }
    public void setIncidentKey(String incidentKey) { this.incidentKey = incidentKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Employee getReporter() { return reporter; }
    public void setReporter(Employee reporter) { this.reporter = reporter; }
    public IncidentCategory getCategory() { return category; }
    public void setCategory(IncidentCategory category) { this.category = category; }
    public ITService getService() { return service; }
    public void setService(ITService service) { this.service = service; }
    public ImpactLevel getImpact() { return impact; }
    public void setImpact(ImpactLevel impact) { this.impact = impact; }
    public UrgencyLevel getUrgency() { return urgency; }
    public void setUrgency(UrgencyLevel urgency) { this.urgency = urgency; }
    public BusinessCriticality getBusinessCriticality() { return businessCriticality; }
    public void setBusinessCriticality(BusinessCriticality businessCriticality) { this.businessCriticality = businessCriticality; }
    public int getAffectedUsers() { return affectedUsers; }
    public void setAffectedUsers(int affectedUsers) { this.affectedUsers = Math.max(1, affectedUsers); }
    public PriorityLevel getPriority() { return priority; }
    public void setPriority(PriorityLevel priority) { this.priority = priority; }
    public double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
    public String getPriorityReasoning() { return priorityReasoning; }
    public void setPriorityReasoning(String priorityReasoning) { this.priorityReasoning = priorityReasoning; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public Technician getAssignedTechnician() { return assignedTechnician; }
    public void setAssignedTechnician(Technician assignedTechnician) { this.assignedTechnician = assignedTechnician; }
    public SLAPolicy getSlaPolicy() { return slaPolicy; }
    public void setSlaPolicy(SLAPolicy slaPolicy) { this.slaPolicy = slaPolicy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public LocalDateTime getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(LocalDateTime firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    public LocalDateTime getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(LocalDateTime responseDeadline) { this.responseDeadline = responseDeadline; }
    public LocalDateTime getResolutionDeadline() { return resolutionDeadline; }
    public void setResolutionDeadline(LocalDateTime resolutionDeadline) { this.resolutionDeadline = resolutionDeadline; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public boolean isResponseSlaViolated() { return responseSlaViolated; }
    public void setResponseSlaViolated(boolean responseSlaViolated) { this.responseSlaViolated = responseSlaViolated; }
    public boolean isResolutionSlaViolated() { return resolutionSlaViolated; }
    public void setResolutionSlaViolated(boolean resolutionSlaViolated) { this.resolutionSlaViolated = resolutionSlaViolated; }
    public boolean isSlaWarningSent() { return slaWarningSent; }
    public void setSlaWarningSent(boolean slaWarningSent) { this.slaWarningSent = slaWarningSent; }
    public SLARiskLevel getSlaRiskLevel() { return slaRiskLevel; }
    public void setSlaRiskLevel(SLARiskLevel slaRiskLevel) { this.slaRiskLevel = slaRiskLevel; }
    public EscalationLevel getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(EscalationLevel escalationLevel) { this.escalationLevel = escalationLevel; }
    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
    public String getClosureNotes() { return closureNotes; }
    public void setClosureNotes(String closureNotes) { this.closureNotes = closureNotes; }
    public String getRecommendedTechnicianReason() { return recommendedTechnicianReason; }
    public void setRecommendedTechnicianReason(String recommendedTechnicianReason) { this.recommendedTechnicianReason = recommendedTechnicianReason; }
    public List<InvestigationNote> getInvestigationNotes() { return investigationNotes; }
    public void setInvestigationNotes(List<InvestigationNote> investigationNotes) { this.investigationNotes = investigationNotes; }
    public List<InformationRequest> getInformationRequests() { return informationRequests; }
    public void setInformationRequests(List<InformationRequest> informationRequests) { this.informationRequests = informationRequests; }
    public Resolution getResolution() { return resolution; }
    public void setResolution(Resolution resolution) { this.resolution = resolution; }
    public IncidentFeedback getFeedback() { return feedback; }
    public void setFeedback(IncidentFeedback feedback) { this.feedback = feedback; }
}
