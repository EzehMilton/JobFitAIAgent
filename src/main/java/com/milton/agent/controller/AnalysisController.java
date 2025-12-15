package com.milton.agent.controller;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.service.MatchPresentationService;
import com.milton.agent.service.RateLimitService;
import com.milton.agent.service.TextExtractor;
import com.milton.agent.util.FileValidationUtil;
import com.milton.agent.util.IpAddressUtil;
import com.milton.agent.util.TimedOperation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AnalysisController {

    private final AgentPlatform agentPlatform;
    private final TextExtractor textExtractor;
    private final RateLimitService rateLimitService;
    private final MatchPresentationService matchPresentationService;

    @GetMapping({"/"})
    public String index(HttpSession session, HttpServletRequest request, Model model) {
        String storedCvName = (String) session.getAttribute(SessionAttributes.CV_NAME);
        if (storedCvName != null) {
            model.addAttribute("storedCvName", storedCvName);
        }

        String ipAddress = IpAddressUtil.getClientIpAddress(request);
        int remainingRequests = rateLimitService.getRemainingRequests(ipAddress);
        int usedRequests = rateLimitService.getRequestCount(ipAddress);
        int maxDailyScans = rateLimitService.getMaxRequestsPerDay();

        model.addAttribute("remainingRequests", remainingRequests);
        model.addAttribute("usedRequests", usedRequests);
        model.addAttribute("maxDailyScans", maxDailyScans);

        log.debug("IP {} - Used: {}/{}, Remaining: {}", ipAddress, usedRequests, maxDailyScans, remainingRequests);
        return "index";
    }

    @PostMapping("/generate")
    public String generateScore(@RequestParam(value = "candidateFile", required = false) MultipartFile cv,
                                @RequestParam(value = "reuseCv", required = false) Boolean reuseCv,
                                @RequestParam("role") String role,
                                @RequestParam("company") String company,
                                @RequestParam("jobDescription") String jobDescription,
                                @RequestParam(value = "analysisMode", required = false) String analysisMode,
                                HttpServletRequest request,
                                HttpSession session,
                                Model model) throws IOException {

        String ipAddress = IpAddressUtil.getClientIpAddress(request);
        log.info("Request from IP: {}", ipAddress);
        model.addAttribute("maxDailyScans", rateLimitService.getMaxRequestsPerDay());

        if (!rateLimitService.isAllowed(ipAddress)) {
            int usedRequests = rateLimitService.getRequestCount(ipAddress);
            int maxDailyScans = rateLimitService.getMaxRequestsPerDay();
            model.addAttribute("error", "Rate limit exceeded. You have used all " + maxDailyScans + " free analyses. Please try again tomorrow.");
            model.addAttribute("rateLimitExceeded", true);
            model.addAttribute("usedRequests", usedRequests);
            model.addAttribute("remainingRequests", 0);
            model.addAttribute("maxDailyScans", maxDailyScans);

            String storedCvName = (String) session.getAttribute(SessionAttributes.CV_NAME);
            if (storedCvName != null) {
                model.addAttribute("storedCvName", storedCvName);
            }

            log.warn("Rate limit exceeded for IP: {}", ipAddress);
            return "index";
        }

        String candidateCvText;
        String cvFileName;

        if (Boolean.TRUE.equals(reuseCv)) {
            candidateCvText = (String) session.getAttribute(SessionAttributes.CV_TEXT);
            cvFileName = (String) session.getAttribute(SessionAttributes.CV_NAME);

            if (candidateCvText == null || cvFileName == null) {
                model.addAttribute("error", "No CV found in session. Please upload a new CV.");
                return "index";
            }

            log.info("Reusing CV from session: {}", cvFileName);
        } else {
            if (cv == null || cv.isEmpty()) {
                model.addAttribute("error", "Please upload a CV PDF file.");
                return "index";
            }
            if (!FileValidationUtil.isPdfFile(cv)) {
                model.addAttribute("error", "Candidate CV must be a PDF file.");
                return "index";
            }

            log.info("UI request received. New CV uploaded: {}", cv.getOriginalFilename());

            try (TimedOperation ignored = TimedOperation.start(log, "CV text extraction")) {
                candidateCvText = textExtractor.extractText(cv);
            }
            cvFileName = cv.getOriginalFilename();

            session.setAttribute(SessionAttributes.CV_TEXT, candidateCvText);
            session.setAttribute(SessionAttributes.CV_NAME, cvFileName);
        }

        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            model.addAttribute("error", "Please paste the job description.");
            model.addAttribute("storedCvName", cvFileName);
            return "index";
        }

        var jobDescriptionText = jobDescription.trim();

        String normalizedMode = analysisMode == null ? "" : analysisMode.trim();
        boolean quickResponseRequested = !"thoughtful".equalsIgnoreCase(normalizedMode);

        JobFitRequest jobFitRequest = new JobFitRequest(candidateCvText, jobDescriptionText, quickResponseRequested);
        var fitScoreAgentInvocation = AgentInvocation.create(agentPlatform, FitScore.class);
        FitScore fitScore;
        try (TimedOperation ignored = TimedOperation.start(log, "Fit score agent invocation")) {
            fitScore = fitScoreAgentInvocation.invoke(jobFitRequest);
        }

        int score = fitScore.score();
        model.addAttribute("score", score);
        model.addAttribute("explanation", fitScore.explanation());
        model.addAttribute("cvName", cvFileName);
        model.addAttribute("role", role);
        model.addAttribute("company", company);
        model.addAttribute("jobDescription", jobDescriptionText);
        model.addAttribute("matchLabel", matchPresentationService.toMatchLabel(score));
        model.addAttribute("matchClass", matchPresentationService.toMatchClass(score));
        model.addAttribute("matchTheme", matchPresentationService.toMatchTheme(score));

        session.setAttribute(SessionAttributes.ROLE, role);
        session.setAttribute(SessionAttributes.COMPANY, company);
        session.setAttribute(SessionAttributes.JOB_DESCRIPTION, jobDescriptionText);
        session.setAttribute(SessionAttributes.FIT_SCORE, score);
        session.setAttribute(SessionAttributes.FIT_EXPLANATION, fitScore.explanation());
        session.removeAttribute(SessionAttributes.UPGRADED_CV);
        session.removeAttribute(SessionAttributes.UPGRADED_KEYWORDS);
        session.removeAttribute(SessionAttributes.UPGRADED_SUMMARY);
        model.addAttribute("storedCvName", cvFileName);

        int remainingRequests = rateLimitService.getRemainingRequests(ipAddress);
        int usedRequests = rateLimitService.getRequestCount(ipAddress);
        model.addAttribute("remainingRequests", remainingRequests);
        model.addAttribute("usedRequests", usedRequests);
        model.addAttribute("maxDailyScans", rateLimitService.getMaxRequestsPerDay());

        log.info("Analysis complete for IP: {}. Remaining requests: {}", ipAddress, remainingRequests);
        return "index";
    }
}
