package com.milton.agent.models;

import java.util.List;

public record JobRequirements(
        List<String> criticalRequirements,
        List<String> importantRequirements,
        List<String> supportingRequirements
) {}
