package com.lakshya.cycletracker.demo;

import com.lakshya.cycletracker.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Creates a demo account full of <b>generated</b> sample data when {@code app.demo.enabled=true}.
 * Used for the public demo so visitors can try the app without anyone's real health data.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoData implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@example.com";
    public static final String DEMO_PASSWORD = "demo-password";
    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private final AppUserRepository users;
    private final CycleRepository cycles;
    private final DailyLogRepository logs;
    private final PasswordEncoder passwordEncoder;

    public DemoData(AppUserRepository users, CycleRepository cycles, DailyLogRepository logs, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.cycles = cycles;
        this.logs = logs;
        this.passwordEncoder = passwordEncoder;
    }

    public static boolean isDemoUser(AppUser user) {
        return DEMO_EMAIL.equalsIgnoreCase(user.getEmail());
    }

    /** Rebuilds the demo account on every start, so it always looks fresh and recent. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        users.findByEmailIgnoreCase(DEMO_EMAIL).ifPresent(existing -> {
            logs.deleteByUser(existing);
            cycles.deleteByUser(existing);
            users.delete(existing);
            users.flush();
        });
        AppUser demo = users.save(new AppUser(DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Demo"));
        demo.setRemindersEnabled(false);
        generate(demo, LocalDate.now(), new Random(42));
        log.info("Demo account ready: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    /** Twelve cycles of plausible, randomly varied data ending with a period that started ~12 days ago. */
    void generate(AppUser user, LocalDate today, Random random) {
        int count = 12;
        LocalDate start = today.minusDays(12);
        int[] lengths = new int[count];
        for (int i = 0; i < count; i++) {
            lengths[i] = (int) Math.round(29 + random.nextGaussian() * 1.8);
        }
        // Walk backwards from the most recent period.
        for (int i = 0; i < count; i++) {
            int periodDays = 4 + random.nextInt(3);
            Cycle cycle = new Cycle(user, start);
            cycle.setEndDate(start.plusDays(periodDays - 1));
            cycles.save(cycle);
            logPeriodDays(user, start, periodDays, random, today);
            logDaysBefore(user, start, random, today);
            start = start.minusDays(lengths[i]);
        }
    }

    private void logPeriodDays(AppUser user, LocalDate start, int days, Random random, LocalDate today) {
        List<Flow> flows = List.of(Flow.MEDIUM, Flow.HEAVY, Flow.MEDIUM, Flow.LIGHT, Flow.LIGHT, Flow.SPOTTING);
        for (int d = 0; d < days; d++) {
            LocalDate date = start.plusDays(d);
            if (date.isAfter(today)) return;
            DailyLog entry = new DailyLog(user, date);
            entry.setFlow(flows.get(Math.min(d, flows.size() - 1)));
            EnumSet<Symptom> symptoms = EnumSet.noneOf(Symptom.class);
            if (d < 2 && random.nextDouble() < 0.8) symptoms.add(Symptom.CRAMPS);
            if (d < 3 && random.nextDouble() < 0.5) symptoms.add(Symptom.FATIGUE);
            if (random.nextDouble() < 0.3) symptoms.add(Symptom.BACKACHE);
            if (random.nextDouble() < 0.2) symptoms.add(Symptom.HEADACHE);
            entry.setSymptoms(symptoms);
            entry.setMood(d < 2 ? (random.nextBoolean() ? Mood.TIRED : Mood.IRRITABLE) : Mood.CALM);
            logs.save(entry);
        }
    }

    private void logDaysBefore(AppUser user, LocalDate start, Random random, LocalDate today) {
        for (int d = 1; d <= 3; d++) {
            LocalDate date = start.minusDays(d);
            if (date.isAfter(today) || random.nextDouble() < 0.3) continue;
            DailyLog entry = new DailyLog(user, date);
            EnumSet<Symptom> symptoms = EnumSet.noneOf(Symptom.class);
            if (random.nextDouble() < 0.6) symptoms.add(Symptom.BLOATING);
            if (random.nextDouble() < 0.5) symptoms.add(Symptom.CRAVINGS);
            if (random.nextDouble() < 0.4) symptoms.add(Symptom.TENDER_BREASTS);
            if (random.nextDouble() < 0.3) symptoms.add(Symptom.ACNE);
            entry.setSymptoms(symptoms);
            entry.setMood(random.nextDouble() < 0.5 ? Mood.IRRITABLE : (random.nextBoolean() ? Mood.ANXIOUS : Mood.TIRED));
            logs.save(entry);
        }
    }
}
