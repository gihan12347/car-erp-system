package com.carsale.erp.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.carsale.erp.dto.AuctionParseResult;
import com.carsale.erp.service.EquipmentInspectionFields.FieldDef;

@Service
public class EquipmentInspectionParser {

    private static final String VALUE_GROUP = "(YES|NO|OK|N\\s*/\\s*A|N\\.?\\s*A\\.?|NA|NIL|NONE|NOT\\s+APPLICABLE"
            + "|SINGLE|DUAL|AUTO|MANUAL|PLASTIC|STEEL|OTHER|HARD|SOFT|PASS|NORMAL|\\d{1,2})";

    private static final Pattern VALUE_AFTER_LABEL = Pattern.compile(
            "\\s*[:\\-]?\\s*(" + VALUE_GROUP + ")\\b",
            Pattern.CASE_INSENSITIVE
    );

    public AuctionParseResult parse(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The equipment condition document was empty.");
            return result;
        }

        String normalized = normalize(text);
        String interiorText = section(normalized, "INTERIOR EQUIPMENT", "EXTERIOR EQUIPMENT", "SAFETY EQUIPMENT");
        String exteriorText = section(normalized, "EXTERIOR EQUIPMENT", "SAFETY EQUIPMENT", "INTERIOR EQUIPMENT");
        String safetyText = section(normalized, "SAFETY EQUIPMENT", "TYRE", "MECHANISM", "CONDITION", "RADIATION");
        String bodyKitText = section(normalized, "Body Kit", "Alloy Wheels", "Radio Antenna");
        String truckText = section(normalized, "Truck Body", "SAFETY EQUIPMENT", "Drivers Airbag", "Driver's Airbag");

        for (FieldDef field : EquipmentInspectionFields.interior()) {
            result.put(field.getKey(), firstNonNull(
                    extractValue(interiorText, field),
                    extractValue(normalized, field)
            ));
        }
        for (FieldDef field : EquipmentInspectionFields.exterior()) {
            String scoped = scopedText(field, exteriorText, bodyKitText, truckText, normalized);
            result.put(field.getKey(), firstNonNull(
                    extractValue(scoped, field),
                    extractValue(normalized, field)
            ));
        }
        for (FieldDef field : EquipmentInspectionFields.safety()) {
            result.put(field.getKey(), firstNonNull(
                    extractValue(safetyText, field),
                    extractValue(normalized, field)
            ));
        }

        if (result.getFields().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Could not read equipment fields. You can fill the form manually.");
            return result;
        }

        result.setSuccess(true);
        result.setMessage("Filled " + result.getFields().size() + " equipment fields from the document.");
        return result;
    }

    private String scopedText(FieldDef field, String exteriorText, String bodyKitText, String truckText, String fullText) {
        if (EquipmentInspectionFields.GROUP_BODY_KIT.equals(field.getGroup())) {
            return firstNonEmpty(bodyKitText, exteriorText, fullText);
        }
        if (EquipmentInspectionFields.GROUP_TRUCK_BODY.equals(field.getGroup())) {
            return firstNonEmpty(truckText, exteriorText, fullText);
        }
        return firstNonEmpty(exteriorText, fullText);
    }

    private String extractValue(String text, FieldDef field) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] aliases = field.getAliases().clone();
        Arrays.sort(aliases, Comparator.comparingInt(String::length).reversed());
        for (int i = 0; i < aliases.length; i++) {
            String value = extractAfterLabel(text, aliases[i]);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String extractAfterLabel(String text, String label) {
        Pattern labelPattern = Pattern.compile(
                "(?i)(?<![A-Za-z0-9])" + Pattern.quote(label)
        );
        Matcher labels = labelPattern.matcher(text);
        while (labels.find()) {
            String tail = text.substring(labels.end());
            Matcher value = VALUE_AFTER_LABEL.matcher(tail);
            if (value.lookingAt()) {
                return normalizeValue(value.group(1));
            }
        }
        return null;
    }

    private String section(String text, String start, String... ends) {
        Pattern startPattern = Pattern.compile("(?i)" + Pattern.quote(start));
        Matcher startMatcher = startPattern.matcher(text);
        if (!startMatcher.find()) {
            return "";
        }
        int from = startMatcher.start();
        int to = text.length();
        for (int i = 0; i < ends.length; i++) {
            Pattern endPattern = Pattern.compile("(?i)" + Pattern.quote(ends[i]));
            Matcher endMatcher = endPattern.matcher(text);
            if (endMatcher.find(startMatcher.end()) && endMatcher.start() < to && endMatcher.start() > from) {
                to = endMatcher.start();
            }
        }
        return text.substring(from, to);
    }

    private String normalize(String text) {
        String value = text.replace('\u00a0', ' ');
        value = value.replace("\r\n", "\n").replace('\r', '\n');
        value = value.replaceAll("[\\t]+", " ");
        value = value.replaceAll(" +", " ");
        return value;
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        String compact = trimmed.toUpperCase(Locale.ROOT).replace(" ", "");
        if ("YES".equals(compact) || "Y".equals(compact)) {
            return "YES";
        }
        if ("NO".equals(compact) || "N".equals(compact)) {
            return "NO";
        }
        if ("OK".equals(compact)) {
            return "OK";
        }
        if ("NA".equals(compact) || "N/A".equals(compact) || "N.A.".equals(compact) || "N.A".equals(compact)
                || "NIL".equals(compact) || "NONE".equals(compact) || "NOTAPPLICABLE".equals(compact)) {
            return "N/A";
        }
        if (trimmed.matches("\\d{1,2}")) {
            return trimmed;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private static String firstNonNull(String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && !values[i].trim().isEmpty()) {
                return values[i];
            }
        }
        return null;
    }

    private static String firstNonEmpty(String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && !values[i].trim().isEmpty()) {
                return values[i];
            }
        }
        return "";
    }
}
