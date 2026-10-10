package com.carsale.erp.shared.ocr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request options for the auction-sheet custom extractor.
 * That processor rejects a call unless the request names at least one entity type.
 */
public final class AuctionSheetDocumentAi {

    private AuctionSheetDocumentAi() {
    }

    public static Map<String, Object> processOptions(String ocrLanguage) {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("schemaOverride", schema());
        return options;
    }

    private static Map<String, Object> schema() {
        List<Map<String, Object>> properties = new ArrayList<Map<String, Object>>();
        properties.add(property("chassis_no"));
        properties.add(property("lot_no"));
        properties.add(property("model_code"));
        properties.add(property("make"));
        properties.add(property("model"));
        properties.add(property("grade"));
        properties.add(property("auction_grade"));
        properties.add(property("exterior_grade"));
        properties.add(property("interior_grade"));
        properties.add(property("mileage"));
        properties.add(property("year"));
        properties.add(property("engine_size"));
        properties.add(property("fuel"));
        properties.add(property("transmission"));
        properties.add(property("color"));
        properties.add(property("color_code"));
        properties.add(property("seats"));
        properties.add(property("body_style"));
        properties.add(property("ac"));
        properties.add(property("history"));

        Map<String, Object> documentType = new LinkedHashMap<String, Object>();
        documentType.put("name", "custom_extraction_document_type");
        documentType.put("baseTypes", java.util.Collections.singletonList("document"));
        documentType.put("properties", properties);

        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("entityTypes", java.util.Collections.singletonList(documentType));
        return schema;
    }

    private static Map<String, Object> property(String name) {
        Map<String, Object> property = new LinkedHashMap<String, Object>();
        property.put("name", name);
        property.put("valueType", "string");
        property.put("occurrenceType", "OPTIONAL_ONCE");
        return property;
    }
}
