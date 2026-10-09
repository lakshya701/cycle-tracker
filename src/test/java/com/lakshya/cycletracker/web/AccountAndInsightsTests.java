package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.*;
import com.lakshya.cycletracker.reminder.ReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountAndInsightsTests {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired CycleRepository cycles;
    @Autowired DailyLogRepository logs;
    @Autowired ReminderService reminders;

    AppUser erin;
    LocalDate lastStart;

    @BeforeEach
    void setUp() {
        logs.deleteAll();
        cycles.deleteAll();
        users.deleteAll();
        erin = users.save(new AppUser("erin@example.com", "x", "Erin"));
        users.save(new AppUser("frank@example.com", "x", "Frank"));
        // Regular 28-day cycles; the last one started 26 days ago, so the next is due in 2 days.
        lastStart = LocalDate.now().minusDays(26);
        for (int i = 4; i >= 0; i--) {
            Cycle c = new Cycle(erin, lastStart.minusDays(28L * i));
            c.setEndDate(c.getStartDate().plusDays(4));
            c.setNotes(i == 0 ? "felt \"fine\", mostly" : null);
            cycles.save(c);
        }
        DailyLog beforePeriod = new DailyLog(erin, lastStart.minusDays(1));
        beforePeriod.setSymptoms(EnumSet.of(Symptom.BLOATING, Symptom.CRAVINGS));
        beforePeriod.setMood(Mood.IRRITABLE);
        logs.save(beforePeriod);
        DailyLog duringPeriod = new DailyLog(erin, lastStart);
        duringPeriod.setFlow(Flow.HEAVY);
        duringPeriod.setSymptoms(EnumSet.of(Symptom.CRAMPS));
        logs.save(duringPeriod);
    }

    @Test
    void insightsAndAccountPagesRender() throws Exception {
        mvc.perform(get("/insights").with(user("erin@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Average cycle")))
                .andExpect(content().string(containsString("Bloating")));
        mvc.perform(get("/insights").with(user("frank@example.com"))).andExpect(status().isOk());
        mvc.perform(get("/account").with(user("erin@example.com"))).andExpect(status().isOk());
    }

    @Test
    void symptomsAreGroupedByCyclePhase() {
        var history = cycles.findByUserOrderByStartDateAsc(erin);
        var all = logs.findByUserOrderByDateDesc(erin);
        assertThat(InsightsController.logsBeforePeriods(all, history)).extracting(DailyLog::getDate)
                .containsExactly(lastStart.minusDays(1));
        assertThat(InsightsController.logsDuringPeriods(all, history)).extracting(DailyLog::getDate)
                .containsExactly(lastStart);
    }

    @Test
    void exportsAreCsvWithEscapedValues() throws Exception {
        mvc.perform(get("/account/export/cycles.csv").with(user("erin@example.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("start_date,end_date")))
                .andExpect(content().string(containsString("\"felt \"\"fine\"\", mostly\"")));
        mvc.perform(get("/account/export/logs.csv").with(user("erin@example.com")))
                .andExpect(content().string(containsString("BLOATING;CRAVINGS")));
        // Another user's export contains only the header.
        mvc.perform(get("/account/export/cycles.csv").with(user("frank@example.com")))
                .andExpect(content().string("start_date,end_date,period_length_days,notes\n"));
    }

    @Test
    void deleteRequiresTypedConfirmationThenRemovesEverything() throws Exception {
        mvc.perform(post("/account/delete").with(user("erin@example.com")).with(csrf()).param("confirm", "yes"))
                .andExpect(redirectedUrl("/account"));
        assertThat(users.findByEmailIgnoreCase("erin@example.com")).isPresent();

        mvc.perform(post("/account/delete").with(user("erin@example.com")).with(csrf()).param("confirm", "DELETE"))
                .andExpect(redirectedUrl("/login?deleted"));
        assertThat(users.findByEmailIgnoreCase("erin@example.com")).isEmpty();
        assertThat(cycles.count()).isZero();
        assertThat(logs.count()).isZero();
        assertThat(users.findByEmailIgnoreCase("frank@example.com")).isPresent();
    }

    @Test
    void settingsCanTurnRemindersOff() throws Exception {
        mvc.perform(post("/account/settings").with(user("erin@example.com")).with(csrf())
                        .param("displayName", "Erin B"))
                .andExpect(redirectedUrl("/account"));
        AppUser updated = users.findByEmailIgnoreCase("erin@example.com").orElseThrow();
        assertThat(updated.getDisplayName()).isEqualTo("Erin B");
        assertThat(updated.isRemindersEnabled()).isFalse();
    }

    @Test
    void reminderIsDueTwoDaysBeforePredictedStart() {
        LocalDate today = LocalDate.now();
        assertThat(reminders.dueReminders(today)).extracting(r -> r.user().getEmail()).containsExactly("erin@example.com");
        assertThat(reminders.dueReminders(today.plusDays(1))).isEmpty();

        erin.setRemindersEnabled(false);
        users.save(erin);
        assertThat(reminders.dueReminders(today)).isEmpty();
    }

    @Test
    void csvEscaping() {
        assertThat(AccountService.escape(null)).isEmpty();
        assertThat(AccountService.escape("plain")).isEqualTo("plain");
        assertThat(AccountService.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(AccountService.escape("line\nbreak")).isEqualTo("\"line\nbreak\"");
    }
}
