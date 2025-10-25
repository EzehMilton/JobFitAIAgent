package com.milton.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Component
public class PromptLoader {

    @Value("classpath:prompts/jobfit-fit-score.prompt")
    private Resource fitScorePromptResource;

    public String loadFitScorePrompt() throws IOException {
        return Files.readString(fitScorePromptResource.getFile().toPath(), StandardCharsets.UTF_8);
    }
}
