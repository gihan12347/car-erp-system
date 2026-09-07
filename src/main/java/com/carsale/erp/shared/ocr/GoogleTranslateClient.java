package com.carsale.erp.shared.ocr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class GoogleTranslateClient {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final GoogleServiceAccountAuth googleAuth;
    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleTranslateClient(
            GoogleServiceAccountAuth googleAuth,
            @Value("${app.translate.google.apiUrl:https://translation.googleapis.com/language/translate/v2}") String apiUrl
    ) {
        this.googleAuth = googleAuth;
        this.apiUrl = apiUrl == null || apiUrl.trim().isEmpty()
                ? "https://translation.googleapis.com/language/translate/v2"
                : apiUrl.trim();
        this.objectMapper = new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return googleAuth.isConfigured();
    }

    @CircuitBreaker(name = "googleTranslate")
    public String translate(String text) throws IOException {
        List<String> translated = translateAll(text);
        return translated.isEmpty() ? text : translated.get(0);
    }

    public List<String> translateAll(String text) throws IOException {
        if (!isConfigured()) {
            throw new IOException(
                    "Google Cloud Translation credentials were not found at "
                            + googleAuth.credentialsFile().toAbsolutePath());
        }
        if (text == null) {
            return Collections.emptyList();
        }

        String token = googleAuth.accessToken(GoogleServiceAccountAuth.TRANSLATE_SCOPE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("q", text);
        body.put("source", "ja");
        body.put("target", "en");
        body.put("format", "text");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        try {
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(json, headers), String.class);
            return parseResponse(response.getBody());
        } catch (HttpClientErrorException ex) {
            throw new IOException("Google Cloud Translation HTTP error: " + ex.getResponseBodyAsString(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseResponse(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new IOException("Google Cloud Translation returned an empty response.");
        }
        Map<String, Object> root = objectMapper.readValue(json, Map.class);
        Object error = root.get("error");
        if (error instanceof Map) {
            throw new IOException(
                    "Google Cloud Translation error: " + ((Map<?, ?>) error).get("message"));
        }
        Object data = root.get("data");
        if (!(data instanceof Map)) {
            throw new IOException("Google Cloud Translation response was missing data.");
        }
        Object translations = ((Map<?, ?>) data).get("translations");
        if (!(translations instanceof List)) {
            throw new IOException("Google Cloud Translation response was missing translations.");
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) translations) {
            if (item instanceof Map) {
                Object translated = ((Map<?, ?>) item).get("translatedText");
                result.add(translated == null ? "" : String.valueOf(translated));
            }
        }
        return result;
    }
}
