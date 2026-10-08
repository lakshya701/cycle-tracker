package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.EnumSet;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Renders every page for a user with real-looking history, so template mistakes fail the build. */
@SpringBootTest
@AutoConfigureMockMvc
class PagesRenderTests {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired CycleRepository cycles;
    @Autowired DailyLogRepository logs;

    @BeforeEach
    void setUp() {
        logs.deleteAll();
        cycles.deleteAll();
        users.deleteAll();
        AppUser dana = users.save(new AppUser("dana@example.com", "x", "Dana"));
        users.save(new AppUser("new@example.com", "x", "Newbie"));
        LocalDate start = LocalDate.now().minusDays(20);
        int[] lengths = {29, 27, 30, 28, 31};
        for (int i = lengths.length - 1; i >= -1; i--) {
            Cycle c = new Cycle(dana, start);
            if (i >= 0 || start.plusDays(4).isBefore(LocalDate.now())) c.setEndDate(start.plusDays(4));
            cycles.save(c);
            if (i >= 0) start = start.minusDays(lengths[i]);
        }
        DailyLog log = new DailyLog(dana, LocalDate.now().minusDays(1));
        log.setFlow(Flow.LIGHT);
        log.setMood(Mood.TIRED);
        log.setSymptoms(EnumSet.of(Symptom.CRAMPS, Symptom.FATIGUE));
        logs.save(log);
    }

    @Test
    void everyPageRendersForAUserWithHistory() throws Exception {
        for (String page : new String[]{"/", "/calendar", "/calendar?month=2026-01", "/cycles", "/cycles/new",
                "/logs", "/logs/day", "/accuracy"}) {
            mvc.perform(get(page).with(user("dana@example.com"))).andExpect(status().isOk());
        }
        mvc.perform(get("/").with(user("dana@example.com"))).andExpect(content().string(containsString("Next period")));
        mvc.perform(get("/accuracy").with(user("dana@example.com"))).andExpect(content().string(containsString("in use")));
    }

    @Test
    void pagesRenderForABrandNewUser() throws Exception {
        for (String page : new String[]{"/", "/calendar", "/cycles", "/logs", "/accuracy"}) {
            mvc.perform(get(page).with(user("new@example.com"))).andExpect(status().isOk());
        }
        mvc.perform(get("/").with(user("new@example.com"))).andExpect(content().string(containsString("Welcome")));
    }

    @Test
    void loginAndSignupPagesArePublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/signup")).andExpect(status().isOk());
    }
}
