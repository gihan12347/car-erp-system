package com.carsale.erp.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.carsale.erp.dto.AuctionParseResult;

@Service
public class AuctionSheetOcrService {

    private static final Logger log = LoggerFactory.getLogger(AuctionSheetOcrService.class);

    private final AuctionSheetParser parser;
    private final OcrImagePreparer imagePreparer;
    private final String language;

    public AuctionSheetOcrService(
            AuctionSheetParser parser,
            OcrImagePreparer imagePreparer,
            @Value("${app.ocr.ocrspace.language:jpn}") String language
    ) {
        this.parser = parser;
        this.imagePreparer = imagePreparer;
        this.language = language == null || language.trim().isEmpty() ? "jpn" : language.trim();
    }

    public AuctionParseResult parseSheet(MultipartFile file) {
        return parseSheet(file, null);
    }

    public AuctionParseResult parseSheet(MultipartFile file, String provider) {
        AuctionParseResult failed = new AuctionParseResult();
        if (file == null || file.isEmpty()) {
            failed.setSuccess(false);
            failed.setMessage("Please upload an auction sheet image or PDF.");
            return failed;
        }

        File temp = null;
        try {
            temp = File.createTempFile("auction-sheet-", suffix(file.getOriginalFilename()));
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            String raw = imagePreparer.readDocumentText(temp, file.getOriginalFilename(), language, provider);
            log.info("Auction sheet OCR text:\n{}", raw);
            AuctionParseResult parsed = parser.parse(raw);
            parsed.setRawText(raw);
            log.info("Auction sheet mapped fields: {}", parsed.getFields());
            return parsed;
        } catch (Throwable ex) {
            log.error("Auction sheet OCR failed", ex);
            failed.setSuccess(false);
            String detail = ex.getMessage();
            if (OcrClient.isUserFacingError(detail)) {
                failed.setMessage(detail + " You can fill the form manually.");
            } else {
                failed.setMessage("Could not read the auction sheet. You can fill the form manually.");
            }
            return failed;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String suffix(String name) {
        if (name == null || name.lastIndexOf('.') < 0) {
            return ".img";
        }
        return name.substring(name.lastIndexOf('.'));
    }
}
