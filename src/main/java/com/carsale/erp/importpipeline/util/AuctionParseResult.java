package com.carsale.erp.importpipeline.util;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@Builder
public class AuctionParseResult {

    private boolean success;
    private String message;
    private String rawText;
    private Map<String, String> fields;
    private List<String> warnings;

    public void put(String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            this.fields.put(key, value.trim());
        }
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
