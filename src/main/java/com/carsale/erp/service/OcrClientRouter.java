package com.carsale.erp.service;

import java.io.File;
import java.io.IOException;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class OcrClientRouter implements OcrClient {

    private final OcrSpaceClient ocrSpaceClient;
    private final GoogleVisionOcrClient googleVisionOcrClient;
    private final OcrProviderSettings settings;

    public OcrClientRouter(
            OcrSpaceClient ocrSpaceClient,
            GoogleVisionOcrClient googleVisionOcrClient,
            OcrProviderSettings settings
    ) {
        this.ocrSpaceClient = ocrSpaceClient;
        this.googleVisionOcrClient = googleVisionOcrClient;
        this.settings = settings;
    }

    public OcrClient clientFor(String provider) {
        if (provider != null && GOOGLE.equalsIgnoreCase(provider.trim())) {
            return googleVisionOcrClient;
        }
        if (provider != null && OCRSPACE.equalsIgnoreCase(provider.trim())) {
            return ocrSpaceClient;
        }
        return active();
    }

    private OcrClient active() {
        if (settings.isGoogle()) {
            return googleVisionOcrClient;
        }
        return ocrSpaceClient;
    }

    @Override
    public String id() {
        return active().id();
    }

    @Override
    public String displayName() {
        return active().displayName();
    }

    @Override
    public long maxUploadBytes() {
        return active().maxUploadBytes();
    }

    @Override
    public String recognize(File imageFile, String languageOverride) throws IOException {
        return active().recognize(imageFile, languageOverride);
    }
}
