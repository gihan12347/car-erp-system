package com.carsale.erp.importpipeline.standards;

import com.carsale.erp.shared.document.document.StandardsCertificateDoc;
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
public class StandardsCertificateOcrService {

    private static final Logger log = LoggerFactory.getLogger(StandardsCertificateOcrService.class);

    private final StandardsCertificateDoc parser;
    private final OcrImagePreparer imagePreparer;
    private final String language;

    public StandardsCertificateOcrService(
            StandardsCertificateDoc parser,
            OcrImagePreparer imagePreparer,
            @Value("${app.ocr.clearance.language:eng}") String language
    ) {
        this.parser = parser;
        this.imagePreparer = imagePreparer;
        this.language = language == null || language.trim().isEmpty() ? "eng" : language.trim();
    }

    public AuctionParseResult parseDocument(MultipartFile file, String provider) {
        if (file == null || file.isEmpty()) {
            return new AuctionParseResult(false, "Please upload a standards certificate image or PDF.");
        }
        File temp = null;
        try {
            temp = File.createTempFile("standards-", suffix(file.getOriginalFilename()));
            InputStream input = file.getInputStream();
            Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            AuctionParseResult parsed = imagePreparer.readDocumentText(temp, file.getOriginalFilename(), language, provider, parser);
            log.info("Standards certificate OCR text:\n{}", parsed.getRawText());
            log.info("Standards certificate mapped fields: {}", parsed.getFields());
            return parsed;
        } catch (Throwable ex) {
            log.error("Standards certificate OCR failed", ex);
            String detail = ex.getMessage();
            String message;
            if (OcrClient.isUserFacingError(detail)) {
                message = detail + " You can fill the form manually.";
            } else {
                message = "Could not read the standards certificate. You can fill the form manually.";
            }
            return new AuctionParseResult(false, message);
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
