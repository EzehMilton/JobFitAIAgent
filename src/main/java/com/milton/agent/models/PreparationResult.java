package com.milton.agent.models;

/**
 * Container for parallel extraction results.
 * Holds both CV skills and job requirements extracted concurrently.
 */
public record PreparationResult(
    CvSkills cvSkills,
    JobRequirements jobRequirements
) {}
