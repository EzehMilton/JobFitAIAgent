package com.milton.agent.models;

import java.util.List;

public record CvSkills(
        List<String> technicalSkills,
        List<String> professionalSkills,
        List<String> softSkills,
        List<String> qualifications
) {}
