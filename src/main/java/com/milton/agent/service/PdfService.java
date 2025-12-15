package com.milton.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import com.milton.agent.util.TimedOperation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PdfService {

    public byte[] renderPdfFromText(String text) throws IOException {
        try (TimedOperation ignored = TimedOperation.start(log, "PDF rendering");
             PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDType1Font font = PDType1Font.HELVETICA;
            float fontSize = 11f;
            float leading = 1.4f * fontSize;
            float margin = 50f;
            float availableWidth = PDRectangle.LETTER.getWidth() - (margin * 2);
            String sanitizedText = sanitizeForPdf(text);
            List<String> lines = wrapText(sanitizedText, font, fontSize, availableWidth);

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = page.getMediaBox().getHeight() - margin;
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(margin, yPosition);

            for (String line : lines) {
                if (yPosition <= margin) {
                    contentStream.endText();
                    contentStream.close();

                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);

                    yPosition = page.getMediaBox().getHeight() - margin;
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(margin, yPosition);
                }

                String printableLine = line == null ? "" : line;
                contentStream.showText(printableLine);
                contentStream.newLineAtOffset(0, -leading);
                yPosition -= leading;
            }

            contentStream.endText();
            contentStream.close();

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    public String buildFileName(String originalName, String defaultBaseName, String suffix) {
        String baseName = (originalName == null || originalName.isBlank()) ? defaultBaseName : originalName;
        baseName = baseName.replaceAll("\\s+", "-");
        baseName = baseName.replaceAll("[^A-Za-z0-9._-]", "");

        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }

        if (baseName.isBlank()) {
            baseName = defaultBaseName;
        }

        return baseName + suffix;
    }

    private List<String> wrapText(String text, PDType1Font font, float fontSize, float availableWidth) throws IOException {
        List<String> wrappedLines = new ArrayList<>();
        String[] rawLines = text.split("\\R", -1);

        for (String rawLine : rawLines) {
            if (rawLine.isBlank()) {
                wrappedLines.add("");
                continue;
            }

            String[] words = rawLine.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
                float candidateWidth = font.getStringWidth(candidate) / 1000 * fontSize;

                if (candidateWidth > availableWidth && currentLine.length() > 0) {
                    wrappedLines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(candidate);
                }
            }

            wrappedLines.add(currentLine.toString());
        }

        return wrappedLines;
    }

    private String sanitizeForPdf(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");
        normalized = normalized
                .replace('\u2022', '-')
                .replace('\u2023', '-')
                .replace('\u25CF', '-');

        return normalized.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", "");
    }
}
