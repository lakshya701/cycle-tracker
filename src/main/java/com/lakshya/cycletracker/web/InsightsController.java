package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.*;
import com.lakshya.cycletracker.prediction.CycleHistory;
import com.lakshya.cycletracker.security.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class InsightsController {

    /** A symptom or mood and how many days it was logged, for the bar lists. */
    public record Count(String label, long count, int percentOfMax) {}

    private static final int DAYS_BEFORE_PERIOD = 3;
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("d MMM yy");

    private final CycleRepository cycles;
    private final DailyLogRepository logs;
    private final CurrentUser currentUser;

    public InsightsController(CycleRepository cycles, DailyLogRepository logs, CurrentUser currentUser) {
        this.cycles = cycles;
        this.logs = logs;
        this.currentUser = currentUser;
    }

    @GetMapping("/insights")
    public String insights(Principal principal, Model model) {
        AppUser user = currentUser.get(principal);
        List<Cycle> history = cycles.findByUserOrderByStartDateAsc(user);
        List<DailyLog> allLogs = logs.findByUserOrderByDateDesc(user);

        // Chart data: one point per completed cycle, labelled by the date it started.
        List<String> cycleLabels = new ArrayList<>();
        List<Integer> cycleLengths = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            int days = (int) java.time.temporal.ChronoUnit.DAYS.between(history.get(i - 1).getStartDate(), history.get(i).getStartDate());
            cycleLabels.add(history.get(i - 1).getStartDate().format(LABEL));
            cycleLengths.add(days);
        }
        List<String> periodLabels = new ArrayList<>();
        List<Integer> periodLengths = new ArrayList<>();
        for (Cycle c : history) {
            if (c.getPeriodLength() != null) {
                periodLabels.add(c.getStartDate().format(LABEL));
                periodLengths.add(c.getPeriodLength());
            }
        }

        List<Integer> plausible = CycleHistory.cycleLengths(history);
        model.addAttribute("cyclesLogged", history.size());
        model.addAttribute("hasCycleStats", !plausible.isEmpty());
        if (!plausible.isEmpty()) {
            model.addAttribute("avgCycle", Math.round(CycleHistory.mean(plausible)));
            model.addAttribute("shortest", Collections.min(plausible));
            model.addAttribute("longest", Collections.max(plausible));
            model.addAttribute("variation", Math.round(CycleHistory.standardDeviation(plausible) * 10) / 10.0);
        }
        model.addAttribute("avgPeriod", periodLengths.isEmpty() ? null : Math.round(CycleHistory.mean(periodLengths) * 10) / 10.0);

        model.addAttribute("cycleLabels", cycleLabels);
        model.addAttribute("cycleLengths", cycleLengths);
        model.addAttribute("periodLabels", periodLabels);
        model.addAttribute("periodLengths", periodLengths);

        model.addAttribute("logCount", allLogs.size());
        model.addAttribute("topSymptoms", symptomCounts(allLogs));
        model.addAttribute("beforePeriod", symptomCounts(logsBeforePeriods(allLogs, history)));
        model.addAttribute("duringPeriod", symptomCounts(logsDuringPeriods(allLogs, history)));
        model.addAttribute("moods", moodCounts(allLogs));
        return "insights";
    }

    /** Logs from the few days just before a period started — useful for spotting PMS patterns. */
    static List<DailyLog> logsBeforePeriods(List<DailyLog> allLogs, List<Cycle> history) {
        return allLogs.stream().filter(log -> history.stream().anyMatch(c ->
                log.getDate().isBefore(c.getStartDate())
                        && !log.getDate().isBefore(c.getStartDate().minusDays(DAYS_BEFORE_PERIOD)))).toList();
    }

    static List<DailyLog> logsDuringPeriods(List<DailyLog> allLogs, List<Cycle> history) {
        return allLogs.stream().filter(log -> history.stream().anyMatch(c -> {
            LocalDate end = c.getEndDate() != null ? c.getEndDate() : c.getStartDate().plusDays(4);
            return !log.getDate().isBefore(c.getStartDate()) && !log.getDate().isAfter(end);
        })).toList();
    }

    static List<Count> symptomCounts(List<DailyLog> logs) {
        Map<Symptom, Long> counts = new EnumMap<>(Symptom.class);
        for (DailyLog log : logs) {
            for (Symptom s : log.getSymptoms()) counts.merge(s, 1L, Long::sum);
        }
        return toCounts(counts.entrySet().stream()
                .map(e -> Map.entry(e.getKey().getLabel(), e.getValue())).toList());
    }

    static List<Count> moodCounts(List<DailyLog> logs) {
        Map<Mood, Long> counts = new EnumMap<>(Mood.class);
        for (DailyLog log : logs) {
            if (log.getMood() != null) counts.merge(log.getMood(), 1L, Long::sum);
        }
        return toCounts(counts.entrySet().stream()
                .map(e -> Map.entry(e.getKey().getLabel(), e.getValue())).toList());
    }

    private static List<Count> toCounts(List<Map.Entry<String, Long>> entries) {
        long max = entries.stream().mapToLong(Map.Entry::getValue).max().orElse(1);
        return entries.stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(e -> new Count(e.getKey(), e.getValue(), (int) Math.round(100.0 * e.getValue() / max)))
                .toList();
    }
}
