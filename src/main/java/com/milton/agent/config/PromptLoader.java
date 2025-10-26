package com.milton.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
//
//@Component
//public class PromptLoader {
//
//    @Value("classpath:prompts/jobfit-fit-score.txt")
//    private Resource fitScorePromptResource;
//
//    public String loadFitScorePrompt() throws IOException {
//        return Files.readString(fitScorePromptResource.getFile().toPath(), StandardCharsets.UTF_8);
//    }
//}

@Component
public class PromptLoader {
    public String loadPrompt(String filename) {
        try {
            var resource = new ClassPathResource("prompts/" + filename);
            Path path = resource.getFile().toPath();
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt: " + filename, e);
        }
    }
}
