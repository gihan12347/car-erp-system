package com.carsale.erp.customspipeline.document;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StandardsCertificateDoc implements DocumentParser {
    private static final String MARK = "[\\u2713\\u2714\\u221A\\u2611xX]|\\[\\s*[xX\\u2713\\u2714]\\s*\\]";
    private static final String VALUE = "([0-9]+(?:\\.[0-9]+)?|N\\s*/\\s*A|N\\.?\\s*A\\.?|NA|-|—|–)";

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
        result.put("emissionCo", extractEmission(normalized, "\\bCO\\b(?!\\w)"));
        result.put("emissionNmhc", extractEmission(normalized, "\\bNMHC\\b"));
        result.put("emissionNox", extractEmission(normalized, "(?<!\\+)\\bNO\\s*x\\b"));
        result.put("emissionPm", extractEmission(normalized, "\\bPM\\b"));
        result.put("emissionHcNox", extractEmission(normalized, "\\bHC\\s*\\+\\s*NO\\s*x\\b"));
        result.put("emissionHc", extractEmission(normalized, "(?<![A-Z])\\bHC\\b(?!\\s*\\+)"));
        result.put("emissionThc", extractEmission(normalized, "\\bTHC\\b"));
        result.put("emissionCh4", extractEmission(normalized, "\\bCH\\s*4\\b"));
        result.put("emissionSmoke", extractEmission(normalized, "\\bSmoke\\b"));
        result.put("threePointSeatBelts", marked(normalized,
                "Three\\s+point\\s+seat\\s+belts(?:\\s+for\\s+driver\\s+and\\s+front\\s+passengers)?"));
        result.put("twoPointSeatBelts", marked(normalized,
                "Minimum\\s+two\\s+point\\s+seat\\s+belts(?:\\s+for\\s+other\\s+passengers)?"));
        result.put("driverAirbag", CustomsDocumentParserUtils.firstNonNull(
                marked(normalized, "Air\\s*Bags?\\s*[:\\-]?\\s*(?:Driver)"),
                marked(normalized, "Driver(?:'s)?\\s+Air\\s*Bag")
        ));
        result.put("passengerAirbag", CustomsDocumentParserUtils.firstNonNull(
                marked(normalized, "Front\\s+Passenger"),
                marked(normalized, "Passenger(?:'s)?\\s+Air\\s*Bag")
        ));
        result.put("absFitted", marked(normalized, "\\bABS\\b"));
        result.put("make", extractLabeled(normalized, "Make", "Model", "Chassis"));
        result.put("model", extractLabeled(normalized, "Model", "Chassis", "Place of Inspection"));
        result.put("chassisVin", CustomsDocumentParserUtils.firstNonNull(
                extractLabeled(normalized, "Chassis Number", "Place of Inspection", "Date of Inspection"),
                extractLabeled(normalized, "Chassis No\\.?", "Place of Inspection", "Date of Inspection"),
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
        Matcher matcher = Pattern.compile(
                "(?:" + MARK + "\\s*)?Schedule\\s+" + schedule + "(?:\\s*" + MARK + ")?",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (!matcher.find()) {
            return false;
        }
        int from = Math.max(0, matcher.start() - 8);
        int to = Math.min(text.length(), matcher.end() + 12);
        return Pattern.compile(MARK).matcher(text.substring(from, to)).find();
    }

    private static String extractEmission(String text, String label) {
        Matcher matcher = Pattern.compile(
                label + "\\s*[:\\-]?\\s*" + VALUE + "(?:\\s*g\\s*/\\s*km)?",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private static String marked(String text, String label) {
        Matcher matcher = Pattern.compile(
                label + "[^\\n]{0,48}|(?:" + MARK + ")\\s*" + label,
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        while (matcher.find()) {
            int from = Math.max(0, matcher.start() - 10);
            int to = Math.min(text.length(), matcher.end() + 10);
            if (Pattern.compile(MARK).matcher(text.substring(from, to)).find()) {
                return "true";
            }
        }
        return null;
    }

    private static String extractLabeled(String text, String label, String... stops) {
        Matcher matcher = Pattern.compile(
                label + "\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanLabeled(matcher.group(1), stops);
        }
        matcher = Pattern.compile(
                label + "\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanLabeled(matcher.group(1), stops);
        }
        return null;
    }

    private static String extractRemarks(String text) {
        Matcher matcher = Pattern.compile(
                "Remarks\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
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
        Matcher matcher = Pattern.compile(
                "\\b([A-Z0-9]{2,8}-[A-Z0-9]{5,12})\\b"
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String cleanLabeled(String value, String... stops) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\u2013\\u2014_]+$", "").trim();
        for (String stop : stops) {
            cleaned = cleaned.replaceAll("(?i)\\s+" + Pattern.quote(stop) + ".*$", "");
        }
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        if (cleaned.isEmpty() || isStampNoise(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private static String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s+", "").replace("—", "-").replace("–", "-");
        if (cleaned.equalsIgnoreCase("N/A") || cleaned.equalsIgnoreCase("N.A.") || cleaned.equalsIgnoreCase("NA")) {
            return "N/A";
        }
        if ("-".equals(cleaned)) {
            return "-";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private static boolean isStampNoise(String value) {
        String upper = value.toUpperCase();
        return upper.contains("AMANA") || upper.contains("BANK") || upper.contains("TRADE SERVICES");
    }
}
