package com.carsale.erp.importpipeline.auction;

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

    public AuctionParseResult parseSheet(MultipartFile file, String provider) {
        AuctionParseResult result;
        if (file == null || file.isEmpty()) {
            result = new AuctionParseResult();
            result.setSuccess(false);
            result.setMessage("Please upload an auction sheet image or PDF.");
            return result;
        }

        File temp = null;
        try {
            temp = File.createTempFile("auction-sheet-", suffix(file.getOriginalFilename()));
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            result = imagePreparer.readDocumentText(temp, file.getOriginalFilename(), language, provider, parser);
            log.info("Auction sheet OCR text:\n{}", result.getRawText());
            return result;
        } catch (Throwable ex) {
            log.error("Auction sheet OCR failed", ex);
            result = new AuctionParseResult();
            result.setSuccess(false);
            String detail = ex.getMessage();
            if (OcrClient.isUserFacingError(detail)) {
                result.setMessage(detail + " You can fill the form manually.");
            } else {
                result.setMessage("Could not read the auction sheet. You can fill the form manually.");
            }
            return result;
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
