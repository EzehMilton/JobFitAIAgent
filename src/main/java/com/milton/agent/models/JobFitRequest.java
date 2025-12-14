package com.milton.agent.models;

public record JobFitRequest(String CvText, String JobDescription, boolean QuickResponse)
        implements CvTextProvider, JobDescriptionProvider {

    @Override
    public String getCvText() {
        return CvText;
    }

    @Override
    public String getJobDescriptionText() {
        return JobDescription;
    }
}
