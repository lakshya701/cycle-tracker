package com.lakshya.cycletracker.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One period: the day bleeding started and (once known) the day it ended. */
@Entity
public class Cycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;   // null while the period is still ongoing

    @Column(length = 500)
    private String notes;

    protected Cycle() {}

    public Cycle(AppUser user, LocalDate startDate) {
        this.user = user;
        this.startDate = startDate;
    }

    public boolean isOngoing() { return endDate == null; }

    /** Period length in days, counting both the first and last day; null while ongoing. */
    public Integer getPeriodLength() {
        return endDate == null ? null : (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
