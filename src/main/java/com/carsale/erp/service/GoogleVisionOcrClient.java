package com.carsale.erp.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
    private static final long TOKEN_REFRESH_SKEW_MS = 60_000L;
    private final Path credentialsFile;
    private final String apiUrl;
    private final long maxUploadBytes;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Object tokenLock = new Object();
    private String accessToken;
    private long tokenExpiresAtMs;

    public GoogleVisionOcrClient(
            @Value("${app.ocr.google.credentialsFile:config/google-vision-credentials.json}") String credentialsFile,
            @Value("${app.ocr.google.apiUrl:https://vision.googleapis.com/v1/images:annotate}") String apiUrl,
            @Value("${app.ocr.google.maxUploadBytes:10000000}") long maxUploadBytes
    ) {
        this.credentialsFile = Paths.get(credentialsFile);
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
        return Files.isRegularFile(credentialsFile);
    }

    @CircuitBreaker(name = "googleVision", fallbackMethod = "recognizeFallback")
    @Override
    public String recognize(File imageFile, String languageOverride) throws IOException {
        if (!isConfigured()) {
            throw new IOException(
                    "Google Cloud Vision credentials were not found at " + credentialsFile.toAbsolutePath()
                            + ". Add the service account JSON file, then try again.");
        }
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        String token = accessToken();
        Map<String, Object> request = buildRequest(imageBytes, languageOverride);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        log.info("Sending {} ({} bytes) to Google Cloud Vision (lang={})",
                imageFile.getName(), imageFile.length(), languageOverride);

        try {
            String json = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<String>(json, headers);
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
        Map<String, Object> image = new LinkedHashMap<String, Object>();
        image.put("content", Base64.getEncoder().encodeToString(imageBytes));

        Map<String, Object> feature = new LinkedHashMap<String, Object>();
        feature.put("type", "DOCUMENT_TEXT_DETECTION");

        Map<String, Object> context = new LinkedHashMap<String, Object>();
        context.put("languageHints", visionLanguageHints(languageOverride));

        Map<String, Object> annotate = new LinkedHashMap<String, Object>();
        annotate.put("image", image);
        annotate.put("features", Collections.singletonList(feature));
        annotate.put("imageContext", context);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
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

    private String accessToken() throws IOException {
        synchronized (tokenLock) {
            long now = System.currentTimeMillis();
            if (accessToken != null && now + TOKEN_REFRESH_SKEW_MS < tokenExpiresAtMs) {
                return accessToken;
            }
            Map<String, Object> credentials = readCredentials();
            String jwt = signedJwt(credentials);
            String tokenUrl = stringValue(credentials.get("token_uri"));
            if (tokenUrl == null || tokenUrl.trim().isEmpty()) {
                tokenUrl = "https://oauth2.googleapis.com/token";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
            body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            body.add("assertion", jwt);

            try {
                ResponseEntity<String> response = restTemplate.postForEntity(
                        tokenUrl, new HttpEntity<MultiValueMap<String, String>>(body, headers), String.class);
                Map<?, ?> token = objectMapper.readValue(response.getBody(), Map.class);
                String access = stringValue(token.get("access_token"));
                if (access == null || access.trim().isEmpty()) {
                    throw new IOException("Google OAuth did not return an access token.");
                }
                int expiresIn = 3600;
                Object expires = token.get("expires_in");
                if (expires instanceof Number) {
                    expiresIn = ((Number) expires).intValue();
                }
                this.accessToken = access;
                this.tokenExpiresAtMs = now + (expiresIn * 1000L);
                return this.accessToken;
            } catch (HttpClientErrorException ex) {
                throw new IOException("Google OAuth token error: " + ex.getResponseBodyAsString(), ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readCredentials() throws IOException {
        if (!isConfigured()) {
            throw new IOException("Google Cloud Vision credentials file is missing.");
        }
        return objectMapper.readValue(credentialsFile.toFile(), Map.class);
    }

    private String signedJwt(Map<String, Object> credentials) throws IOException {
        String email = stringValue(credentials.get("client_email"));
        String privateKeyPem = stringValue(credentials.get("private_key"));
        String audience = stringValue(credentials.get("token_uri"));
        if (email == null || privateKeyPem == null) {
            throw new IOException("Google Cloud Vision credentials JSON is missing client_email or private_key.");
        }
        if (audience == null || audience.trim().isEmpty()) {
            audience = "https://oauth2.googleapis.com/token";
        }
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = new LinkedHashMap<String, Object>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("iss", email);
        payload.put("scope", "https://www.googleapis.com/auth/cloud-vision");
        payload.put("aud", audience);
        payload.put("iat", Long.valueOf(now));
        payload.put("exp", Long.valueOf(now + 3600L));

        String headerPart = base64Url(toJson(header));
        String payloadPart = base64Url(toJson(payload));
        String signingInput = headerPart + "." + payloadPart;
        String signature = base64Url(signRs256(signingInput.getBytes(StandardCharsets.UTF_8), privateKeyPem));
        return signingInput + "." + signature;
    }

    private byte[] signRs256(byte[] data, String pem) throws IOException {
        try {
            String encoded = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(encoded);
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(data);
            return signature.sign();
        } catch (Exception ex) {
            throw new IOException("Could not sign Google Cloud Vision JWT: " + ex.getMessage(), ex);
        }
    }

    private String toJson(Map<String, Object> map) throws IOException {
        return objectMapper.writeValueAsString(map);
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String base64Url(String json) {
        return base64Url(json.getBytes(StandardCharsets.UTF_8));
    }

    static List<String> visionLanguageHints(String language) {
        List<String> hints = new ArrayList<String>();
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
