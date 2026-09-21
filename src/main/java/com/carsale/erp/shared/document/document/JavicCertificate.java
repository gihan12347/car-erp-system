package com.carsale.erp.shared.document.document;

import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavicCertificate implements DocumentParser {

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("JEVIC certificate was empty.");
            return result;
        }
        String normalized = CustomsDocumentParserUtils.normalize(text);

        result.put("jevicCertificateNo", extractJevicCertificateNo(normalized));
        result.put("jevicIssueDate", CustomsDocumentParserUtils.firstNonNull(
                extractJevicDate(normalized, RegexConstants.Javic.DATE_OF_ISSUE,
                        RegexConstants.Javic.INSPECTION_BRANCH, "CERTIFICATE"),
                extractJevicIssueDate(normalized)
        ));
        result.put("jevicLocation", CustomsDocumentParserUtils.firstNonNull(
                extractJevicScalar(normalized, RegexConstants.Javic.INSPECTION_BRANCH, "Inspected Motor", "Make"),
                extractJevicLocation(normalized)
        ));
        result.put("jevicMake", extractJevicScalar(normalized, "Make", "Model", "Date of Inspection"));
        result.put("jevicModel", extractJevicScalar(
                normalized, "Model", "Engine Capacity", "Date of Inspection", "Location"));
        result.put("jevicEngineCapacity", extractJevicScalar(
                normalized, RegexConstants.Javic.ENGINE_CAPACITY, "Year of First", "Chassis Number", "Chassis"));
        result.put("jevicFirstRegistration", extractJevicScalar(
                normalized, RegexConstants.Javic.FIRST_REGISTRATION, "Chassis Number", "Chassis", "Engine Number"));
        result.put("jevicChassisVin", extractJevicChassis(normalized));
        result.put("jevicEngineNo", extractJevicScalar(
                normalized, RegexConstants.Javic.ENGINE_NUMBER, "Inspected Mileage", "Odometer", "Inspection Date"));
        result.put("jevicCurrentOdometer", CustomsDocumentParserUtils.firstNonNull(
                extractInspectedMileage(normalized),
                extractJevicOdometer(normalized)
        ));
        result.put("jevicInspectionDate", CustomsDocumentParserUtils.firstNonNull(
                extractJevicDate(normalized, RegexConstants.Javic.INSPECTION_DATE, "Remarks"),
                extractJevicDate(normalized, RegexConstants.Javic.DATE_OF_INSPECTION, "Location", "Certificate")
        ));
        result.put("jevicRemarks", extractJevicRemarks(normalized));
        result.put("jevicAuctionReadingDate", extractJevicReadingRow(normalized, "Auction Reading", "Dealer Reading"));
        result.put("jevicDealerReadingDate", extractJevicReadingRow(normalized, "Dealer Reading", "De-Registration Reading", "De Registration Reading"));
        result.put("jevicDeregistrationReadingDate", extractJevicReadingRow(
                normalized, "De-Registration Reading", "De Registration Reading", "Authorized"));

        CustomsDocumentParserUtils.finish(result, "JEVIC certificate of inspection (page 1)");
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
        return "javic certificate";
    }

    private static String extractJevicCertificateNo(String text) {
        Matcher matcher = RegexConstants.Javic.CERT_NO_LABELED.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }

        matcher = RegexConstants.Javic.CERT_NO_NEXT.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        return findJevicCertNo(text);
    }

    private static String findJevicCertNo(String text) {
        Matcher matcher = RegexConstants.Javic.CERT_NO.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    private static String extractJevicDate(String text, String labelPattern, String... afterStops) {
        Matcher matcher = RegexConstants.Javic.slashDateAfterLabel(labelPattern, joinStopLabels(afterStops)).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = RegexConstants.Javic.slashDateOnNextLineAfterLabel(labelPattern).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String joinStopLabels(String... labels) {
        if (labels == null || labels.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                builder.append("|");
            }
            builder.append(RegexConstants.Labeled.nonCapturingGroup(labels[i]));
        }
        return builder.toString();
    }

    private static String extractJevicIssueDate(String text) {
        String direct = extractJevicDate(text, RegexConstants.Javic.DATE_OF_ISSUE,
                RegexConstants.Javic.CURRENT_ODOMETER_LABEL, "Auction");
        if (direct != null) {
            return direct;
        }

        Matcher matcher = RegexConstants.Javic.ISSUE_AFTER_CERT.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = RegexConstants.Javic.ISSUE_AFTER_CERT_NEXT.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractJevicScalar(String text, String labelPattern, String... stopLabels) {
        Matcher matcher = RegexConstants.Labeled.valueAfterLabelOrNumber(labelPattern).matcher(text);
        if (matcher.find()) {
            String inline = trimBeforeNextJevicLabel(matcher.group(1), stopLabels);
            String cleaned = cleanJevicValue(inline);
            if (cleaned != null) {
                return cleaned;
            }
        }

        matcher = RegexConstants.Labeled.valueOnNextLineAfterLabelOrNumber(labelPattern).matcher(text);
        if (matcher.find()) {
            String nextLine = trimBeforeNextJevicLabel(matcher.group(1), stopLabels);
            return cleanJevicValue(nextLine);
        }
        return null;
    }

    private static String trimBeforeNextJevicLabel(String value, String... stopLabels) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        String[] defaults = new String[] {
                "Make", "Model", "Engine Capacity", "Year of First Registration",
                "Chassis Number", "Engine Number", "Inspected Mileage", "Inspection Date",
                "Date of Inspection", "Inspection Branch", "Location", "Certificate No",
                "Date of Issue", "Current Odometer", "Remarks", "Auction Reading", "Dealer Reading",
                "De-Registration Reading", "De Registration Reading", "Authorized"
        };

        int cut = trimmed.length();
        for (String aDefault : defaults) {
            Pattern pattern = RegexConstants.Labeled.stopAfterJevicLabel(aDefault);
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find() && matcher.start() < cut) {
                cut = matcher.start();
            }
        }
        for (String stopLabel : stopLabels) {
            Pattern pattern = RegexConstants.Labeled.stopAfterJevicLabel(stopLabel);
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find() && matcher.start() < cut) {
                cut = matcher.start();
            }
        }
        return trimmed.substring(0, cut).trim();
    }

    private static String cleanJevicValue(String value) {
        String cleaned = CustomsDocumentParserUtils.clean(value);
        if (cleaned == null || isJevicFieldLabel(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private static boolean isJevicFieldLabel(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.endsWith(":") && trimmed.length() < 60) {
            return true;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.contains("reading / date")
                || lower.contains("reading/ date")
                || lower.contains("reading /date")
                || lower.startsWith("certificate no")
                || lower.startsWith("date of issue")
                || lower.startsWith("date of inspection")
                || lower.startsWith("current odometer reading")
                || lower.startsWith("inspected mileage")
                || lower.startsWith("inspection branch")
                || lower.startsWith("engine capacity")
                || lower.startsWith("year of first registration")
                || lower.startsWith("chassis number")
                || lower.startsWith("engine number")
                || lower.startsWith("inspection date")
                || lower.equals("remarks")
                || lower.startsWith("dealer reading")
                || lower.startsWith("auction reading")
                || lower.startsWith("de-registration reading")
                || lower.startsWith("de registration reading")
                || lower.equals("location");
    }

    private String extractJevicLocation(String text) {
        Matcher matcher = RegexConstants.Javic.LOCATION.matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(CustomsDocumentParserUtils.trimBeforeToken(matcher.group(1), "LK1-"));
            if (value != null) {
                return value;
            }
        }

        matcher = RegexConstants.Javic.LOCATION_NEXT.matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(CustomsDocumentParserUtils.trimBeforeToken(matcher.group(1), "LK1-"));
            if (value != null) {
                return value;
            }
        }

        return extractJevicLocationNearCertificate(text);
    }

    private static String extractJevicLocationNearCertificate(String text) {
        Matcher matcher = RegexConstants.Javic.LOCATION_BEFORE_CERT.matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }

        matcher = RegexConstants.Javic.LOCATION_BEFORE_CERT_NEXT.matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }
        return null;
    }

    private static String extractJevicChassis(String text) {
        String value = extractJevicScalar(text, RegexConstants.Javic.CHASSIS_NUMBER, "Engine Number", "Inspected Mileage");
        if (value == null) {
            value = extractJevicScalar(text, RegexConstants.Javic.CHASSIS_VIN, "Make", "Model");
        }
        if (value == null) {
            value = extractJevicScalar(text, "Chassis/VIN", "Make", "Model");
        }
        if (value == null) {
            value = CustomsDocumentParserUtils.findChassis(text);
        }
        return value;
    }

    private static String extractInspectedMileage(String text) {
        Matcher matcher = RegexConstants.Javic.INSPECTED_MILEAGE.matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(matcher.group(1));
            if (value != null && !isJevicFieldLabel(value)) {
                return value;
            }
        }
        matcher = RegexConstants.Javic.INSPECTED_MILEAGE_NEXT.matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }
        return null;
    }

    private static String extractJevicRemarks(String text) {
        Matcher matcher = RegexConstants.Javic.REMARKS.matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(trimBeforeNextJevicLabel(matcher.group(1)));
        }
        matcher = RegexConstants.Javic.REMARKS_NEXT.matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(trimBeforeNextJevicLabel(matcher.group(1)));
        }
        return null;
    }

    private static String extractJevicOdometer(String text) {
        Matcher matcher = RegexConstants.Javic.CURRENT_ODOMETER_NEAR.matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(matcher.group(1));
            if (value != null) {
                return value;
            }
        }

        matcher = RegexConstants.Javic.CURRENT_ODOMETER.matcher(text);
        if (matcher.find()) {
            String value = cleanJevicValue(matcher.group(1));
            if (value != null && !isJevicFieldLabel(value)) {
                return value;
            }
        }

        matcher = RegexConstants.Javic.CURRENT_ODOMETER_NEXT.matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }

        matcher = RegexConstants.Javic.CURRENT_ODOMETER_SKIP_NEXT.matcher(text);
        if (matcher.find()) {
            return cleanJevicValue(matcher.group(1));
        }
        return null;
    }

    private static String extractJevicReadingRow(String text, String rowLabel, String... followingRowLabels) {
        String labelPattern = RegexConstants.Javic.replaceSpacesWithWhitespace(rowLabel);
        Matcher matcher = RegexConstants.Javic.odometerRowDateValue(labelPattern).matcher(text);
        if (!matcher.find()) {
            return null;
        }

        String raw = matcher.group(1).trim();
        for (String following : followingRowLabels) {
            if (raw.toLowerCase(Locale.ROOT).contains(following.toLowerCase(Locale.ROOT))) {
                return null;
            }
        }
        if (isJevicFieldLabel(raw) || isOdometerReadingValue(raw)) {
            return null;
        }
        return cleanReadingValue(raw);
    }

    private static boolean isOdometerReadingValue(String value) {
        return value != null && value.trim().matches(RegexConstants.Javic.ODOMETER_MATCH);
    }

    private static String cleanReadingValue(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.replaceAll(RegexConstants.Text.SLASH_SPACES, " / ").trim();
        if (normalized.matches(RegexConstants.Text.EMPTY_MARKERS)) {
            return null;
        }
        if (isJevicFieldLabel(normalized)) {
            return null;
        }
        return CustomsDocumentParserUtils.clean(normalized);
    }
}
