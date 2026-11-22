package com.resolvr.enums;

public enum PriorityLevel {
    P1("P1 - Critical", 1, "#DC2626"),
    P2("P2 - High", 2, "#EA580C"),
    P3("P3 - Medium", 3, "#F59E0B"),
    P4("P4 - Low", 4, "#16A34A");

    private final String label;
    private final int rank;
    private final String hexColor;

    PriorityLevel(String label, int rank, String hexColor) {
        this.label = label;
        this.rank = rank;
        this.hexColor = hexColor;
    }

    public String getLabel() {
        return label;
    }

    public int getRank() {
        return rank;
    }

    public String getHexColor() {
        return hexColor;
    }
}
