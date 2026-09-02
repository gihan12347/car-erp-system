package com.carsale.erp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JapaneseTextTranslatorService implements JapaneseTextTranslator {

    private static final Logger log = LoggerFactory.getLogger(JapaneseTextTranslatorService.class);

    private final GoogleTranslateClient googleTranslate;
    private final MyMemoryTranslateClient myMemoryTranslate;
    private final String provider;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean googleDisabled = new AtomicBoolean(false);

    public JapaneseTextTranslatorService(
            GoogleTranslateClient googleTranslate,
            MyMemoryTranslateClient myMemoryTranslate,
            @Value("${app.translate.provider:auto}") String provider
    ) {
        this.googleTranslate = googleTranslate;
        this.myMemoryTranslate = myMemoryTranslate;
        this.provider = provider == null ? "auto" : provider.trim().toLowerCase();
    }

    @Override
    public String translateJaToEn(String text) {
        if (text == null) {
            return null;
        }
        String source = text.trim();
        if (source.isEmpty() || !AuctionSheetEnglish.containsJapanese(source)) {
            return text;
        }
        String cached = cache.get(source);
        if (cached != null) {
            return cached;
        }
        String translated = translateDocumentUncached(source);
        if (translated == null || translated.trim().isEmpty()) {
            return text;
        }
        String cleaned = unescapeHtml(translated).replaceAll("[ \\t\\u00A0]+", " ").replaceAll(" *\\n *", "\n").trim();
        cache.put(source, cleaned);
        return cleaned;
    }

    @Override
    public String translateDocument(String text) {
        if (text == null || text.isEmpty() || !AuctionSheetEnglish.containsJapanese(text)) {
            return text;
        }
        String cached = cache.get(text);
        if (cached != null) {
            return cached;
        }
        List<String> tokens = new ArrayList<>();
        String masked = AuctionSheetEnglish.maskSheetTokens(text, tokens);
        String translated = translateDocumentUncached(masked);
        if (translated == null || translated.trim().isEmpty()) {
            return text;
        }
        String cleaned = unescapeHtml(translated).replaceAll("[ \\t\\u00A0]+", " ").replaceAll(" *\\n *", "\n");
        String restored = AuctionSheetEnglish.restoreSheetTokens(cleaned, tokens);
        cache.put(text, restored);
        return restored;
    }

    private String translateDocumentUncached(String source) {
        if (useGoogle()) {
            try {
                String translated = googleTranslate.translate(source);
                log.info("Translated auction sheet in 1 Google Cloud Translation request.");
                return translated;
            } catch (Exception ex) {
                noteGoogleFailure(ex);
                if ("google".equals(provider)) {
                    return null;
                }
            }
        }
        return translateWithMyMemory(source);
    }

    private boolean useGoogle() {
        if ("mymemory".equals(provider) || googleDisabled.get()) {
            return false;
        }
        return "google".equals(provider)
                || ("auto".equals(provider) && googleTranslate.isConfigured());
    }

    private void noteGoogleFailure(Exception ex) {
        if (isGoogleDisabledError(ex) && googleDisabled.compareAndSet(false, true)) {
            log.warn("Google Cloud Translation API is not enabled for this credentials project. "
                    + "Auto fill will use MyMemory until the app restarts. Enable the API at "
                    + "https://console.developers.google.com/apis/api/translate.googleapis.com/overview");
            return;
        }
        if (!googleDisabled.get()) {
            log.warn("Google Cloud Translation failed, falling back to MyMemory: {}", shortMessage(ex));
        }
    }

    private static boolean isGoogleDisabledError(Exception ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        return msg.contains("SERVICE_DISABLED")
                || msg.contains("accessNotConfigured")
                || msg.contains("has not been used")
                || msg.contains("PERMISSION_DENIED")
                || msg.contains("\"code\": 403");
    }

    private static String shortMessage(Exception ex) {
        String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        int json = msg.indexOf('{');
        if (json > 0) {
            msg = msg.substring(0, json).trim();
        }
        if (msg.length() > 180) {
            return msg.substring(0, 180) + "...";
        }
        return msg;
    }

    private String translateWithMyMemory(String source) {
        try {
            String translated = myMemoryTranslate.translate(source);
            log.info("Translated auction sheet in 1 MyMemory request.");
            return translated;
        } catch (Exception ex) {
            log.warn("MyMemory translation failed: {}", ex.getMessage());
            return null;
        }
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
