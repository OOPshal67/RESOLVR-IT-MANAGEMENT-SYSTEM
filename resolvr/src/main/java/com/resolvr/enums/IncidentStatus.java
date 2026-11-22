package com.resolvr.enums;

public enum IncidentStatus {
    NEW("New", 1),
    CLASSIFIED("Classified", 2),
    ASSIGNED("Assigned", 3),
    ACKNOWLEDGED("Acknowledged", 4),
    IN_PROGRESS("In Progress", 5),
    WAITING_FOR_USER("Waiting for User", 5),
    ESCALATED("Escalated", 5),
    RESOLVED("Resolved", 6),
    VERIFIED("Verified", 7),
    CLOSED("Closed", 8),
    REOPENED("Reopened", 5),
    CANCELLED("Cancelled", 8);

    private final String displayName;
    private final int stepOrder;

    IncidentStatus(String displayName, int stepOrder) {
        this.displayName = displayName;
        this.stepOrder = stepOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }

    public boolean isActive() {
        return this != RESOLVED && this != VERIFIED && this != CLOSED && this != CANCELLED;
    }
}
