package com.milton.agent.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for validating uploaded files.
 * Provides helper methods to check if a MultipartFile is a valid PDF.
 */
public final class FileValidationUtil {

    // Private constructor to prevent instantiation
    private FileValidationUtil() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Checks whether the provided MultipartFile is a valid PDF file.
     *
     * @param file MultipartFile uploaded by the user
     * @return true if the file is a PDF, false otherwise
     */
    public static boolean isPdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // Quick checks on file name and MIME type
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            return false;
        }

        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return false;
        }

        // Deep check: verify the PDF header magic bytes (%PDF-)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[5];
            int read = is.read(header);
            if (read < 5) {
                return false;
            }
            String headerStr = new String(header, StandardCharsets.US_ASCII);
            return headerStr.equals("%PDF-");
        } catch (IOException e) {
            // Log and treat as an invalid file
            return false;
        }
    }
}
