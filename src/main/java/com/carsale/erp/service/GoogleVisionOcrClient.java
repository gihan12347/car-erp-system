package com.carsale.erp.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class GoogleVisionOcrClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleVisionOcrClient.class);
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 120_000;
    private final GoogleServiceAccountAuth googleAuth;
    private final String apiUrl;
    private final long maxUploadBytes;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleVisionOcrClient(
            GoogleServiceAccountAuth googleAuth,
            @Value("${app.ocr.google.apiUrl:https://vision.googleapis.com/v1/images:annotate}") String apiUrl,
            @Value("${app.ocr.google.maxUploadBytes:10000000}") long maxUploadBytes
    ) {
        this.googleAuth = googleAuth;
        this.apiUrl = apiUrl == null || apiUrl.trim().isEmpty()
                ? "https://vision.googleapis.com/v1/images:annotate"
                : apiUrl.trim();
        this.maxUploadBytes = maxUploadBytes > 0 ? maxUploadBytes : 10_000_000L;
        this.objectMapper = new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String id() {
        return GOOGLE;
    }

    @Override
    public String displayName() {
        return "Google Cloud Vision";
    }

    @Override
    public long maxUploadBytes() {
        return maxUploadBytes;
    }

    public boolean isConfigured() {
        return googleAuth.isConfigured();
    }

    @CircuitBreaker(name = "googleVision", fallbackMethod = "recognizeFallback")
    @Override
    public String recognize(File imageFile, String languageOverride) throws IOException {
        if (!isConfigured()) {
            throw new IOException(
                    "Google Cloud Vision credentials were not found at " + googleAuth.credentialsFile().toAbsolutePath()
                            + ". Add the service account JSON file, then try again.");
        }
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String token = googleAuth.accessToken(GoogleServiceAccountAuth.VISION_SCOPE);
        Map<String, Object> request = buildRequest(imageBytes, languageOverride);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        log.info("Sending {} ({} bytes) to Google Cloud Vision (lang={})",
                imageFile.getName(), imageFile.length(), languageOverride);

        try {
            String json = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return parseResponse(response.getBody());
        } catch (HttpClientErrorException ex) {
            throw new IOException("Google Cloud Vision HTTP error: " + ex.getResponseBodyAsString(), ex);
        }
    }

    public String recognizeFallback(File imageFile, String languageOverride, Throwable ex) throws IOException {
        if (ex instanceof CallNotPermittedException) {
            throw new IOException(
                    "Google Cloud Vision is temporarily unavailable because recent calls failed. Try again shortly.",
                    ex
            );
        }
        if (ex instanceof IOException) {
            throw (IOException) ex;
        }
        throw new IOException("Google Cloud Vision request failed: " + ex.getMessage(), ex);
    }

    private Map<String, Object> buildRequest(byte[] imageBytes, String languageOverride) {
        Map<String, Object> image = new LinkedHashMap<>();
        image.put("content", Base64.getEncoder().encodeToString(imageBytes));

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "DOCUMENT_TEXT_DETECTION");

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("languageHints", visionLanguageHints(languageOverride));

        Map<String, Object> annotate = new LinkedHashMap<>();
        annotate.put("image", image);
        annotate.put("features", Collections.singletonList(feature));
        annotate.put("imageContext", context);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requests", Collections.singletonList(annotate));
        return body;
    }

    @SuppressWarnings("unchecked")
    private String parseResponse(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new IOException("Google Cloud Vision returned an empty response.");
        }
        Map<String, Object> root = objectMapper.readValue(json, Map.class);
        Object topError = root.get("error");
        if (topError instanceof Map) {
            throw new IOException("Google Cloud Vision error: " + stringValue(((Map<?, ?>) topError).get("message")));
        }
        Object responses = root.get("responses");
        if (!(responses instanceof List) || ((List<?>) responses).isEmpty()) {
            return "";
        }
        Object first = ((List<?>) responses).get(0);
        if (!(first instanceof Map)) {
            return "";
        }
        Map<String, Object> response = (Map<String, Object>) first;
        Object responseError = response.get("error");
        if (responseError instanceof Map) {
            throw new IOException(
                    "Google Cloud Vision error: " + stringValue(((Map<?, ?>) responseError).get("message")));
        }
        Object fullText = response.get("fullTextAnnotation");
        if (fullText instanceof Map) {
            String text = stringValue(((Map<?, ?>) fullText).get("text"));
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
        }
        Object annotations = response.get("textAnnotations");
        if (annotations instanceof List && !((List<?>) annotations).isEmpty()) {
            Object firstAnn = ((List<?>) annotations).get(0);
            if (firstAnn instanceof Map) {
                String text = stringValue(((Map<?, ?>) firstAnn).get("description"));
                if (text != null) {
                    return text.trim();
                }
            }
        }
        return "";
    }

    static List<String> visionLanguageHints(String language) {
        List<String> hints = new ArrayList<>();
        String value = language == null ? "" : language.toLowerCase();
        if (value.contains("jpn") || value.contains("ja")) {
            hints.add("ja");
        }
        if (value.contains("eng") || value.contains("en")) {
            hints.add("en");
        }
        if (hints.isEmpty()) {
            hints.add("ja");
        }
        return hints;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
