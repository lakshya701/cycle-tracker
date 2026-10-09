package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.demo.DemoData;
import com.lakshya.cycletracker.domain.AppUser;
import com.lakshya.cycletracker.domain.AppUserRepository;
import com.lakshya.cycletracker.security.CurrentUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AppUserRepository users;
    private final AccountService accountService;
    private final CurrentUser currentUser;

    public AccountController(AppUserRepository users, AccountService accountService, CurrentUser currentUser) {
        this.users = users;
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String account(Principal principal, Model model) {
        AppUser user = currentUser.get(principal);
        model.addAttribute("user", user);
        model.addAttribute("isDemo", DemoData.isDemoUser(user));
        return "account";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam String displayName,
                               @RequestParam(defaultValue = "false") boolean remindersEnabled,
                               Principal principal, RedirectAttributes flash) {
        AppUser user = currentUser.get(principal);
        if (DemoData.isDemoUser(user)) {
            flash.addFlashAttribute("error", "Settings can't be changed on the demo account.");
            return "redirect:/account";
        }
        String name = displayName.trim();
        if (name.isEmpty() || name.length() > 60) {
            flash.addFlashAttribute("error", "Name must be 1 to 60 characters.");
            return "redirect:/account";
        }
        user.setDisplayName(name);
        user.setRemindersEnabled(remindersEnabled);
        users.save(user);
        flash.addFlashAttribute("message", "Settings saved.");
        return "redirect:/account";
    }

    @GetMapping("/export/cycles.csv")
    public ResponseEntity<byte[]> exportCycles(Principal principal) {
        return csv("cycle-tracker-periods", accountService.cyclesCsv(currentUser.get(principal)));
    }

    @GetMapping("/export/logs.csv")
    public ResponseEntity<byte[]> exportLogs(Principal principal) {
        return csv("cycle-tracker-daily-logs", accountService.logsCsv(currentUser.get(principal)));
    }

    @PostMapping("/delete")
    public String deleteAccount(@RequestParam(defaultValue = "") String confirm, Principal principal,
                                HttpServletRequest request, RedirectAttributes flash) throws ServletException {
        AppUser user = currentUser.get(principal);
        if (DemoData.isDemoUser(user)) {
            flash.addFlashAttribute("error", "The demo account can't be deleted.");
            return "redirect:/account";
        }
        if (!"DELETE".equals(confirm.trim())) {
            flash.addFlashAttribute("error", "Type DELETE in capitals to confirm.");
            return "redirect:/account";
        }
        accountService.deleteEverything(user);
        request.logout();
        return "redirect:/login?deleted";
    }

    private static ResponseEntity<byte[]> csv(String name, String body) {
        String filename = name + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body.getBytes(StandardCharsets.UTF_8));
    }
}
