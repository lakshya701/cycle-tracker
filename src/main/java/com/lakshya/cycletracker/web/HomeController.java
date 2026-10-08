package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.Cycle;
import com.lakshya.cycletracker.domain.CycleRepository;
import com.lakshya.cycletracker.domain.DailyLogRepository;
import com.lakshya.cycletracker.prediction.PredictionService;
import com.lakshya.cycletracker.security.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class HomeController {

    private final CycleRepository cycles;
    private final DailyLogRepository logs;
    private final PredictionService predictions;
    private final CurrentUser currentUser;

    public HomeController(CycleRepository cycles, DailyLogRepository logs,
                          PredictionService predictions, CurrentUser currentUser) {
        this.cycles = cycles;
        this.logs = logs;
        this.predictions = predictions;
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        AppUser user = currentUser.get(principal);
        LocalDate today = LocalDate.now();
        List<Cycle> history = cycles.findByUserOrderByStartDateAsc(user);

        model.addAttribute("user", user);
        model.addAttribute("today", today);
        model.addAttribute("todayLog", logs.findByUserAndDate(user, today).orElse(null));
        if (!history.isEmpty()) {
            Cycle latest = history.getLast();
            model.addAttribute("latest", latest);
            model.addAttribute("cycleDay", ChronoUnit.DAYS.between(latest.getStartDate(), today) + 1);
        }
        predictions.predict(history).ifPresent(p -> {
            model.addAttribute("prediction", p);
            model.addAttribute("daysUntil", p.daysUntil(today));
            model.addAttribute("daysLate", p.daysLate(today));
            model.addAttribute("inFertileWindow", !today.isBefore(p.fertileStart()) && !today.isAfter(p.fertileEnd()));
        });
        model.addAttribute("healthNotes", predictions.healthNotes(history));
        return "index";
    }
}
