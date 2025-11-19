package com.milton.agent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.config.models.OpenAiModels;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.model.Thinking;
import com.milton.agent.config.PromptLoader;
import com.milton.agent.models.CvRewriteRequest;
import com.milton.agent.models.CvSkills;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.models.JobRequirements;
import com.milton.agent.models.UpgradedCv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

@Slf4j
@Agent(name = "job-fit-provider",
        description = "Assesses how well a CV matches a job description and provides a fit score with an explanation",
        version = "1.0.0",
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
                        .withModel(OpenAiModels.GPT_41_MINI)
                        .withTemperature(0.1)
                        .withTopP(0.90)
                        .withFrequencyPenalty(0.0)
                        .withPresencePenalty(0.0)
                        .withMaxTokens(500)
                )
                .createObject(prompt,
                        CvSkills.class);

        Assert.notNull(cvSkills, "CV skills cannot be null");
        log.info("Skills extracted from CV");
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
                        .withModel(OpenAiModels.GPT_41_MINI)
                        .withTemperature(0.2)
                        .withTopP(0.90)
                        .withFrequencyPenalty(0.0)
                        .withPresencePenalty(0.0)
                        .withMaxTokens(600)
                )
                .createObject(prompt, JobRequirements.class);

        Assert.notNull(requirements, "Job requirements cannot be null");
        log.info("Job requirements extracted from pasted requirements");
        return requirements;
    }



    @AchievesGoal(description = "Computes the fit score between CV and job description")
    @Action
    public FitScore calculateFitScore(JobFitRequest request, CvSkills cvSkills, JobRequirements jobRequirements, OperationContext context) {
        log.info("Calculating fit score for CV skills and job requirements");

        String promptTemplate = promptLoader.loadPrompt("jobfit-fit-score.txt");
        String finalPrompt = promptTemplate.formatted(
                cvSkills,
                jobRequirements
        );
        log.debug("Final Prompt: {}", finalPrompt);

        boolean quickResponseRequested = request != null && request.QuickResponse();
        var llmOptions = LlmOptions
                .withModel(quickResponseRequested ? OpenAiModels.GPT_41_MINI : OpenAiModels.GPT_5);

        FitScore fitScore = context.ai()
                .withLlm(llmOptions)
                .createObject(finalPrompt, FitScore.class);

        Assert.notNull(fitScore, "Fit score cannot be null");
        return fitScore;
    }

    @AchievesGoal(description = "Rewrites candidate CV tailored to target role using ATS keywords")
    @Action
    public UpgradedCv rewriteCvForRole(CvRewriteRequest request, OperationContext context) {
        log.info("Rewriting CV for upgraded version with fit score {}", request.fitScore());

        String promptTemplate = promptLoader.loadPrompt("cv-rewriter.txt");
        String prompt = promptTemplate.formatted(
                request.fitScore(),
                request.fitExplanation(),
                request.jobDescription(),
                request.originalCv()
        );

        UpgradedCv upgradedCv = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_5)
                )
                .createObject(prompt, UpgradedCv.class);

        Assert.notNull(upgradedCv, "Upgraded CV cannot be null");
        return upgradedCv;
    }
}
