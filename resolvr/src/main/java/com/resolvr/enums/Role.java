package com.resolvr.enums;

public enum Role {
    ROLE_EMPLOYEE("Employee / Reporter"),
    ROLE_TECHNICIAN("Technician / Support Engineer"),
    ROLE_ADMIN("Administrator / IT Manager");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
