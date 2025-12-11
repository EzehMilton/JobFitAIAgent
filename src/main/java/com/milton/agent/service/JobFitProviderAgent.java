package com.milton.agent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.config.models.OpenAiModels;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.model.Thinking;
import com.milton.agent.config.PromptLoader;
import com.milton.agent.models.*;

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
        return extractCvSkills(request.CvText(), context, "");
    }

    @Action
    public JobRequirements extractJobRequirements(JobFitRequest request, OperationContext context) {
        return extractJobRequirements(request.JobDescription(), context, " from job description");
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
                        .withModel(OpenAiModels.GPT_5_MINI)
                )
                .createObject(prompt, UpgradedCv.class);

        Assert.notNull(upgradedCv, "Upgraded CV cannot be null");
        return upgradedCv;
    }

    @AchievesGoal(description = "Generates personalized career suggestions based on CV and job description analysis")
    @Action
    public CareerSuggestions generateCareerSuggestions(SuggestionsRequest request, CvSkills cvSkills, JobRequirements jobRequirements, OperationContext context) {
        log.info("Generating career suggestions for fit score {}", request.fitScore());

        String promptTemplate = promptLoader.loadPrompt("suggestions.txt");

        String cvSkillsStr = cvSkills != null ? cvSkills.toString() : "";
        String jobReqStr = jobRequirements != null ? jobRequirements.toString() : "";
        String fitExplanation = request.fitExplanation() != null ? request.fitExplanation() : "";
        String cvText = request.candidateCv() != null ? request.candidateCv() : "";

        String prompt = promptTemplate
                .replace("%s", "%s") // keep placeholders literal safety
                .replace("- FIT SCORE (0-100): %s", "- FIT SCORE (0-100): " + request.fitScore())
                .replace("- FIT EXPLANATION: %s", "- FIT EXPLANATION: " + fitExplanation)
                .replace("- EXTRACTED CV SKILLS: %s", "- EXTRACTED CV SKILLS: " + cvSkillsStr)
                .replace("- JOB REQUIREMENTS: %s", "- JOB REQUIREMENTS: " + jobReqStr)
                .replace("- CV TEXT: %s", "- CV TEXT: " + cvText);

        CareerSuggestions suggestions = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41_MINI)
                )
                .createObject(prompt, CareerSuggestions.class);

        Assert.notNull(suggestions, "Career suggestions cannot be null");
        return suggestions;
    }

    @Action
    public CvSkills extractSkillsForSuggestions(SuggestionsRequest request, OperationContext context) {
        return extractCvSkills(request.candidateCv(), context, " for suggestions");
    }

    @Action
    public JobRequirements extractRequirementsForSuggestions(SuggestionsRequest request, OperationContext context) {
        return extractJobRequirements(request.jobDescription(), context, " for suggestions");
    }

    @AchievesGoal(description = "Generates improvement recommendations for candidates with moderate fit scores (40-74%)")
    @Action
    public ImproveScore generateImproveScore(ImproveScoreRequest request, CvSkills cvSkills, JobRequirements jobRequirements, OperationContext context) {
        log.info("Generating improvement recommendations for fit score {}", request.fitScore());

        String promptTemplate = promptLoader.loadPrompt("improve.txt");

        String cvSkillsStr = cvSkills != null ? cvSkills.toString() : "";
        String jobReqStr = jobRequirements != null ? jobRequirements.toString() : "";
        String fitExplanation = request.fitExplanation() != null ? request.fitExplanation() : "";
        String cvText = request.candidateCv() != null ? request.candidateCv() : "";
        String jobDesc = request.jobDescription() != null ? request.jobDescription() : "";

        String prompt = promptTemplate
                .replace("- FIT SCORE (0-100): %s", "- FIT SCORE (0-100): " + request.fitScore())
                .replace("- FIT EXPLANATION: %s", "- FIT EXPLANATION: " + fitExplanation)
                .replace("- EXTRACTED CV SKILLS: %s", "- EXTRACTED CV SKILLS: " + cvSkillsStr)
                .replace("- JOB REQUIREMENTS: %s", "- JOB REQUIREMENTS: " + jobReqStr)
                .replace("- CV TEXT: %s", "- CV TEXT: " + cvText)
                .replace("- JOB DESCRIPTION: %s", "- JOB DESCRIPTION: " + jobDesc);

        ImproveScore improveScore = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41)
                )
                .createObject(prompt, ImproveScore.class);

        Assert.notNull(improveScore, "Improve score recommendations cannot be null");
        return improveScore;
    }

    @Action
    public CvSkills extractSkillsForImprove(ImproveScoreRequest request, OperationContext context) {
        return extractCvSkills(request.candidateCv(), context, " for improve score");
    }

    @Action
    public JobRequirements extractRequirementsForImprove(ImproveScoreRequest request, OperationContext context) {
        return extractJobRequirements(request.jobDescription(), context, " for improve score");
    }

    @AchievesGoal(description = "Generates interview preparation content for candidates with excellent fit scores (>85%)")
    @Action
    public InterviewPrep generateInterviewPrep(InterviewPrepRequest request, CvSkills cvSkills, JobRequirements jobRequirements, OperationContext context) {
        log.info("Generating interview prep for fit score {}", request.fitScore());

        String promptTemplate = promptLoader.loadPrompt("getready.txt");

        String cvSkillsStr = cvSkills != null ? cvSkills.toString() : "";
        String jobReqStr = jobRequirements != null ? jobRequirements.toString() : "";
        String fitExplanation = request.fitExplanation() != null ? request.fitExplanation() : "";
        String cvText = request.candidateCv() != null ? request.candidateCv() : "";
        String jobDesc = request.jobDescription() != null ? request.jobDescription() : "";

        String prompt = promptTemplate
                .replace("- FIT SCORE (0-100): %s", "- FIT SCORE (0-100): " + request.fitScore())
                .replace("- FIT EXPLANATION: %s", "- FIT EXPLANATION: " + fitExplanation)
                .replace("- EXTRACTED CV SKILLS: %s", "- EXTRACTED CV SKILLS: " + cvSkillsStr)
                .replace("- JOB REQUIREMENTS: %s", "- JOB REQUIREMENTS: " + jobReqStr)
                .replace("- CV TEXT: %s", "- CV TEXT: " + cvText)
                .replace("- JOB DESCRIPTION: %s", "- JOB DESCRIPTION: " + jobDesc);

        InterviewPrep interviewPrep = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41)
                )
                .createObject(prompt, InterviewPrep.class);

        Assert.notNull(interviewPrep, "Interview prep cannot be null");
        return interviewPrep;
    }

    @Action
    public CvSkills extractSkillsForInterviewPrep(InterviewPrepRequest request, OperationContext context) {
        return extractCvSkills(request.candidateCv(), context, " for interview prep");
    }

    @Action
    public JobRequirements extractRequirementsForInterviewPrep(InterviewPrepRequest request, OperationContext context) {
        return extractJobRequirements(request.jobDescription(), context, " for interview prep");
    }

    private CvSkills extractCvSkills(String cvText, OperationContext context, String logSuffix) {
        log.info("Extracting key skills from CV{}", logSuffix);

        String skillsExtractorPrompt = promptLoader.loadPrompt("skills-extractor.txt");
        String prompt = skillsExtractorPrompt + "\n\nCV TEXT:\n" + (cvText == null ? "" : cvText);

        CvSkills cvSkills = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41_MINI)
                        .withTemperature(0.1)
                        .withTopP(0.90)
                        .withFrequencyPenalty(0.0)
                        .withPresencePenalty(0.0)
                        .withMaxTokens(500)
                )
                .createObject(prompt, CvSkills.class);

        Assert.notNull(cvSkills, "CV skills cannot be null");
        log.info("Skills extracted from CV{}", logSuffix);
        return cvSkills;
    }

    private JobRequirements extractJobRequirements(String jobDescription, OperationContext context, String logSuffix) {
        log.info("Extracting job requirements{}", logSuffix);

        String jobDescriptionPrompt = promptLoader.loadPrompt("job-description-extractor.txt");
        String prompt = jobDescriptionPrompt + "\n\nJOB DESCRIPTION:\n" +
                (jobDescription == null ? "" : jobDescription);

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
        log.info("Job requirements extracted{}", logSuffix);
        return requirements;
    }
}
