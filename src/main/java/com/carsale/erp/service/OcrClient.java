package com.carsale.erp.service;

import java.io.File;
import java.io.IOException;

public interface OcrClient {

    String OCRSPACE = "ocrspace";
    String GOOGLE = "google";

    String id();

    String displayName();

    long maxUploadBytes();

    String recognize(File imageFile, String languageOverride) throws IOException;

    static boolean isUserFacingError(String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            return false;
        }
        String value = detail.toLowerCase();
        return value.contains("ocr.space")
                || value.contains("google cloud vision")
                || value.contains("1.5 mb")
                || value.contains("size limit");
    }
}
