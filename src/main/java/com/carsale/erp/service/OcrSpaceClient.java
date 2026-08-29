package com.carsale.erp.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Free-tier cloud OCR via <a href="https://ocr.space/OCRAPI">OCR.space</a>.
 * Register for a free API key (25k requests/month) and set OCR_SPACE_API_KEY.
 */
@Component
public class OcrSpaceClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(OcrSpaceClient.class);

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 120_000;

    private final String apiUrl;
    private final String apiKey;
    private final String language;
    private final int engine;
    private final long maxUploadBytes;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OcrSpaceClient(
            @Value("${app.ocr.ocrspace.apiUrl:https://api.ocr.space/parse/image}") String apiUrl,
            @Value("${app.ocr.ocrspace.apiKey:helloworld}") String apiKey,
            @Value("${app.ocr.ocrspace.language:jpn}") String language,
            @Value("${app.ocr.ocrspace.engine:2}") int engine,
            @Value("${app.ocr.ocrspace.maxUploadBytes:1440000}") long maxUploadBytes
    ) {
        this.apiUrl = apiUrl == null ? "https://api.ocr.space/parse/image" : apiUrl.trim();
        this.apiKey = apiKey == null ? "hello world" : apiKey.trim();
        this.language = language == null ? "jpn" : language.trim();
        this.engine = engine;
        this.maxUploadBytes = maxUploadBytes > 0 ? maxUploadBytes : 1440000L;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String id() {
        return OCRSPACE;
    }

    @Override
    public String displayName() {
        return "OCR.space";
    }

    @Override
    public long maxUploadBytes() {
        return maxUploadBytes;
    }

    @CircuitBreaker(name = "ocrSpace", fallbackMethod = "recognizeFallback")
    @Override
    public String recognize(File imageFile, String languageOverride) throws IOException {
        String lang = languageOverride == null || languageOverride.trim().isEmpty()
                ? language
                : languageOverride.trim();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("apikey", apiKey);
        body.add("language", lang);
        body.add("isOverlayRequired", "false");
        body.add("detectOrientation", "true");
        body.add("scale", "true");
        body.add("isTable", "true");
        body.add("OCREngine", String.valueOf(engine));
        body.add("file", new FileSystemResource(imageFile));

        log.info("Sending {} ({} bytes) to OCR.space (lang={}, engine={})",
                imageFile.getName(), imageFile.length(), lang, engine);

        try {
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            return parseResponse(response.getBody());
        } catch (HttpClientErrorException ex) {
            if (ex.getRawStatusCode() == 413) {
                throw new IOException(
                        "OCR.space free plan limit is 1.5 MB per image. " +
                                "The server compressed the sheet but it is still too large."
                );
            }
            throw ex;
        }
    }

    public String recognizeFallback(File imageFile, String languageOverride, Throwable ex) throws IOException {
        if (ex instanceof CallNotPermittedException) {
            throw new IOException(
                    "OCR.space is temporarily unavailable because recent calls failed. Try again shortly.",
                    ex
            );
        }
        if (ex instanceof HttpClientErrorException) {
            throw new IOException(
                    "OCR.space HTTP error: " + ((HttpClientErrorException) ex).getResponseBodyAsString(),
                    ex
            );
        }
        if (ex instanceof IOException) {
            throw (IOException) ex;
        }
        throw new IOException("OCR.space request failed: " + ex.getMessage(), ex);
    }

    private String parseResponse(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new IOException("OCR.space returned an empty response.");
        }

        Map<?, ?> root = objectMapper.readValue(json, Map.class);
        Object error = root.get("IsErrorOnProcessing");
        if (error instanceof Boolean && (Boolean) error) {
            String message = stringValue(root.get("ErrorMessage"));
            if (message == null || message.isEmpty()) {
                message = "OCR.space could not process the image.";
            }
            throw new IOException("OCR.space error: " + message);
        }

        Object exitCode = root.get("OCRExitCode");
        if (exitCode instanceof Number && ((Number) exitCode).intValue() != 1) {
            String message = stringValue(root.get("ErrorMessage"));
            throw new IOException(
                    "OCR.space failed (exit=" + exitCode + "). " +
                            (message == null ? "Check API key and daily quota." : message)
            );
        }

        Object parsedResults = root.get("ParsedResults");
        if (!(parsedResults instanceof List)) {
            return "";
        }

        List<?> results = (List<?>) parsedResults;
        List<String> lines = new ArrayList<>();
        for (Object item : results) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> parsed = (Map<?, ?>) item;
            String text = stringValue(parsed.get("ParsedText"));
            if (text != null && !text.trim().isEmpty()) {
                lines.add(text.trim());
            }
        }
        return joinLines(lines);
    }

    private static String joinLines(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(lines.get(i));
        }
        return builder.toString();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
