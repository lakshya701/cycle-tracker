package com.lakshya.cycletracker.domain;

public enum Mood {
    HAPPY("Happy"), CALM("Calm"), TIRED("Tired"), IRRITABLE("Irritable"), SAD("Sad"), ANXIOUS("Anxious");

    private final String label;
    Mood(String label) { this.label = label; }
    public String getLabel() { return label; }
}
