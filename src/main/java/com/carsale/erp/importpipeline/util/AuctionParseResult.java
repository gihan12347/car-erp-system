package com.carsale.erp.importpipeline.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AuctionParseResult {

    private boolean success;
    private String message;
    private String rawText;
    private Map<String, String> fields = new LinkedHashMap<>();
    private List<String> warnings = new ArrayList<>();

    public AuctionParseResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuctionParseResult() {

    }

    public AuctionParseResult(String rawText){
        this.rawText = rawText;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public void put(String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            this.fields.put(key, value.trim());
        }
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public void addWarning(String warning) {
        if (warning != null && !warning.trim().isEmpty()) {
            this.warnings.add(warning.trim());
        }
    }
}
