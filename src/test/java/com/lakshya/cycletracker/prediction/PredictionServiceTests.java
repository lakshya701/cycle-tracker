package com.lakshya.cycletracker.prediction;

import com.lakshya.cycletracker.domain.Cycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionServiceTests {

    private static final LocalDate FIRST = LocalDate.of(2026, 1, 1);
    private final PredictionService service = new PredictionService();

    /** Builds periods (oldest first) separated by the given cycle lengths, each lasting periodDays. */
    private static List<Cycle> cycles(int periodDays, int... lengths) {
        List<Cycle> result = new ArrayList<>();
        LocalDate start = FIRST;
        result.add(period(start, periodDays));
        for (int length : lengths) {
            start = start.plusDays(length);
            result.add(period(start, periodDays));
        }
        return result;
    }

    private static Cycle period(LocalDate start, int days) {
        Cycle c = new Cycle(null, start);
        c.setEndDate(start.plusDays(days - 1));
        return c;
    }

    @Test
    void noPredictionUntilOneFullCycleIsLogged() {
        assertThat(service.predict(List.of())).isEmpty();
        assertThat(service.predict(cycles(5))).isEmpty();
        assertThat(service.predict(cycles(5, 30))).isPresent();
    }

    @Test
    void regularCyclesArePredictedExactlyWithMinimumRange() {
        List<Cycle> history = cycles(5, 28, 28, 28, 28, 28, 28);
        Prediction p = service.predict(history).orElseThrow();

        LocalDate lastStart = history.getLast().getStartDate();
        assertThat(p.cycleLength()).isEqualTo(28);
        assertThat(p.nextStart()).isEqualTo(lastStart.plusDays(28));
        assertThat(p.earliest()).isEqualTo(p.nextStart().minusDays(2));
        assertThat(p.latest()).isEqualTo(p.nextStart().plusDays(2));
        assertThat(p.periodLength()).isEqualTo(5);
    }

    @Test
    void fertileWindowIsBasedOnOvulationFourteenDaysBeforeNextPeriod() {
        Prediction p = service.predict(cycles(5, 28, 28, 28)).orElseThrow();
        assertThat(p.ovulation()).isEqualTo(p.nextStart().minusDays(14));
        assertThat(p.fertileStart()).isEqualTo(p.ovulation().minusDays(5));
        assertThat(p.fertileEnd()).isEqualTo(p.ovulation().plusDays(1));
    }

    @Test
    void medianWinsWhenHistoryHasOneOutlier() {
        Prediction p = service.predict(cycles(5, 28, 28, 28, 45, 28, 28, 28)).orElseThrow();
        assertThat(p.method()).isEqualTo("Median");
        assertThat(p.cycleLength()).isEqualTo(28);
    }

    @Test
    void trendingCyclesPickAMethodThatFavoursRecentData() {
        Prediction p = service.predict(cycles(5, 26, 27, 28, 29, 30, 31, 32)).orElseThrow();
        assertThat(p.method()).isIn("Weighted recent", "Exponential smoothing");
    }

    @Test
    void evaluationsAreSortedBestFirstWithExactlyOneChosen() {
        List<Prediction.MethodScore> scores = service.evaluate(List.of(27, 30, 26, 31, 28, 29));
        assertThat(scores).hasSize(4);
        assertThat(scores).filteredOn(Prediction.MethodScore::chosen).hasSize(1);
        assertThat(scores.getFirst().chosen()).isTrue();
        for (int i = 1; i < scores.size(); i++) {
            assertThat(scores.get(i).meanAbsoluteError()).isGreaterThanOrEqualTo(scores.get(i - 1).meanAbsoluteError());
        }
        assertThat(scores.getFirst().cyclesTested()).isEqualTo(5);
    }

    @Test
    void tooLittleHistoryFallsBackToAverageWithoutComparison() {
        Prediction p = service.predict(cycles(5, 30, 26)).orElseThrow();
        assertThat(p.evaluations()).isEmpty();
        assertThat(p.method()).isEqualTo("Average");
        assertThat(p.cycleLength()).isEqualTo(28);
    }

    @Test
    void implausiblyLongGapsAreTreatedAsMissedLogs() {
        assertThat(CycleHistory.cycleLengths(cycles(5, 28, 90, 28))).containsExactly(28, 28);
    }

    @Test
    void healthNotesFlagRepeatedShortCyclesOnly() {
        assertThat(service.healthNotes(cycles(5, 28, 29, 28, 27))).isEmpty();
        assertThat(service.healthNotes(cycles(5, 19, 28, 20)))
                .extracting(HealthNote::title).containsExactly("Short cycles");
    }

    @Test
    void healthNotesFlagIrregularAndLongPeriods() {
        assertThat(service.healthNotes(cycles(5, 24, 36, 25, 30)))
                .extracting(HealthNote::title).contains("Irregular timing");
        assertThat(service.healthNotes(cycles(9, 28, 28)))
                .extracting(HealthNote::title).contains("Long periods");
    }
}
