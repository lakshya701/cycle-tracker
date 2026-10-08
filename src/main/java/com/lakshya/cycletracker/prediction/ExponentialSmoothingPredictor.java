package com.lakshya.cycletracker.prediction;

import java.util.List;

/**
 * Exponential smoothing over the whole history: estimate = alpha * latest + (1 - alpha) * previous estimate.
 * Uses every cycle, with influence fading the older a cycle is.
 */
public class ExponentialSmoothingPredictor implements CyclePredictor {

    private final double alpha;

    public ExponentialSmoothingPredictor(double alpha) {
        if (alpha <= 0 || alpha > 1) throw new IllegalArgumentException("alpha must be in (0, 1]");
        this.alpha = alpha;
    }

    @Override
    public String name() { return "Exponential smoothing"; }

    @Override
    public String description() { return "Uses all history; older cycles fade out (alpha = " + alpha + ")"; }

    @Override
    public double predictLength(List<Integer> pastLengths) {
        double estimate = pastLengths.getFirst();
        for (int i = 1; i < pastLengths.size(); i++) {
            estimate = alpha * pastLengths.get(i) + (1 - alpha) * estimate;
        }
        return estimate;
    }
}
