package com.carsale.erp.shared.document.document;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.carsale.erp.importpipeline.equipment.EquipmentInspectionFields;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import org.springframework.stereotype.Service;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.importpipeline.equipment.EquipmentInspectionFields.FieldDef;
import com.carsale.erp.shared.regex.RegexConstants;

@Service
public class EquipmentInspectionParser implements DocumentParser {

    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult(text);
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
            result.put(field.getKey(), CustomsDocumentParserUtils.firstNonNull(
                    extractValue(interiorText, field),
                    extractValue(normalized, field)
            ));
        }
        for (FieldDef field : EquipmentInspectionFields.exterior()) {
            String scoped = scopedText(field, exteriorText, bodyKitText, truckText, normalized);
            result.put(field.getKey(), CustomsDocumentParserUtils.firstNonNull(
                    extractValue(scoped, field),
                    extractValue(normalized, field)
            ));
        }
        for (FieldDef field : EquipmentInspectionFields.safety()) {
            result.put(field.getKey(), CustomsDocumentParserUtils.firstNonNull(
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

    @Override
    public AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi) {
        return null;
    }

    @Override
    public String getProcessorId() {
        return "";
    }

    @Override
    public String getDocumentName() {
        return "equipment inspection";
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
        for (String alias : aliases) {
            String value = extractAfterLabel(text, alias);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String extractAfterLabel(String text, String label) {
        Pattern labelPattern = RegexConstants.Labeled.ignoreCaseAtWordBoundary(label);
        Matcher labels = labelPattern.matcher(text);
        while (labels.find()) {
            String tail = text.substring(labels.end());
            Matcher value = RegexConstants.Equipment.VALUE_AFTER_LABEL.matcher(tail);
            if (value.lookingAt()) {
                return normalizeValue(value.group(1));
            }
        }
        return null;
    }

    private String section(String text, String start, String... ends) {
        Pattern startPattern = RegexConstants.Labeled.quotedTextIgnoreCase(start);
        Matcher startMatcher = startPattern.matcher(text);
        if (!startMatcher.find()) {
            return "";
        }
        int from = startMatcher.start();
        int to = text.length();
        for (String end : ends) {
            Pattern endPattern = RegexConstants.Labeled.quotedTextIgnoreCase(end);
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
        value = value.replaceAll(RegexConstants.Text.TABS, " ");
        value = value.replaceAll(RegexConstants.Text.SPACES, " ");
        return value;
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        return CustomsDocumentParserUtils.getNormalizedValue(trimmed.toUpperCase().replace(" ", ""));
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }
}
