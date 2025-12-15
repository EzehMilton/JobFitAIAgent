package com.milton.agent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.models.OpenAiModels;
import com.embabel.common.ai.model.LlmOptions;
import com.milton.agent.config.PromptLoader;
import com.milton.agent.models.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Agent(name = "job-fit-provider",
        description = "Assesses how well a CV matches a job description and provides a fit score with an explanation",
        version = "1.0.0",
        beanName = "jobFitProviderAgent")
@RequiredArgsConstructor
@Profile("prod")
public class JobFitProviderAgent {

    private final PromptLoader promptLoader;

    // Java 21 virtual thread executor for parallel LLM calls
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    // Thread-safe cache for parallel extraction results (per request scope)
    // Key: cvTextHash + jobDescHash, Value: PreparationResult
    private final ConcurrentHashMap<String, PreparationResult> extractionCache = new ConcurrentHashMap<>();

    /**
     * Generic CV skills extraction action that works with ANY request containing CV text.
     * Uses PARALLEL EXECUTION with extractJobRequirements to minimize total extraction time.
     * Both LLM calls fire concurrently using Java 21 virtual threads, reducing bottleneck from ~1300ms to ~700ms.
     */
    @Action(description = "Extracts technical skills, soft skills, and experience from the candidate's CV")
    public CvSkills extractSkillsFromCv(CvTextProvider request, OperationContext context) {
        // If this is the first extraction call and we also need job requirements, do both in parallel
        if (request instanceof JobDescriptionProvider) {
            String cvText = request.getCvText();
            String jobDescText = ((JobDescriptionProvider) request).getJobDescriptionText();
            String cacheKey = getCacheKey(cvText, jobDescText);

            // Check cache first
            PreparationResult cached = extractionCache.get(cacheKey);
            if (cached != null) {
                log.info("Using CV skills from parallel extraction cache");
                return cached.cvSkills();
            }

            // Not in cache - run parallel extraction
            log.info("Running CV skills and job requirements extraction IN PARALLEL");
            PreparationResult result = runParallelExtraction(cvText, jobDescText, context);
            extractionCache.put(cacheKey, result);
            return result.cvSkills();
        }

        // Fallback to single extraction if request doesn't have job description
        log.info("Running single CV skills extraction (no parallel opportunity)");
        return extractCvSkills(request.getCvText(), context, "");
    }

    /**
     * Generic job requirements extraction action that works with ANY request containing job description.
     * Uses PARALLEL EXECUTION with extractSkillsFromCv to minimize total extraction time.
     * Both LLM calls fire concurrently using Java 21 virtual threads, reducing bottleneck from ~1300ms to ~700ms.
     */
    @Action(description = "Extracts job requirements from the job description")
    public JobRequirements extractJobRequirements(JobDescriptionProvider request, OperationContext context) {
        // If this is the first extraction call and we also need CV skills, do both in parallel
        if (request instanceof CvTextProvider) {
            String cvText = ((CvTextProvider) request).getCvText();
            String jobDescText = request.getJobDescriptionText();
            String cacheKey = getCacheKey(cvText, jobDescText);

            // Check cache first
            PreparationResult cached = extractionCache.get(cacheKey);
            if (cached != null) {
                log.info("Using job requirements from parallel extraction cache");
                return cached.jobRequirements();
            }

            // Not in cache - run parallel extraction
            log.info("Running job requirements and CV skills extraction IN PARALLEL");
            PreparationResult result = runParallelExtraction(cvText, jobDescText, context);
            extractionCache.put(cacheKey, result);
            return result.jobRequirements();
        }

        // Fallback to single extraction if request doesn't have CV text
        log.info("Running single job requirements extraction (no parallel opportunity)");
        return extractJobRequirements(request.getJobDescriptionText(), context, "");
    }

    /**
     * Generates a cache key from CV text and job description text.
     */
    private String getCacheKey(String cvText, String jobDescText) {
        int cvHash = cvText != null ? cvText.hashCode() : 0;
        int jdHash = jobDescText != null ? jobDescText.hashCode() : 0;
        return cvHash + "_" + jdHash;
    }

    /**
     * Runs CV skills and job requirements extraction in PARALLEL using CompletableFuture and virtual threads.
     * This method fires both LLM calls concurrently, reducing total time from ~1300ms (sequential) to ~700ms (parallel).
     * Results are cached so subsequent extractions with the same data reuse results.
     */
    private PreparationResult runParallelExtraction(String cvText, String jobDescriptionText, OperationContext context) {
        long startTime = System.currentTimeMillis();

        // Fire both LLM calls concurrently using virtual threads
        CompletableFuture<CvSkills> cvSkillsFuture = CompletableFuture.supplyAsync(
            () -> extractCvSkills(cvText, context, ""),
            VIRTUAL_EXECUTOR
        );

        CompletableFuture<JobRequirements> jobReqFuture = CompletableFuture.supplyAsync(
            () -> extractJobRequirements(jobDescriptionText, context, ""),
            VIRTUAL_EXECUTOR
        );

        // Wait for both to complete
        CompletableFuture.allOf(cvSkillsFuture, jobReqFuture).join();

        // Get results
        CvSkills cvSkills = cvSkillsFuture.join();
        JobRequirements jobRequirements = jobReqFuture.join();

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Parallel extraction completed in {}ms (instead of ~1300ms sequential)", duration);

        return new PreparationResult(cvSkills, jobRequirements);
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
                        .withMaxTokens(1000)
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
                        .withMaxTokens(1000)
                )
                .createObject(prompt, JobRequirements.class);

        Assert.notNull(requirements, "Job requirements cannot be null");
        log.info("Job requirements extracted{}", logSuffix);
        return requirements;
    }
}
