package com.carsale.erp.customspipeline.document;

import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkingSheet implements DocumentParser {

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Working sheet was empty.");
            return result;
        }
        String normalized = CustomsDocumentParserUtils.normalize(text);

        result.put("worksheetRef", extractWorksheetRef(normalized));
        result.put("worksheetHsCode", CustomsDocumentParserUtils.firstNonNull(
                extractAfterLabel(normalized, "H.S. CODE", "H.S.CODE", "HS CODE", "HS. CODE"),
                extractWorksheetHsCode(normalized)
        ));
        result.put("worksheetVehicleType", CustomsDocumentParserUtils.firstNonNull(
                extractAfterLabel(normalized, "TYPE OF VEHICLE", "Type of Vehicle"),
                extractUnitUsedVehicle(normalized)
        ));
        result.put("worksheetReferenceNo", extractAfterLabel(normalized, "Reference No", "Reference No."));
        result.put("worksheetVesselName", extractAfterLabel(normalized,
                "NAME OF VESSEL", "Name of Vessel"));
        result.put("worksheetChassisNo", CustomsDocumentParserUtils.firstNonNull(
                extractAfterLabel(normalized, "CHASSIS NOS", "CHASSIS NO", "Chassis Nos", "Chassis No"),
                CustomsDocumentParserUtils.findChassis(normalized)
        ));
        result.put("worksheetAgentsFob", extractAgentsFob(normalized));
        result.put("worksheetInvoicedFob", extractAmountAfter(normalized, "Invoiced FOB", "Invoice FOB"));
        result.put("worksheetAgentsFreight", extractAmountAfter(normalized, "Agents Freight", "Agent Freight"));
        result.put("worksheetInvoicedFreight", extractAmountAfter(normalized, "Invoiced Freight", "Invoice Freight"));
        result.put("worksheetAgentsInsurance", extractAmountAfter(normalized, "Agents Insurance", "Agent Insurance"));
        result.put("worksheetInvoicedInsurance", extractAmountAfter(normalized, "Invoiced Insurance", "Invoice Insurance"));
        result.put("worksheetOptionsValue", extractAmountAfter(normalized, "Total Value of Options"));
        result.put("worksheetBlFreightCalc", extractBlFreightCalc(normalized));
        result.put("worksheetBlFreightAmount", CustomsDocumentParserUtils.firstNonNull(
                extractLastAmount(extractBlFreightCalc(normalized)),
                extractAmountAfter(normalized, "B/L Freight Calculation", "BL Freight Calculation")
        ));
        result.put("worksheetBlDate", extractYmdAfter(normalized, "Date of B/L", "Date of BL"));
        result.put("worksheetManufactureDate", extractYmdAfter(normalized,
                "Date of Manufacture", "Date of Manufact"));
        result.put("worksheetAgeDifference", extractAgeDifference(normalized));
        result.put("worksheetFirstRegistrationDate", extractYmdAfter(normalized,
                "Date of 1st Registration", "Date of First Registration"));
        result.put("worksheetWebsiteValue", extractAmountAfter(normalized, "Website Value"));
        result.put("worksheetLocalTaxes", extractAmountAfter(normalized, "Less Local Taxes", "Local Taxes"));
        result.put("worksheetFifteenPercent", CustomsDocumentParserUtils.firstNonNull(
                extractAmountAfter(normalized, "15% of Value", "Less 15% of Value"),
                extractFifteenPercent(normalized)
        ));
        result.put("worksheetFobValue85", extractFobValue85(normalized));
        result.put("worksheetLcNo", extractLcNo(normalized));
        result.put("worksheetLcAmount", extractLcAmount(normalized));
        result.put("worksheetLcBank", extractLcBank(normalized));
        result.put("worksheetLcImporter", extractWorksheetImporter(normalized));
        result.put("worksheetLcIssueDate", CustomsDocumentParserUtils.firstNonNull(
                extractSlashDateAfter(normalized, "Date of Issue"),
                extractYmdAfter(normalized, "Date of Issue")
        ));
        result.put("worksheetLcExpiryDate", extractSlashDateAfter(normalized, "Date of Expiry"));
        result.put("worksheetLcAmendmentDate", extractSlashDateAfter(normalized, "Date of Amendment"));
        result.put("worksheetClearingAgent", extractClearingAgent(normalized));
        result.put("worksheetFiscalFob", extractAmountAfter(normalized, "FOB for Fiscal Levies"));
        result.put("worksheetFiscalFreight", extractAmountAfter(normalized,
                "Freight Charges for Fiscal Levies"));
        result.put("worksheetFiscalInsurance", extractAmountAfter(normalized,
                "Insurance Charges for Fiscal Levies"));
        result.put("worksheetFiscalOptions", extractAmountAfter(normalized,
                "Value of Options for Fiscal Levies"));
        result.put("worksheetFiscalTotal", extractAmountAfter(normalized,
                "Total Value for Fiscal Levies"));

        CustomsDocumentParserUtils.finish(result, "Working sheet for motor vehicles");
        return result;
    }

    @Override
    public AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi) {
        return null;
    }

    private static String extractWorksheetRef(String text) {
        Matcher matcher = Pattern.compile("\\b([A-Z]\\d[-\\s]\\d{5,}[-\\s][A-Z]{2})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", "-");
        }
        return extractAfterLabel(text, "Ref", "Reference ID");
    }

    private static String extractWorksheetHsCode(String text) {
        Matcher matcher = Pattern.compile("\\b(\\d{4}\\.\\d{2}\\.\\d{2})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return CustomsDocumentParserUtils.extractHsCode(text);
    }

    private static String extractAgentsFob(String text) {
        Matcher calc = Pattern.compile(
                "(?:/\\s*110\\s*[Xx×]\\s*100\\s*=\\s*)(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (calc.find()) {
            return calc.group(1);
        }
        Matcher line = Pattern.compile(
                "Agents?\\s+FOB\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (line.find()) {
            String lastEquals = extractLastAmountAfterEquals(line.group(1));
            if (lastEquals != null) {
                return lastEquals;
            }
        }
        return extractAmountAfter(text, "Agents FOB", "Agent FOB");
    }

    private static String extractBlFreightCalc(String text) {
        Matcher matcher = Pattern.compile(
                "B/?L\\s*Freight\\s*Calculation\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = CustomsDocumentParserUtils.clean(matcher.group(1));
            if (value != null) {
                return value;
            }
        }
        matcher = Pattern.compile(
                "B/?L\\s*Freight\\s*Calculation\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        matcher = Pattern.compile(
                "(USD\\s+[\\d,.]+\\s*@\\s*[\\d.]+\\s*=\\s*JPY\\s*@\\s*[\\d.]+\\s*=\\s*[\\d,.]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return null;
    }

    private static String extractAgeDifference(String text) {
        Matcher matcher = Pattern.compile(
                "Age\\s+Difference(?:\\s+for\\s+I\\.?C\\.?L)?\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d+)\\s+(\\d+)\\s+(\\d+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " years " + matcher.group(2) + " months " + matcher.group(3) + " days";
        }
        return extractAfterLabel(text, "Age Difference for I.C.L", "Age Difference");
    }

    private static String extractLcNo(String text) {
        String labeled = extractAfterLabel(text, "LC No", "L/C No", "LC No.");
        if (labeled != null) {
            Matcher token = Pattern.compile("([A-Z]{5,}\\d{8,})", Pattern.CASE_INSENSITIVE).matcher(labeled);
            if (token.find()) {
                return token.group(1).toUpperCase(Locale.ROOT);
            }
        }
        Matcher matcher = Pattern.compile("\\b([A-Z]{5,}\\d{8,})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return labeled;
    }

    private static String extractLcAmount(String text) {
        Matcher matcher = Pattern.compile(
                "LC\\s*No\\.?.{0,120}?Amount\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractAmountAfter(text, "Amount");
    }

    private static String extractLcBank(String text) {
        Matcher matcher = Pattern.compile(
                "(?:^|\\n)\\s*Bank\\s*[:\\.]?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanWorksheetValue(matcher.group(1));
            if (isUsableWorksheetValue(value) && value.toUpperCase(Locale.ROOT).contains("BANK")) {
                return value;
            }
        }
        matcher = Pattern.compile(
                "(?:^|\\n)\\s*Bank\\s*[:\\.]?\\s*\\n\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            String value = cleanWorksheetValue(matcher.group(1));
            if (isUsableWorksheetValue(value) && value.toUpperCase(Locale.ROOT).contains("BANK")) {
                return value;
            }
        }
        matcher = Pattern.compile("\\b([A-Z][A-Z ]{2,}BANK[A-Z ]*(?:LIMITED|LTD\\.?|PLC)?)\\b").matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return null;
    }

    private static String extractWorksheetImporter(String text) {
        Matcher matcher = Pattern.compile(
                "\\b([A-Z][A-Z ]{3,}(?:MOTOR\\s+)?TRADING(?:\\s+COMPANY)?)\\b"
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        matcher = Pattern.compile(
                "\\b([A-Z][A-Z ]{6,}(?:MOTORS|COMPANY|PVT)[A-Z ]*)\\b"
        ).matcher(text);
        while (matcher.find()) {
            String value = CustomsDocumentParserUtils.clean(matcher.group(1));
            if (value == null) {
                continue;
            }
            String upper = value.toUpperCase(Locale.ROOT);
            if (upper.contains("BANK")
                    || upper.contains("CLEARING")
                    || upper.contains("FORWARDING")
                    || upper.contains("WORKING SHEET")
                    || upper.contains("MOTOR VEHICLES")) {
                continue;
            }
            return value;
        }
        return extractAfterLabel(text, "Importer", "Company Name");
    }

    private static String extractClearingAgent(String text) {
        String labeled = extractAfterLabel(text, "Clearing Agent Mentioned", "Clearing Agent");
        if (labeled != null && labeled.toUpperCase(Locale.ROOT).contains("CLEARING")) {
            return labeled;
        }
        Matcher matcher = Pattern.compile(
                "\\b([A-Z][A-Z0-9 &./]{2,}CLEARING[A-Z0-9 &./]*)\\b"
        ).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return labeled;
    }

    private static String extractUnitUsedVehicle(String text) {
        Matcher matcher = Pattern.compile(
                "(0?1\\s+UNIT\\s+USED\\s+[^\\n]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return cleanWorksheetValue(matcher.group(1));
        }
        return null;
    }

    private static String extractFifteenPercent(String text) {
        Matcher matcher = Pattern.compile(
                "15\\s*%\\s*of\\s+Value[^\\n]{0,40}?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = Pattern.compile(
                "15\\s*%\\s*of\\s+Value\\s*[:\\.]?\\s*\\n\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractFobValue85(String text) {
        Matcher matcher = Pattern.compile(
                "FOB\\s+Value\\s*\\(?\\s*85\\s*%?\\s*\\)?[^\\n]{0,30}?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = Pattern.compile(
                "FOB\\s+Value\\s*\\(?\\s*85\\s*%?\\s*\\)?\\s*[:\\.]?\\s*\\n\\s*(?:JPY\\s*)?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractAmountAfter(text, "FOB Value (85%)", "FOB Value 85%");
    }

    private static String extractAfterLabel(String text, String... labels) {
        for (String label : labels) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(label) + "\\s*[:\\.]?\\s*([^\\n]*)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                String value = cleanWorksheetValue(matcher.group(1));
                if (isUsableWorksheetValue(value)) {
                    return value;
                }
                Matcher next = Pattern.compile("^\\n\\s*([^\\n]+)").matcher(text.substring(matcher.end()));
                if (next.find()) {
                    value = cleanWorksheetValue(next.group(1));
                    if (isUsableWorksheetValue(value)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private static String extractAmountAfter(String text, String... labels) {
        for (String label : labels) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(label) + "[^\\n]{0,80}?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            matcher = Pattern.compile(
                    Pattern.quote(label) + "\\s*[:\\.]?\\s*\\n\\s*(?:JPY\\s*)?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String extractYmdAfter(String text, String... labels) {
        for (String label : labels) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(label) + "\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d{4})\\s+(\\d{1,2})\\s+(\\d{1,2})",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return pad2(matcher.group(3)) + "/" + pad2(matcher.group(2)) + "/" + matcher.group(1);
            }
        }
        return extractSlashDateAfter(text, labels);
    }

    private static String extractSlashDateAfter(String text, String... labels) {
        for (String label : labels) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(label) + "\\s*[:\\.]?\\s*(?:\\n\\s*)?(\\d{1,2}/\\d{1,2}/\\d{4})",
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String extractLastAmountAfterEquals(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("=\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)").matcher(text);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

    private static boolean isUsableWorksheetValue(String value) {
        return value != null && !isWorksheetFieldLabel(value);
    }

    private static boolean isWorksheetFieldLabel(String value) {
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("TYPE OF VEHICLE")
                || upper.startsWith("NAME OF VESSEL")
                || upper.startsWith("CHASSIS")
                || upper.startsWith("AGENTS")
                || upper.startsWith("INVOICED")
                || upper.startsWith("DATE OF")
                || upper.startsWith("LC NO")
                || upper.startsWith("L/C")
                || upper.startsWith("H.S")
                || upper.startsWith("HS CODE")
                || upper.startsWith("REFERENCE")
                || upper.startsWith("WEBSITE VALUE")
                || upper.startsWith("FOB VALUE")
                || upper.startsWith("FOB FOR")
                || upper.equals("BANK")
                || upper.equals("AMOUNT")
                || upper.startsWith("CLEARING AGENT")
                || upper.startsWith("TOTAL VALUE")
                || upper.startsWith("VALUE OF OPTIONS")
                || upper.startsWith("FREIGHT CHARGES")
                || upper.startsWith("INSURANCE CHARGES")
                || upper.startsWith("LESS LOCAL")
                || upper.startsWith("AGE DIFFERENCE")
                || upper.startsWith("WORKING SHEET")
                || upper.equals("YEAR")
                || upper.equals("MONTH")
                || upper.equals("DATE")
                || upper.startsWith("DETAILS OF");
    }

    private static String extractLastAmount(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)").matcher(text);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

    private static String cleanWorksheetValue(String value) {
        String cleaned = CustomsDocumentParserUtils.clean(value);
        if (cleaned == null) {
            return null;
        }
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        if (cleaned.isEmpty() || "-".equals(cleaned) || "—".equals(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private static String pad2(String value) {
        if (value == null) {
            return "";
        }
        return value.length() == 1 ? "0" + value : value;
    }
}
