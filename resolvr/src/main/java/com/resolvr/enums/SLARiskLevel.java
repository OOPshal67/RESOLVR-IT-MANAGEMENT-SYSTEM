package com.resolvr.enums;

public enum SLARiskLevel {
    LOW("Low Risk", "#16A34A"),
    MEDIUM("Medium Risk", "#F59E0B"),
    HIGH("High Risk", "#EA580C"),
    CRITICAL("Critical Risk", "#DC2626"),
    BREACHED("SLA Breached", "#991B1B");

    private final String label;
    private final String badgeColor;

    SLARiskLevel(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }
}
