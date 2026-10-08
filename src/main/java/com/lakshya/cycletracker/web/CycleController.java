package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.Cycle;
import com.lakshya.cycletracker.domain.CycleRepository;
import com.lakshya.cycletracker.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/cycles")
public class CycleController {

    /** A row in the history table: the period plus the length of the cycle it started. */
    public record CycleRow(Cycle cycle, Integer cycleLength) {}

    private final CycleRepository cycles;
    private final CycleService cycleService;
    private final CurrentUser currentUser;

    public CycleController(CycleRepository cycles, CycleService cycleService, CurrentUser currentUser) {
        this.cycles = cycles;
        this.cycleService = cycleService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        List<Cycle> newestFirst = cycles.findByUserOrderByStartDateDesc(currentUser.get(principal));
        List<CycleRow> rows = new ArrayList<>();
        for (int i = 0; i < newestFirst.size(); i++) {
            Cycle cycle = newestFirst.get(i);
            // A cycle runs from this period's start to the next period's start.
            Integer length = i == 0 ? null
                    : (int) ChronoUnit.DAYS.between(cycle.getStartDate(), newestFirst.get(i - 1).getStartDate());
            rows.add(new CycleRow(cycle, length));
        }
        model.addAttribute("rows", rows);
        return "cycles/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CycleForm());
        model.addAttribute("editing", false);
        return "cycles/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CycleForm form, BindingResult errors,
                         Principal principal, Model model, RedirectAttributes flash) {
        AppUser user = currentUser.get(principal);
        cycleService.validate(form, user, null, errors);
        if (errors.hasErrors()) {
            model.addAttribute("editing", false);
            return "cycles/form";
        }
        cycleService.create(form, user);
        flash.addFlashAttribute("message", "Period saved.");
        return "redirect:/cycles";
    }

    @PostMapping("/start-today")
    public String startToday(Principal principal, RedirectAttributes flash) {
        AppUser user = currentUser.get(principal);
        CycleForm form = new CycleForm();
        BindingResult errors = new org.springframework.validation.BeanPropertyBindingResult(form, "form");
        cycleService.validate(form, user, null, errors);
        if (errors.hasErrors()) {
            flash.addFlashAttribute("error", errors.getFieldError().getDefaultMessage());
        } else {
            cycleService.create(form, user);
            flash.addFlashAttribute("message", "Logged: period started today.");
        }
        return "redirect:/";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        Cycle cycle = cycleService.getOwned(id, currentUser.get(principal));
        model.addAttribute("form", CycleForm.from(cycle));
        model.addAttribute("cycleId", id);
        model.addAttribute("editing", true);
        return "cycles/form";
    }

    @PostMapping("/{id}")
    @Transactional
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") CycleForm form, BindingResult errors,
                         Principal principal, Model model, RedirectAttributes flash) {
        AppUser user = currentUser.get(principal);
        Cycle cycle = cycleService.getOwned(id, user);
        cycleService.validate(form, user, id, errors);
        if (errors.hasErrors()) {
            model.addAttribute("cycleId", id);
            model.addAttribute("editing", true);
            return "cycles/form";
        }
        cycleService.update(cycle, form);
        flash.addFlashAttribute("message", "Period updated.");
        return "redirect:/cycles";
    }

    @PostMapping("/{id}/end-today")
    @Transactional
    public String endToday(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        Cycle cycle = cycleService.getOwned(id, currentUser.get(principal));
        if (cycle.isOngoing()) {
            cycle.setEndDate(LocalDate.now());
            flash.addFlashAttribute("message", "Logged: period ended today.");
        }
        return "redirect:/";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        cycles.delete(cycleService.getOwned(id, currentUser.get(principal)));
        flash.addFlashAttribute("message", "Period deleted.");
        return "redirect:/cycles";
    }
}
