package com.milton.agent.controller;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.service.TextExtractor;
import com.milton.agent.util.FileValidationUtil;
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
public class UiController {

    private final AgentPlatform agentPlatform;
    private final TextExtractor textExtractor;

    @GetMapping({"/"})
    public String index() {
        return "index";
    }

    @PostMapping("/generate")
    public String generateScore(@RequestParam("candidateFile") MultipartFile cv,
                                @RequestParam("jobDescription") String jobDescription,
                                Model model) throws IOException {

        if (cv == null || cv.isEmpty()) {
            model.addAttribute("error", "Please upload a CV PDF file.");
            return "index";
        }
        if (!FileValidationUtil.isPdfFile(cv)) {
            model.addAttribute("error", "Candidate CV must be a PDF file.");
            return "index";
        }
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            model.addAttribute("error", "Please paste the job description.");
            return "index";
        }

        log.info("UI request received. CV: {}", cv.getOriginalFilename());

        var candidateCvText = textExtractor.extractText(cv);
        var jobDescriptionText = jobDescription.trim();

        JobFitRequest request = new JobFitRequest(candidateCvText, jobDescriptionText);
        var fitScoreAgentInvocation = AgentInvocation.create(agentPlatform, FitScore.class);
        FitScore fitScore = fitScoreAgentInvocation.invoke(request);

        int score = fitScore.score();
        model.addAttribute("score", score);
        model.addAttribute("explanation", fitScore.explanation());
        model.addAttribute("cvName", cv.getOriginalFilename());
        model.addAttribute("matchLabel", toMatchLabel(score));
        model.addAttribute("matchClass", toMatchClass(score));

        return "index";
    }

    private String toMatchLabel(int score) {
        if (score >= 90) return "EXCELLENT MATCH";
        if (score >= 70) return "GOOD MATCH";
        if (score >= 50) return "PARTIAL MATCH";
        return "WEAK MATCH";
    }

    private String toMatchClass(int score) {
        if (score >= 90) return "bg-success";
        if (score >= 70) return "bg-primary";
        if (score >= 50) return "bg-warning text-dark";
        return "bg-danger";
    }
}
