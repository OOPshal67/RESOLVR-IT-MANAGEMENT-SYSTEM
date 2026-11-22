package com.resolvr.enums;

public enum EscalationLevel {
    NONE(0, "No Escalation"),
    LEVEL_1_TECHNICIAN(1, "Level 1: Assigned Technician Alerted"),
    LEVEL_2_TEAM_LEAD(2, "Level 2: Escalated to Team Lead"),
    LEVEL_3_ADMIN(3, "Level 3: Escalated to System Administrator"),
    LEVEL_4_IT_MANAGER(4, "Level 4: Escalated to IT Manager");

    private final int level;
    private final String description;

    EscalationLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }
}
