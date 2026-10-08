package com.lakshya.cycletracker.domain;

public enum Flow {
    NONE("None"), SPOTTING("Spotting"), LIGHT("Light"), MEDIUM("Medium"), HEAVY("Heavy");

    private final String label;
    Flow(String label) { this.label = label; }
    public String getLabel() { return label; }
}
