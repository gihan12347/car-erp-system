package com.carsale.erp.shared.document.document;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

@Component
public class StandardsCertificateDoc implements DocumentParser {

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The standards certificate was empty.");
            return result;
        }

        String normalized = CustomsDocumentParserUtils.normalize(text);
        result.put("scheduleType", extractSchedule(normalized));
        result.put("emissionCo", extractEmission(normalized, RegexConstants.Standards.CO));
        result.put("emissionNmhc", extractEmission(normalized, RegexConstants.Standards.NMHC));
        result.put("emissionNox", extractEmission(normalized, RegexConstants.Standards.NOX));
        result.put("emissionPm", extractEmission(normalized, RegexConstants.Standards.PM));
        result.put("emissionHcNox", extractEmission(normalized, RegexConstants.Standards.HC_NOX));
        result.put("emissionHc", extractEmission(normalized, RegexConstants.Standards.HC));
        result.put("emissionThc", extractEmission(normalized, RegexConstants.Standards.THC));
        result.put("emissionCh4", extractEmission(normalized, RegexConstants.Standards.CH4));
        result.put("emissionSmoke", extractEmission(normalized, RegexConstants.Standards.SMOKE));
        result.put("threePointSeatBelts", marked(normalized, RegexConstants.Standards.THREE_POINT_BELTS));
        result.put("twoPointSeatBelts", marked(normalized, RegexConstants.Standards.TWO_POINT_BELTS));
        result.put("driverAirbag", CustomsDocumentParserUtils.firstNonNull(
                marked(normalized, RegexConstants.Standards.DRIVER_AIRBAG_MARK),
                marked(normalized, RegexConstants.Standards.DRIVER_AIRBAG)
        ));
        result.put("passengerAirbag", CustomsDocumentParserUtils.firstNonNull(
                marked(normalized, RegexConstants.Standards.FRONT_PASSENGER),
                marked(normalized, RegexConstants.Standards.PASSENGER_AIRBAG)
        ));
        result.put("absFitted", marked(normalized, RegexConstants.Standards.ABS));
        result.put("make", extractLabeled(normalized, "Make", "Model", "Chassis"));
        result.put("model", extractLabeled(normalized, "Model", "Chassis", "Place of Inspection"));
        result.put("chassisVin", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Chassis Number", "Place of Inspection", "Date of Inspection"),
                extractLabeled(normalized, RegexConstants.Standards.CHASSIS_NO, "Place of Inspection", "Date of Inspection"),
                findChassis(normalized)
        ));
        result.put("placeOfInspection", extractLabeled(
                normalized, "Place of Inspection", "Date of Inspection", "Remarks"));
        result.put("inspectionDate", extractLabeled(
                normalized, "Date of Inspection", "Remarks", "Make"));
        result.put("remarks", extractRemarks(normalized));

        boolean any = !result.getFields().isEmpty();
        result.setSuccess(any);
        result.setMessage(any
                ? "Filled " + result.getFields().size() + " fields from the standards certificate. Please review."
                : "The document was read, but fields could not be mapped. Please fill them manually.");
        result.setRawText(text);
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
        return "standards certificate doc";
    }

    private static String extractSchedule(String text) {
        if (scheduleMarked(text, "V") && !scheduleMarked(text, "III")) {
            return "V";
        }
        if (scheduleMarked(text, "III") && !scheduleMarked(text, "V")) {
            return "III";
        }
        if (scheduleMarked(text, "V")) {
            return "V";
        }
        if (scheduleMarked(text, "III")) {
            return "III";
        }
        return null;
    }

    private static boolean scheduleMarked(String text, String schedule) {
        Matcher matcher = RegexConstants.Standards.markedSchedule(schedule).matcher(text);
        if (!matcher.find()) {
            return false;
        }
        int from = Math.max(0, matcher.start() - 8);
        int to = Math.min(text.length(), matcher.end() + 12);
        return RegexConstants.Standards.MARK_PATTERN.matcher(text.substring(from, to)).find();
    }

    private static String extractEmission(String text, String label) {
        Matcher matcher = RegexConstants.Standards.emissionValueAfterLabel(label).matcher(text);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private static String marked(String text, String label) {
        Matcher matcher = RegexConstants.Standards.checkmarkNearLabel(label).matcher(text);
        while (matcher.find()) {
            int from = Math.max(0, matcher.start() - 10);
            int to = Math.min(text.length(), matcher.end() + 10);
            if (RegexConstants.Standards.MARK_PATTERN.matcher(text.substring(from, to)).find()) {
                return "true";
            }
        }
        return null;
    }

    private static String extractLabeled(String text, String label, String... stops) {
        Matcher matcher = RegexConstants.Labeled.valueAfterLabel(label).matcher(text);
        if (matcher.find()) {
            return cleanLabeled(matcher.group(1), stops);
        }
        matcher = RegexConstants.Labeled.valueOnNextLineAfterLabel(label).matcher(text);
        if (matcher.find()) {
            return cleanLabeled(matcher.group(1), stops);
        }
        return null;
    }

    private static String extractRemarks(String text) {
        Matcher matcher = RegexConstants.Standards.REMARKS.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = cleanLabeled(matcher.group(1));
        if (value == null || isStampNoise(value)) {
            return null;
        }
        return value;
    }

    private static String findChassis(String text) {
        Matcher matcher = RegexConstants.Identifiers.CHASSIS_WIDE.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String cleanLabeled(String value, String... stops) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll(RegexConstants.Text.TRAILING_EMDASH, "").trim();
        for (String stop : stops) {
            cleaned = RegexConstants.Labeled.fromQuotedStopToEnd(stop).matcher(cleaned).replaceAll("");
        }
        cleaned = cleaned.replaceAll(RegexConstants.Text.WHITESPACE_RUN, " ").trim();
        if (cleaned.isEmpty() || isStampNoise(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private static String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll(RegexConstants.Text.WHITESPACE, "").replace("—", "-").replace("–", "-");
        if (cleaned.equalsIgnoreCase("N/A") || cleaned.equalsIgnoreCase("N.A.") || cleaned.equalsIgnoreCase("NA")) {
            return "N/A";
        }
        if ("-".equals(cleaned)) {
            return "-";
        }
        return value.replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
    }

    private static boolean isStampNoise(String value) {
        String upper = value.toUpperCase();
        return upper.contains("AMANA") || upper.contains("BANK") || upper.contains("TRADE SERVICES");
    }
}
