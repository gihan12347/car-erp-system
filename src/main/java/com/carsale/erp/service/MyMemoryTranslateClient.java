package com.carsale.erp.service;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class MyMemoryTranslateClient {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private final String apiUrl;
    private final String email;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MyMemoryTranslateClient(
            @Value("${app.translate.mymemory.apiUrl:https://api.mymemory.translated.net/get}") String apiUrl,
            @Value("${app.translate.mymemory.email:}") String email
    ) {
        this.apiUrl = apiUrl == null || apiUrl.trim().isEmpty()
                ? "https://api.mymemory.translated.net/get"
                : apiUrl.trim();
        this.email = email == null ? "" : email.trim();
        this.objectMapper = new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    @CircuitBreaker(name = "myMemoryTranslate")
    public String translate(String text) throws IOException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("q", text == null ? "" : text)
                .queryParam("langpair", "ja|en");
        if (!email.isEmpty()) {
            builder.queryParam("de", email);
        }
        URI uri = builder.build().encode().toUri();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return parseResponse(response.getBody(), text);
        } catch (HttpClientErrorException ex) {
            throw new IOException("MyMemory translation HTTP error: " + ex.getResponseBodyAsString(), ex);
        }
    }

   @SuppressWarnings("unchecked")
    private String parseResponse(String json, String original) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new IOException("MyMemory translation returned an empty response.");
        }
        Map<String, Object> root = objectMapper.readValue(json, Map.class);
        Object data = root.get("responseData");
        if (data instanceof Map) {
            Object translated = ((Map<?, ?>) data).get("translatedText");
            if (translated != null) {
                String value = String.valueOf(translated).trim();
                if (!value.isEmpty() && !"NO QUERY SPECIFIED. EXAMPLE REQUEST: GET?Q=HELLO&LANGPAIR=EN|IT".equalsIgnoreCase(value)) {
                    return unescapeHtml(value);
                }
            }
        }
        throw new IOException("MyMemory translation did not return text for: " + original);
    }

    private static String unescapeHtml(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
