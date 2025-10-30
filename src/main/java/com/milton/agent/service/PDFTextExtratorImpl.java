package com.milton.agent.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PDFTextExtratorImpl implements TextExtractor{
    @Override
    public String extractText(MultipartFile filename) throws IOException {

        try(PDDocument document = PDDocument.load(filename.getInputStream())) {
            if(document.isEncrypted()) {
                throw new IOException("File is encrypted, please decrypt and upload again.");
            }
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
}

