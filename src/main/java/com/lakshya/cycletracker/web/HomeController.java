package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.CycleRepository;
import com.lakshya.cycletracker.domain.DailyLogRepository;
import com.lakshya.cycletracker.security.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Controller
public class HomeController {

    private final CycleRepository cycles;
    private final DailyLogRepository logs;
    private final CurrentUser currentUser;

    public HomeController(CycleRepository cycles, DailyLogRepository logs, CurrentUser currentUser) {
        this.cycles = cycles;
        this.logs = logs;
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        AppUser user = currentUser.get(principal);
        LocalDate today = LocalDate.now();
        model.addAttribute("user", user);
        model.addAttribute("today", today);
        cycles.findFirstByUserOrderByStartDateDesc(user).ifPresent(latest -> {
            model.addAttribute("latest", latest);
            model.addAttribute("cycleDay", ChronoUnit.DAYS.between(latest.getStartDate(), today) + 1);
        });
        model.addAttribute("todayLog", logs.findByUserAndDate(user, today).orElse(null));
        return "index";
    }
}
