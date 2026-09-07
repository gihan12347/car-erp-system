package com.carsale.erp.importpipeline.equipment;

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
public class EquipmentInspectionOcrService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentInspectionOcrService.class);

    private final EquipmentInspectionParser parser;
    private final OcrImagePreparer imagePreparer;
    private final String language;

    public EquipmentInspectionOcrService(
            EquipmentInspectionParser parser,
            OcrImagePreparer imagePreparer,
            @Value("${app.ocr.equipment.language:eng}") String language
    ) {
        this.parser = parser;
        this.imagePreparer = imagePreparer;
        this.language = language == null || language.trim().isEmpty() ? "eng" : language.trim();
    }

    public AuctionParseResult parseDocument(MultipartFile file, String provider) {
        if (file == null || file.isEmpty()) {
            return new AuctionParseResult(false, "Please upload an equipment condition image or PDF.");
        }
        File temp = null;
        try {
            temp = File.createTempFile("equipment-", suffix(file.getOriginalFilename()));
            InputStream input = file.getInputStream();
            Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String raw = imagePreparer.readDocumentText(temp, file.getOriginalFilename(), language, provider);
            log.info("Equipment inspection OCR text:\n{}", raw);
            AuctionParseResult parsed = parser.parse(raw);
            log.info("Equipment inspection mapped fields: {}", parsed.getFields());
            return parsed;
        } catch (Throwable ex) {
            log.error("Equipment inspection OCR failed", ex);
            String message, detail = ex.getMessage();
            if (OcrClient.isUserFacingError(detail)) {
                message = detail + " You can fill the form manually.";
            } else {
                message = "Could not read the equipment condition document. You can fill the form manually.";
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
