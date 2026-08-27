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
public class ClearanceOcrService {

    private static final Logger log = LoggerFactory.getLogger(ClearanceOcrService.class);

    private final ClearanceParser parser;
    private final OcrSpaceClient ocrSpaceClient;
    private final OcrImagePreparer imagePreparer;
    private final String language;

    public ClearanceOcrService(
            ClearanceParser parser,
            OcrSpaceClient ocrSpaceClient,
            OcrImagePreparer imagePreparer,
            @Value("${app.ocr.clearance.language:eng}") String language
    ) {
        this.parser = parser;
        this.ocrSpaceClient = ocrSpaceClient;
        this.imagePreparer = imagePreparer;
        this.language = language == null || language.trim().isEmpty() ? "eng" : language.trim();
    }

    public AuctionParseResult parsePage(MultipartFile file, int page) {
        AuctionParseResult failed = new AuctionParseResult();
        if (file == null || file.isEmpty()) {
            failed.setSuccess(false);
            failed.setMessage("Please upload clearance document page " + page + ".");
            return failed;
        }
        if (page < 1 || page > 3) {
            failed.setSuccess(false);
            failed.setMessage("Invalid page number. Use page 1, 2, or 3.");
            return failed;
        }

        File temp = null;
        try {
            temp = File.createTempFile("clearance-page" + page + "-", suffix(file.getOriginalFilename()));
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            String raw = imagePreparer.readDocumentText(temp, file.getOriginalFilename(), ocrSpaceClient, language);
            log.info("Clearance page {} OCR text:\n{}", page, raw);
            AuctionParseResult parsed;
            if (page == 1) {
                parsed = parser.parsePage1(raw);
            } else if (page == 2) {
                parsed = parser.parsePage2(raw);
            } else {
                parsed = parser.parsePage3(raw);
            }
            parsed.setRawText(raw);
            return parsed;
        } catch (Throwable ex) {
            log.error("Clearance OCR failed for page {}", page, ex);
            failed.setSuccess(false);
            String detail = ex.getMessage();
            if (detail != null && (detail.contains("OCR.space") || detail.contains("1.5 MB"))) {
                failed.setMessage(detail + " You can fill the form manually.");
            } else {
                failed.setMessage("Could not read page " + page + ". You can fill the form manually.");
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
