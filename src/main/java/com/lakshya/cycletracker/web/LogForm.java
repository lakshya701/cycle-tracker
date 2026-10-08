package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.DailyLog;
import com.lakshya.cycletracker.domain.Flow;
import com.lakshya.cycletracker.domain.Mood;
import com.lakshya.cycletracker.domain.Symptom;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public class LogForm {

    @NotNull(message = "Please choose a date.")
    @PastOrPresent(message = "You can't log a future day.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date = LocalDate.now();

    private Flow flow = Flow.NONE;
    private Mood mood;
    private Set<Symptom> symptoms = EnumSet.noneOf(Symptom.class);

    @Size(max = 500)
    private String notes;

    public static LogForm from(DailyLog log) {
        LogForm form = new LogForm();
        form.date = log.getDate();
        form.flow = log.getFlow();
        form.mood = log.getMood();
        form.symptoms = EnumSet.noneOf(Symptom.class);
        form.symptoms.addAll(log.getSymptoms());
        form.notes = log.getNotes();
        return form;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Flow getFlow() { return flow; }
    public void setFlow(Flow flow) { this.flow = flow; }
    public Mood getMood() { return mood; }
    public void setMood(Mood mood) { this.mood = mood; }
    public Set<Symptom> getSymptoms() { return symptoms; }
    public void setSymptoms(Set<Symptom> symptoms) { this.symptoms = symptoms; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
