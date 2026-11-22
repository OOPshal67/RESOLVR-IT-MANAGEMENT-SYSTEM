package com.resolvr.enums;

public enum UrgencyLevel {
    LOW(1, "Workaround Available / Deferrable"),
    MEDIUM(2, "Moderate Time Sensitivity"),
    HIGH(3, "Time-Critical Business Operation"),
    CRITICAL(4, "Immediate Action Required");

    private final int weight;
    private final String description;

    UrgencyLevel(int weight, String description) {
        this.weight = weight;
        this.description = description;
    }

    public int getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }
}
