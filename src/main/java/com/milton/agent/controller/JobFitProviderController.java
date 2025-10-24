package com.milton.agent.controller;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class JobFitProviderController {
    private final AgentPlatform agentPlatform;
    private final Ai ai;

    @PostMapping("/score")
    FitScore getJobFit(@RequestBody JobFitRequest request) {
        var fitScoreAgentInvocation = AgentInvocation.create(agentPlatform, FitScore.class);
        var fitScore = fitScoreAgentInvocation.invoke(request);
        log.info("Fit score: {}", fitScore);
        return fitScore;
    }
}
