package com.lakshya.cycletracker.prediction;

import com.lakshya.cycletracker.domain.Cycle;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Turns logged periods into the numbers the predictors work with. */
public final class CycleHistory {

    /** Gaps longer than this are most likely missed logs, not real cycles, so they're skipped. */
    static final int MAX_PLAUSIBLE_CYCLE = 60;

    private CycleHistory() {}

    /** Cycle lengths in days (start to next start), oldest first. */
    public static List<Integer> cycleLengths(List<Cycle> oldestFirst) {
        List<Integer> lengths = new ArrayList<>();
        for (int i = 1; i < oldestFirst.size(); i++) {
            int days = (int) ChronoUnit.DAYS.between(oldestFirst.get(i - 1).getStartDate(), oldestFirst.get(i).getStartDate());
            if (days > 0 && days <= MAX_PLAUSIBLE_CYCLE) {
                lengths.add(days);
            }
        }
        return lengths;
    }

    /** Lengths of finished periods in days, oldest first. */
    public static List<Integer> periodLengths(List<Cycle> oldestFirst) {
        return oldestFirst.stream().map(Cycle::getPeriodLength).filter(Objects::nonNull).toList();
    }

    public static double mean(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).average().orElse(Double.NaN);
    }

    public static double standardDeviation(List<Integer> values) {
        if (values.size() < 2) return 0;
        double mean = mean(values);
        double sumSquares = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum();
        return Math.sqrt(sumSquares / (values.size() - 1));
    }
}
