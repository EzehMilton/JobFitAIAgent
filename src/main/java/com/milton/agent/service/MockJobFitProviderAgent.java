package com.milton.agent.service;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.common.OperationContext;
import com.milton.agent.models.CareerSuggestions;
import com.milton.agent.models.CvRewriteRequest;
import com.milton.agent.models.CvSkills;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.ImproveScore;
import com.milton.agent.models.ImproveScoreRequest;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.models.JobRequirements;
import com.milton.agent.models.InterviewPrep;
import com.milton.agent.models.InterviewPrepRequest;
import com.milton.agent.models.SuggestionsRequest;
import com.milton.agent.models.UpgradedCv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Profile("dev")
@Agent(
        name = "job-fit-provider-mock",
        description = "Mock agent for local dev mode (no LLM calls, returns static data)",
        version = "1.0.0"
)
public class MockJobFitProviderAgent {

    private static final int[] CATEGORICAL_SCORES = new int[]{35, 60, 80, 95};
    private static final AtomicInteger SCORE_CURSOR = new AtomicInteger(0);

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
    public FitScore calculateFitScore(JobFitRequest request, CvSkills cvSkills, JobRequirements jobRequirements, OperationContext context) {
        log.info("[DEV MOCK] Returning mock fit score");

        int score = nextRotatingScore();
        String explanation = switch (score) {
            case 35 -> "Weak match: Limited overlap with leadership and cloud requirements; highlight transferable projects.";
            case 60 -> "Partial match: Solid backend fundamentals but gaps in cloud depth and team leadership.";
            case 80 -> "Good match: Strong alignment on Java/Spring; add examples of scaling and AWS usage to strengthen.";
            default -> "Excellent match: Clear overlap with required stack and leadership; ensure achievements stay quantified.";
        };

        return new FitScore(score, explanation);
    }

    @AchievesGoal(description = "Mocked CV rewrite for dev mode")
    @Action
    public UpgradedCv rewriteCvForRole(CvRewriteRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning mock upgraded CV");
        return new UpgradedCv(
                """
                ANTHONY EZEH
                Product Manager | AI & Talent Platforms

                SUMMARY
                Product leader leveraging data-driven experimentation to build inclusive hiring workflows. Aligns stakeholder goals, ships AI-assisted candidate experiences, and drives measurable improvements in funnel quality.

                CORE SKILLS
                - Product strategy, backlog prioritisation, stakeholder alignment
                - Agile delivery (Scrum/Kanban), roadmap storytelling, experiment design
                - HR tech analytics, ATS optimisation, generative AI copilots

                EXPERIENCE
                Senior Product Manager, TalentFlow AI (2020–Present)
                • Shipped Role Readiness scorer that lifted qualified applicants by 27% using ATS keyword mapping.
                • Partnered with data science to launch GPT résumé rewrite assistant, sustaining 92% CSAT.
                • Instituted OKR-led discovery sprints synchronising design, engineering, and GTM teams.

                Product Manager, CareerLaunch Labs (2016–2020)
                • Owned insights dashboard used by 1,200+ recruiters to surface pipeline gaps in real time.
                • Grew experimentation cadence 35%, shortening feedback loops on candidate journeys.
                • Enabled enterprise rollout across regulated sectors by codifying compliance workflows.

                EDUCATION & CERTIFICATIONS
                MBA, Strategy & Analytics – Northwestern Kellogg
                BS, Information Systems – University of Nottingham
                Certified Scrum Product Owner (CSPO)

                """,
                List.of("ATS optimisation", "Generative AI", "Stakeholder alignment", "Experiment design"),
                "Emphasised AI-enabled hiring impact, highlighted ATS optimisation wins, and tightened bullet outcomes."
        );
    }

