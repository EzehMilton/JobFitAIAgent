package com.milton.agent.service;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.milton.agent.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;

@Slf4j
@Profile("dev")
@Agent(name = "job-fit-provider-mock", description = "Mock agent for local dev mode", version = "1.0.0")
public class MockJobFitProviderAgent {

    @Action
    public CvSkills extractSkillsFromCv(JobFitRequest request) {
        log.info("[DEV MOCK] Returning fake skills");
        return new CvSkills(java.util.List.of("Java", "Spring Boot", "REST APIs", "SQL"));
    }

    @Action
    public JobRequirements extractJobRequirements(JobFitRequest request) {
        log.info("[DEV MOCK] Returning fake job requirements");
        return new JobRequirements(java.util.List.of("Java", "Spring Boot", "AWS", "Microservices"));
    }

    @AchievesGoal(description = "Mocked fit score computation")
    @Action
    public FitScore calculateFitScore(CvSkills cv, JobRequirements job) {
        log.info("[DEV MOCK] Returning mock fit score");
        return new FitScore(78, """
                Mocked explanation:
                • Strong overlap on Java and Spring Boot.
                • Missing exposure to AWS and microservices.
                • Overall good fit with room for growth.
                """);
    }
}
