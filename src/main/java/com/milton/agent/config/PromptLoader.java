package com.milton.agent.config;

import com.milton.agent.exceptions.PromptLoaderException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@Component
public class PromptLoader {

    public String loadPrompt(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new PromptLoaderException("Filename cannot be null or empty");
        }

        String path = "prompts/" + filename;
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PromptLoaderException("Failed to load prompt file: " + path, e);
        }
    }
}

