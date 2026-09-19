package com.carsale.erp.importpipeline.exportcert;

import com.carsale.erp.shared.document.document.ExportCertificateParser;
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
public class ExportCertificateOcrService {

    private static final Logger log = LoggerFactory.getLogger(ExportCertificateOcrService.class);

    private final ExportCertificateParser parser;
    private final OcrImagePreparer imagePreparer;
    private final String language;

    public ExportCertificateOcrService(
            ExportCertificateParser parser,
            OcrImagePreparer imagePreparer,
            @Value("${app.ocr.export.language:jpn}") String language
    ) {
        this.parser = parser;
        this.imagePreparer = imagePreparer;
        this.language = language == null || language.trim().isEmpty() ? "jpn" : language.trim();
    }

    public AuctionParseResult parseDocument(MultipartFile file, String provider) {
        if (file == null || file.isEmpty()) {
            return new AuctionParseResult(false, "Please upload an English or Japanese export certificate image or PDF.");
        }
        File temp = null;
        try {
            temp = File.createTempFile("export-", suffix(file.getOriginalFilename()));
            InputStream input = file.getInputStream();
            Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            AuctionParseResult parsed = imagePreparer.readDocumentText(temp, file.getOriginalFilename(), language, provider, parser);
            log.info("Export certificate OCR text:\n{}", parsed.getRawText());
            if (!parser.looksJapanese(parsed.getRawText()) && !"eng".equalsIgnoreCase(language)) {
                AuctionParseResult englishParsed = imagePreparer.readDocumentText(temp, file.getOriginalFilename(), "eng", provider, parser);
                if (englishParsed.getFields().size() > parsed.getFields().size()) {
                    log.info("Export certificate English OCR mapped more fields: {}", englishParsed.getFields());
                    return englishParsed;
                }
            }
            return parsed;
        } catch (Throwable ex) {
            log.error("Export certificate OCR failed", ex);
            String detail = ex.getMessage();
            String message;
            if (OcrClient.isUserFacingError(detail)) {
                message = detail + " You can fill the form manually.";
            } else {
                message = "Could not read the export certificate. You can fill the form manually.";
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
