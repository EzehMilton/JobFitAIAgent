package com.milton.agent.models;

public record SuggestionsRequest(
        String candidateCv,
        String jobDescription,
        int fitScore,
        String fitExplanation
) implements CvTextProvider, JobDescriptionProvider {

    @Override
    public String getCvText() {
        return candidateCv;
    }

    @Override
    public String getJobDescriptionText() {
        return jobDescription;
    }
}