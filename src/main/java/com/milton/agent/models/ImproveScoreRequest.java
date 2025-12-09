package com.milton.agent.models;

public record ImproveScoreRequest(
        String candidateCv,
        String jobDescription,
        int fitScore,
        String fitExplanation
) {
}