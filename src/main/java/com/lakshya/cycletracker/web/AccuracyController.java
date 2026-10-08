package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.CycleRepository;
import com.lakshya.cycletracker.prediction.PredictionService;
import com.lakshya.cycletracker.security.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/** Shows how each prediction method scored on the user's own history. */
@Controller
public class AccuracyController {

    private final CycleRepository cycles;
    private final PredictionService predictions;
    private final CurrentUser currentUser;

    public AccuracyController(CycleRepository cycles, PredictionService predictions, CurrentUser currentUser) {
        this.cycles = cycles;
        this.predictions = predictions;
        this.currentUser = currentUser;
    }

    @GetMapping("/accuracy")
    public String accuracy(Principal principal, Model model) {
        var history = cycles.findByUserOrderByStartDateAsc(currentUser.get(principal));
        model.addAttribute("prediction", predictions.predict(history).orElse(null));
        return "accuracy";
    }
}
