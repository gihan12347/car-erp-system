package com.carsale.erp.shared.ocr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.carsale.erp.shared.document.DocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Calls a Document AI Custom Extractor processor and returns typed entities.
 */
@Component
public class DocumentAiClient {

    private static final Logger log = LoggerFactory.getLogger(DocumentAiClient.class);
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 180_000;

    private final GoogleServiceAccountAuth googleAuth;
    private final String configuredProjectId;
    private final String location;
    private final String processorVersion;
    private final long maxUploadBytes;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DocumentAiClient(
            GoogleServiceAccountAuth googleAuth,
            @Value("${app.ocr.documentAi.projectId:}") String projectId,
            @Value("${app.ocr.documentAi.location:us}") String location,
            @Value("${app.ocr.documentAi.processorVersion:}") String processorVersion,
            @Value("${app.ocr.documentAi.maxUploadBytes:20000000}") long maxUploadBytes
    ) {
        this.googleAuth = googleAuth;
        this.configuredProjectId = projectId == null ? "" : projectId.trim();
        this.location = location == null || location.trim().isEmpty() ? "us" : location.trim();
        this.processorVersion = processorVersion == null ? "" : processorVersion.trim();
        this.maxUploadBytes = maxUploadBytes > 0 ? maxUploadBytes : 20_000_000L;
        this.objectMapper = new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    public DocumentAiResult process(
            File file,
            String originalName,
            DocumentParser parser) throws IOException {
        String processId = parser != null ? parser.getProcessorId() : null;
        if (!googleAuth.isConfigured() || !StringUtils.hasText(processId)) {
            return null;
        }
        if (file == null || !file.isFile()) {
            throw new IOException("Document AI input file is missing.");
        }
        long size = file.length();
        if (size <= 0) {
            throw new IOException("Document AI input file is empty.");
        }
        if (size > maxUploadBytes) {
            throw new IOException("Document exceeds Document AI size limit ("
                    + (maxUploadBytes / 1_000_000) + " MB).");
        }

        String projectId = resolveProjectId();
        byte[] bytes = Files.readAllBytes(file.toPath());
        String mimeType = mimeType(originalName, file.getName());
        String token = googleAuth.accessToken(GoogleServiceAccountAuth.CLOUD_PLATFORM_SCOPE);

        Map<String, Object> rawDocument = new LinkedHashMap<>();
        rawDocument.put("content", Base64.getEncoder().encodeToString(bytes));
        rawDocument.put("mimeType", mimeType);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rawDocument", rawDocument);

        String url = processUrl(projectId, location, processId, processorVersion);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        log.info("Sending {} ({} bytes, {}) to Document AI processor {} ({})",
                originalName != null ? originalName : file.getName(), size, mimeType, processId, location);

        try {
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(json, headers), String.class);
            return parseResponse(response.getBody());
        } catch (HttpClientErrorException ex) {
            throw new IOException("Document AI HTTP error: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private String resolveProjectId() throws IOException {
        if (StringUtils.hasText(configuredProjectId)) {
            return configuredProjectId;
        }
        String fromCredentials = googleAuth.projectId();
        if (StringUtils.hasText(fromCredentials)) {
            return fromCredentials.trim();
        }
        throw new IOException(
                "Document AI projectId is missing. Set app.ocr.documentAi.projectId or use a credentials JSON with project_id.");
    }

    private String processUrl(String projectId, String location, String processorId, String processorVersion) {
        StringBuilder name = new StringBuilder();
        name.append("https://").append(location).append("-documentai.googleapis.com/v1/");
        name.append("projects/").append(projectId);
        name.append("/locations/").append(location);
        name.append("/processors/").append(processorId);
        if (StringUtils.hasText(processorVersion)) {
            name.append("/processorVersions/").append(processorVersion);
        }
        name.append(":process");
        return name.toString();
    }

    private static String firstText(String override, String fallback) {
        return StringUtils.hasText(override) ? override.trim() : fallback;
    }

    @SuppressWarnings("unchecked")
    private DocumentAiResult parseResponse(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new IOException("Document AI returned an empty response.");
        }
        Map<String, Object> root = objectMapper.readValue(json, Map.class);
        Object error = root.get("error");
        if (error instanceof Map) {
            throw new IOException("Document AI error: " + stringValue(((Map<?, ?>) error).get("message")));
        }
        Object documentObj = root.get("document");
        if (!(documentObj instanceof Map)) {
            return new DocumentAiResult("", Collections.emptyList());
        }
        Map<String, Object> document = (Map<String, Object>) documentObj;
        String text = stringValue(document.get("text"));
        if (text == null) {
            text = "";
        }
        List<DocumentAiEntity> entities = new ArrayList<>();
        Object entitiesObj = document.get("entities");
        if (entitiesObj instanceof List) {
            for (Object item : (List<?>) entitiesObj) {
                if (item instanceof Map) {
                    collectEntities((Map<String, Object>) item, entities);
                }
            }
        }
        return new DocumentAiResult(text, entities);
    }

    @SuppressWarnings("unchecked")
    private void collectEntities(Map<String, Object> entity, List<DocumentAiEntity> out) {
        String type = stringValue(entity.get("type"));
        String mention = stringValue(entity.get("mentionText"));
        String normalized = null;
        Object normalizedValue = entity.get("normalizedValue");
        if (normalizedValue instanceof Map) {
            normalized = stringValue(((Map<?, ?>) normalizedValue).get("text"));
        }
        String value = firstNonBlank(normalized, mention);
        Double confidence = null;
        Object conf = entity.get("confidence");
        if (conf instanceof Number) {
            confidence = ((Number) conf).doubleValue();
        }
        if (StringUtils.hasText(type) && StringUtils.hasText(value)) {
            out.add(new DocumentAiEntity(type.trim(), value.trim(), confidence));
        }
        Object properties = entity.get("properties");
        if (properties instanceof List) {
            for (Object child : (List<?>) properties) {
                if (child instanceof Map) {
                    collectEntities((Map<String, Object>) child, out);
                }
            }
        }
    }

    static String mimeType(String originalName, String fallbackName) {
        String name = originalName != null ? originalName : fallbackName;
        if (name == null) {
            return "application/octet-stream";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".tif") || lower.endsWith(".tiff")) {
            return "image/tiff";
        }
        return "image/jpeg";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public static final class DocumentAiResult {
        private final String text;
        private final List<DocumentAiEntity> entities;

        public DocumentAiResult(String text, List<DocumentAiEntity> entities) {
            this.text = text == null ? "" : text;
            this.entities = entities == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(entities));
        }

        public String getText() {
            return text;
        }

        public List<DocumentAiEntity> getEntities() {
            return entities;
        }
    }

    public static final class DocumentAiEntity {
        private final String type;
        private final String value;
        private final Double confidence;

        public DocumentAiEntity(String type, String value, Double confidence) {
            this.type = type;
            this.value = value;
            this.confidence = confidence;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public Double getConfidence() {
            return confidence;
        }
    }
}
