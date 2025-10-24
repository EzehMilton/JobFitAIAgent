package com.milton.agent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.milton.agent.models.CvSkills;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.models.JobRequirements;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
@Agent(name = "job-fit-provider",
        description = "Assesses how well a CV matches a job description and provides a fit score",
        version = "1.0.0",
        beanName = "jobFitProviderAgent")
public class JobFitProviderAgent {

    @Action
    public CvSkills extractSkillsFromCv(JobFitRequest request, OperationContext context) {
        CvSkills cvSkills = context.ai()
                .withDefaultLlm()
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
        JobRequirements requirements = context.ai()
                .withDefaultLlm()
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
        FitScore fitScore = context.ai()
                .withDefaultLlm()
                .createObjectIfPossible("""
                                Compare these CV skills: %s
                                with these job requirements: %s.
                                
                                Compute a fit score from 0 to 100, where 100 is a perfect match.
                                Provide a brief explanation of the score.
                                Create a FitScore object with the score and explanation.
                                """.formatted(cvSkills.skills(), jobRequirements.requirements()),
                        FitScore.class);
        Assert.notNull(fitScore, "Fit score cannot be null");
        return fitScore;
    }
}
