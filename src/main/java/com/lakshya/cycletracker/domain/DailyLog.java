package com.lakshya.cycletracker.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/** How a single day went: flow, symptoms and mood. At most one per user per day. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Flow flow = Flow.NONE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Mood mood;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "daily_log_symptom")
    @Enumerated(EnumType.STRING)
    @Column(name = "symptom", length = 30)
    private Set<Symptom> symptoms = EnumSet.noneOf(Symptom.class);

    @Column(length = 500)
    private String notes;

    protected DailyLog() {}

    public DailyLog(AppUser user, LocalDate date) {
        this.user = user;
        this.date = date;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalDate getDate() { return date; }
    public Flow getFlow() { return flow; }
    public void setFlow(Flow flow) { this.flow = flow == null ? Flow.NONE : flow; }
    public Mood getMood() { return mood; }
    public void setMood(Mood mood) { this.mood = mood; }
    public Set<Symptom> getSymptoms() { return symptoms; }
    public void setSymptoms(Set<Symptom> symptoms) {
        this.symptoms.clear();
        if (symptoms != null) this.symptoms.addAll(symptoms);
    }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
