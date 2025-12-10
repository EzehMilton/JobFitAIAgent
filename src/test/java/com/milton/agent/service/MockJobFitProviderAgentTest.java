package com.milton.agent.service;

import com.milton.agent.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MockJobFitProviderAgentTest {

    private MockJobFitProviderAgent mockAgent;

    @BeforeEach
    void setUp() {
        mockAgent = new MockJobFitProviderAgent();
    }

    @Test
    void extractSkillsFromCv_ShouldReturnMockSkills() {
        // Arrange
        JobFitRequest request = new JobFitRequest("fake cv", "fake job desc", true);

        // Act
        CvSkills skills = mockAgent.extractSkillsFromCv(request, null);

        // Assert
        assertNotNull(skills);
        assertNotNull(skills.technicalSkills());
        assertTrue(skills.technicalSkills().contains("Java"));
        assertTrue(skills.technicalSkills().contains("Spring Boot"));
        assertNotNull(skills.professionalSkills());
        assertNotNull(skills.softSkills());
        assertNotNull(skills.qualifications());
    }

    @Test
    void extractJobRequirements_ShouldReturnMockRequirements() {
        // Arrange
        JobFitRequest request = new JobFitRequest("cv", "job desc", false);

        // Act
        JobRequirements requirements = mockAgent.extractJobRequirements(request, null);

        // Assert
        assertNotNull(requirements);
        assertNotNull(requirements.criticalRequirements());
        assertTrue(requirements.criticalRequirements().contains("Leadership"));
        assertNotNull(requirements.importantRequirements());
        assertNotNull(requirements.supportingRequirements());
    }

    @Test
    void calculateFitScore_ShouldReturnRotatingScores() {
        // Arrange
        JobFitRequest request = new JobFitRequest("cv", "job", true);
        CvSkills skills = mockAgent.extractSkillsFromCv(request, null);
        JobRequirements requirements = mockAgent.extractJobRequirements(request, null);
        Set<Integer> seenScores = new HashSet<>();

        // Act - Call multiple times to see rotating behavior
        for (int i = 0; i < 10; i++) {
            FitScore fitScore = mockAgent.calculateFitScore(request, skills, requirements, null);
            assertNotNull(fitScore);
            assertTrue(fitScore.score() >= 0 && fitScore.score() <= 100,
                "Score should be between 0 and 100");
            assertNotNull(fitScore.explanation());
            assertFalse(fitScore.explanation().isEmpty());
            seenScores.add(fitScore.score());
        }

        // Assert - Should see variation in scores (with jitter, we'll see different values)
        assertTrue(seenScores.size() >= 2,
            "Should see different scores across multiple calls due to rotation");
    }

    @Test
    @Disabled
    void calculateFitScore_ShouldProvideAppropriateExplanation() {
        // Arrange
        JobFitRequest request = new JobFitRequest("cv", "job", true);
        CvSkills skills = mockAgent.extractSkillsFromCv(request, null);
        JobRequirements requirements = mockAgent.extractJobRequirements(request, null);

        // Act - Generate multiple scores to test explanations
        for (int i = 0; i < 20; i++) {
            FitScore fitScore = mockAgent.calculateFitScore(request, skills, requirements, null);

            // Assert
            if (fitScore.score() < 40) {
                assertTrue(fitScore.explanation().toLowerCase().contains("weak") ||
                          fitScore.explanation().toLowerCase().contains("limited"),
                    "Low scores should have appropriate explanations");
            } else if (fitScore.score() >= 90) {
                assertTrue(fitScore.explanation().toLowerCase().contains("excellent") ||
                          fitScore.explanation().toLowerCase().contains("clear"),
                    "High scores should have positive explanations");
            }
        }
    }

    @Test
    void rewriteCvForRole_ShouldReturnUpgradedCv() {
        // Arrange
        CvRewriteRequest request = new CvRewriteRequest("original cv", "job desc", 75, "explanation");

        // Act
        UpgradedCv upgradedCv = mockAgent.rewriteCvForRole(request, null);

        // Assert
        assertNotNull(upgradedCv);
        assertNotNull(upgradedCv.cvText());
        assertFalse(upgradedCv.cvText().isEmpty());
        assertTrue(upgradedCv.cvText().contains("ANTHONY EZEH"),
            "Mock CV should contain the mock candidate name");
        assertNotNull(upgradedCv.atsKeywords());
        assertFalse(upgradedCv.atsKeywords().isEmpty());
        assertTrue(upgradedCv.atsKeywords().contains("ATS optimisation"));
        assertNotNull(upgradedCv.optimisationSummary());
    }

    @Test
    void generateCareerSuggestions_ShouldReturnValidSuggestions() {
        // Arrange
        SuggestionsRequest request = new SuggestionsRequest("cv", "job", 35, "explanation");
        CvSkills skills = mockAgent.extractSkillsForSuggestions(request, null);
        JobRequirements requirements = mockAgent.extractRequirementsForSuggestions(request, null);

        // Act
        CareerSuggestions suggestions = mockAgent.generateCareerSuggestions(request, skills, requirements, null);

        // Assert
        assertNotNull(suggestions);
        assertNotNull(suggestions.suggestedTitles());
        assertFalse(suggestions.suggestedTitles().isEmpty());
        assertTrue(suggestions.suggestedTitles().contains("Associate Product Manager"));
        assertNotNull(suggestions.skillClusters());
        assertNotNull(suggestions.strengths());
        assertNotNull(suggestions.weaknesses());
        assertNotNull(suggestions.careerDirection());
        assertFalse(suggestions.careerDirection().isEmpty());
    }

    @Test
    void generateImproveScore_ShouldReturnValidRecommendations() {
        // Arrange
        ImproveScoreRequest request = new ImproveScoreRequest("cv", "job", 60, "explanation");
        CvSkills skills = mockAgent.extractSkillsForImprove(request, null);
        JobRequirements requirements = mockAgent.extractRequirementsForImprove(request, null);

        // Act
        ImproveScore improveScore = mockAgent.generateImproveScore(request, skills, requirements, null);

        // Assert
        assertNotNull(improveScore);
        assertNotNull(improveScore.gaps());
        assertFalse(improveScore.gaps().isEmpty());
        assertTrue(improveScore.gaps().get(0).contains("leadership") ||
                  improveScore.gaps().get(0).contains("cloud"),
            "Gaps should mention common missing elements");
        assertNotNull(improveScore.alignmentIssues());
        assertNotNull(improveScore.keywordSuggestions());
        assertTrue(improveScore.keywordSuggestions().contains("AWS"));
        assertNotNull(improveScore.courseRecommendations());
        assertFalse(improveScore.courseRecommendations().isEmpty());
        assertTrue(improveScore.courseRecommendations().stream()
                .anyMatch(course -> course.contains("AWS") || course.contains("Scrum")),
            "Course recommendations should include relevant certifications");
        assertNotNull(improveScore.achievementAdvice());
    }

    @Test
    void generateInterviewPrep_ShouldReturnValidPrepContent() {
        // Arrange
        InterviewPrepRequest request = new InterviewPrepRequest("cv", "job", 92, "explanation");
        CvSkills skills = mockAgent.extractSkillsForInterviewPrep(request, null);
        JobRequirements requirements = mockAgent.extractRequirementsForInterviewPrep(request, null);

        // Act
        InterviewPrep interviewPrep = mockAgent.generateInterviewPrep(request, skills, requirements, null);

        // Assert
        assertNotNull(interviewPrep);
        assertNotNull(interviewPrep.pitch());
        assertFalse(interviewPrep.pitch().isEmpty());
        assertTrue(interviewPrep.pitch().length() > 20,
            "Pitch should be a reasonable length");
        assertNotNull(interviewPrep.questions());
        assertFalse(interviewPrep.questions().isEmpty());
        assertTrue(interviewPrep.questions().size() >= 3,
            "Should provide multiple interview questions");
        assertNotNull(interviewPrep.starStories());
        assertFalse(interviewPrep.starStories().isEmpty());
        assertNotNull(interviewPrep.prepAdvice());
        assertTrue(interviewPrep.prepAdvice().contains("STAR") ||
                  interviewPrep.prepAdvice().contains("company"),
            "Prep advice should mention preparation strategies");
    }

    @Test
    void extractSkillsForSuggestions_ShouldReturnConsistentSkills() {
        // Arrange
        SuggestionsRequest request = new SuggestionsRequest("cv", "job", 35, "explanation");

        // Act
        CvSkills skills1 = mockAgent.extractSkillsForSuggestions(request, null);
        CvSkills skills2 = mockAgent.extractSkillsForSuggestions(request, null);

        // Assert
        assertNotNull(skills1);
        assertNotNull(skills2);
        assertEquals(skills1.technicalSkills(), skills2.technicalSkills(),
            "Mock skills should be consistent across calls");
    }

    @Test
    void extractRequirementsForSuggestions_ShouldReturnConsistentRequirements() {
        // Arrange
        SuggestionsRequest request = new SuggestionsRequest("cv", "job", 35, "explanation");

        // Act
        JobRequirements req1 = mockAgent.extractRequirementsForSuggestions(request, null);
        JobRequirements req2 = mockAgent.extractRequirementsForSuggestions(request, null);

        // Assert
        assertNotNull(req1);
        assertNotNull(req2);
        assertEquals(req1.criticalRequirements(), req2.criticalRequirements(),
            "Mock requirements should be consistent across calls");
    }

    @Test
    void allMethods_ShouldHandleNullOperationContext() {
        // This test ensures all methods can handle null OperationContext
        // which is the case in our mock implementation

        // Arrange
        JobFitRequest jobFitRequest = new JobFitRequest("cv", "job", true);
        CvRewriteRequest cvRewriteRequest = new CvRewriteRequest("cv", "job", 75, "exp");
        SuggestionsRequest suggestionsRequest = new SuggestionsRequest("cv", "job", 35, "exp");
        ImproveScoreRequest improveScoreRequest = new ImproveScoreRequest("cv", "job", 60, "exp");
        InterviewPrepRequest interviewPrepRequest = new InterviewPrepRequest("cv", "job", 92, "exp");

        // Act & Assert - None should throw exceptions
        assertDoesNotThrow(() -> {
            mockAgent.extractSkillsFromCv(jobFitRequest, null);
            mockAgent.extractJobRequirements(jobFitRequest, null);
            mockAgent.calculateFitScore(jobFitRequest, null, null, null);
            mockAgent.rewriteCvForRole(cvRewriteRequest, null);
            mockAgent.generateCareerSuggestions(suggestionsRequest, null, null, null);
            mockAgent.extractSkillsForSuggestions(suggestionsRequest, null);
            mockAgent.extractRequirementsForSuggestions(suggestionsRequest, null);
            mockAgent.generateImproveScore(improveScoreRequest, null, null, null);
            mockAgent.extractSkillsForImprove(improveScoreRequest, null);
            mockAgent.extractRequirementsForImprove(improveScoreRequest, null);
            mockAgent.generateInterviewPrep(interviewPrepRequest, null, null, null);
            mockAgent.extractSkillsForInterviewPrep(interviewPrepRequest, null);
            mockAgent.extractRequirementsForInterviewPrep(interviewPrepRequest, null);
        }, "Mock agent methods should handle null OperationContext");
    }

    @Test
    void upgradedCv_ShouldContainStructuredSections() {
        // Arrange
        CvRewriteRequest request = new CvRewriteRequest("cv", "job", 80, "exp");

        // Act
        UpgradedCv upgradedCv = mockAgent.rewriteCvForRole(request, null);

        // Assert
        String cvText = upgradedCv.cvText();
        assertTrue(cvText.contains("SUMMARY"), "CV should have a summary section");
        assertTrue(cvText.contains("SKILLS") || cvText.contains("CORE SKILLS"),
            "CV should have a skills section");
        assertTrue(cvText.contains("EXPERIENCE"), "CV should have an experience section");
        assertTrue(cvText.contains("EDUCATION"), "CV should have an education section");
    }

    @Test
    @Disabled
    void fitScoreExplanations_ShouldMatchScoreRange() {
        // This test verifies that explanations are contextually appropriate
        JobFitRequest request = new JobFitRequest("cv", "job", true);

        // Generate many scores to cover different ranges
        for (int i = 0; i < 50; i++) {
            FitScore fitScore = mockAgent.calculateFitScore(request, null, null, null);
            String explanation = fitScore.explanation();
            int score = fitScore.score();

            // Assert explanations make sense for their score range
            assertNotNull(explanation);
            assertFalse(explanation.isEmpty());

            if (score < 50) {
                // Low scores should indicate issues or gaps
                assertTrue(explanation.toLowerCase().contains("limited") ||
                          explanation.toLowerCase().contains("gaps") ||
                          explanation.toLowerCase().contains("weak"),
                    "Low score explanations should indicate gaps");
            } else if (score >= 85) {
                // High scores should be positive
                assertTrue(explanation.toLowerCase().contains("good") ||
                          explanation.toLowerCase().contains("strong") ||
                          explanation.toLowerCase().contains("excellent"),
                    "High score explanations should be positive");
            }
        }
    }
}