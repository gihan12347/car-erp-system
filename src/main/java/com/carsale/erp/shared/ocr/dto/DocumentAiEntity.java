package com.carsale.erp.shared.ocr.dto;

public class DocumentAiEntity {
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
