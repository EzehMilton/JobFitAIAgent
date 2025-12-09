package com.milton.agent.models;

import java.util.List;

public record InterviewPrep(
        String pitch,
        List<String> questions,
        List<String> starStories,
        String prepAdvice
) {
}