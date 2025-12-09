package com.milton.agent.models;

import java.util.List;

public record ImproveScore(
        List<String> gaps,
        List<String> alignmentIssues,
        List<String> keywordSuggestions,
        List<String> courseRecommendations,
        String achievementAdvice
) {
}