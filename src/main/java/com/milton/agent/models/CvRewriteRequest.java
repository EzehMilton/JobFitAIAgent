package com.milton.agent.models;

public record CvRewriteRequest(
        String originalCv,
        String jobDescription,
        int fitScore,
        String fitExplanation
) {
}
