package com.lakshya.cycletracker.prediction;

import com.lakshya.cycletracker.domain.Cycle;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Predicts the next period.
 *
 * <p>Each {@link CyclePredictor} is <b>backtested</b> on the user's own history: for every past cycle,
 * it predicts that cycle's length using only the cycles before it, and we record how many days off it was.
 * The method with the lowest mean absolute error (MAE) is used for the real prediction.
 */
@Service
public class PredictionService {

    /** Below this many cycle lengths there isn't enough history to compare methods fairly. */
    static final int MIN_LENGTHS_TO_COMPARE = 3;
    static final int DEFAULT_PERIOD_LENGTH = 5;
    /** The luteal phase (ovulation to next period) is fairly constant at about 14 days. */
    static final int LUTEAL_PHASE_DAYS = 14;

    private final List<CyclePredictor> predictors = List.of(
            new MeanPredictor(),
            new MedianPredictor(),
            new WeightedRecentPredictor(),
            new ExponentialSmoothingPredictor(0.4));

    /** @return empty until at least two periods are logged (one full cycle). */
    public Optional<Prediction> predict(List<Cycle> cyclesOldestFirst) {
        List<Integer> lengths = CycleHistory.cycleLengths(cyclesOldestFirst);
        if (lengths.isEmpty()) {
            return Optional.empty();
        }

        List<Prediction.MethodScore> scores = evaluate(lengths);
        String bestName = scores.isEmpty() ? predictors.getFirst().name() : scores.getFirst().name();
        CyclePredictor best = predictors.stream().filter(p -> p.name().equals(bestName)).findFirst().orElseThrow();

        int cycleLength = (int) Math.round(best.predictLength(lengths));
        LocalDate lastStart = cyclesOldestFirst.getLast().getStartDate();
        LocalDate nextStart = lastStart.plusDays(cycleLength);

        int margin = rangeMargin(lengths);
        List<Integer> periodLengths = CycleHistory.periodLengths(cyclesOldestFirst);
        int periodLength = periodLengths.isEmpty() ? DEFAULT_PERIOD_LENGTH
                : (int) Math.round(CycleHistory.mean(CyclePredictor.lastWindow(periodLengths)));

        LocalDate ovulation = nextStart.minusDays(LUTEAL_PHASE_DAYS);
        return Optional.of(new Prediction(
                nextStart, nextStart.minusDays(margin), nextStart.plusDays(margin),
                cycleLength, periodLength, best.name(), lengths.size(),
                ovulation, ovulation.minusDays(5), ovulation.plusDays(1),
                scores));
    }

    /**
     * Backtests every predictor. For cycle i (i >= 1) each method sees only cycles 0..i-1.
     * Returns scores sorted best-first, with the winner marked as chosen; empty if there's too little data.
     */
    public List<Prediction.MethodScore> evaluate(List<Integer> lengths) {
        if (lengths.size() < MIN_LENGTHS_TO_COMPARE) {
            return List.of();
        }
        record Result(CyclePredictor predictor, double mae, int tested) {}
        List<Result> results = new ArrayList<>();
        for (CyclePredictor predictor : predictors) {
            double totalError = 0;
            int tested = 0;
            for (int i = 1; i < lengths.size(); i++) {
                double guess = predictor.predictLength(lengths.subList(0, i));
                totalError += Math.abs(Math.round(guess) - lengths.get(i));
                tested++;
            }
            results.add(new Result(predictor, totalError / tested, tested));
        }
        // Stable sort: on a tie the simpler method listed first (Average) wins.
        results.sort(Comparator.comparingDouble(Result::mae));
        List<Prediction.MethodScore> scores = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Result r = results.get(i);
            scores.add(new Prediction.MethodScore(r.predictor().name(), r.predictor().description(),
                    r.mae(), r.tested(), i == 0));
        }
        return scores;
    }

    /** Half-width of the "likely" range: the recent variation in cycle length, between 2 and 7 days. */
    static int rangeMargin(List<Integer> lengths) {
        long sd = Math.round(CycleHistory.standardDeviation(CyclePredictor.lastWindow(lengths)));
        return (int) Math.min(7, Math.max(2, sd));
    }

    /** Gentle notes when patterns are outside typical ranges. Never a diagnosis. */
    public List<HealthNote> healthNotes(List<Cycle> cyclesOldestFirst) {
        List<HealthNote> notes = new ArrayList<>();
        List<Integer> recent = CyclePredictor.lastWindow(CycleHistory.cycleLengths(cyclesOldestFirst));
        long shortCycles = recent.stream().filter(l -> l < 21).count();
        long longCycles = recent.stream().filter(l -> l > 35).count();
        if (shortCycles >= 2) {
            notes.add(new HealthNote("Short cycles",
                    shortCycles + " of your recent cycles were shorter than 21 days. If this continues, it may be worth mentioning to a doctor."));
        }
        if (longCycles >= 2) {
            notes.add(new HealthNote("Long cycles",
                    longCycles + " of your recent cycles were longer than 35 days. If this continues, it may be worth mentioning to a doctor."));
        }
        if (recent.size() >= 4) {
            int spread = recent.stream().max(Integer::compare).orElseThrow() - recent.stream().min(Integer::compare).orElseThrow();
            if (spread > 9) {
                notes.add(new HealthNote("Irregular timing",
                        "Your recent cycles varied by " + spread + " days, so predictions are less certain. Variation is common, especially with stress, travel or illness."));
            }
        }
        List<Integer> recentPeriods = CyclePredictor.lastWindow(CycleHistory.periodLengths(cyclesOldestFirst));
        long longPeriods = recentPeriods.stream().filter(l -> l > 7).count();
        if (longPeriods >= 2) {
            notes.add(new HealthNote("Long periods",
                    longPeriods + " of your recent periods lasted more than 7 days. It may be worth mentioning to a doctor."));
        }
        return notes;
    }
}
