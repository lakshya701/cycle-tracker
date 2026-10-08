package com.lakshya.cycletracker.prediction;

import java.util.List;

/** Middle value of the last 6 cycles — not thrown off by one unusually long or short cycle. */
public class MedianPredictor implements CyclePredictor {

    @Override
    public String name() { return "Median"; }

    @Override
    public String description() { return "Middle value of the last " + WINDOW + " cycles (ignores one-off outliers)"; }

    @Override
    public double predictLength(List<Integer> pastLengths) {
        List<Integer> sorted = CyclePredictor.lastWindow(pastLengths).stream().sorted().toList();
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