    @AchievesGoal(description = "Mocked career suggestions for low-score entries")
    @Action
    public CareerSuggestions generateCareerSuggestions(SuggestionsRequest request,
                                                       CvSkills cvSkills,
                                                       JobRequirements jobRequirements,
                                                       OperationContext context) {
        log.info("[DEV MOCK] Returning mock career suggestions");
        return new CareerSuggestions(
                List.of("Associate Product Manager", "Business Analyst", "Product Ops Specialist"),
                List.of("Product discovery basics", "Stakeholder communication", "Data storytelling"),
                List.of("Clear communication", "Comfort with data", "Cross-functional collaboration"),
                List.of("Limited launch ownership", "Shallow cloud exposure"),
                "Target associate/analyst roles in tech or consulting while building launch and cloud experience through side projects and certifications."
        );
    }

    @Action
    public CvSkills extractSkillsForSuggestions(SuggestionsRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning mock skills for suggestions");
        return extractSkillsFromCv(null, context);
    }

    @Action
    public JobRequirements extractRequirementsForSuggestions(SuggestionsRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning mock job requirements for suggestions");
        return extractJobRequirements(null, context);
    }

    @AchievesGoal(description = "Mocked improvement recommendations for mid scores")
    @Action
    public ImproveScore generateImproveScore(ImproveScoreRequest request,
                                             CvSkills cvSkills,
                                             JobRequirements jobRequirements,
                                             OperationContext context) {
        log.info("[DEV MOCK] Returning mock improve score data");
        return new ImproveScore(
                List.of("Add concrete leadership examples", "Show cloud migration involvement", "Quantify delivery outcomes"),
                List.of("CV lacks agile delivery metrics", "JD stresses AWS depth"),
                List.of("AWS", "Stakeholder management", "Roadmapping"),
                List.of("AWS Certified Cloud Practitioner", "Scrum.org PSM I", "Udacity Cloud DevOps Nanodegree"),
                "Rewrite achievements with metrics, add a cloud-focused project, and weave in agile ceremonies you lead."
        );
    }

    @Action
    public CvSkills extractSkillsForImprove(ImproveScoreRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning mock skills for improve");
        return extractSkillsFromCv(null, context);
    }

    @Action
    public JobRequirements extractRequirementsForImprove(ImproveScoreRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning mock requirements for improve");
        return extractJobRequirements(null, context);
    }

    @AchievesGoal(description = "Mocked interview prep for high scores")
    @Action
    public InterviewPrep generateInterviewPrep(InterviewPrepRequest request,
                                               CvSkills cvSkills,
                                               JobRequirements jobRequirements,
                                               OperationContext context) {
        log.info("[DEV MOCK] Returning mock interview prep");
        return new InterviewPrep(
                "I lead backend teams to ship scalable services; excited to bring my Java/Spring and leadership chops here.",
                List.of("Describe a time you improved service reliability.", "How do you mentor juniors?", "Tell us about a difficult stakeholder."),
                List.of("Reduced API latency by 30% with caching", "Led migration to AWS", "Introduced code review guild"),
                "Review the company's stack, prepare STAR stories on scale/reliability, and draft thoughtful questions on team culture."
        );
    }

    @Action
    public CvSkills extractSkillsForInterviewPrep(InterviewPrepRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning mock skills for interview prep");
        return extractSkillsFromCv(null, context);
    }

    @Action
    public JobRequirements extractRequirementsForInterviewPrep(InterviewPrepRequest request, OperationContext context) {
        log.info("[DEV MOCK] Returning mock requirements for interview prep");
        return extractJobRequirements(null, context);
    }

    private int nextRotatingScore() {
        int idx = Math.abs(SCORE_CURSOR.getAndIncrement() % CATEGORICAL_SCORES.length);
        // Add small jitter within band to avoid identical repeats
        int base = CATEGORICAL_SCORES[idx];
        int jitter = ThreadLocalRandom.current().nextInt(-2, 3);
        int candidate = Math.max(0, Math.min(100, base + jitter));
        return candidate;
    }
}
