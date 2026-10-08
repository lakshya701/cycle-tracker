package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.*;
import com.lakshya.cycletracker.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/logs")
public class LogController {

    private final DailyLogRepository logs;
    private final CurrentUser currentUser;

    public LogController(DailyLogRepository logs, CurrentUser currentUser) {
        this.logs = logs;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("logs", logs.findByUserOrderByDateDesc(currentUser.get(principal)));
        return "logs/list";
    }

    /** Opens the form for a day; if that day already has a log, it's loaded for editing. */
    @GetMapping("/day")
    public String form(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Principal principal, Model model) {
        LocalDate day = date != null ? date : LocalDate.now();
        if (day.isAfter(LocalDate.now())) {
            day = LocalDate.now();
        }
        AppUser user = currentUser.get(principal);
        LogForm form = logs.findByUserAndDate(user, day).map(LogForm::from).orElseGet(() -> {
            LogForm f = new LogForm();
            f.setDate(date != null ? date : LocalDate.now());
            return f;
        });
        form.setDate(day);
        addOptions(model);
        model.addAttribute("form", form);
        return "logs/form";
    }

    @PostMapping
    @Transactional
    public String save(@Valid @ModelAttribute("form") LogForm form, BindingResult errors,
                       Principal principal, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            addOptions(model);
            return "logs/form";
        }
        AppUser user = currentUser.get(principal);
        DailyLog log = logs.findByUserAndDate(user, form.getDate())
                .orElseGet(() -> new DailyLog(user, form.getDate()));
        log.setFlow(form.getFlow());
        log.setMood(form.getMood());
        log.setSymptoms(form.getSymptoms());
        log.setNotes(form.getNotes() == null || form.getNotes().isBlank() ? null : form.getNotes().trim());
        logs.save(log);
        flash.addFlashAttribute("message", "Saved your log for " + form.getDate() + ".");
        return "redirect:/";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        DailyLog log = logs.findByIdAndUser(id, currentUser.get(principal))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        logs.delete(log);
        flash.addFlashAttribute("message", "Log deleted.");
        return "redirect:/logs";
    }

    private void addOptions(Model model) {
        model.addAttribute("flows", Flow.values());
        model.addAttribute("moods", Mood.values());
        model.addAttribute("allSymptoms", Symptom.values());
    }
}
