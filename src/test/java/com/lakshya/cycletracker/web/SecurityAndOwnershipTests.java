package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndOwnershipTests {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired CycleRepository cycles;
    @Autowired DailyLogRepository logs;

    AppUser alice;
    AppUser bob;
    Cycle alicesCycle;

    @BeforeEach
    void setUp() {
        logs.deleteAll();
        cycles.deleteAll();
        users.deleteAll();
        alice = users.save(new AppUser("alice@example.com", "x", "Alice"));
        bob = users.save(new AppUser("bob@example.com", "x", "Bob"));
        alicesCycle = cycles.save(new Cycle(alice, LocalDate.now().minusDays(10)));
    }

    @Test
    void pagesRequireLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/cycles")).andExpect(status().is3xxRedirection());
    }

    @Test
    void ownerCanEditTheirCycle() throws Exception {
        mvc.perform(get("/cycles/{id}/edit", alicesCycle.getId()).with(user("alice@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    void otherUserCannotSeeOrChangeSomeoneElsesCycle() throws Exception {
        mvc.perform(get("/cycles/{id}/edit", alicesCycle.getId()).with(user("bob@example.com")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/cycles/{id}/delete", alicesCycle.getId()).with(user("bob@example.com")).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(cycles.findById(alicesCycle.getId())).isPresent();
    }

    @Test
    void cyclesListOnlyShowsYourOwnPeriods() throws Exception {
        mvc.perform(get("/cycles").with(user("bob@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No periods logged yet")));
    }

    @Test
    void signupCreatesAccountWithHashedPassword() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                        .param("displayName", "Carol")
                        .param("email", "Carol@Example.com")
                        .param("password", "a-long-password"))
                .andExpect(redirectedUrl("/login?registered"));
        AppUser carol = users.findByEmailIgnoreCase("carol@example.com").orElseThrow();
        assertThat(carol.getPasswordHash()).isNotEqualTo("a-long-password").startsWith("$2");
    }

    @Test
    void overlappingPeriodIsRejected() throws Exception {
        mvc.perform(post("/cycles").with(user("alice@example.com")).with(csrf())
                        .param("startDate", alicesCycle.getStartDate().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("overlaps")));
    }
}
