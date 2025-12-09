package com.milton.agent.models;

import java.util.List;

public record CareerSuggestions(
        List<String> suggestedTitles,
        List<String> skillClusters,
        List<String> strengths,
        List<String> weaknesses,
        String careerDirection
) {
}