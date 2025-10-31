package com.milton.agent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.config.models.OpenAiModels;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.model.Thinking;
import com.milton.agent.config.PromptLoader;
import com.milton.agent.models.CvSkills;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.models.JobRequirements;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

@Slf4j
@Agent(name = "job-fit-provider",
        description = "Assesses how well a CV matches a job description and provides a fit score with an explanation",
        version = "0.0.1",
        beanName = "jobFitProviderAgent")
@RequiredArgsConstructor
@Profile("prod")
public class JobFitProviderAgent {

    private final PromptLoader promptLoader;

    @Action
    public CvSkills extractSkillsFromCv(JobFitRequest request, OperationContext context) {
        log.info("Extracting key skills from CV" );

        String skillsExtractorPrompt = promptLoader.loadPrompt("skills-extractor.txt");
        String cvText = request.CvText();
        String prompt = skillsExtractorPrompt + "\n\nCV TEXT:\n" + cvText;


        CvSkills cvSkills = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41_NANO)
                        .withTemperature(0.2)
                        .withTopP(0.90)
                        .withFrequencyPenalty(0.0)
                        .withPresencePenalty(0.0)
                        .withMaxTokens(3000)
                )
                .createObject(prompt,
                        CvSkills.class);

        Assert.notNull(cvSkills, "CV skills cannot be null");
        log.info("Skills extracted: {}", cvSkills);
        return cvSkills;
    }

    @Action
    public JobRequirements extractJobRequirements(JobFitRequest request, OperationContext context) {
        log.info("Extracting a list of key requirements from job description");

        String jobDescriptionPrompt = promptLoader.loadPrompt("job-description-extractor.txt");

        String prompt =
                jobDescriptionPrompt + "\n\nJOB DESCRIPTION:\n" +
                        (request.JobDescription() == null ? "" : request.JobDescription());

        JobRequirements requirements = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41_NANO)
                        .withTemperature(0.2)
                        .withTopP(0.90)
                        .withFrequencyPenalty(0.0)
                        .withPresencePenalty(0.0)
                        .withMaxTokens(3000)
                )
                .createObject(prompt, JobRequirements.class);

        Assert.notNull(requirements, "Job requirements cannot be null");
        log.info("Job requirements extracted: {}", requirements);
        return requirements;
    }



    @AchievesGoal(description = "Computes the fit score between CV and job description")
    @Action
    public FitScore calculateFitScore(CvSkills cvSkills, JobRequirements jobRequirements, OperationContext context) {
        log.info("Calculating fit score for CV skills: {} and job requirements: {}", cvSkills, jobRequirements);

        String promptTemplate = promptLoader.loadPrompt("jobfit-fit-score.txt");
        String finalPrompt = promptTemplate.formatted(
                cvSkills,
                jobRequirements
        );
        log.info("Final Prompt: {}", finalPrompt);

        FitScore fitScore = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41_MINI)
                        .withTemperature(0.2)
                        .withTopP(0.95)
                        .withFrequencyPenalty(0.0)
                        .withPresencePenalty(0.0)
                        .withThinking(Thinking.withTokenBudget(10))
                )
                .createObject(finalPrompt, FitScore.class);

        Assert.notNull(fitScore, "Fit score cannot be null");
        return fitScore;
    }
}
