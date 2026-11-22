package com.resolvr.enums;

public enum ImpactLevel {
    LOW(1, "Single User / Minor Inconvenience"),
    MEDIUM(2, "Department / Partial Disruption"),
    HIGH(3, "Multiple Departments / Core Degradation"),
    CRITICAL(4, "Enterprise-Wide / Complete Outage");

    private final int weight;
    private final String description;

    ImpactLevel(int weight, String description) {
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
