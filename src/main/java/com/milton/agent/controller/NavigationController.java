package com.milton.agent.controller;

import com.milton.agent.service.DashboardService;
import jakarta.servlet.http.HttpSession;
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
    public String dashboard(Model model, HttpSession session) {
        Long userId = getUserId(session);
        var entries = dashboardService.getAllEntries(userId);
        model.addAttribute("entries", entries);
        model.addAttribute("totalAnalyses", dashboardService.getTotalAnalyses(entries));
        model.addAttribute("bestScore", dashboardService.getBestScoreLabel(entries));
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

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Clear all session data to simulate fresh login
        session.invalidate();
        return "redirect:/";
    }

    /**
     * Converts session ID to a consistent Long userId.
     * Each unique session gets a unique userId based on session ID hash.
     */
    private Long getUserId(HttpSession session) {
        return (long) Math.abs(session.getId().hashCode());
    }
}
