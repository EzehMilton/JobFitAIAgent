package com.milton.agent.service;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.common.OperationContext;
import com.milton.agent.models.CvRewriteRequest;
import com.milton.agent.models.CvSkills;
import com.milton.agent.models.FitScore;
import com.milton.agent.models.JobFitRequest;
import com.milton.agent.models.JobRequirements;
import com.milton.agent.models.UpgradedCv;
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
}
