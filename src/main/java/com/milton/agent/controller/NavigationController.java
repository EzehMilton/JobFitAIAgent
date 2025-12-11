package com.milton.agent.controller;

import com.milton.agent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class NavigationController {

    private final DashboardService dashboardService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var entries = dashboardService.getAllEntries();
        model.addAttribute("entries", entries);
        model.addAttribute("totalAnalyses", dashboardService.getTotalAnalyses(entries));
        model.addAttribute("bestScore", dashboardService.getBestScore(entries));
        model.addAttribute("lastActivity", dashboardService.getLastActivityLabel(entries));
        return "dashboard";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration() {
        // DUMMY implementation — ignore all input
        return "redirect:/";
    }
}
