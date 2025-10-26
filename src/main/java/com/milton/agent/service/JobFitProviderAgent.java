package com.milton.agent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.config.models.OpenAiModels;
import com.embabel.common.ai.model.LlmOptions;
import com.milton.agent.config.PromptLoader;
import com.milton.agent.models.CvSkills;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.models.JobRequirements;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
@Agent(name = "job-fit-provider",
        description = "Assesses how well a CV matches a job description and provides a fit score",
        version = "1.0.0",
        beanName = "jobFitProviderAgent")
@RequiredArgsConstructor
public class JobFitProviderAgent {

    private final PromptLoader promptLoader;

    @Action
    public CvSkills extractSkillsFromCv(JobFitRequest request, OperationContext context) {
        log.info("Extracting key skills from CV" );
        CvSkills cvSkills = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41_NANO)
                        .withTemperature(0.0)
                )
                .createObjectIfPossible("""
                                Extract a list of key skills from this CV text: %s.
                                Create a CvSkills object with the list.
                                """.formatted(request.CvText()),
                        CvSkills.class);
        Assert.notNull(cvSkills, "CV skills cannot be null");
        return cvSkills;
    }
    @Action
    public JobRequirements extractJobRequirements(JobFitRequest request, OperationContext context) {
        log.info("Extracting a list of key requirements from job description");
        JobRequirements requirements = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41_NANO)
                        .withTemperature(0.0)
                )
                .createObjectIfPossible(
                        """
                        Extract a list of key requirements from this job description: %s.
                        Create a JobRequirements object with the list.
                        """.formatted(request.JobDescription()),
                        JobRequirements.class
                );
        Assert.notNull(requirements, "Job requirements cannot be null");
        return requirements;
    }

    @AchievesGoal(description = "Computes the fit score between CV and job description")
    @Action
    public FitScore calculateFitScore(CvSkills cvSkills, JobRequirements jobRequirements, OperationContext context) {
        log.info("Calculating fit score for CV skills: {} and job requirements: {}", cvSkills, jobRequirements);

        String promptTemplate = promptLoader.loadPrompt("jobfit-fit-score.txt");
        String finalPrompt = promptTemplate.formatted(cvSkills.skills(), jobRequirements.requirements());

        FitScore fitScore = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_5)
                        .withTemperature(0.0)
                )
                .createObjectIfPossible(finalPrompt, FitScore.class);

        Assert.notNull(fitScore, "Fit score cannot be null");
        return fitScore;
    }
}
