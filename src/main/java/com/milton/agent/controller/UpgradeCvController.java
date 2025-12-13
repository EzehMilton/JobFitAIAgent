package com.milton.agent.controller;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.milton.agent.models.CvRewriteRequest;
import com.milton.agent.models.UpgradedCv;
import com.milton.agent.service.PdfService;
import com.milton.agent.util.TimedOperation;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UpgradeCvController {

    private final AgentPlatform agentPlatform;
    private final PdfService pdfService;

    @GetMapping({"/upgrade-cv", "/upgrade_cv.html"})
    public String showUpgradeCv(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String originalCv = (String) session.getAttribute(SessionAttributes.CV_TEXT);
        String jobDescription = (String) session.getAttribute(SessionAttributes.JOB_DESCRIPTION);
        Integer fitScore = (Integer) session.getAttribute(SessionAttributes.FIT_SCORE);
        String fitExplanation = (String) session.getAttribute(SessionAttributes.FIT_EXPLANATION);

        if (originalCv == null || jobDescription == null || fitScore == null || fitExplanation == null) {
            redirectAttributes.addFlashAttribute("error", "Please run an analysis before requesting an upgraded CV.");
            return "redirect:/";
        }

        String cachedUpgradedCv = (String) session.getAttribute(SessionAttributes.UPGRADED_CV);
        @SuppressWarnings("unchecked")
        List<String> cachedKeywords = (List<String>) session.getAttribute(SessionAttributes.UPGRADED_KEYWORDS);
        String cachedSummary = (String) session.getAttribute(SessionAttributes.UPGRADED_SUMMARY);

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
            UpgradedCv upgradedCv;
            try (TimedOperation ignored = TimedOperation.start(log, "Upgraded CV agent invocation")) {
                upgradedCv = rewriteInvocation.invoke(rewriteRequest);
            }

            String rewrittenText = (upgradedCv.cvText() == null || upgradedCv.cvText().isBlank())
                    ? originalCv
                    : upgradedCv.cvText();
            List<String> keywords = upgradedCv.atsKeywords() == null ? List.of() : upgradedCv.atsKeywords();
            String optimisationSummary = upgradedCv.optimisationSummary();

            session.setAttribute(SessionAttributes.UPGRADED_CV, rewrittenText);
            session.setAttribute(SessionAttributes.UPGRADED_KEYWORDS, keywords);
            session.setAttribute(SessionAttributes.UPGRADED_SUMMARY, optimisationSummary);

            populateUpgradeModel(model, session, rewrittenText, keywords, optimisationSummary, fitScore);

            return "upgrade_cv";
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid data supplied for upgraded CV generation", ex);
            redirectAttributes.addFlashAttribute("error", "We could not process your request. Please upload your CV and job description again.");
            return "redirect:/";
        } catch (RuntimeException ex) {
            log.error("Failed to generate upgraded CV", ex);
            redirectAttributes.addFlashAttribute("error", "We couldn't generate an upgraded CV right now. Please try again in a moment.");
            return "redirect:/";
        }
    }

    @GetMapping("/upgrade-cv/download")
    public ResponseEntity<ByteArrayResource> downloadUpgradedCv(HttpSession session) {
        String upgradedCv = (String) session.getAttribute(SessionAttributes.UPGRADED_CV);
        if (upgradedCv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String originalName = (String) session.getAttribute(SessionAttributes.CV_NAME);
        String downloadName = pdfService.buildFileName(originalName, "upgraded-cv", "-role-ready.pdf");

        try {
            byte[] data = pdfService.renderPdfFromText(upgradedCv);
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
        Integer fitScore = (Integer) session.getAttribute(SessionAttributes.FIT_SCORE);
        String cvName = (String) session.getAttribute(SessionAttributes.CV_NAME);
        String jobDescription = (String) session.getAttribute(SessionAttributes.JOB_DESCRIPTION);

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

        String downloadName = pdfService.buildFileName(cvName, "interview-prep-guide", "-interview-prep.pdf");

        try {
            byte[] data = pdfService.renderPdfFromText(guideText);
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
        model.addAttribute("cvName", session.getAttribute(SessionAttributes.CV_NAME));
        model.addAttribute("fitScore", fitScore);
    }
}
