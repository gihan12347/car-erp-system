package com.carsale.erp.shared.ocr;

import java.util.Map;

/**
 * Request options for the Sri Lanka certificate-of-registration custom extractor.
 * The processor rejects a call unless the request names at least one entity type.
 */
public final class RegistrationCertificateDocumentAi {

    private RegistrationCertificateDocumentAi() {
    }

    public static Map<String, Object> processOptions() {
        return DocumentAiConstants.customExtractorOptions(DocumentAiConstants.REGISTRATION_FIELDS);
    }

    public static String[] fieldNames() {
        return DocumentAiConstants.REGISTRATION_FIELDS.clone();
    }
}
