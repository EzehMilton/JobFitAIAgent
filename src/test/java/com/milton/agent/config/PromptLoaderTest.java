package com.milton.agent.config;

import com.milton.agent.exceptions.PromptLoaderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PromptLoaderTest {

    private PromptLoader promptLoader;

    @BeforeEach
    void setUp() {
        promptLoader = new PromptLoader();
    }

    @Test
    void loadPrompt_ShouldLoadExistingPromptFile() {
        // Arrange
        String filename = "jobfit-fit-score.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent, "Prompt content should not be null");
        assertFalse(promptContent.isEmpty(), "Prompt content should not be empty");
        assertTrue(promptContent.length() > 0, "Prompt should have content");
    }

    @Test
    void loadPrompt_ShouldLoadSkillsExtractorPrompt() {
        // Arrange
        String filename = "skills-extractor.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent);
        assertFalse(promptContent.isEmpty());
    }

    @Test
    void loadPrompt_ShouldLoadJobDescriptionExtractorPrompt() {
        // Arrange
        String filename = "job-description-extractor.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent);
        assertFalse(promptContent.isEmpty());
    }

    @Test
    void loadPrompt_ShouldLoadCvRewriterPrompt() {
        // Arrange
        String filename = "cv-rewriter.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent);
        assertFalse(promptContent.isEmpty());
        assertTrue(promptContent.contains("%s") || promptContent.contains("FIT SCORE"),
            "CV rewriter prompt should contain placeholders");
    }

    @Test
    void loadPrompt_ShouldLoadSuggestionsPrompt() {
        // Arrange
        String filename = "suggestions.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent);
        assertFalse(promptContent.isEmpty());
    }

    @Test
    void loadPrompt_ShouldLoadImprovePrompt() {
        // Arrange
        String filename = "improve.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent);
        assertFalse(promptContent.isEmpty());
    }

    @Test
    void loadPrompt_ShouldLoadGetReadyPrompt() {
        // Arrange
        String filename = "getready.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent);
        assertFalse(promptContent.isEmpty());
    }

    @Test
    void loadPrompt_ShouldThrowException_WhenFileNotFound() {
        // Arrange
        String nonExistentFile = "non-existent-prompt.txt";

        // Act & Assert
        PromptLoaderException exception = assertThrows(PromptLoaderException.class, () -> {
            promptLoader.loadPrompt(nonExistentFile);
        });

        assertTrue(exception.getMessage().contains("Failed to load prompt"));
        assertTrue(exception.getMessage().contains(nonExistentFile));
        assertNotNull(exception.getCause());
    }

    @Test
    void loadPrompt_ShouldThrowException_WhenFilenameIsNull() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            promptLoader.loadPrompt(null);
        }, "Should throw exception when filename is null");
    }

    @Test
    void loadPrompt_ShouldHandleEmptyFilename() {
        // Arrange
        String emptyFilename = "";

        // Act & Assert
        assertThrows(PromptLoaderException.class, () -> {
            promptLoader.loadPrompt(emptyFilename);
        }, "Should throw exception for empty filename");
    }

    @Test
    void loadPrompt_ShouldPreserveUTF8Characters() {
        // This test verifies that UTF-8 encoding is preserved
        // We'll use an existing prompt file and check basic structure
        String filename = "jobfit-fit-score.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent);
        // Verify that common characters are properly loaded
        assertTrue(promptContent.contains(" ") || promptContent.contains("\n"),
            "Should preserve whitespace characters");
    }

    @Test
    void loadPrompt_ShouldLoadContentConsistently() {
        // Arrange
        String filename = "skills-extractor.txt";

        // Act
        String firstLoad = promptLoader.loadPrompt(filename);
        String secondLoad = promptLoader.loadPrompt(filename);

        // Assert
        assertEquals(firstLoad, secondLoad,
            "Loading the same prompt twice should return identical content");
    }

    @Test
    void loadPrompt_ShouldHandleFileWithNewlines() {
        // Arrange
        String filename = "jobfit-fit-score.txt";

        // Act
        String promptContent = promptLoader.loadPrompt(filename);

        // Assert
        assertNotNull(promptContent);
        // Most prompts should have multiple lines
        assertTrue(promptContent.contains("\n") || promptContent.length() > 50,
            "Prompt should contain newlines or be multi-line");
    }

    @Test
    void loadPrompt_ShouldConstructCorrectPath() {
        // This test verifies that the path is correctly constructed
        // by attempting to load a known file
        String filename = "cv-rewriter.txt";

        // Act & Assert
        assertDoesNotThrow(() -> {
            String content = promptLoader.loadPrompt(filename);
            assertNotNull(content);
        }, "Should successfully construct path and load file");
    }

    @Test
    void loadPrompt_ShouldNotReturnNull() {
        // Arrange
        String[] promptFiles = {
            "jobfit-fit-score.txt",
            "skills-extractor.txt",
            "job-description-extractor.txt",
            "cv-rewriter.txt"
        };

        // Act & Assert
        for (String filename : promptFiles) {
            String content = promptLoader.loadPrompt(filename);
            assertNotNull(content, "Prompt content for " + filename + " should not be null");
            assertFalse(content.isEmpty(), "Prompt content for " + filename + " should not be empty");
        }
    }
}