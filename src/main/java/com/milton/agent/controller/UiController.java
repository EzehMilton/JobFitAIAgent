package com.milton.agent.controller;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.service.RateLimitService;
import com.milton.agent.service.TextExtractor;
import com.milton.agent.util.FileValidationUtil;
import com.milton.agent.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UiController {

    private final AgentPlatform agentPlatform;
    private final TextExtractor textExtractor;
    private final RateLimitService rateLimitService;

    private static final String SESSION_CV_TEXT = "storedCvText";
    private static final String SESSION_CV_NAME = "storedCvName";
    private static final String DUMMY_CV_CONTENT = """
            JORDAN TAYLOR
            Email: jordan.taylor@example.com | Phone: (555) 123-4567 | LinkedIn: linkedin.com/in/jordantaylor

            PROFESSIONAL SUMMARY
            Product manager with 8+ years of experience guiding cross-functional teams to deliver AI-enabled HR solutions.
            Skilled at translating user research into product strategy, prioritising roadmaps, and launching features that
            improve talent acquisition outcomes. Passionate about leveraging data and automation to create equitable hiring experiences.

            CORE SKILLS
            • Product Strategy & Roadmapping • Agile Delivery • Stakeholder Management
            • UX Research & Experimentation • Data-Driven Decision Making • AI/ML for HR Tech

            EXPERIENCE
            Senior Product Manager | TalentFlow AI | 2020 - Present | San Francisco, CA
            - Led launch of “Role Readiness” assessment that increased qualified applicant matches by 27%.
            - Partnered with data science to deploy GPT-4 powered résumé rewriting assistant with 92% user satisfaction.
            - Built go-to-market collateral and enablement for enterprise clients across three verticals.

            Product Manager | CareerLaunch Labs | 2016 - 2020 | Remote
            - Owned roadmap for candidate insights dashboard adopted by 1200+ recruiting teams.
            - Implemented outcome-based OKRs and experimentation cadence that accelerated release velocity by 35%.
            - Collaborated with design to run discovery sprints, reducing candidate drop-off by 18%.

            EDUCATION
            MBA, Strategy & Analytics | Northwestern Kellogg School of Management
            B.S., Information Systems | University of Washington

            CERTIFICATIONS
            • Pragmatic Institute Product Management
            • Certified Scrum Product Owner (CSPO)

            ADDITIONAL HIGHLIGHTS
            - Speaker at 2023 Talent Innovation Summit on “Personalised CV Experiences with Generative AI”.
            - Volunteer mentor for Women In Product’s early career accelerator.
            """;

    @GetMapping({"/"})
    public String index(HttpSession session, HttpServletRequest request, Model model) {
        // Check if there's a CV in session and add it to model
        String storedCvName = (String) session.getAttribute(SESSION_CV_NAME);
        if (storedCvName != null) {
            model.addAttribute("storedCvName", storedCvName);
        }

        // Add rate limit info to model
        String ipAddress = IpAddressUtil.getClientIpAddress(request);
        int remainingRequests = rateLimitService.getRemainingRequests(ipAddress);
        int usedRequests = rateLimitService.getRequestCount(ipAddress);

        model.addAttribute("remainingRequests", remainingRequests);
        model.addAttribute("usedRequests", usedRequests);

        log.debug("IP {} - Used: {}/5, Remaining: {}", ipAddress, usedRequests, remainingRequests);

        return "index";
    }

    @PostMapping("/generate")
    public String generateScore(@RequestParam(value = "candidateFile", required = false) MultipartFile cv,
                                @RequestParam(value = "reuseCv", required = false) Boolean reuseCv,
                                @RequestParam("jobDescription") String jobDescription,
                                HttpServletRequest request,
                                HttpSession session,
                                Model model) throws IOException {

        // Get client IP address
        String ipAddress = IpAddressUtil.getClientIpAddress(request);
        log.info("Request from IP: {}", ipAddress);

        // Check rate limit
        if (!rateLimitService.isAllowed(ipAddress)) {
            int usedRequests = rateLimitService.getRequestCount(ipAddress);
            model.addAttribute("error", "Rate limit exceeded. You have used all 5 free analyses. Please try again tomorrow.");
            model.addAttribute("rateLimitExceeded", true);
            model.addAttribute("usedRequests", usedRequests);
            model.addAttribute("remainingRequests", 0);

            // Preserve stored CV info if available
            String storedCvName = (String) session.getAttribute(SESSION_CV_NAME);
            if (storedCvName != null) {
                model.addAttribute("storedCvName", storedCvName);
            }

            log.warn("Rate limit exceeded for IP: {}", ipAddress);
            return "index";
        }

        String candidateCvText;
        String cvFileName;

        // Determine whether to use new CV or reuse from session
        if (Boolean.TRUE.equals(reuseCv)) {
            // Reuse CV from session
            candidateCvText = (String) session.getAttribute(SESSION_CV_TEXT);
            cvFileName = (String) session.getAttribute(SESSION_CV_NAME);

            if (candidateCvText == null || cvFileName == null) {
                model.addAttribute("error", "No CV found in session. Please upload a new CV.");
                return "index";
            }

            log.info("Reusing CV from session: {}", cvFileName);
        } else {
            // Validate and process new CV upload
            if (cv == null || cv.isEmpty()) {
                model.addAttribute("error", "Please upload a CV PDF file.");
                return "index";
            }
            if (!FileValidationUtil.isPdfFile(cv)) {
                model.addAttribute("error", "Candidate CV must be a PDF file.");
                return "index";
            }

            log.info("UI request received. New CV uploaded: {}", cv.getOriginalFilename());

            candidateCvText = textExtractor.extractText(cv);
            cvFileName = cv.getOriginalFilename();

            // Store in session for future reuse
            session.setAttribute(SESSION_CV_TEXT, candidateCvText);
            session.setAttribute(SESSION_CV_NAME, cvFileName);
        }

        // Validate job description
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            model.addAttribute("error", "Please paste the job description.");
            model.addAttribute("storedCvName", cvFileName); // Preserve CV info in case of error
            return "index";
        }

        var jobDescriptionText = jobDescription.trim();

        JobFitRequest jobFitRequest = new JobFitRequest(candidateCvText, jobDescriptionText);
        var fitScoreAgentInvocation = AgentInvocation.create(agentPlatform, FitScore.class);
        FitScore fitScore = fitScoreAgentInvocation.invoke(jobFitRequest);

        int score = fitScore.score();
        model.addAttribute("score", score);
        model.addAttribute("explanation", fitScore.explanation());
        model.addAttribute("cvName", cvFileName);
        model.addAttribute("matchLabel", toMatchLabel(score));
        model.addAttribute("matchClass", toMatchClass(score));
        model.addAttribute("matchTheme", toMatchTheme(score));
        model.addAttribute("showUpgradeButton", shouldShowUpgradeButton(score));
        model.addAttribute("storedCvName", cvFileName); // Keep showing stored CV option

        // Update rate limit info
        int remainingRequests = rateLimitService.getRemainingRequests(ipAddress);
        int usedRequests = rateLimitService.getRequestCount(ipAddress);
        model.addAttribute("remainingRequests", remainingRequests);
        model.addAttribute("usedRequests", usedRequests);

        log.info("Analysis complete for IP: {}. Remaining requests: {}", ipAddress, remainingRequests);

        return "index";
    }

    @GetMapping("/upgrade-cv")
    public String showUpgradeCv(Model model) {
        model.addAttribute("dummyCv", DUMMY_CV_CONTENT);
        return "upgrade_cv";
    }

    @GetMapping("/dummy-cv/download")
    public ResponseEntity<ByteArrayResource> downloadDummyCv() {
        byte[] data = DUMMY_CV_CONTENT.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"upgrade-role-cv.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(data.length)
                .body(resource);
    }

    private String toMatchLabel(int score) {
        if (score >= 90) return "EXCELLENT MATCH";
        if (score >= 70) return "GOOD MATCH";
        if (score >= 50) return "PARTIAL MATCH";
        return "WEAK MATCH";
    }

    private String toMatchClass(int score) {
        if (score >= 90) return "match-badge-excellent";
        if (score >= 70) return "match-badge-good";
        if (score >= 50) return "match-badge-partial";
        return "match-badge-weak";
    }

    private String toMatchTheme(int score) {
        if (score >= 90) return "match-theme-excellent";
        if (score >= 70) return "match-theme-good";
        if (score >= 50) return "match-theme-partial";
        return "match-theme-weak";
    }

    private boolean shouldShowUpgradeButton(int score) {
        return score >= 70 && score <= 85;
    }

    @PostMapping("/clear-cv")
    public String clearStoredCv(HttpSession session) {
        session.removeAttribute(SESSION_CV_TEXT);
        session.removeAttribute(SESSION_CV_NAME);
        log.info("Cleared stored CV from session");
        return "redirect:/";
    }
}
