package com.lakshya.cycletracker.prediction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PredictorTests {

    private final List<CyclePredictor> all = List.of(
            new MeanPredictor(), new MedianPredictor(), new WeightedRecentPredictor(), new ExponentialSmoothingPredictor(0.4));

    @Test
    void singleCycleIsPredictedAsItself() {
        for (CyclePredictor p : all) {
            assertThat(p.predictLength(List.of(30))).as(p.name()).isEqualTo(30.0);
        }
    }

    @Test
    void identicalCyclesArePredictedExactly() {
        for (CyclePredictor p : all) {
            assertThat(p.predictLength(List.of(28, 28, 28, 28, 28))).as(p.name()).isCloseTo(28.0, within(1e-9));
        }
    }

    @Test
    void meanUsesOnlyLastSixCycles() {
        // The 100 is outside the window of six, so it must not count.
        assertThat(new MeanPredictor().predictLength(List.of(100, 28, 28, 28, 28, 28, 28))).isEqualTo(28.0);
    }

    @Test
    void medianIgnoresAnOutlier() {
        assertThat(new MedianPredictor().predictLength(List.of(28, 29, 45, 28, 29))).isEqualTo(29.0);
        assertThat(new MeanPredictor().predictLength(List.of(28, 29, 45, 28, 29))).isGreaterThan(31.0);
    }

    @Test
    void medianOfEvenCountAveragesMiddleTwo() {
        assertThat(new MedianPredictor().predictLength(List.of(26, 28, 30, 32))).isEqualTo(29.0);
    }

    @Test
    void weightedRecentFavoursNewestCycles() {
        // Weights 1,2,3: (1*24 + 2*28 + 3*32) / 6 = 29.33
        assertThat(new WeightedRecentPredictor().predictLength(List.of(24, 28, 32))).isCloseTo(29.333, within(0.001));
    }

    @Test
    void exponentialSmoothingFollowsTheFormula() {
        // 26 -> 0.4*30 + 0.6*26 = 27.6 -> 0.4*30 + 0.6*27.6 = 28.56
        assertThat(new ExponentialSmoothingPredictor(0.4).predictLength(List.of(26, 30, 30))).isCloseTo(28.56, within(1e-9));
    }

    @Test
    void exponentialSmoothingRejectsBadAlpha() {
        assertThatThrownBy(() -> new ExponentialSmoothingPredictor(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExponentialSmoothingPredictor(1.5)).isInstanceOf(IllegalArgumentException.class);
    }
}
