package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final boolean demoEnabled;

    public AuthController(AppUserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${app.demo.enabled:false}") boolean demoEnabled) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.demoEnabled = demoEnabled;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("demoEnabled", demoEnabled);
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("form", new SignupForm());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") SignupForm form, BindingResult errors) {
        // The public demo must never collect real people's health data.
        if (demoEnabled) {
            return "redirect:/signup";
        }
        if (!errors.hasFieldErrors("email") && users.existsByEmailIgnoreCase(form.getEmail().trim())) {
            errors.rejectValue("email", "taken", "An account with this email already exists.");
        }
        if (errors.hasErrors()) {
            return "signup";
        }
        users.save(new AppUser(form.getEmail(), passwordEncoder.encode(form.getPassword()), form.getDisplayName()));
        return "redirect:/login?registered";
    }
}
