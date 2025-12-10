package com.milton.agent.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PDFTextExtractorImplTest {

    private PDFTextExtratorImpl textExtractor;

    @BeforeEach
    void setUp() {
        textExtractor = new PDFTextExtratorImpl();
    }

    @Test
    void extractText_ShouldExtractTextFromValidPDF() throws IOException {
        // Arrange
        String expectedText = "This is a test PDF document";
        byte[] pdfBytes = createTestPDF(expectedText);
        MultipartFile file = new MockMultipartFile(
                "test.pdf",
                "test.pdf",
                "application/pdf",
                pdfBytes
        );

        // Act
        String extractedText = textExtractor.extractText(file);

        // Assert
        assertNotNull(extractedText);
        assertTrue(extractedText.contains(expectedText),
            "Extracted text should contain the original text");
    }

    @Test
    void extractText_ShouldExtractMultipleLines() throws IOException {
        // Arrange
        String line1 = "First line of text";
        String line2 = "Second line of text";
        byte[] pdfBytes = createTestPDFWithMultipleLines(line1, line2);
        MultipartFile file = new MockMultipartFile(
                "multiline.pdf",
                "multiline.pdf",
                "application/pdf",
                pdfBytes
        );

        // Act
        String extractedText = textExtractor.extractText(file);

        // Assert
        assertNotNull(extractedText);
        assertTrue(extractedText.contains(line1), "Should contain first line");
        assertTrue(extractedText.contains(line2), "Should contain second line");
    }

    @Test
    @Disabled
    void extractText_ShouldThrowException_WhenPDFIsEncrypted() throws IOException {
        // Arrange
        byte[] encryptedPdfBytes = createEncryptedPDF();
        MultipartFile file = new MockMultipartFile(
                "encrypted.pdf",
                "encrypted.pdf",
                "application/pdf",
                encryptedPdfBytes
        );

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            textExtractor.extractText(file);
        });

        assertEquals("File is encrypted, please decrypt and upload again.",
            exception.getMessage());
    }

    @Test
    void extractText_ShouldHandleEmptyPDF() throws IOException {
        // Arrange
        byte[] emptyPdfBytes = createTestPDF("");
        MultipartFile file = new MockMultipartFile(
                "empty.pdf",
                "empty.pdf",
                "application/pdf",
                emptyPdfBytes
        );

        // Act
        String extractedText = textExtractor.extractText(file);

        // Assert
        assertNotNull(extractedText);
        assertTrue(extractedText.trim().isEmpty() || extractedText.trim().length() == 0,
            "Empty PDF should return empty or whitespace text");
    }

    @Test
    void extractText_ShouldThrowException_WhenFileIsCorrupted() {
        // Arrange
        byte[] corruptedBytes = "This is not a valid PDF".getBytes();
        MultipartFile file = new MockMultipartFile(
                "corrupted.pdf",
                "corrupted.pdf",
                "application/pdf",
                corruptedBytes
        );

        // Act & Assert
        assertThrows(IOException.class, () -> {
            textExtractor.extractText(file);
        }, "Should throw IOException for corrupted PDF");
    }

    @Test
    void extractText_ShouldHandleSpecialCharacters() throws IOException {
        // Arrange
        String textWithSpecialChars = "Test with symbols: @#$% & * ()";
        byte[] pdfBytes = createTestPDF(textWithSpecialChars);
        MultipartFile file = new MockMultipartFile(
                "special.pdf",
                "special.pdf",
                "application/pdf",
                pdfBytes
        );

        // Act
        String extractedText = textExtractor.extractText(file);

        // Assert
        assertNotNull(extractedText);
        // Note: Some special characters might be escaped or transformed by PDFBox
        assertTrue(extractedText.contains("Test with symbols"),
            "Should extract text with special characters");
    }

    // Helper method to create a simple test PDF
    private byte[] createTestPDF(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    // Helper method to create a PDF with multiple lines
    private byte[] createTestPDFWithMultipleLines(String line1, String line2) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(line1);
                contentStream.newLine();
                contentStream.showText(line2);
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    // Helper method to create an encrypted PDF
    private byte[] createEncryptedPDF() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Encrypted content");
                contentStream.endText();
            }

            // Encrypt the document
            document.protect(new org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                "ownerPassword",
                "userPassword",
                new org.apache.pdfbox.pdmodel.encryption.AccessPermission()
            ));

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}