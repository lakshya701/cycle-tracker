package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.Cycle;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class CycleForm {

    @NotNull(message = "Please choose the start date.")
    @PastOrPresent(message = "Start date can't be in the future.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate = LocalDate.now();

    @PastOrPresent(message = "End date can't be in the future.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Size(max = 500)
    private String notes;

    public static CycleForm from(Cycle cycle) {
        CycleForm form = new CycleForm();
        form.startDate = cycle.getStartDate();
        form.endDate = cycle.getEndDate();
        form.notes = cycle.getNotes();
        return form;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
