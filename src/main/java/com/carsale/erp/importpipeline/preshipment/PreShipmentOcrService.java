package com.carsale.erp.importpipeline.preshipment;

import com.carsale.erp.customspipeline.document.PreShipmentParser;
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
public class PreShipmentOcrService {

    private static final Logger log = LoggerFactory.getLogger(PreShipmentOcrService.class);

    private final PreShipmentParser parser;
    private final OcrImagePreparer imagePreparer;
    private final String language;

    public PreShipmentOcrService(
            PreShipmentParser parser,
            OcrImagePreparer imagePreparer,
            @Value("${app.ocr.preship.language:eng}") String language
    ) {
        this.parser = parser;
        this.imagePreparer = imagePreparer;
        this.language = language == null || language.trim().isEmpty() ? "eng" : language.trim();
    }

    public AuctionParseResult parseDocument(MultipartFile file) {
        return parseDocument(file, null);
    }

    public AuctionParseResult parseDocument(MultipartFile file, String provider) {
        AuctionParseResult failed = new AuctionParseResult();
        if (file == null || file.isEmpty()) {
            failed.setSuccess(false);
            failed.setMessage("Please upload a pre-shipment certificate image or PDF.");
            return failed;
        }

        File temp = null;
        try {
            temp = File.createTempFile("preship-", suffix(file.getOriginalFilename()));
            InputStream input = file.getInputStream();
            try {
                Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                input.close();
            }
            AuctionParseResult parsed = imagePreparer.readDocumentText(temp, file.getOriginalFilename(), language, provider, parser);
            log.info("Pre-shipment OCR text:\n{}", parsed.getRawText());
            log.info("Pre-shipment mapped fields: {}", parsed.getFields());
            return parsed;
        } catch (Throwable ex) {
            log.error("Pre-shipment OCR failed", ex);
            failed.setSuccess(false);
            String detail = ex.getMessage();
            if (OcrClient.isUserFacingError(detail)) {
                failed.setMessage(detail + " You can fill the form manually.");
            } else {
                failed.setMessage("Could not read the pre-shipment certificate. You can fill the form manually.");
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
