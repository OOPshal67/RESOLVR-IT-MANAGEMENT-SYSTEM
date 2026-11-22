package com.resolvr.dto;

import com.resolvr.enums.ImpactLevel;
import com.resolvr.enums.UrgencyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IncidentCreateRequest {

    @NotBlank(message = "Incident title is required")
    @Size(min = 5, max = 180, message = "Title must be between 5 and 180 characters")
    private String title;

    @NotBlank(message = "Detailed incident description is required")
    @Size(min = 10, message = "Please provide at least 10 characters describing the issue")
    private String description;

    @NotNull(message = "Please select an incident category")
    private Long categoryId;

    @NotNull(message = "Please select the affected IT service")
    private Long serviceId;

    @NotNull(message = "Impact level is required")
    private ImpactLevel impact = ImpactLevel.MEDIUM;

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgency = UrgencyLevel.MEDIUM;

    @Min(value = 1, message = "Affected user count must be at least 1")
    private int affectedUsers = 1;

    @Size(max = 255)
    private String attachmentName;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public ImpactLevel getImpact() { return impact; }
    public void setImpact(ImpactLevel impact) { this.impact = impact; }
    public UrgencyLevel getUrgency() { return urgency; }
    public void setUrgency(UrgencyLevel urgency) { this.urgency = urgency; }
    public int getAffectedUsers() { return affectedUsers; }
    public void setAffectedUsers(int affectedUsers) { this.affectedUsers = affectedUsers; }
    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
}
