package com.carsale.erp.shared.ocr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

@Component
public class GoogleServiceAccountAuth {

    static final String VISION_SCOPE = "https://www.googleapis.com/auth/cloud-vision";
    static final String TRANSLATE_SCOPE = "https://www.googleapis.com/auth/cloud-translation";

    private static final long TOKEN_REFRESH_SKEW_MS = 60_000L;
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final Path credentialsFile;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Object tokenLock = new Object();
    private final Map<String, CachedToken> tokens = new HashMap<>();

    public GoogleServiceAccountAuth(
            @Value("${app.ocr.google.credentialsFile:config/google-vision-credentials.json}") String credentialsFile
    ) {
        this.credentialsFile = Paths.get(credentialsFile);
        this.objectMapper = new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return Files.isRegularFile(credentialsFile);
    }

    public Path credentialsFile() {
        return credentialsFile;
    }

    public String accessToken(String scope) throws IOException {
        String requested = scope == null || scope.trim().isEmpty() ? VISION_SCOPE : scope.trim();
        synchronized (tokenLock) {
            long now = System.currentTimeMillis();
            CachedToken cached = tokens.get(requested);
            if (cached != null && now + TOKEN_REFRESH_SKEW_MS < cached.expiresAtMs) {
                return cached.value;
            }
            Map<String, Object> credentials = readCredentials();
            String jwt = signedJwt(credentials, requested);
            String tokenUrl = stringValue(credentials.get("token_uri"));
            if (tokenUrl == null || tokenUrl.trim().isEmpty()) {
                tokenUrl = "https://oauth2.googleapis.com/token";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            body.add("assertion", jwt);

            try {
                ResponseEntity<String> response = restTemplate.postForEntity(
                        tokenUrl, new HttpEntity<>(body, headers), String.class);
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
                CachedToken next = new CachedToken();
                next.value = access;
                next.expiresAtMs = now + (expiresIn * 1000L);
                tokens.put(requested, next);
                return access;
            } catch (HttpClientErrorException ex) {
                throw new IOException("Google OAuth token error: " + ex.getResponseBodyAsString(), ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readCredentials() throws IOException {
        if (!isConfigured()) {
            throw new IOException("Google service account credentials file is missing.");
        }
        return objectMapper.readValue(credentialsFile.toFile(), Map.class);
    }

    private String signedJwt(Map<String, Object> credentials, String scope) throws IOException {
        String email = stringValue(credentials.get("client_email"));
        String privateKeyPem = stringValue(credentials.get("private_key"));
        String audience = stringValue(credentials.get("token_uri"));
        if (email == null || privateKeyPem == null) {
            throw new IOException("Google credentials JSON is missing client_email or private_key.");
        }
        if (audience == null || audience.trim().isEmpty()) {
            audience = "https://oauth2.googleapis.com/token";
        }
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", email);
        payload.put("scope", scope);
        payload.put("aud", audience);
        payload.put("iat", now);
        payload.put("exp", now + 3600L);

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
            throw new IOException("Could not sign Google service-account JWT: " + ex.getMessage(), ex);
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

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static final class CachedToken {
        private String value;
        private long expiresAtMs;
    }
}
