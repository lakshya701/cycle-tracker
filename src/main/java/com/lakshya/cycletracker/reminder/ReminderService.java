package com.lakshya.cycletracker.reminder;

import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.AppUserRepository;
import com.lakshya.cycletracker.domain.CycleRepository;
import com.lakshya.cycletracker.prediction.Prediction;
import com.lakshya.cycletracker.prediction.PredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Once a day, emails users whose next period is predicted to start in {@value #DAYS_AHEAD} days.
 * Email is only sent when SMTP is configured (spring.mail.host); otherwise reminders are just logged.
 */
@Service
public class ReminderService {

    static final int DAYS_AHEAD = 2;
    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM");

    private final AppUserRepository users;
    private final CycleRepository cycles;
    private final PredictionService predictions;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String appUrl;

    public ReminderService(AppUserRepository users, CycleRepository cycles, PredictionService predictions,
                           ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.from:cycle-tracker@localhost}") String from,
                           @Value("${app.url:http://localhost:8080}") String appUrl) {
        this.users = users;
        this.cycles = cycles;
        this.predictions = predictions;
        this.mailSender = mailSender;
        this.from = from;
        this.appUrl = appUrl;
    }

    /** A reminder that is due: who it's for and the predicted start. */
    public record Reminder(AppUser user, LocalDate predictedStart) {}

    /** Runs every day at 08:00 server time. */
    @Scheduled(cron = "${app.reminders.cron:0 0 8 * * *}")
    public void sendDailyReminders() {
        for (Reminder reminder : dueReminders(LocalDate.now())) {
            send(reminder);
        }
    }

    @Transactional(readOnly = true)
    public List<Reminder> dueReminders(LocalDate today) {
        List<Reminder> due = new ArrayList<>();
        for (AppUser user : users.findByRemindersEnabledTrue()) {
            Optional<Prediction> prediction = predictions.predict(cycles.findByUserOrderByStartDateAsc(user));
            prediction.filter(p -> p.nextStart().equals(today.plusDays(DAYS_AHEAD)))
                    .ifPresent(p -> due.add(new Reminder(user, p.nextStart())));
        }
        return due;
    }

    private void send(Reminder reminder) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("Reminder due for user {} (no mail server configured, not sent)", reminder.user().getId());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(reminder.user().getEmail());
        message.setSubject("Heads-up: your period may start in " + DAYS_AHEAD + " days");
        message.setText("Hi " + reminder.user().getDisplayName() + ",\n\n"
                + "Based on your recent cycles, your next period is predicted to start around "
                + reminder.predictedStart().format(DATE) + ".\n\n"
                + "This is an estimate, not medical advice.\n\n"
                + "Open Cycle Tracker: " + appUrl + "\n"
                + "Turn off reminders: " + appUrl + "/account\n");
        try {
            sender.send(message);
        } catch (RuntimeException e) {
            log.warn("Could not send reminder to user {}: {}", reminder.user().getId(), e.getMessage());
        }
    }
}
