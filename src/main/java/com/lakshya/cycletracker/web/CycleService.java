package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.Cycle;
import com.lakshya.cycletracker.domain.CycleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@Transactional
public class CycleService {

    private final CycleRepository cycles;

    public CycleService(CycleRepository cycles) {
        this.cycles = cycles;
    }

    /** Returns the cycle only if it belongs to this user; otherwise 404 (we don't reveal that it exists). */
    @Transactional(readOnly = true)
    public Cycle getOwned(Long id, AppUser user) {
        return cycles.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Checks rules that need the database: end after start, and no overlap with other periods. */
    public void validate(CycleForm form, AppUser user, Long excludeId, BindingResult errors) {
        LocalDate start = form.getStartDate();
        LocalDate end = form.getEndDate();
        if (start == null) return;
        if (end != null && end.isBefore(start)) {
            errors.rejectValue("endDate", "beforeStart", "End date can't be before the start date.");
            return;
        }
        LocalDate newEnd = end != null ? end : start;
        for (Cycle other : cycles.findByUserOrderByStartDateDesc(user)) {
            if (other.getId().equals(excludeId)) continue;
            LocalDate otherEnd = other.getEndDate() != null ? other.getEndDate() : other.getStartDate();
            boolean overlaps = !start.isAfter(otherEnd) && !newEnd.isBefore(other.getStartDate());
            if (overlaps) {
                errors.rejectValue("startDate", "overlap",
                        "This overlaps the period you logged starting " + other.getStartDate() + ".");
                return;
            }
        }
    }

    public Cycle create(CycleForm form, AppUser user) {
        Cycle cycle = new Cycle(user, form.getStartDate());
        apply(form, cycle);
        return cycles.save(cycle);
    }

    public void update(Cycle cycle, CycleForm form) {
        cycle.setStartDate(form.getStartDate());
        apply(form, cycle);
    }

    private void apply(CycleForm form, Cycle cycle) {
        cycle.setEndDate(form.getEndDate());
        cycle.setNotes(form.getNotes() == null || form.getNotes().isBlank() ? null : form.getNotes().trim());
    }
}
