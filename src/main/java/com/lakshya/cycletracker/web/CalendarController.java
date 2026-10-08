package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.Cycle;
import com.lakshya.cycletracker.domain.CycleRepository;
import com.lakshya.cycletracker.domain.DailyLog;
import com.lakshya.cycletracker.domain.DailyLogRepository;
import com.lakshya.cycletracker.prediction.Prediction;
import com.lakshya.cycletracker.prediction.PredictionService;
import com.lakshya.cycletracker.security.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CalendarController {

    /** One square on the calendar. {@code kind} drives the colour: period, predicted, fertile, ovulation or none. */
    public record Day(LocalDate date, boolean inMonth, boolean today, boolean future, String kind, boolean logged) {}

    /** How many future cycles to draw predictions for. */
    private static final int CYCLES_AHEAD = 3;

    private final CycleRepository cycles;
    private final DailyLogRepository logs;
    private final PredictionService predictions;
    private final CurrentUser currentUser;

    public CalendarController(CycleRepository cycles, DailyLogRepository logs,
                              PredictionService predictions, CurrentUser currentUser) {
        this.cycles = cycles;
        this.logs = logs;
        this.predictions = predictions;
        this.currentUser = currentUser;
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                           Principal principal, Model model) {
        AppUser user = currentUser.get(principal);
        LocalDate today = LocalDate.now();
        YearMonth shown = month != null ? month : YearMonth.from(today);

        List<Cycle> history = cycles.findByUserOrderByStartDateAsc(user);
        Optional<Prediction> prediction = predictions.predict(history);
        Map<LocalDate, String> kinds = dayKinds(history, prediction.orElse(null), today);

        LocalDate gridStart = shown.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = shown.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        Set<LocalDate> loggedDays = logs.findByUserAndDateBetween(user, gridStart, gridEnd).stream()
                .map(DailyLog::getDate).collect(Collectors.toSet());

        List<List<Day>> weeks = new ArrayList<>();
        for (LocalDate weekStart = gridStart; !weekStart.isAfter(gridEnd); weekStart = weekStart.plusWeeks(1)) {
            List<Day> week = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                LocalDate d = weekStart.plusDays(i);
                week.add(new Day(d, YearMonth.from(d).equals(shown), d.equals(today), d.isAfter(today),
                        kinds.getOrDefault(d, ""), loggedDays.contains(d)));
            }
            weeks.add(week);
        }

        model.addAttribute("month", shown);
        model.addAttribute("prevMonth", shown.minusMonths(1));
        model.addAttribute("nextMonth", shown.plusMonths(1));
        model.addAttribute("weeks", weeks);
        model.addAttribute("prediction", prediction.orElse(null));
        return "calendar";
    }

    /**
     * Works out the colour of every relevant day. Logged periods win over predictions,
     * and predictions are only drawn for today onwards.
     */
    static Map<LocalDate, String> dayKinds(List<Cycle> history, Prediction prediction, LocalDate today) {
        Map<LocalDate, String> kinds = new HashMap<>();
        if (prediction != null) {
            // Fertile window of the current cycle, then the following ones.
            for (int k = 0; k < CYCLES_AHEAD; k++) {
                LocalDate periodStart = prediction.startAfter(k);
                LocalDate ovulation = periodStart.minusDays(14);
                for (LocalDate d = ovulation.minusDays(5); !d.isAfter(ovulation.plusDays(1)); d = d.plusDays(1)) {
                    kinds.put(d, d.equals(ovulation) ? "ovulation" : "fertile");
                }
                for (int i = 0; i < prediction.periodLength(); i++) {
                    LocalDate d = periodStart.plusDays(i);
                    if (!d.isBefore(today)) kinds.put(d, "predicted");
                }
            }
        }
        for (Cycle cycle : history) {
            LocalDate end = cycle.getEndDate() != null ? cycle.getEndDate()
                    : min(today, cycle.getStartDate().plusDays(prediction != null ? prediction.periodLength() - 1 : 4));
            for (LocalDate d = cycle.getStartDate(); !d.isAfter(end); d = d.plusDays(1)) {
                kinds.put(d, "period");
            }
        }
        return kinds;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
