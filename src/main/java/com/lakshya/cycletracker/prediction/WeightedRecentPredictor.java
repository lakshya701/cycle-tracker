package com.lakshya.cycletracker.prediction;

import java.util.List;

/** Weighted average of the last 6 cycles: the newest counts 6x, the oldest 1x. Adapts to gradual change. */
public class WeightedRecentPredictor implements CyclePredictor {

    @Override
    public String name() { return "Weighted recent"; }

    @Override
    public String description() { return "Recent cycles count more than older ones"; }

    @Override
    public double predictLength(List<Integer> pastLengths) {
        List<Integer> window = CyclePredictor.lastWindow(pastLengths);
        double weightedSum = 0;
        int totalWeight = 0;
        for (int i = 0; i < window.size(); i++) {
            int weight = i + 1;
            weightedSum += weight * window.get(i);
            totalWeight += weight;
        }
        return weightedSum / totalWeight;
    }
}
