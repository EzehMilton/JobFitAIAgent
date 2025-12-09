package com.milton.agent.models;

public record InterviewPrepRequest(
        String candidateCv,
        String jobDescription,
        int fitScore,
        String fitExplanation
) {
}