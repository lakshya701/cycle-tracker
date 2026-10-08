package com.lakshya.cycletracker.domain;

public enum Symptom {
    CRAMPS("Cramps"), HEADACHE("Headache"), BLOATING("Bloating"), BACKACHE("Back pain"),
    TENDER_BREASTS("Tender breasts"), ACNE("Acne"), FATIGUE("Fatigue"), NAUSEA("Nausea"),
    CRAVINGS("Cravings"), INSOMNIA("Trouble sleeping");

    private final String label;
    Symptom(String label) { this.label = label; }
    public String getLabel() { return label; }
}
