package com.milton.agent.controller;

/**
 * Central place for session attribute keys shared across controllers.
 */
final class SessionAttributes {

    private SessionAttributes() {
    }

    static final String CV_TEXT = "storedCvText";
    static final String CV_NAME = "storedCvName";
    static final String ROLE = "storedRole";
    static final String COMPANY = "storedCompany";
    static final String JOB_DESCRIPTION = "storedJobDescription";
    static final String FIT_SCORE = "storedFitScore";
    static final String FIT_EXPLANATION = "storedFitExplanation";
    static final String UPGRADED_CV = "generatedUpgradedCv";
    static final String UPGRADED_KEYWORDS = "generatedAtsKeywords";
    static final String UPGRADED_SUMMARY = "generatedOptimisationSummary";
    static final String SUGGESTIONS = "generatedSuggestions";
    static final String IMPROVE_SCORE = "generatedImproveScore";
    static final String INTERVIEW_PREP = "generatedInterviewPrep";
}
