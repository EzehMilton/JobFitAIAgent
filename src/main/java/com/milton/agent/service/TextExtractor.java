package com.milton.agent.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface TextExtractor {
    String extractText(MultipartFile filename) throws IOException;
}
