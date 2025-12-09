package com.milton.agent.models;

public record SuggestionsRequest(
        String candidateCv,
        String jobDescription,
        int fitScore,
        String fitExplanation
) {
}