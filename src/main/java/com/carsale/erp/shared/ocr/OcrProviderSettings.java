package com.carsale.erp.shared.ocr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OcrProviderSettings {


    private final String defaultProvider;
    private final Path storeFile;
    private volatile String provider;

    public OcrProviderSettings(
            @Value("${app.ocr.provider:ocrspace}") String defaultProvider,
            @Value("${app.ocr.settingsFile:uploads/ocr-provider.txt}") String settingsFile
    ) {
        this.defaultProvider = normalize(defaultProvider);
        this.storeFile = Paths.get(settingsFile);
        this.provider = loadOrDefault();
        log.info("OCR provider is {}", this.provider);
    }

    public String current() {
        return provider;
    }

    public boolean isGoogle() {
        return OcrClient.GOOGLE.equals(current());
    }

    public synchronized void setProvider(String value) {
        String next = normalize(value);
        this.provider = next;
        try {
            Path parent = storeFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(storeFile, next.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.warn("Could not persist OCR provider setting to {}: {}", storeFile, ex.getMessage());
        }
    }

    private String loadOrDefault() {
        try {
            if (Files.exists(storeFile)) {
                String stored = new String(Files.readAllBytes(storeFile), StandardCharsets.UTF_8).trim();
                if (!stored.isEmpty()) {
                    return normalize(stored);
                }
            }
        } catch (IOException ex) {
            log.warn("Could not read OCR provider setting from {}: {}", storeFile, ex.getMessage());
        }
        return defaultProvider;
    }

    private static String normalize(String value) {
        if (value != null && OcrClient.GOOGLE.equalsIgnoreCase(value.trim())) {
            return OcrClient.GOOGLE;
        }
        return OcrClient.OCRSPACE;
    }
}
