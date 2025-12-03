package com.milton.agent.controller;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.milton.agent.models.CvRewriteRequest;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.models.UpgradedCv;
import com.milton.agent.service.RateLimitService;
import com.milton.agent.service.TextExtractor;
import com.milton.agent.util.FileValidationUtil;
import com.milton.agent.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class UiController {

    private final AgentPlatform agentPlatform;
    private final TextExtractor textExtractor;
    private final RateLimitService rateLimitService;

    private final int upgradeScoreLowerBound;
    private final int upgradeScoreUpperBound;

    private static final String SESSION_CV_TEXT = "storedCvText";
    private static final String SESSION_CV_NAME = "storedCvName";
    private static final String SESSION_JOB_DESCRIPTION = "storedJobDescription";
    private static final String SESSION_FIT_SCORE = "storedFitScore";
    private static final String SESSION_FIT_EXPLANATION = "storedFitExplanation";
    private static final String SESSION_UPGRADED_CV = "generatedUpgradedCv";
    private static final String SESSION_UPGRADED_KEYWORDS = "generatedAtsKeywords";
    private static final String SESSION_UPGRADED_SUMMARY = "generatedOptimisationSummary";

    public UiController(AgentPlatform agentPlatform,
                        TextExtractor textExtractor,
                        RateLimitService rateLimitService,
                        @Value("${jobfit.upgrade-button.lower-score:75}") int upgradeScoreLowerBound,
                        @Value("${jobfit.upgrade-button.upper-score:85}") int upgradeScoreUpperBound) {
        Assert.isTrue(upgradeScoreLowerBound < upgradeScoreUpperBound,
                "Upgrade score lower bound must be less than upper bound");
        this.agentPlatform = agentPlatform;
        this.textExtractor = textExtractor;
        this.rateLimitService = rateLimitService;
        this.upgradeScoreLowerBound = upgradeScoreLowerBound;
        this.upgradeScoreUpperBound = upgradeScoreUpperBound;
    }

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
                                @RequestParam("jobDescription") String jobDescription,
                                @RequestParam(value = "analysisMode", required = false) String analysisMode,
                                HttpServletRequest request,
                                HttpSession session,
                                Model model) throws IOException {

        // Get client IP address
        String ipAddress = IpAddressUtil.getClientIpAddress(request);
        log.info("Request from IP: {}", ipAddress);
        model.addAttribute("maxDailyScans", rateLimitService.getMaxRequestsPerDay());

        // Check rate limit
        if (!rateLimitService.isAllowed(ipAddress)) {
            int usedRequests = rateLimitService.getRequestCount(ipAddress);
            int maxDailyScans = rateLimitService.getMaxRequestsPerDay();
            model.addAttribute("error", "Rate limit exceeded. You have used all " + maxDailyScans + " free analyses. Please try again tomorrow.");
            model.addAttribute("rateLimitExceeded", true);
            model.addAttribute("usedRequests", usedRequests);
            model.addAttribute("remainingRequests", 0);
            model.addAttribute("maxDailyScans", maxDailyScans);

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

        String normalizedMode = analysisMode == null ? "" : analysisMode.trim();
        boolean quickResponseRequested = !"thoughtful".equalsIgnoreCase(normalizedMode);

        JobFitRequest jobFitRequest = new JobFitRequest(candidateCvText, jobDescriptionText, quickResponseRequested);
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
        model.addAttribute("showInterviewPrepButton", shouldShowInterviewPrepButton(score));

        // Persist data for future CV upgrade
        session.setAttribute(SESSION_JOB_DESCRIPTION, jobDescriptionText);
        session.setAttribute(SESSION_FIT_SCORE, score);
        session.setAttribute(SESSION_FIT_EXPLANATION, fitScore.explanation());
        session.removeAttribute(SESSION_UPGRADED_CV);
        session.removeAttribute(SESSION_UPGRADED_KEYWORDS);
        session.removeAttribute(SESSION_UPGRADED_SUMMARY);
        model.addAttribute("storedCvName", cvFileName); // Keep showing stored CV option

        // Update rate limit info
        int remainingRequests = rateLimitService.getRemainingRequests(ipAddress);
        int usedRequests = rateLimitService.getRequestCount(ipAddress);
        model.addAttribute("remainingRequests", remainingRequests);
        model.addAttribute("usedRequests", usedRequests);
        model.addAttribute("maxDailyScans", rateLimitService.getMaxRequestsPerDay());

        log.info("Analysis complete for IP: {}. Remaining requests: {}", ipAddress, remainingRequests);

        return "index";
    }

    @GetMapping("/upgrade-cv")
    public String showUpgradeCv(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String originalCv = (String) session.getAttribute(SESSION_CV_TEXT);
        String jobDescription = (String) session.getAttribute(SESSION_JOB_DESCRIPTION);
        Integer fitScore = (Integer) session.getAttribute(SESSION_FIT_SCORE);
        String fitExplanation = (String) session.getAttribute(SESSION_FIT_EXPLANATION);

        if (originalCv == null || jobDescription == null || fitScore == null || fitExplanation == null) {
            redirectAttributes.addFlashAttribute("error", "Please run an analysis before requesting an upgraded CV.");
            return "redirect:/";
        }

        String cachedUpgradedCv = (String) session.getAttribute(SESSION_UPGRADED_CV);
        @SuppressWarnings("unchecked")
        List<String> cachedKeywords = (List<String>) session.getAttribute(SESSION_UPGRADED_KEYWORDS);
        String cachedSummary = (String) session.getAttribute(SESSION_UPGRADED_SUMMARY);

        if (cachedUpgradedCv != null) {
            log.debug("Serving upgraded CV from session cache");
            populateUpgradeModel(model, session, cachedUpgradedCv, cachedKeywords, cachedSummary, fitScore);
            return "upgrade_cv";
        }

        try {
            CvRewriteRequest rewriteRequest = new CvRewriteRequest(
                    originalCv,
                    jobDescription,
                    fitScore,
                    fitExplanation
            );

            var rewriteInvocation = AgentInvocation.create(agentPlatform, UpgradedCv.class);
            UpgradedCv upgradedCv = rewriteInvocation.invoke(rewriteRequest);

            String rewrittenText = (upgradedCv.cvText() == null || upgradedCv.cvText().isBlank())
                    ? originalCv
                    : upgradedCv.cvText();
            List<String> keywords = upgradedCv.atsKeywords() == null ? List.of() : upgradedCv.atsKeywords();
            String optimisationSummary = upgradedCv.optimisationSummary();

            session.setAttribute(SESSION_UPGRADED_CV, rewrittenText);
            session.setAttribute(SESSION_UPGRADED_KEYWORDS, keywords);
            session.setAttribute(SESSION_UPGRADED_SUMMARY, optimisationSummary);

            populateUpgradeModel(model, session, rewrittenText, keywords, optimisationSummary, fitScore);

            return "upgrade_cv";
        } catch (Exception ex) {
            log.error("Failed to generate upgraded CV", ex);
            redirectAttributes.addFlashAttribute("error", "We couldn't generate an upgraded CV right now. Please try again in a moment.");
            return "redirect:/";
        }
    }

    @GetMapping("/upgrade-cv/download")
    public ResponseEntity<ByteArrayResource> downloadUpgradedCv(HttpSession session) {
        String upgradedCv = (String) session.getAttribute(SESSION_UPGRADED_CV);
        if (upgradedCv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String originalName = (String) session.getAttribute(SESSION_CV_NAME);
        String downloadName = buildDownloadFileName(originalName);

        try {
            byte[] data = renderPdfFromText(upgradedCv);
            ByteArrayResource resource = new ByteArrayResource(data);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(data.length)
                    .body(resource);
        } catch (IOException e) {
            log.error("Failed to create PDF for upgraded CV download", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/interview-prep-guide")
    public ResponseEntity<ByteArrayResource> downloadInterviewPrepGuide(HttpSession session) {
        Integer fitScore = (Integer) session.getAttribute(SESSION_FIT_SCORE);
        String cvName = (String) session.getAttribute(SESSION_CV_NAME);
        String jobDescription = (String) session.getAttribute(SESSION_JOB_DESCRIPTION);

        if (fitScore == null || jobDescription == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String guideText = "Interview Preparation Guide\n\n"
                + "This placeholder guide shows the type of prep package you will receive.\n\n"
                + "Suggested actions:\n"
                + "- Review the job description and match 3-5 achievements to the top requirements.\n"
                + "- Prepare STAR stories that highlight ownership, impact, and teamwork.\n"
                + "- Practice a 60-second pitch on why you fit this role.\n"
                + "- Draft thoughtful questions for the hiring team.\n"
                + "- Refresh your salary expectations and relocation/remote preferences.\n\n"
                + "Coming soon: a personalised, AI-generated interview prep kit tailored to your profile.";

        String downloadName = buildInterviewPrepFileName(cvName);

        try {
            byte[] data = renderPdfFromText(guideText);
            ByteArrayResource resource = new ByteArrayResource(data);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(data.length)
                    .body(resource);
        } catch (IOException e) {
            log.error("Failed to create interview prep guide PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
        if (upgradeScoreLowerBound >= upgradeScoreUpperBound) {
            log.warn("Invalid upgrade score bounds configured: lower={} upper={}", upgradeScoreLowerBound, upgradeScoreUpperBound);
            return false;
        }
        return score >= upgradeScoreLowerBound && score <= upgradeScoreUpperBound;
    }

    private boolean shouldShowInterviewPrepButton(int score) {
        if (upgradeScoreLowerBound >= upgradeScoreUpperBound) {
            log.warn("Invalid upgrade score bounds configured: lower={} upper={}", upgradeScoreLowerBound, upgradeScoreUpperBound);
            return false;
        }
        return score > upgradeScoreUpperBound;
    }

    private void populateUpgradeModel(Model model,
                                      HttpSession session,
                                      String upgradedCvText,
                                      List<String> atsKeywords,
                                      String optimisationSummary,
                                      int fitScore) {
        model.addAttribute("upgradedCvText", upgradedCvText);
        model.addAttribute("atsKeywords", (atsKeywords == null || atsKeywords.isEmpty()) ? null : atsKeywords);
        String summary = (optimisationSummary == null || optimisationSummary.isBlank())
                ? "The CV was refreshed to emphasise role-aligned achievements and embed relevant ATS keywords."
                : optimisationSummary;
        model.addAttribute("optimisationSummary", summary);
        model.addAttribute("cvName", session.getAttribute(SESSION_CV_NAME));
        model.addAttribute("fitScore", fitScore);
    }

    private String buildDownloadFileName(String originalName) {
        String baseName = (originalName == null || originalName.isBlank()) ? "upgraded-cv" : originalName;
        baseName = baseName.replaceAll("\\s+", "-");
        baseName = baseName.replaceAll("[^A-Za-z0-9._-]", "");

        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }

        if (baseName.isBlank()) {
            baseName = "upgraded-cv";
        }

        return baseName + "-role-ready.pdf";
    }

    private String buildInterviewPrepFileName(String cvName) {
        String baseName = (cvName == null || cvName.isBlank()) ? "interview-prep-guide" : cvName;
        baseName = baseName.replaceAll("\\s+", "-");
        baseName = baseName.replaceAll("[^A-Za-z0-9._-]", "");

        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }

        if (baseName.isBlank()) {
            baseName = "interview-prep-guide";
        }

        return baseName + "-interview-prep.pdf";
    }

    private byte[] renderPdfFromText(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDType1Font font = PDType1Font.HELVETICA;
            float fontSize = 11f;
            float leading = 1.4f * fontSize;
            float margin = 50f;
            float availableWidth = PDRectangle.LETTER.getWidth() - (margin * 2);
            String sanitizedText = sanitizeForPdf(text);
            List<String> lines = wrapText(sanitizedText, font, fontSize, availableWidth);

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = page.getMediaBox().getHeight() - margin;
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(margin, yPosition);

            for (String line : lines) {
                if (yPosition <= margin) {
                    contentStream.endText();
                    contentStream.close();

                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);

                    yPosition = page.getMediaBox().getHeight() - margin;
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(margin, yPosition);
                }

                String printableLine = line == null ? "" : line;
                contentStream.showText(printableLine);
                contentStream.newLineAtOffset(0, -leading);
                yPosition -= leading;
            }

            contentStream.endText();
            contentStream.close();

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<String> wrapText(String text, PDType1Font font, float fontSize, float availableWidth) throws IOException {
        List<String> wrappedLines = new ArrayList<>();
        String[] rawLines = text.split("\\R", -1);

        for (String rawLine : rawLines) {
            if (rawLine.isBlank()) {
                wrappedLines.add("");
                continue;
            }

            String[] words = rawLine.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
                float candidateWidth = font.getStringWidth(candidate) / 1000 * fontSize;

                if (candidateWidth > availableWidth && currentLine.length() > 0) {
                    wrappedLines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(candidate);
                }
            }

            wrappedLines.add(currentLine.toString());
        }

        return wrappedLines;
    }

    private String sanitizeForPdf(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");
        normalized = normalized
                .replace('\u2022', '-')
                .replace('\u2023', '-')
                .replace('\u25CF', '-');

        return normalized.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", "");
    }

    @PostMapping("/clear-cv")
    public String clearStoredCv(HttpSession session) {
        session.removeAttribute(SESSION_CV_TEXT);
        session.removeAttribute(SESSION_CV_NAME);
        session.removeAttribute(SESSION_JOB_DESCRIPTION);
        session.removeAttribute(SESSION_FIT_SCORE);
        session.removeAttribute(SESSION_FIT_EXPLANATION);
        session.removeAttribute(SESSION_UPGRADED_CV);
        session.removeAttribute(SESSION_UPGRADED_KEYWORDS);
        session.removeAttribute(SESSION_UPGRADED_SUMMARY);
        log.info("Cleared stored CV from session");
        return "redirect:/";
    }
}
