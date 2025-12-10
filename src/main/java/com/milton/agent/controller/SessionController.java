package com.milton.agent.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
public class SessionController {

    @PostMapping("/clear-cv")
    public String clearStoredCv(HttpSession session) {
        session.removeAttribute(SessionAttributes.CV_TEXT);
        session.removeAttribute(SessionAttributes.CV_NAME);
        session.removeAttribute(SessionAttributes.JOB_DESCRIPTION);
        session.removeAttribute(SessionAttributes.FIT_SCORE);
        session.removeAttribute(SessionAttributes.FIT_EXPLANATION);
        session.removeAttribute(SessionAttributes.UPGRADED_CV);
        session.removeAttribute(SessionAttributes.UPGRADED_KEYWORDS);
        session.removeAttribute(SessionAttributes.UPGRADED_SUMMARY);
        log.info("Cleared stored CV from session");
        return "redirect:/";
    }
}
