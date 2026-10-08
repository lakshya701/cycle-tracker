package com.lakshya.cycletracker.prediction;

import java.util.List;

/**
 * A strategy for guessing the length of the next cycle from past cycle lengths.
 * Several implementations are compared on the user's own history and the most
 * accurate one is used (see {@link PredictionService}).
 */
public interface CyclePredictor {

    /** Number of most recent cycles the simple predictors look at. */
    int WINDOW = 6;

    String name();

    String description();

    /**
     * @param pastLengths cycle lengths in days, oldest first; never empty
     * @return predicted length of the next cycle in days
     */
    double predictLength(List<Integer> pastLengths);

    static List<Integer> lastWindow(List<Integer> lengths) {
        return lengths.subList(Math.max(0, lengths.size() - WINDOW), lengths.size());
    }
}
