package com.milton.agent.controller;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.milton.agent.models.CareerSuggestions;
import com.milton.agent.models.DashboardEntry;
import com.milton.agent.models.ImproveScore;
import com.milton.agent.models.ImproveScoreRequest;
import com.milton.agent.models.InterviewPrep;
import com.milton.agent.models.InterviewPrepRequest;
import com.milton.agent.models.SuggestionsRequest;
import com.milton.agent.service.DashboardService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RecommendationsController {

    private final AgentPlatform agentPlatform;
    private final DashboardService dashboardService;
    private final PdfService pdfService;

    @GetMapping({"/suggestions/{id}", "/suggestions.html"})
    public String showSuggestions(@PathVariable(required = false) Long id,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        String candidateCv = (String) session.getAttribute(SessionAttributes.CV_TEXT);
        String jobDescription = (String) session.getAttribute(SessionAttributes.JOB_DESCRIPTION);
        Integer fitScore = (Integer) session.getAttribute(SessionAttributes.FIT_SCORE);
        String fitExplanation = (String) session.getAttribute(SessionAttributes.FIT_EXPLANATION);

        if (id != null) {
            DashboardEntry entry = dashboardService.getEntryById(id);
            if (entry != null) {
                jobDescription = entry.getJobDescription();
                fitScore = entry.getScore();
            }
        }

        if (candidateCv == null || jobDescription == null || fitScore == null) {
            redirectAttributes.addFlashAttribute("error", "Please run an analysis before requesting career suggestions.");
            return "redirect:/";
        }

        @SuppressWarnings("unchecked")
        CareerSuggestions cachedSuggestions = (CareerSuggestions) session.getAttribute(SessionAttributes.SUGGESTIONS);

        if (cachedSuggestions != null) {
            log.debug("Serving career suggestions from session cache");
            populateSuggestionsModel(model, cachedSuggestions, id != null ? id : 1L);
            return "suggestions";
        }

        try {
            SuggestionsRequest suggestionsRequest = new SuggestionsRequest(
                    candidateCv,
                    jobDescription,
                    fitScore,
                    fitExplanation != null ? fitExplanation : ""
            );

            var suggestionsInvocation = AgentInvocation.create(agentPlatform, CareerSuggestions.class);
            CareerSuggestions suggestions;
            try (TimedOperation ignored = TimedOperation.start(log, "Career suggestions agent invocation")) {
                suggestions = suggestionsInvocation.invoke(suggestionsRequest);
            }

            session.setAttribute(SessionAttributes.SUGGESTIONS, suggestions);
            populateSuggestionsModel(model, suggestions, id != null ? id : 1L);

            return "suggestions";
        } catch (Exception ex) {
            log.error("Failed to generate career suggestions", ex);
            redirectAttributes.addFlashAttribute("error", "We couldn't generate career suggestions right now. Please try again in a moment.");
            return "redirect:/";
        }
    }

    @GetMapping({"/improve-score/{id}", "/improve-score", "/improve", "/improve.html"})
    public String showImproveScore(@PathVariable(required = false) Long id,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        String candidateCv = (String) session.getAttribute(SessionAttributes.CV_TEXT);
        String jobDescription = (String) session.getAttribute(SessionAttributes.JOB_DESCRIPTION);
        Integer fitScore = (Integer) session.getAttribute(SessionAttributes.FIT_SCORE);
        String fitExplanation = (String) session.getAttribute(SessionAttributes.FIT_EXPLANATION);

        if (id != null) {
            DashboardEntry entry = dashboardService.getEntryById(id);
            if (entry != null) {
                jobDescription = entry.getJobDescription();
                fitScore = entry.getScore();
            }
        }

        if (candidateCv == null || jobDescription == null || fitScore == null) {
            redirectAttributes.addFlashAttribute("error", "Please run an analysis before requesting improvement recommendations.");
            return "redirect:/";
        }

        ImproveScore cachedImproveScore = (ImproveScore) session.getAttribute(SessionAttributes.IMPROVE_SCORE);

        if (cachedImproveScore != null) {
            log.debug("Serving improve score recommendations from session cache");
            populateImproveScoreModel(model, cachedImproveScore, id != null ? id : 1L);
            return "improve";
        }

        try {
            ImproveScoreRequest improveScoreRequest = new ImproveScoreRequest(
                    candidateCv,
                    jobDescription,
                    fitScore,
                    fitExplanation != null ? fitExplanation : ""
            );

            var improveScoreInvocation = AgentInvocation.create(agentPlatform, ImproveScore.class);
            ImproveScore improveScore;
            try (TimedOperation ignored = TimedOperation.start(log, "Improve score agent invocation")) {
                improveScore = improveScoreInvocation.invoke(improveScoreRequest);
            }

            session.setAttribute(SessionAttributes.IMPROVE_SCORE, improveScore);
            populateImproveScoreModel(model, improveScore, id != null ? id : 1L);

            return "improve";
        } catch (Exception ex) {
            log.error("Failed to generate improve score recommendations", ex);
            redirectAttributes.addFlashAttribute("error", "We couldn't generate improvement recommendations right now. Please try again in a moment.");
            return "redirect:/";
        }
    }

    @GetMapping({"/get-ready/{id}", "/get-ready", "/getready"})
    public String showGetReady(@PathVariable(required = false) Long id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        String candidateCv = (String) session.getAttribute(SessionAttributes.CV_TEXT);
        String jobDescription = (String) session.getAttribute(SessionAttributes.JOB_DESCRIPTION);
        Integer fitScore = (Integer) session.getAttribute(SessionAttributes.FIT_SCORE);
        String fitExplanation = (String) session.getAttribute(SessionAttributes.FIT_EXPLANATION);

        if (id != null) {
            DashboardEntry entry = dashboardService.getEntryById(id);
            if (entry != null) {
                jobDescription = entry.getJobDescription();
                fitScore = entry.getScore();
            }
        }

        if (candidateCv == null || jobDescription == null || fitScore == null) {
            redirectAttributes.addFlashAttribute("error", "Please run an analysis before requesting interview preparation.");
            return "redirect:/";
        }

        InterviewPrep cachedInterviewPrep = (InterviewPrep) session.getAttribute(SessionAttributes.INTERVIEW_PREP);

        if (cachedInterviewPrep != null) {
            log.debug("Serving interview prep from session cache");
            populateInterviewPrepModel(model, cachedInterviewPrep, id != null ? id : 1L);
            return "getready";
        }

        try {
            InterviewPrepRequest interviewPrepRequest = new InterviewPrepRequest(
                    candidateCv,
                    jobDescription,
                    fitScore,
                    fitExplanation != null ? fitExplanation : ""
            );

            var interviewPrepInvocation = AgentInvocation.create(agentPlatform, InterviewPrep.class);
            InterviewPrep interviewPrep;
            try (TimedOperation ignored = TimedOperation.start(log, "Interview prep agent invocation")) {
                interviewPrep = interviewPrepInvocation.invoke(interviewPrepRequest);
            }

            session.setAttribute(SessionAttributes.INTERVIEW_PREP, interviewPrep);
            populateInterviewPrepModel(model, interviewPrep, id != null ? id : 1L);

            return "getready";
        } catch (Exception ex) {
            log.error("Failed to generate interview prep", ex);
            redirectAttributes.addFlashAttribute("error", "We couldn't generate interview preparation content right now. Please try again in a moment.");
            return "redirect:/";
        }
    }

    @PostMapping("/dashboard/save")
    public String saveToDashboard(@RequestParam String role,
                                  @RequestParam String company,
                                  @RequestParam String jobDescription,
                                  @RequestParam int score,
                                  RedirectAttributes redirectAttributes) {

        if (!dashboardService.canAddNewEntry()) {
            redirectAttributes.addFlashAttribute("error", "You have reached the maximum of 20 saved results.");
            return "redirect:/dashboard";
        }

        dashboardService.saveEntry(role, company, jobDescription, score);

        redirectAttributes.addFlashAttribute("success", "Saved to dashboard!");
        return "redirect:/dashboard";
    }

    @GetMapping("/suggestions/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadSuggestionsPdf(@PathVariable Long id, HttpSession session) {
        CareerSuggestions suggestions = (CareerSuggestions) session.getAttribute(SessionAttributes.SUGGESTIONS);

        if (suggestions == null) {
            log.warn("No suggestions found in session for download");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            // Format the suggestions as text
            StringBuilder content = new StringBuilder();
            content.append("CAREER SUGGESTIONS\n");
            content.append("==================\n\n");

            content.append("RECOMMENDED JOB TITLES\n");
            content.append("----------------------\n");
            if (suggestions.suggestedTitles() != null) {
                for (String title : suggestions.suggestedTitles()) {
                    content.append("- ").append(title).append("\n");
                }
            }
            content.append("\n");

            content.append("SKILL CLUSTERS\n");
            content.append("-------------\n");
            if (suggestions.skillClusters() != null) {
                for (String skill : suggestions.skillClusters()) {
                    content.append("- ").append(skill).append("\n");
                }
            }
            content.append("\n");

            content.append("YOUR STRENGTHS\n");
            content.append("-------------\n");
            if (suggestions.strengths() != null) {
                for (String strength : suggestions.strengths()) {
                    content.append("- ").append(strength).append("\n");
                }
            }
            content.append("\n");

            content.append("AREAS TO IMPROVE\n");
            content.append("----------------\n");
            if (suggestions.weaknesses() != null) {
                for (String weakness : suggestions.weaknesses()) {
                    content.append("- ").append(weakness).append("\n");
                }
            }
            content.append("\n");

            content.append("CAREER DIRECTION\n");
            content.append("----------------\n");
            if (suggestions.careerDirection() != null) {
                content.append(suggestions.careerDirection()).append("\n");
            }

            byte[] pdfData = pdfService.renderPdfFromText(content.toString());
            ByteArrayResource resource = new ByteArrayResource(pdfData);

            String filename = pdfService.buildFileName(null, "career-suggestions", "-report.pdf");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to generate suggestions PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/improve-score/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadImproveScorePdf(@PathVariable Long id, HttpSession session) {
        ImproveScore improveScore = (ImproveScore) session.getAttribute(SessionAttributes.IMPROVE_SCORE);

        if (improveScore == null) {
            log.warn("No improve score data found in session for download");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            // Format the improve score data as text
            StringBuilder content = new StringBuilder();
            content.append("IMPROVE YOUR FIT SCORE\n");
            content.append("======================\n\n");

            content.append("MISSING EXPERIENCE / SKILLS\n");
            content.append("---------------------------\n");
            if (improveScore.gaps() != null) {
                for (String gap : improveScore.gaps()) {
                    content.append("- ").append(gap).append("\n");
                }
            }
            content.append("\n");

            content.append("JD ALIGNMENT ISSUES\n");
            content.append("-------------------\n");
            if (improveScore.alignmentIssues() != null) {
                for (String issue : improveScore.alignmentIssues()) {
                    content.append("- ").append(issue).append("\n");
                }
            }
            content.append("\n");

            content.append("RECOMMENDED KEYWORDS\n");
            content.append("--------------------\n");
            if (improveScore.keywordSuggestions() != null) {
                for (String keyword : improveScore.keywordSuggestions()) {
                    content.append("- ").append(keyword).append("\n");
                }
            }
            content.append("\n");

            content.append("RECOMMENDED COURSES\n");
            content.append("-------------------\n");
            if (improveScore.courseRecommendations() != null) {
                for (String course : improveScore.courseRecommendations()) {
                    content.append("- ").append(course).append("\n");
                }
            }
            content.append("\n");

            content.append("ACHIEVEMENT ENHANCEMENTS\n");
            content.append("------------------------\n");
            if (improveScore.achievementAdvice() != null) {
                content.append(improveScore.achievementAdvice()).append("\n");
            }

            byte[] pdfData = pdfService.renderPdfFromText(content.toString());
            ByteArrayResource resource = new ByteArrayResource(pdfData);

            String filename = pdfService.buildFileName(null, "improve-fit-score", "-report.pdf");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to generate improve score PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/get-ready/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadGetReadyPdf(@PathVariable Long id, HttpSession session) {
        InterviewPrep interviewPrep = (InterviewPrep) session.getAttribute(SessionAttributes.INTERVIEW_PREP);

        if (interviewPrep == null) {
            log.warn("No interview prep data found in session for download");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            // Format the interview prep data as text
            StringBuilder content = new StringBuilder();
            content.append("GET READY - INTERVIEW PREPARATION\n");
            content.append("==================================\n\n");

            content.append("YOUR 60-SECOND PITCH\n");
            content.append("--------------------\n");
            if (interviewPrep.pitch() != null) {
                content.append(interviewPrep.pitch()).append("\n");
            }
            content.append("\n");

            content.append("LIKELY INTERVIEW QUESTIONS\n");
            content.append("--------------------------\n");
            if (interviewPrep.questions() != null) {
                for (String question : interviewPrep.questions()) {
                    content.append("- ").append(question).append("\n");
                }
            }
            content.append("\n");

            content.append("STAR STORIES TO PREPARE\n");
            content.append("-----------------------\n");
            if (interviewPrep.starStories() != null) {
                for (String story : interviewPrep.starStories()) {
                    content.append("- ").append(story).append("\n");
                }
            }
            content.append("\n");

            content.append("RECOMMENDATIONS BEFORE THE INTERVIEW\n");
            content.append("------------------------------------\n");
            if (interviewPrep.prepAdvice() != null) {
                content.append(interviewPrep.prepAdvice()).append("\n");
            }

            byte[] pdfData = pdfService.renderPdfFromText(content.toString());
            ByteArrayResource resource = new ByteArrayResource(pdfData);

            String filename = pdfService.buildFileName(null, "interview-prep", "-guide.pdf");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to generate interview prep PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void populateSuggestionsModel(Model model, CareerSuggestions suggestions, Long entryId) {
        model.addAttribute("entry", new Object() {
            public final java.util.List<String> suggestedTitles = suggestions.suggestedTitles() != null ?
                    suggestions.suggestedTitles() : List.of();
            public final java.util.List<String> skillClusters = suggestions.skillClusters() != null ?
                    suggestions.skillClusters() : List.of();
            public final java.util.List<String> strengths = suggestions.strengths() != null ?
                    suggestions.strengths() : List.of();
            public final java.util.List<String> weaknesses = suggestions.weaknesses() != null ?
                    suggestions.weaknesses() : List.of();
            public final String careerDirection = suggestions.careerDirection() != null ?
                    suggestions.careerDirection() : "Focus on developing your skills and gaining relevant experience.";
            public final Long id = entryId;
        });
    }

    private void populateImproveScoreModel(Model model, ImproveScore improveScore, Long entryId) {
        model.addAttribute("entry", new Object() {
            public final java.util.List<String> gaps = improveScore.gaps() != null ?
                    improveScore.gaps() : List.of();
            public final java.util.List<String> alignmentIssues = improveScore.alignmentIssues() != null ?
                    improveScore.alignmentIssues() : List.of();
            public final java.util.List<String> keywordSuggestions = improveScore.keywordSuggestions() != null ?
                    improveScore.keywordSuggestions() : List.of();
            public final java.util.List<String> courseRecommendations = improveScore.courseRecommendations() != null ?
                    improveScore.courseRecommendations() : List.of();
            public final String achievementAdvice = improveScore.achievementAdvice() != null ?
                    improveScore.achievementAdvice() : "Focus on adding measurable achievements to demonstrate impact.";
            public final Long id = entryId;
        });
    }

    private void populateInterviewPrepModel(Model model, InterviewPrep interviewPrep, Long entryId) {
        model.addAttribute("entry", new Object() {
            public final String pitch = interviewPrep.pitch() != null ?
                    interviewPrep.pitch() : "Prepare a 60-second pitch highlighting your key achievements and why you're excited about this role.";
            public final java.util.List<String> questions = interviewPrep.questions() != null ?
                    interviewPrep.questions() : List.of();
            public final java.util.List<String> starStories = interviewPrep.starStories() != null ?
                    interviewPrep.starStories() : List.of();
            public final String prepAdvice = interviewPrep.prepAdvice() != null ?
                    interviewPrep.prepAdvice() : "Research the company culture and review your key achievements relevant to this role.";
            public final Long id = entryId;
        });
    }
}
