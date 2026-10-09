package com.lakshya.cycletracker.demo;

import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.AppUserRepository;
import com.lakshya.cycletracker.domain.CycleRepository;
import com.lakshya.cycletracker.prediction.PredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.demo.enabled=true")
@AutoConfigureMockMvc
class DemoModeTests {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired CycleRepository cycles;
    @Autowired PredictionService predictions;

    @Test
    void demoAccountIsSeededWithEnoughHistoryForPredictions() {
        AppUser demo = users.findByEmailIgnoreCase(DemoData.DEMO_EMAIL).orElseThrow();
        var history = cycles.findByUserOrderByStartDateAsc(demo);
        assertThat(history).hasSize(12);
        assertThat(predictions.predict(history)).hasValueSatisfying(p -> assertThat(p.evaluations()).hasSize(4));
    }

    @Test
    void demoShowsBannerAndBlocksSignupAndDeletion() throws Exception {
        mvc.perform(get("/").with(user(DemoData.DEMO_EMAIL)))
                .andExpect(content().string(containsString("Public demo")));
        mvc.perform(get("/signup")).andExpect(content().string(containsString("Sign-up is turned off")));
        mvc.perform(post("/signup").with(csrf())
                        .param("displayName", "Real Person").param("email", "real@example.com").param("password", "a-long-password"))
                .andExpect(redirectedUrl("/signup"));
        assertThat(users.existsByEmailIgnoreCase("real@example.com")).isFalse();

        mvc.perform(post("/account/delete").with(user(DemoData.DEMO_EMAIL)).with(csrf()).param("confirm", "DELETE"))
                .andExpect(redirectedUrl("/account"));
        assertThat(users.existsByEmailIgnoreCase(DemoData.DEMO_EMAIL)).isTrue();
    }
}
