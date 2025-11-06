package com.milton.agent.models;

import java.util.List;

public record UpgradedCv(
        String cvText,
        List<String> atsKeywords,
        String optimisationSummary
) {
}
