package com.carsale.erp.shared.ocr;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSheetDocumentAiTest {

    @Test
    @SuppressWarnings("unchecked")
    void requestIncludesEntityTypesAndJapaneseOcr() {
        Map<String, Object> options = AuctionSheetDocumentAi.processOptions("jpn");
        assertFalse(options.containsKey("ocrConfig"));

        Map<String, Object> schema = (Map<String, Object>) options.get("schemaOverride");
        List<Map<String, Object>> entityTypes = (List<Map<String, Object>>) schema.get("entityTypes");
        assertFalse(entityTypes.isEmpty());
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entityTypes.get(0).get("properties");
        assertTrue(properties.stream().anyMatch(property -> "chassis_no".equals(property.get("name"))));
        assertTrue(properties.stream().anyMatch(property -> "mileage".equals(property.get("name"))));
        assertTrue(properties.stream().anyMatch(property -> "auction_grade".equals(property.get("name"))));
        assertTrue(properties.stream().noneMatch(property -> property.containsKey("description")));
    }
}
