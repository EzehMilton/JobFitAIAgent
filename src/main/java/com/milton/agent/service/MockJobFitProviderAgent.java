package com.milton.agent.service;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.common.OperationContext;
import com.milton.agent.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Slf4j
@Profile("dev")
@Agent(
        name = "job-fit-provider-mock",
        description = "Mock agent for local dev mode (no LLM calls, returns static data)",
        version = "1.0.0"
)
public class MockJobFitProviderAgent {

    @Action
    public CvSkills extractSkillsFromCv(JobFitRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning fake CV skills");
        return new CvSkills(
                List.of("Java", "Spring Boot", "REST APIs", "SQL"),
                List.of("Agile project management", "Stakeholder engagement"),
                List.of("Communication", "Leadership"),
                List.of("PRINCE2 Practitioner")
        );
    }

    @Action
    public JobRequirements extractJobRequirements(JobFitRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning fake job requirements");

        return new JobRequirements(
                List.of("Leadership"),
                List.of("Risk taker"),
                List.of("Communication", "Leadership")
        );
    }

    @AchievesGoal(description = "Mocked fit score computation for dev mode")
    @Action
    public FitScore calculateFitScore(CvSkills cvSkills, JobRequirements jobRequirements, OperationContext context) {
        log.info("[DEV MOCK] Returning mock fit score");
        return new FitScore(
                78,
                """
                Mocked explanation:
                • Strong overlap on Java and Spring Boot.
                • Missing exposure to AWS and microservices depth.
                • Overall good fit with room for growth.
                """
        );
    }
}
