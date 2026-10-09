package com.lakshya.cycletracker.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every page needs, e.g. whether this is the public demo (shows a warning banner). */
@ControllerAdvice
public class GlobalModel {

    private final boolean demoMode;

    public GlobalModel(@Value("${app.demo.enabled:false}") boolean demoMode) {
        this.demoMode = demoMode;
    }

    @ModelAttribute("demoMode")
    public boolean demoMode() {
        return demoMode;
    }
}
