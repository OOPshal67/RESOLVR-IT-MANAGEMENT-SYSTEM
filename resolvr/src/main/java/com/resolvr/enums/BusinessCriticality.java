package com.resolvr.enums;

public enum BusinessCriticality {
    LOW(1, "Non-Essential Support Tool"),
    MEDIUM(2, "Standard Internal Service"),
    HIGH(3, "Core Operational Platform"),
    MISSION_CRITICAL(4, "Mission-Critical Revenue/Security System");

    private final int weight;
    private final String description;

    BusinessCriticality(int weight, String description) {
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
