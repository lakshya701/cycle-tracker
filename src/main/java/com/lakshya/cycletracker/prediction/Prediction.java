package com.lakshya.cycletracker.prediction;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * The forecast for the next period plus everything needed to explain it.
 *
 * @param nextStart    most likely first day of the next period
 * @param earliest     start of the likely range
 * @param latest       end of the likely range
 * @param cycleLength  predicted cycle length in days
 * @param periodLength typical period length in days (used to draw predicted days)
 * @param method       name of the predictor that was chosen
 * @param cyclesUsed   how many past cycle lengths the forecast is based on
 * @param ovulation    estimated ovulation day (about 14 days before the next period)
 * @param fertileStart first day of the estimated fertile window
 * @param fertileEnd   last day of the estimated fertile window
 * @param evaluations  how each method scored on this user's history (empty if not enough data)
 */
public record Prediction(
        LocalDate nextStart,
        LocalDate earliest,
        LocalDate latest,
        int cycleLength,
        int periodLength,
        String method,
        int cyclesUsed,
        LocalDate ovulation,
        LocalDate fertileStart,
        LocalDate fertileEnd,
        List<MethodScore> evaluations) {

    /** How a method did when backtested: mean absolute error in days over the cycles it predicted. */
    public record MethodScore(String name, String description, double meanAbsoluteError, int cyclesTested, boolean chosen) {}

    public long daysUntil(LocalDate today) {
        return ChronoUnit.DAYS.between(today, nextStart);
    }

    /** Days past the end of the likely range, or 0 if not late. */
    public long daysLate(LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(latest, today));
    }

    /** Predicted start of the period {@code k} cycles after the next one (k = 0 is the next one). */
    public LocalDate startAfter(int k) {
        return nextStart.plusDays((long) k * cycleLength);
    }
}
