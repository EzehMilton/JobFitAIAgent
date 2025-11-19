package com.milton.agent.controller;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.service.TextExtractor;
import com.milton.agent.util.FileValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@RestController
public class JobFitProviderController {
    private final AgentPlatform agentPlatform;
    private final TextExtractor textExtractor;

    @PostMapping("/score")
    FitScore extractSkillsFromCVAndJobDescription(@RequestParam("candidateFile") MultipartFile cv,
                                                  @RequestParam("jobDescriptionFile") MultipartFile jobDescription,
                                                  @RequestParam(value = "analysisMode", required = false, defaultValue = "quick") String analysisMode) throws IOException {


        if (!FileValidationUtil.isPdfFile(cv)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate CV must be a PDF");
        }

        if (!FileValidationUtil.isPdfFile(jobDescription)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job Description must be added");
        }

        log.debug("CV uploaded {} - Job Description pasted {} - PDF files ", cv.getOriginalFilename(), jobDescription.getOriginalFilename());
        var candidateCvText = textExtractor.extractText(cv);
        var jobDescriptionText = textExtractor.extractText(jobDescription);
        log.debug("Text extracted from documents.");
        boolean quickResponseRequested = !"thoughtful".equalsIgnoreCase(analysisMode);
        JobFitRequest request = new JobFitRequest(candidateCvText, jobDescriptionText, quickResponseRequested);
        var fitScoreAgentInvocation = AgentInvocation.create(agentPlatform, FitScore.class);
        var fitScore = fitScoreAgentInvocation.invoke(request);
        log.info("Fit score of your application: {}", fitScore);
        return fitScore;
    }
}
