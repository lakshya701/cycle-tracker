package com.lakshya.cycletracker.prediction;

import java.util.List;

/** Average of the last 6 cycles — the method most period apps start with. */
public class MeanPredictor implements CyclePredictor {

    @Override
    public String name() { return "Average"; }

    @Override
    public String description() { return "Mean length of the last " + WINDOW + " cycles"; }

    @Override
    public double predictLength(List<Integer> pastLengths) {
        return CyclePredictor.lastWindow(pastLengths).stream().mapToInt(Integer::intValue).average().orElseThrow();
    }
}
