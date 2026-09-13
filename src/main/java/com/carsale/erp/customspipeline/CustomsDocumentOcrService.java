package com.carsale.erp.customspipeline;

import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.OcrClient;
import com.carsale.erp.shared.ocr.OcrImagePreparer;
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

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

@Service
public class CustomsDocumentOcrService {

    private static final Logger log = LoggerFactory.getLogger(CustomsDocumentOcrService.class);

    private final OcrImagePreparer imagePreparer;
    private final String language;

    public CustomsDocumentOcrService(
            OcrImagePreparer imagePreparer,
            @Value("${app.ocr.clearance.language:eng}") String language
    ) {
        this.imagePreparer = imagePreparer;
        this.language = language == null || language.trim().isEmpty() ? "eng" : language.trim();
    }

    public AuctionParseResult parsePage(MultipartFile file, DocumentParser documentParser, String provider) {
        AuctionParseResult failed = new AuctionParseResult();
        if (file == null || file.isEmpty()) {
            failed.setSuccess(false);
            failed.setMessage("Please upload clearance document.");
            return failed;
        }
        File temp = null;
        try {
            temp = File.createTempFile("clearance-page-", suffix(file.getOriginalFilename()));
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            AuctionParseResult parsed = imagePreparer.readDocumentText(
                        temp, file.getOriginalFilename(), language, provider, documentParser);
            log.info("OCR text:\n{}", parsed.getRawText());
            return parsed;
        } catch (Throwable ex) {
            log.error("Clearance OCR failed", ex);
            failed.setSuccess(false);
            String detail = ex.getMessage();
            if (OcrClient.isUserFacingError(detail)) {
                failed.setMessage(detail + " You can fill the form manually.");
            } else {
                failed.setMessage("Could not read page. You can fill the form manually.");
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
