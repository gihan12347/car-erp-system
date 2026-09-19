package com.carsale.erp.shared.document.document;

import com.carsale.erp.importpipeline.auction.AuctionSheetEnglish;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.ocr.JapaneseTextTranslator;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.stereotype.Component;

import java.util.*;
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

    @Override
    public String getProcessorId() {
        return "";
    }

    private static String extractWorksheetRef(String text) {
        Matcher matcher = RegexConstants.Worksheet.REF.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll(RegexConstants.Text.WHITESPACE, "-");
        }
        return extractAfterLabel(text, "Ref", "Reference ID");
    }

    private static String extractWorksheetHsCode(String text) {
        Matcher matcher = RegexConstants.Identifiers.HS_DOTTED.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return CustomsDocumentParserUtils.extractHsCode(text);
    }

    private static String extractAgentsFob(String text) {
        Matcher calc = RegexConstants.Worksheet.AGENTS_FOB_CALC.matcher(text);
        if (calc.find()) {
            return calc.group(1);
        }
        Matcher line = RegexConstants.Worksheet.AGENTS_FOB_LINE.matcher(text);
        if (line.find()) {
            String lastEquals = extractLastAmountAfterEquals(line.group(1));
            if (lastEquals != null) {
                return lastEquals;
            }
        }
        return extractAmountAfter(text, "Agents FOB", "Agent FOB");
    }

    private static String extractBlFreightCalc(String text) {
        Matcher matcher = RegexConstants.Worksheet.BL_FREIGHT.matcher(text);
        if (matcher.find()) {
            String value = CustomsDocumentParserUtils.clean(matcher.group(1));
            if (value != null) {
                return value;
            }
        }
        matcher = RegexConstants.Worksheet.BL_FREIGHT_NEXT.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        matcher = RegexConstants.Worksheet.BL_FREIGHT_USD.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return null;
    }

    private static String extractAgeDifference(String text) {
        Matcher matcher = RegexConstants.Worksheet.AGE_DIFFERENCE.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " years " + matcher.group(2) + " months " + matcher.group(3) + " days";
        }
        return extractAfterLabel(text, "Age Difference for I.C.L", "Age Difference");
    }

    private static String extractLcNo(String text) {
        String labeled = extractAfterLabel(text, "LC No", "L/C No", "LC No.");
        if (labeled != null) {
            Matcher token = RegexConstants.Worksheet.LC_NO_TOKEN.matcher(labeled);
            if (token.find()) {
                return token.group(1).toUpperCase(Locale.ROOT);
            }
        }
        Matcher matcher = RegexConstants.Worksheet.LC_NO_WORD.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return labeled;
    }

    private static String extractLcAmount(String text) {
        Matcher matcher = RegexConstants.Worksheet.LC_AMOUNT.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractAmountAfter(text, "Amount");
    }

    private static String extractLcBank(String text) {
        Matcher matcher = RegexConstants.Worksheet.BANK_LINE.matcher(text);
        if (matcher.find()) {
            String value = cleanWorksheetValue(matcher.group(1));
            if (isUsableWorksheetValue(value) && value.toUpperCase(Locale.ROOT).contains("BANK")) {
                return value;
            }
        }
        matcher = RegexConstants.Worksheet.BANK_NEXT.matcher(text);
        if (matcher.find()) {
            String value = cleanWorksheetValue(matcher.group(1));
            if (isUsableWorksheetValue(value) && value.toUpperCase(Locale.ROOT).contains("BANK")) {
                return value;
            }
        }
        matcher = RegexConstants.Worksheet.BANK_NAME.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return null;
    }

    private static String extractWorksheetImporter(String text) {
        Matcher matcher = RegexConstants.Worksheet.IMPORTER_TRADING.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        matcher = RegexConstants.Worksheet.IMPORTER_MOTORS.matcher(text);
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
        Matcher matcher = RegexConstants.Worksheet.CLEARING.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return labeled;
    }

    private static String extractUnitUsedVehicle(String text) {
        Matcher matcher = RegexConstants.Worksheet.UNIT_USED.matcher(text);
        if (matcher.find()) {
            return cleanWorksheetValue(matcher.group(1));
        }
        return null;
    }

    private static String extractFifteenPercent(String text) {
        Matcher matcher = RegexConstants.Worksheet.FIFTEEN_PERCENT.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = RegexConstants.Worksheet.FIFTEEN_PERCENT_NEXT.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractFobValue85(String text) {
        Matcher matcher = RegexConstants.Worksheet.FOB_85.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = RegexConstants.Worksheet.FOB_85_NEXT.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractAmountAfter(text, "FOB Value (85%)", "FOB Value 85%");
    }

    private static String extractAfterLabel(String text, String... labels) {
        for (String label : labels) {
            Matcher matcher = RegexConstants.Labeled.optionalValueAfterQuotedLabel(label).matcher(text);
            if (matcher.find()) {
                String value = cleanWorksheetValue(matcher.group(1));
                if (isUsableWorksheetValue(value)) {
                    return value;
                }
                Matcher next = RegexConstants.Text.NEXT_LINE_VALUE.matcher(text.substring(matcher.end()));
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
            Matcher matcher = RegexConstants.Worksheet.amountNearQuotedLabel(label).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            matcher = RegexConstants.Worksheet.amountOnNextLineAfterQuotedLabel(label).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String extractYmdAfter(String text, String... labels) {
        for (String label : labels) {
            Matcher matcher = RegexConstants.Worksheet.yearMonthDayAfterQuotedLabel(label).matcher(text);
            if (matcher.find()) {
                return pad2(matcher.group(3)) + "/" + pad2(matcher.group(2)) + "/" + matcher.group(1);
            }
        }
        return extractSlashDateAfter(text, labels);
    }

    private static String extractSlashDateAfter(String text, String... labels) {
        for (String label : labels) {
            Matcher matcher = RegexConstants.Worksheet.slashDateAfterQuotedLabel(label).matcher(text);
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
        Matcher matcher = RegexConstants.Amounts.AFTER_EQUALS_PATTERN.matcher(text);
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
        Matcher matcher = RegexConstants.Amounts.GROUPED_COMMA_PATTERN.matcher(text);
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
        cleaned = cleaned.replaceAll(RegexConstants.Text.WHITESPACE_RUN, " ").trim();
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

    @Component
    public static class AuctionSheetParser implements DocumentParser {

        private static final Map<String, String> MODEL_MAKES = new LinkedHashMap<>();
        private static final Map<String, String[]> CODE_MODELS = new LinkedHashMap<>();
        private static final Map<String, String[]> MODEL_SPECS = new LinkedHashMap<>();

        private final JapaneseTextTranslator translator;

        public AuctionSheetParser(JapaneseTextTranslator translator) {
            this.translator = translator;
        }

        static {
            putCode("NHP10", "Aqua", "Toyota");
            putCode("NHP11", "Aqua", "Toyota");
            putCode("NKE165", "Corolla Axio", "Toyota");
            putCode("NZE161", "Corolla Axio", "Toyota");
            putCode("NHW20", "Prius", "Toyota");
            putCode("M900A", "Roomy", "Toyota");
            putCode("M910A", "Roomy", "Toyota");
            putCode("MH95S", "Wagon R", "Suzuki");
            putCode("MH55S", "Wagon R", "Suzuki");
            putCode("MH35S", "Wagon R", "Suzuki");
            putCode("ZVW30", "Prius", "Toyota");
            putCode("ZVW50", "Prius", "Toyota");
            putCode("ZVW51", "Prius", "Toyota");
            putCode("RU1", "Vezel", "Honda");
            putCode("RU3", "Vezel", "Honda");
            putCode("A202A", "Raize", "Toyota");
            putCode("A210A", "Raize", "Toyota");
            putCode("A200A", "Raize", "Toyota");
            putCode("A201A", "Raize", "Toyota");
            putCode("A202S", "Rocky", "Daihatsu");
            putCode("A200S", "Rocky", "Daihatsu");
            putSpec("A202A", "5AA-A202A", "Raize", "Toyota", "Hybrid Z", "Hybrid", "1200", "5-door", "5", "CVT");
            putSpec("A210A", "5BA-A210A", "Raize", "Toyota", null, "Gasoline", "1000", "5-door", "5", "CVT");
            putSpec("A200A", "5BA-A200A", "Raize", "Toyota", null, "Gasoline", "1000", "5-door", "5", "CVT");
            putSpec("A201A", "5BA-A201A", "Raize", "Toyota", null, "Gasoline", "1000", "5-door", "5", "CVT");
            putSpec("A202S", "5AA-A202S", "Rocky", "Daihatsu", "Hybrid Z", "Hybrid", "1200", "5-door", "5", "CVT");
            putSpec("A200S", "5BA-A200S", "Rocky", "Daihatsu", null, "Gasoline", "1000", "5-door", "5", "CVT");
        }

        private static void putCode(String code, String model, String make) {
            CODE_MODELS.put(code, new String[] {model, make});
            MODEL_MAKES.put(model, make);
        }

        private static void putSpec(String chassisPrefix, String modelCode, String model, String make,
                String grade, String fuel, String engineCc, String body, String seats, String transmission) {
            MODEL_SPECS.put(chassisPrefix, new String[] {
                    modelCode, model, make, grade, fuel, engineCc, body, seats, transmission
            });
        }

        public AuctionParseResult parsePage(String rawText) {
            AuctionParseResult result = new AuctionParseResult();
            if (rawText == null || rawText.trim().isEmpty()) {
                result.setSuccess(false);
                result.setMessage("No text was read from the auction sheet.");
                return result;
            }
            String original = repairOcrNoise(normalizeLabels(rawText.replace('\r', '\n')));
            String text = translator.translateDocument(original);
            result.setRawText(text);
            fillIdentity(result, text);
            result.put("grade", extractGrade(text));
            result.put("modelCode", firstNonNull(extractModelCode(text), extractModelCode(original)));
            result.put("chassisNo", firstNonNull(
                    extractChassis(text, result.getFields().get("modelCode")),
                    extractChassis(original, result.getFields().get("modelCode"))));
            fillFromModelCode(result, text + "\n" + original);
            fillVehicleName(result);
            result.put("year", firstNonNull(extractFirstRegistration(text), extractFirstRegistration(original)));
            result.put("mileage", formatKm(firstNonNull(extractMileage(text), extractMileage(original))));
            result.put("colorCode", firstNonNull(extractColorCode(text), extractColorCode(original)));
            result.put("color", firstNonNull(
                    colorFromCode(result.getFields().get("colorCode")),
                    firstNonNull(extractColor(text), extractColor(original))));
            result.put("engineSize", formatCc(firstNonNull(extractEngine(text), extractEngine(original))));
            result.put("transmission", firstNonNull(extractTransmission(text), extractTransmission(original)));
            result.put("fuel", firstNonNull(extractFuel(text), extractFuel(original)));
            result.put("acType", firstNonNull(extractAc(text), extractAc(original)));
            result.put("bodyStyle", firstNonNull(extractBodyStyle(text), extractBodyStyle(original)));
            result.put("seats", formatSeats(firstNonNull(extractSeats(text), extractSeats(original))));
            result.put("lotNo", firstNonNull(
                    extractLot(text, result.getFields().get("mileage")),
                    extractLot(original, result.getFields().get("mileage"))));
            result.put("auctionGrade", firstNonNull(extractAuctionGrade(text), extractAuctionGrade(original)));
            result.put("exteriorGrade", firstNonNull(
                    extractPanelGrade(text, "Exterior", "外装", "外装評"),
                    extractPanelGrade(original, "Exterior", "外装", "外装評")));
            result.put("interiorGrade", firstNonNull(
                    extractPanelGrade(text, "Interior", "内装", "内"),
                    extractPanelGrade(original, "Interior", "内装", "内")));
            fillPanelGradesFromNoise(result, text);
            fillPanelGradesFromNoise(result, original);
            fillAuctionSheetGrades(result, text);
            fillAuctionSheetGrades(result, original);
            result.put("history", firstNonNull(extractHistory(text), extractHistory(original)));
            extractDimensions(result, text);
            if (isBlank(result.getFields().get("lengthCm"))) {
                extractDimensions(result, original);
            }
            fillKnownSpecs(result, text + "\n" + original);
            englishize(result);
            fillVehicleName(result);
            keepFormFields(result);

            boolean any = !result.getFields().isEmpty();
            result.setSuccess(any);
            result.setMessage(any
                    ? "Filled " + result.getFields().size() + " fields from the auction sheet. Please check and edit."
                    : "The sheet was read, but fields could not be mapped. Please fill them manually.");
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

        private String repairOcrNoise(String text) {
            String repaired = foldFullWidthDigits(text);
            repaired = repaired.replace('ｋ', 'k').replace('ｍ', 'm').replace("㎞", "km");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.KM_THOUSANDS, "$1,$2 km");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.CC_THOUSANDS, "$1$2 cc");
            repaired = repaired.replace("ハイプリッド", "ハイブリッド");
            repaired = repaired.replace("ハイプリット", "ハイブリッド");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.SAA_PREFIX, "5AA-");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.RISE_OCR, "車名 ライズ");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.KN_SPACED, "$1 km");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.KN, "$1 km");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.REIWA_ERA, "R$1 $2");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.HEISEI_SLASH, "H$1 $2");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.HEISEI_SPACE, "H$1 $2");
            repaired = repaired.replaceAll(RegexConstants.AuctionSheet.Ocr.G_AS_REIWA, "初度登録$1R$2");
            return repaired;
        }

        private String foldFullWidthDigits(String text) {
            StringBuilder builder = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch >= '０' && ch <= '９') {
                    builder.append((char) ('0' + (ch - '０')));
                } else if (ch == '／') {
                    builder.append('/');
                } else if (ch == 'Ｚ' || ch == 'ｚ') {
                    builder.append('Z');
                } else if (ch == 'Ｓ' || ch == 'ｓ') {
                    builder.append('S');
                } else if (ch == 'Ａ' || ch == 'ａ') {
                    builder.append('A');
                } else {
                    builder.append(ch);
                }
            }
            return builder.toString();
        }

        private String normalizeLabels(String text) {
            String normalized = text;
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.LOT_NO, "出品番号");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.FIRST_REG, "初度登録");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.YEAR_MONTH, "年月");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.MILEAGE, "走行");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.MODEL, "型式");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.DISPLACEMENT, "排気量");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.GRADE, "グレード");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.ROOMY, "ルーミー");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.EVAL_POINT, "評価点");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.EVAL, "評点");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.INTERIOR, "内装");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.EXTERIOR, "外装");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.CHASSIS_NO, "車台番号");
            normalized = normalized.replaceAll(RegexConstants.AuctionSheet.Ocr.DOOR_SHAPE, "ドア形状");
            return normalized;
        }

        private void englishize(AuctionParseResult result) {
            Map<String, String> fields = result.getFields();
            Map<String, String> updated = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                String english = AuctionSheetEnglish.convert(entry.getValue(), null);
                if (english != null && !english.isEmpty()) {
                    updated.put(entry.getKey(), english);
                } else if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    updated.put(entry.getKey(), entry.getValue().trim());
                }
            }
            fields.clear();
            fields.putAll(updated);
        }

        private void fillIdentity(AuctionParseResult result, String text) {
            String nameLine = extract(text, new String[] {"車名", "Vehicle name", "Car name", "Model name"});
            applyIdentity(result, nameLine);
            if (result.getFields().get("model") == null) {
                applyIdentity(result, extract(text, new String[] {"メーカー", "Maker", "Manufacturer"}));
            }
            if (result.getFields().get("model") == null) {
                applyIdentity(result, text);
            }
            if (result.getFields().get("make") == null) {
                result.put("make", inferMake(text, result.getFields().get("model")));
            }
        }

        private void applyIdentity(AuctionParseResult result, String haystack) {
            if (haystack == null || haystack.trim().isEmpty()) {
                return;
            }
            String matched = matchKnownModel(haystack);
            if (matched != null) {
                result.put("model", matched);
                result.put("make", inferMake(haystack, matched));
                return;
            }
            if (haystack.length() > 40 || haystack.contains("\n") || looksLikeSpecDump(haystack)) {
                return;
            }
            String translated = translateMaker(haystack).replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
            if (translated.isEmpty() || isFieldLabel(translated) || isSpecToken(translated)) {
                return;
            }
            if (result.getFields().get("model") == null) {
                result.put("model", firstModelToken(translated));
                result.put("make", inferMake(haystack, translated));
            }
        }

        private String matchKnownModel(String haystack) {
            String upper = haystack.toUpperCase();
            for (String english : MODEL_MAKES.keySet()) {
                if (english != null && upper.contains(english.toUpperCase())) {
                    return english;
                }
            }
            return null;
        }

        private String inferMake(String haystack, String model) {
            if (model != null && MODEL_MAKES.containsKey(model)) {
                return MODEL_MAKES.get(model);
            }
            String source = haystack == null ? "" : haystack;
            String translated = translateMaker(source);
            if (translated.contains("Toyota") || source.toUpperCase().contains("TOYOTA")) {
                return "Toyota";
            }
            if (translated.contains("Suzuki") || source.toUpperCase().contains("SUZUKI") || source.contains("スズキ")) {
                return "Suzuki";
            }
            if (translated.contains("Honda") || source.contains("ホンダ")) {
                return "Honda";
            }
            if (translated.contains("Nissan") || source.contains("日産")) {
                return "Nissan";
            }
            if (translated.contains("Mazda") || source.contains("マツダ")) {
                return "Mazda";
            }
            if (translated.contains("Daihatsu") || source.contains("ダイハツ")) {
                return "Daihatsu";
            }
            return MODEL_MAKES.get(translated);
        }

        private String firstModelToken(String value) {
            if (value == null) {
                return null;
            }
            String cleaned = cleanValue(value);
            if (cleaned == null || cleaned.isEmpty()) {
                return null;
            }
            return cleaned.replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
        }

        private void fillFromModelCode(AuctionParseResult result, String text) {
            String haystack = codeHaystack(text + " " + nullToEmpty(result.getFields().get("modelCode"))
                    + " " + nullToEmpty(result.getFields().get("chassisNo")));
            for (Map.Entry<String, String[]> entry : CODE_MODELS.entrySet()) {
                if (haystack.contains(entry.getKey())) {
                    if (result.getFields().get("model") == null) {
                        result.put("model", entry.getValue()[0]);
                    }
                    if (result.getFields().get("make") == null) {
                        result.put("make", entry.getValue()[1]);
                    }
                    break;
                }
            }
        }

        private void fillKnownSpecs(AuctionParseResult result, String text) {
            String key = findSpecKey(result, text);
            if (key == null) {
                return;
            }
            String[] spec = MODEL_SPECS.get(key);
            result.put("modelCode", spec[0]);
            String model = result.getFields().get("model");
            if (spec[1] != null && (isBlank(model) || !model.contains(spec[1]))) {
                result.put("model", spec[1]);
            }
            putIfBlank(result, "make", spec[2]);
            String grade = result.getFields().get("grade");
            if (spec[3] != null && (isBlank(grade) || AuctionSheetEnglish.containsJapanese(grade))) {
                result.put("grade", spec[3]);
            }
            String fuel = result.getFields().get("fuel");
            if (isBlank(fuel) || ("Hybrid".equals(spec[4]) && "Gasoline".equals(fuel))) {
                result.put("fuel", spec[4]);
            }
            putIfBlank(result, "engineSize", formatCc(spec[5]));
            String body = result.getFields().get("bodyStyle");
            if (isBlank(body) || (spec[6] != null && !spec[6].equals(body))) {
                result.put("bodyStyle", spec[6]);
            }
            putIfBlank(result, "seats", formatSeats(spec[7]));
            putIfBlank(result, "transmission", spec[8]);
        }

        private String findSpecKey(AuctionParseResult result, String text) {
            String haystack = codeHaystack(
                    nullToEmpty(result.getFields().get("modelCode")) + " "
                            + nullToEmpty(result.getFields().get("chassisNo")) + " "
                            + text
            );
            for (String key : MODEL_SPECS.keySet()) {
                if (haystack.contains(key)) {
                    return key;
                }
            }
            return null;
        }

        private void putIfBlank(AuctionParseResult result, String key, String value) {
            if (value != null && isBlank(result.getFields().get(key))) {
                result.put(key, value);
            }
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }

        private boolean looksLikeSpecDump(String value) {
            String upper = value.toUpperCase();
            return upper.contains("WD") || upper.contains("CC") || upper.contains("-A")
                    || RegexConstants.Text.TWO_NUMBER_CLUSTERS.matcher(value).find();
        }

        private void fillAuctionSheetGrades(AuctionParseResult result, String text) {
            if (isBlank(result.getFields().get("auctionGrade"))) {
                String overall = findStandaloneAuctionGrade(text);
                if (overall != null) {
                    result.put("auctionGrade", overall);
                }
            }
            List<String> panelLetters = findStandalonePanelGrades(text, result.getFields().get("auctionGrade"));
            if (isBlank(result.getFields().get("exteriorGrade")) && isBlank(result.getFields().get("interiorGrade"))
                    && panelLetters.size() >= 2) {
                result.put("exteriorGrade", panelLetters.get(0));
                result.put("interiorGrade", panelLetters.get(1));
            }
            if (isBlank(result.getFields().get("interiorGrade"))) {
                putFirstUnusedGrade(result, "interiorGrade", panelLetters);
            }
            if (isBlank(result.getFields().get("exteriorGrade")) && panelLetters.size() >= 2) {
                putFirstUnusedGrade(result, "exteriorGrade", panelLetters);
            }
            Matcher together = RegexConstants.AuctionSheet.Grade.TOGETHER.matcher(text.toUpperCase());
            if (together.find()) {
                if (isBlank(result.getFields().get("auctionGrade"))) {
                    result.put("auctionGrade", together.group(1));
                }
                if (isBlank(result.getFields().get("interiorGrade"))) {
                    result.put("interiorGrade", together.group(2));
                }
            }
        }

        private String findStandaloneAuctionGrade(String text) {
            Matcher line = RegexConstants.AuctionSheet.Grade.STANDALONE_LINE
                    .matcher(text);
            if (line.find()) {
                return line.group(1);
            }
            Matcher labeled = RegexConstants.AuctionSheet.Grade.LABELED
                    .matcher(text);
            if (labeled.find()) {
                return labeled.group(1);
            }
            Matcher token = RegexConstants.AuctionSheet.Grade.TOKEN_S.matcher(text);
            if (token.find()) {
                return token.group(1);
            }
            return null;
        }

        private List<String> findStandalonePanelGrades(String text, String auctionGrade) {
            List<String> grades = new ArrayList<>();
            Matcher line = RegexConstants.AuctionSheet.Grade.PANEL_LINE.matcher(text);
            while (line.find()) {
                String grade = line.group(1).toUpperCase();
                if (!grade.equals(auctionGrade) && !grades.contains(grade)) {
                    grades.add(grade);
                }
            }
            Matcher token = RegexConstants.AuctionSheet.Grade.PANEL_TOKEN.matcher(text.toUpperCase());
            while (token.find()) {
                if (!isGradeLetterContext(text, token.start())) {
                    continue;
                }
                String grade = token.group(1);
                if (!grade.equals(auctionGrade) && !grades.contains(grade)) {
                    grades.add(grade);
                }
            }
            return grades;
        }

        private void putFirstUnusedGrade(AuctionParseResult result, String key, List<String> panelLetters) {
            for (String grade : panelLetters) {
                if (!grade.equals(result.getFields().get("auctionGrade"))
                        && !grade.equals(result.getFields().get("exteriorGrade"))
                        && !grade.equals(result.getFields().get("interiorGrade"))) {
                    result.put(key, grade);
                    return;
                }
            }
        }

        private boolean isGradeLetterContext(String text, int start) {
            int from = Math.max(0, start - 8);
            int to = Math.min(text.length(), start + 8);
            String window = text.substring(from, to).toUpperCase();
            if (window.contains("車歴") || window.contains("履歴") || window.contains("HISTORY")
                    || window.contains("B )") || window.contains("B)")) {
                return false;
            }
            if (window.contains("AAC") || window.contains("IAT") || window.contains("ABS")
                    || window.contains("5AA") || window.contains("5BA") || window.contains("6AA")) {
                return false;
            }
            if (start > 0 && isCodeChar(text.charAt(start - 1))) {
                return false;
            }
            return start + 1 >= text.length() || !isCodeChar(text.charAt(start + 1));
        }

        private boolean isCodeChar(char ch) {
            return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
        }

        private void fillVehicleName(AuctionParseResult result) {
            String make = result.getFields().get("make");
            String model = result.getFields().get("model");
            if (make == null || model == null) {
                return;
            }
            if (!model.toLowerCase().startsWith(make.toLowerCase())) {
                result.getFields().put("model", make + " " + model);
            }
        }

        private void keepFormFields(AuctionParseResult result) {
            String[] keys = new String[] {
                    "lotNo", "year", "make", "model", "bodyStyle", "grade",
                    "auctionGrade", "exteriorGrade", "interiorGrade",
                    "mileage", "history", "engineSize", "fuel", "modelCode", "chassisNo",
                    "seats", "color", "colorCode", "lengthCm", "widthCm", "heightCm",
                    "transmission", "acType"
            };
            Map<String, String> kept = new LinkedHashMap<>();
            for (String key : keys) {
                String value = result.getFields().get(key);
                if (value != null && !value.trim().isEmpty()) {
                    kept.put(key, value.trim());
                }
            }
            result.getFields().clear();
            result.getFields().putAll(kept);
        }

        private String formatKm(String mileage) {
            if (mileage == null) {
                return null;
            }
            String digits = mileage.replaceAll(RegexConstants.Text.DIGITS_ONLY, "");
            if (digits.isEmpty()) {
                return mileage;
            }
            return withComma(digits) + " km";
        }

        private String formatCc(String engine) {
            if (engine == null) {
                return null;
            }
            String digits = engine.replaceAll(RegexConstants.Text.DIGITS_ONLY, "");
            if (digits.isEmpty()) {
                return engine;
            }
            return withComma(digits) + " cc";
        }

        private String formatSeats(String seats) {
            if (seats == null) {
                return null;
            }
            String digits = seats.replaceAll(RegexConstants.Text.DIGITS_ONLY, "");
            if (digits.isEmpty()) {
                return seats;
            }
            return digits + " people";
        }

        private String withComma(String digits) {
            if (digits.length() <= 3) {
                return digits;
            }
            StringBuilder builder = new StringBuilder();
            int count = 0;
            for (int i = digits.length() - 1; i >= 0; i--) {
                if (count > 0 && count % 3 == 0) {
                    builder.append(',');
                }
                builder.append(digits.charAt(i));
                count++;
            }
            return builder.reverse().toString();
        }

        private String extractFirstRegistration(String text) {
            int[] labeled = extractEraDateNearLabel(text);
            if (labeled != null) {
                return formatFirstRegistration(labeled[0], labeled[1]);
            }
            String english = extractEnglishRegistrationDate(text);
            if (english != null) {
                return english;
            }
            int[] besideLot = extractEraDateBesideLot(text);
            if (besideLot != null) {
                return formatFirstRegistration(besideLot[0], besideLot[1]);
            }
            String header = text.substring(0, Math.min(text.length(), 1200));
            int[] headerDate = extractEraDate(header, true);
            if (headerDate != null) {
                return formatFirstRegistration(headerDate[0], headerDate[1]);
            }
            return extractEnglishRegistrationDate(header);
        }

        private String extractEnglishRegistrationDate(String text) {
            Matcher labeled = RegexConstants.AuctionSheet.Identity.ENGLISH_REG
                    .matcher(text);
            if (labeled.find()) {
                return formatEnglishMonthYear(labeled.group(1), labeled.group(2));
            }
            Matcher monthYear = RegexConstants.AuctionSheet.Identity.ENGLISH_MONTH_YEAR.matcher(text);
            if (monthYear.find()) {
                return formatEnglishMonthYear(monthYear.group(1), monthYear.group(2));
            }
            Matcher yearMonth = RegexConstants.Dates.YEAR_MONTH_LOOSE_PATTERN.matcher(text);
            if (yearMonth.find()) {
                return formatFirstRegistration(Integer.parseInt(yearMonth.group(1)), Integer.parseInt(yearMonth.group(2)));
            }
            return null;
        }

        private String formatEnglishMonthYear(String monthToken, String year) {
            String[] names = new String[] {
                    "jan", "feb", "mar", "apr", "may", "jun",
                    "jul", "aug", "sep", "oct", "nov", "dec"
            };
            String key = monthToken.toLowerCase().replace(".", "");
            if (key.startsWith("sept")) {
                key = "sep";
            }
            for (int i = 0; i < names.length; i++) {
                if (key.startsWith(names[i])) {
                    return formatFirstRegistration(Integer.parseInt(year), i + 1);
                }
            }
            return monthToken + " " + year;
        }

        private String formatFirstRegistration(int year, int month) {
            if (month >= 1 && month <= 12) {
                return monthName(month) + " " + year;
            }
            return String.valueOf(year);
        }

        private int[] extractEraDateNearLabel(String text) {
            Matcher label = RegexConstants.AuctionSheet.Identity.FIRST_REG_LABEL.matcher(text);
            while (label.find()) {
                int from = label.start();
                int to = Math.min(text.length(), label.end() + 80);
                int[] parsed = extractEraDate(text.substring(from, to), false);
                if (parsed != null) {
                    return parsed;
                }
            }
            Matcher yearMonth = RegexConstants.AuctionSheet.Identity.MODEL_YEAR_LABEL.matcher(text);
            if (yearMonth.find()) {
                int to = Math.min(text.length(), yearMonth.end() + 40);
                return extractEraDate(text.substring(yearMonth.start(), to), false);
            }
            return null;
        }

        private int[] extractEraDateBesideLot(String text) {
            Matcher lot = RegexConstants.AuctionSheet.Identity.LOT_ERA
                    .matcher(text);
            if (lot.find()) {
                return extractEraDate(lot.group(2).replaceFirst(RegexConstants.Text.LEADING_G, "R"), false);
            }
            return null;
        }

        private int[] extractEraDate(String window, boolean headerOnly) {
            Matcher reiwa = RegexConstants.AuctionSheet.Identity.REIWA.matcher(window);
            if (reiwa.find()) {
                int month = Integer.parseInt(reiwa.group(2));
                if (month >= 1 && month <= 12) {
                    return new int[] {toWesternYear("令和", reiwa.group(1)), month};
                }
            }
            Matcher heisei = RegexConstants.AuctionSheet.Identity.HEISEI.matcher(window);
            if (heisei.find()) {
                int eraYear = Integer.parseInt(heisei.group(1));
                int month = Integer.parseInt(heisei.group(2));
                if (eraYear >= 1 && eraYear <= 31 && month >= 1 && month <= 12) {
                    return new int[] {toWesternYear("平成", heisei.group(1)), month};
                }
            }
            Matcher jpYearMonth = RegexConstants.Dates.JP_YEAR_MONTH_PATTERN.matcher(window);
            if (jpYearMonth.find()) {
                int eraYear = Integer.parseInt(jpYearMonth.group(1));
                int month = Integer.parseInt(jpYearMonth.group(2));
                if (month >= 1 && month <= 12) {
                    if (eraYear >= 9 && eraYear <= 31) {
                        return new int[] {1988 + eraYear, month};
                    }
                    if (eraYear >= 1 && eraYear <= 8) {
                        return new int[] {2018 + eraYear, month};
                    }
                }
            }
            if (headerOnly) {
                Matcher gAsR = RegexConstants.AuctionSheet.Identity.G_AS_R.matcher(window.toUpperCase());
                if (gAsR.find()) {
                    int month = Integer.parseInt(gAsR.group(2));
                    return new int[] {2018 + Integer.parseInt(gAsR.group(1)), month};
                }
            }
            Matcher two = RegexConstants.AuctionSheet.Identity.FIRST_REG_NUMBERS.matcher(window);
            if (two.find()) {
                int eraYear = Integer.parseInt(two.group(1));
                int month = Integer.parseInt(two.group(2));
                if (month >= 1 && month <= 12) {
                    if (eraYear >= 9 && eraYear <= 31) {
                        return new int[] {1988 + eraYear, month};
                    }
                    if (eraYear >= 1 && eraYear <= 8) {
                        return new int[] {2018 + eraYear, month};
                    }
                }
            }
            return null;
        }

        private String monthName(int month) {
            String[] names = new String[] {
                    "", "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
            };
            if (month >= 1 && month <= 12) {
                return names[month];
            }
            return String.valueOf(month);
        }

        private String extract(String text, String[] keys) {
            for (String key : keys) {
                String quoted = Pattern.quote(key);
                if (key.matches(RegexConstants.Text.STARTS_WITH_LETTER)) {
                    quoted = RegexConstants.Labeled.notAdjacentToLetters(quoted);
                }
                Pattern pattern = RegexConstants.AuctionSheet.Identity.valueAfterQuotedKey(quoted);
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    String sameLine = cleanValue(matcher.group(1));
                    if (hasContent(sameLine) && !isFieldLabel(sameLine)) {
                        return sameLine;
                    }
                    String next = nextLine(text, matcher.end());
                    if (hasContent(next) && !isFieldLabel(next)) {
                        return cleanValue(next);
                    }
                }
            }
            return null;
        }

        private String nextLine(String text, int from) {
            int nl = text.indexOf('\n', from);
            if (nl < 0) {
                return null;
            }
            int end = text.indexOf('\n', nl + 1);
            if (end < 0) {
                end = text.length();
            }
            return text.substring(nl + 1, end).trim();
        }

        private String extractChassis(String text, String modelCode) {
            String labeled = extract(text, new String[] {"車台番号", "Chassis number", "Chassis no", "Frame number", "VIN"});
            String digits = null;
            if (labeled != null) {
                String cleaned = codeHaystack(labeled).replaceAll(RegexConstants.Text.CHASSIS_CHARS, "");
                Matcher full = RegexConstants.Identifiers.CHASSIS_CAPTURE.matcher(cleaned);
                if (full.find()) {
                    return full.group(1);
                }
                if (cleaned.matches(RegexConstants.Identifiers.CHASSIS_SHAPE)) {
                    return cleaned;
                }
                Matcher labeledDigits = RegexConstants.AuctionSheet.Identity.CHASSIS_DIGITS.matcher(cleaned);
                if (labeledDigits.find()) {
                    digits = labeledDigits.group(1);
                }
            }
            if (digits == null) {
                Matcher suffix = RegexConstants.AuctionSheet.Identity.CHASSIS_LABELED_DIGITS
                        .matcher(text);
                if (suffix.find()) {
                    digits = suffix.group(1);
                }
            }
            if (digits != null) {
                String prefix = chassisPrefix(modelCode);
                return prefix == null ? digits : prefix + "-" + digits;
            }
            Matcher matcher = RegexConstants.Identifiers.CHASSIS_CAPTURE.matcher(codeHaystack(text));
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }

        private String chassisPrefix(String modelCode) {
            if (modelCode == null || modelCode.trim().isEmpty()) {
                return null;
            }
            int dash = modelCode.lastIndexOf('-');
            if (dash >= 0 && dash < modelCode.length() - 1) {
                return modelCode.substring(dash + 1);
            }
            return modelCode;
        }

        private String extractModelCode(String text) {
            String haystack = codeHaystack(text);
            String labeled = extract(text, new String[] {"型式", "Model code", "Model type"});
            if (labeled != null) {
                Matcher labeledCode = RegexConstants.Identifiers.MODEL_CODE_OPTIONAL_SEP
                        .matcher(codeHaystack(labeled));
                if (labeledCode.find()) {
                    return normalizeKnownModelCode(labeledCode.group(1).replace(' ', '-'));
                }
                String cleaned = codeHaystack(labeled).replaceAll(RegexConstants.Text.CHASSIS_CHARS, "");
                if (cleaned.length() >= 4) {
                    return cleaned;
                }
            }
            Matcher matcher = RegexConstants.Identifiers.MODEL_CODE_CAPTURE.matcher(haystack);
            if (matcher.find()) {
                return normalizeKnownModelCode(matcher.group(1));
            }
            Matcher spaced = RegexConstants.Identifiers.MODEL_CODE_SPACED.matcher(haystack);
            if (spaced.find()) {
                return normalizeKnownModelCode(spaced.group(1).replace(' ', '-'));
            }
            return null;
        }

        private String normalizeKnownModelCode(String code) {
            if (code == null) {
                return null;
            }
            String upper = codeHaystack(code);
            for (Map.Entry<String, String[]> entry : MODEL_SPECS.entrySet()) {
                if (upper.endsWith(entry.getKey()) || upper.contains("-" + entry.getKey())) {
                    return entry.getValue()[0];
                }
            }
            return code;
        }

        private String codeHaystack(String text) {
            return text.toUpperCase().replace('Ｏ', '0').replace('O', '0');
        }

        private String extractMileage(String text) {
            Matcher withUnit = RegexConstants.AuctionSheet.Specs.MILEAGE_LABELED_UNIT
                    .matcher(text);
            if (withUnit.find()) {
                return withUnit.group(1).replace(",", "").replace(".", "") + " km";
            }
            Matcher labeled = RegexConstants.AuctionSheet.Specs.MILEAGE_LABELED
                    .matcher(text);
            if (labeled.find()) {
                String digits = labeled.group(1).replace(",", "").replace(".", "");
                if (!digits.isEmpty()) {
                    return digits + " km";
                }
            }
            Matcher dotted = RegexConstants.AuctionSheet.Specs.KM_DOTTED.matcher(text);
            if (dotted.find()) {
                return dotted.group(1).replace(",", "").replace(".", "") + " km";
            }
            Matcher spaced = RegexConstants.AuctionSheet.Specs.KM_SPACED.matcher(text);
            if (spaced.find()) {
                return spaced.group(1) + spaced.group(2) + " km";
            }
            Matcher matcher = RegexConstants.AuctionSheet.Specs.KM_PLAIN.matcher(text);
            if (matcher.find()) {
                String digits = matcher.group(1);
                int from = Math.max(0, matcher.start() - 8);
                String before = text.substring(from, matcher.start()).toLowerCase();
                if (!before.contains("cc")) {
                    return digits + " km";
                }
            }
            return null;
        }

        private String extractEngine(String text) {
            Matcher labeledComma = RegexConstants.AuctionSheet.Specs.CC_LABELED_COMMA
                    .matcher(text);
            if (labeledComma.find()) {
                return labeledComma.group(1) + labeledComma.group(2) + " cc";
            }
            Matcher labeled = RegexConstants.AuctionSheet.Specs.CC_LABELED
                    .matcher(text);
            if (labeled.find()) {
                return labeled.group(1) + " cc";
            }
            Matcher ccComma = RegexConstants.AuctionSheet.Specs.CC_COMMA.matcher(text);
            if (ccComma.find()) {
                return ccComma.group(1) + ccComma.group(2) + " cc";
            }
            Matcher cc = RegexConstants.AuctionSheet.Specs.CC.matcher(text);
            if (cc.find()) {
                return cc.group(1) + " cc";
            }
            return null;
        }

        private int toWesternYear(String era, String number) {
            int n = "元".equals(number) ? 1 : Integer.parseInt(number);
            if ("令和".equals(era)) {
                return 2018 + n;
            }
            if ("平成".equals(era)) {
                return 1988 + n;
            }
            return 1925 + n;
        }

        private String extractLot(String text, String mileage) {
            String mileageDigits = mileage == null ? "" : mileage.replaceAll(RegexConstants.Text.DIGITS_ONLY, "");
            Matcher labeled = RegexConstants.AuctionSheet.Identity.LOT_LABELED
                    .matcher(text);
            if (labeled.find()) {
                String lot = normalizeLot(labeled.group(1), mileageDigits);
                if (lot != null && !isUnlikelyLot(lot, mileageDigits, text)) {
                    return lot;
                }
            }
            String extracted = extract(text, new String[] {
                    "出品番号", "出品No", "出品NO", "ロット", "Lot number", "Lot no", "Exhibition number", "Listing number"
            });
            if (extracted != null) {
                Matcher digits = RegexConstants.AuctionSheet.Identity.LOT_DIGITS.matcher(extracted);
                if (digits.find()) {
                    String lot = normalizeLot(digits.group(1), mileageDigits);
                    if (lot != null && !isUnlikelyLot(lot, mileageDigits, text)) {
                        return lot;
                    }
                }
            }
            String head = text.substring(0, Math.min(text.length(), 900));
            Matcher rows = RegexConstants.AuctionSheet.Identity.LOT_LINE.matcher(head);
            String best = null;
            int bestCount = 0;
            while (rows.find()) {
                String candidate = normalizeLot(rows.group(1), mileageDigits);
                if (candidate == null || isUnlikelyLot(candidate, mileageDigits, text)) {
                    continue;
                }
                int count = countToken(head, candidate);
                if (count > bestCount) {
                    best = candidate;
                    bestCount = count;
                }
            }
            if (best != null) {
                return best;
            }
            Matcher any = RegexConstants.AuctionSheet.Identity.LOT_LOOSE.matcher(head);
            while (any.find()) {
                String candidate = normalizeLot(any.group(1), mileageDigits);
                if (candidate != null && !isUnlikelyLot(candidate, mileageDigits, text)) {
                    return candidate;
                }
            }
            return null;
        }

        private String normalizeLot(String digits, String mileageDigits) {
            if (digits == null || digits.length() < 3) {
                return null;
            }
            if (digits.equals(mileageDigits)) {
                return null;
            }
            return digits;
        }

        private boolean isUnlikelyLot(String digits, String mileageDigits, String text) {
            if (digits.equals(mileageDigits)) {
                return true;
            }
            if (digits.matches(RegexConstants.AuctionSheet.Specs.ENGINE_SIZES)) {
                return true;
            }
            if (text.contains(digits + " km") || text.contains(digits + "km")) {
                return true;
            }
            return RegexConstants.Identifiers.chassisContainingDigits(digits).matcher(text.toUpperCase()).find();
        }

        private int countToken(String text, String token) {
            Matcher matcher = RegexConstants.Identifiers.tokenNotTouchingDigits(token).matcher(text);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            return count;
        }

        private String extractAuctionGrade(String text) {
            Matcher matcher = RegexConstants.AuctionSheet.Grade.EVALUATION
                    .matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            String labeled = extract(text, new String[] {
                    "評価点", "評点", "点数", "総合評価", "評価", "Evaluation", "Rating", "Auction grade", "Overall grade"
            });
            if (labeled != null) {
                Matcher value = RegexConstants.AuctionSheet.Grade.EVAL_VALUE.matcher(labeled.trim());
                if (value.find()) {
                    return value.group(1);
                }
            }
            Matcher besideGrade = RegexConstants.AuctionSheet.Grade.G_GRADE.matcher(text);
            if (besideGrade.find()) {
                return besideGrade.group(1);
            }
            return null;
        }

        private String extractPanelGrade(String text, String... labels) {
            StringBuilder alt = new StringBuilder();
            for (String item : labels) {
                if (alt.length() > 0) {
                    alt.append('|');
                }
                alt.append(Pattern.quote(item));
            }
            String label = RegexConstants.Labeled.nonCapturingGroup(alt.toString())
                    + RegexConstants.AuctionSheet.Grade.NOT_COLOR;
            Matcher matcher = RegexConstants.AuctionSheet.Grade.gradeLetterAfterLabel(label)
                    .matcher(text);
            if (matcher.find() && isGradeLetterContext(text, matcher.start(1))) {
                return matcher.group(1).toUpperCase();
            }
            Matcher nextLine = RegexConstants.AuctionSheet.Grade.gradeLetterOnNextLineAfterLabel(label).matcher(text);
            if (nextLine.find() && isGradeLetterContext(text, nextLine.start(1))) {
                return nextLine.group(1).toUpperCase();
            }
            return null;
        }

        private String extractGrade(String text) {
            String raw = extract(text, new String[] {"グレード", "Vehicle grade", "Trim"});
            if (isVehicleGrade(raw)) {
                return Objects.requireNonNull(raw).replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
            }
            Matcher labeled = RegexConstants.AuctionSheet.Identity.GRADE_NAME
                    .matcher(text);
            if (labeled.find()) {
                String grade = cleanValue(labeled.group(1));
                if (isVehicleGrade(grade) && !isFieldLabel(grade)) {
                    return grade.replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
                }
            }
            return null;
        }

        private boolean isVehicleGrade(String value) {
            if (!hasContent(value)) {
                return false;
            }
            return !value.trim().matches(RegexConstants.AuctionSheet.Grade.GRADE_TOKEN);
        }

        private String extractBodyStyle(String text) {
            String labeled = extract(text, new String[] {"ドア形状", "ドア・形状", "Door shape", "Doors"});
            if (labeled != null && labeled.toUpperCase().contains("WD") && !labeled.contains("ドア")
                    && !labeled.toUpperCase().contains("SD") && !labeled.toUpperCase().contains("HB")) {
                labeled = null;
            }
            String fromLabel = bodyStyleFromToken(labeled);
            if (fromLabel != null) {
                return fromLabel;
            }
            return bodyStyleFromToken(text);
        }

        private String bodyStyleFromToken(String text) {
            if (text == null) {
                return null;
            }
            String upper = text.toUpperCase();
            if (text.length() <= 40 && upper.contains("WD") && !text.contains("ドア")
                    && !upper.contains("SD") && !upper.contains("HB") && !text.contains("ハコ")) {
                return null;
            }
            Matcher any = RegexConstants.AuctionSheet.Specs.DOORS_ANY
                    .matcher(text);
            if (any.find()) {
                String n = any.group(1) != null ? any.group(1) : (any.group(2) != null ? any.group(2) : any.group(3));
                return n + "-door";
            }
            Matcher doorsAfterLabel = RegexConstants.AuctionSheet.Specs.DOORS_LABELED
                    .matcher(text);
            if (doorsAfterLabel.find()) {
                return doorsAfterLabel.group(1) + "-door";
            }
            String digits = text.replaceAll(RegexConstants.Text.DIGITS_ONLY, "");
            if (digits.length() == 1 && digits.charAt(0) >= '2' && digits.charAt(0) <= '5') {
                return digits + "-door";
            }
            return null;
        }

        private String extractSeats(String text) {
            Matcher matcher = RegexConstants.AuctionSheet.Specs.SEATS_LABELED.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            Matcher people = RegexConstants.AuctionSheet.Specs.SEATS_PEOPLE.matcher(text);
            if (people.find()) {
                return people.group(1);
            }
            return digitsOnly(extract(text, new String[] {"乗車定員", "定員", "Seating capacity", "Passengers"}));
        }

        private String extractFuel(String text) {
            if (text.contains("ハイブリッド") || text.contains("ハイプリッド")
                    || text.toUpperCase().contains("HYBRID")) {
                return "Hybrid";
            }
            String labeled = translateFuel(extract(text, new String[] {"燃料", "Fuel"}));
            if (labeled != null && (labeled.contains("Gasoline") || labeled.contains("Petrol") || labeled.contains("Diesel")
                    || labeled.contains("Hybrid") || labeled.contains("Electric"))) {
                if (labeled.contains("Gasoline") || labeled.contains("Petrol")) {
                    return "Gasoline";
                }
                if (labeled.contains("Diesel")) {
                    return "Diesel";
                }
                if (labeled.contains("Electric")) {
                    return "Electric";
                }
                return "Hybrid";
            }
            if (text.contains("ガソリン") || text.contains("ガソ") || text.contains("がソ") || text.contains("ヵソ")
                    || text.contains("カソ") || text.toUpperCase().contains("GASOLINE")
                    || text.toUpperCase().contains("PETROL")) {
                return "Gasoline";
            }
            if (text.contains("軽油") || text.contains("ディーゼル") || text.toUpperCase().contains("DIESEL")) {
                return "Diesel";
            }
            if (text.contains("電気")) {
                return "Electric";
            }
            return labeled;
        }

        private String extractTransmission(String text) {
            String fromLabel = translateTransmission(extract(text, new String[] {
                    "シフト", "ミッション", "Shift", "Transmission", "Gear"
            }));
            if (fromLabel != null && fromLabel.matches(RegexConstants.AuctionSheet.Grade.TRANSMISSION)) {
                return fromLabel;
            }
            String upper = text.toUpperCase();
            if (upper.contains("CVT") || text.contains("無段")) {
                return "CVT";
            }
            if (upper.contains("IAT")) {
                return "IAT";
            }
            if (upper.contains("F.AT") || upper.contains("FAT")) {
                return "F.AT";
            }
            if (RegexConstants.AuctionSheet.Grade.AT.matcher(upper).find() || text.contains("オート") || text.contains("自動")) {
                return "AT";
            }
            if (upper.contains("MT") || text.contains("マニュアル") || text.contains("手動")) {
                return "MT";
            }
            return fromLabel;
        }

        private String extractAc(String text) {
            Matcher labeled = RegexConstants.AuctionSheet.Grade.AC_LABELED
                    .matcher(text.toUpperCase());
            if (labeled.find()) {
                return labeled.group(1);
            }
            String upper = text.toUpperCase();
            if (upper.contains("AAC")) {
                return "AAC";
            }
            Matcher ac = RegexConstants.AuctionSheet.Grade.AC.matcher(upper);
            if (ac.find()) {
                return "AC";
            }
            return null;
        }

        private String extractHistory(String text) {
            String labeled = extract(text, new String[] {"車歴", "History", "Vehicle history"});
            String source = labeled != null ? labeled : text;
            if (source.contains("レンタ") || (labeled != null && labeled.matches(RegexConstants.AuctionSheet.Grade.RENTAL))) {
                return "Rental";
            }
            if (source.contains("自家用") || (labeled != null && (labeled.contains("自家") || labeled.matches(RegexConstants.AuctionSheet.Grade.PRIVATE)))) {
                return "Private";
            }
            if (source.contains("事業") || (labeled != null && labeled.matches(RegexConstants.AuctionSheet.Grade.COMMERCIAL))) {
                return "Commercial";
            }
            if (labeled != null && labeled.matches(RegexConstants.AuctionSheet.Grade.HISTORY)) {
                return labeled;
            }
            return null;
        }

        private void extractDimensions(AuctionParseResult result, String text) {
            Matcher times = RegexConstants.AuctionSheet.Specs.DIMENSIONS.matcher(text);
            if (times.find()) {
                putDimension(result, times.group(1), times.group(2), times.group(3));
                return;
            }
            Matcher labeled = RegexConstants.AuctionSheet.Specs.DIMENSIONS_LABELED
                    .matcher(text);
            if (labeled.find()) {
                putDimension(result, labeled.group(1), labeled.group(2), labeled.group(3));
                return;
            }
            Matcher length = RegexConstants.AuctionSheet.Specs.LENGTH.matcher(text);
            Matcher width = RegexConstants.AuctionSheet.Specs.WIDTH.matcher(text);
            Matcher height = RegexConstants.AuctionSheet.Specs.HEIGHT.matcher(text);
            if (length.find() && width.find() && height.find()) {
                putDimension(result, length.group(1), width.group(1), height.group(1));
                return;
            }
            Matcher loose = RegexConstants.AuctionSheet.Specs.DIMENSIONS_LOOSE.matcher(text);
            while (loose.find()) {
                if (putDimension(result, loose.group(1), loose.group(2), loose.group(3))) {
                    return;
                }
            }
        }

        private boolean putDimension(AuctionParseResult result, String length, String width, String height) {
            int l = Integer.parseInt(length);
            int w = Integer.parseInt(width);
            int h = Integer.parseInt(height);
            if (l >= 250 && l <= 600 && w >= 120 && w <= 220 && h >= 120 && h <= 220) {
                result.put("lengthCm", l + " cm");
                result.put("widthCm", w + " cm");
                result.put("heightCm", h + " cm");
                return true;
            }
            return false;
        }

        private String extractColor(String text) {
            Matcher labeled = RegexConstants.AuctionSheet.Color.NAME_LABEL
                    .matcher(text);
            while (labeled.find()) {
                String candidate = usableColorName(sameLineAfter(text, labeled.end()));
                if (candidate == null) {
                    String next = nextLine(text, labeled.end());
                    if (!looksLikeChassis(next) && !looksLikeModelCode(next)) {
                        candidate = usableColorName(next);
                    }
                }
                if (candidate != null) {
                    return candidate;
                }
            }
            return null;
        }

        private String usableColorName(String value) {
            if (!hasContent(value) || isFieldLabel(value) || isSpecToken(value) || isPanelGradeToken(value)) {
                return null;
            }
            if (isPaintColorCode(value)) {
                return null;
            }
            String translated = translateColor(value);
            if (!hasContent(translated) || isPaintColorCode(translated) || isPanelGradeToken(translated)) {
                return null;
            }
            String cleaned = getString(translated);
            if (!hasContent(cleaned) || isPaintColorCode(cleaned) || isPanelGradeToken(cleaned)
                    || looksLikeChassis(cleaned) || looksLikeModelCode(cleaned)) {
                return null;
            }
            return cleaned;
        }

        private static String getString(String translated) {
            String cleaned = translated.replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
            cleaned = cleaned.replaceAll(RegexConstants.AuctionSheet.Color.STRIP_ROLE, "");
            cleaned = cleaned.replaceAll(RegexConstants.AuctionSheet.Color.STRIP_WORD, "").trim();
            cleaned = cleaned.replaceAll(RegexConstants.AuctionSheet.Color.STRIP_CODE, "").trim();
            cleaned = cleaned.replaceAll(RegexConstants.AuctionSheet.Color.STRIP_CHASSIS, "").trim();
            cleaned = cleaned.replaceAll(RegexConstants.AuctionSheet.Color.STRIP_MODEL, "").trim();
            cleaned = cleaned.replaceAll(RegexConstants.Text.WHITESPACE, " ").trim();
            return cleaned;
        }

        private String extractColorCode(String text) {
            Matcher labeled = RegexConstants.AuctionSheet.Color.CODE_LABEL
                    .matcher(text);
            while (labeled.find()) {
                String code = firstPaintColorCode(sameLineAfter(text, labeled.end()), text);
                if (code != null) {
                    return code;
                }
                String next = nextLine(text, labeled.end());
                if (looksLikeChassis(next) || looksLikeModelCode(next)) {
                    continue;
                }
                code = firstPaintColorCode(next, text);
                if (code != null) {
                    return code;
                }
            }
            return firstPaintColorCode(looseWhiteColorWindow(text), text);
        }

        private String sameLineAfter(String text, int from) {
            if (from >= text.length()) {
                return "";
            }
            int end = text.indexOf('\n', from);
            if (end < 0) {
                end = text.length();
            }
            return cleanValue(text.substring(from, end));
        }

        private String looseWhiteColorWindow(String text) {
            Matcher loose = RegexConstants.Identifiers.WHITE_CODE.matcher(text.toUpperCase());
            if (loose.find() && !text.toUpperCase().contains(loose.group(1) + "-")) {
                return loose.group(1);
            }
            return null;
        }

        private String firstPaintColorCode(String value, String wholeText) {
            if (value == null) {
                return null;
            }
            Matcher matcher = RegexConstants.Identifiers.COLOR_CODE_CAPTURE
                    .matcher(value.toUpperCase());
            String upper = wholeText == null ? value.toUpperCase() : wholeText.toUpperCase();
            while (matcher.find()) {
                String code = matcher.group(1);
                if (isIgnoredColorCode(code) || upper.contains(code + "-") || isPanelGradeToken(code)) {
                    continue;
                }
                return code;
            }
            return null;
        }

        private boolean isPaintColorCode(String value) {
            return value != null && value.trim().toUpperCase().matches(RegexConstants.Identifiers.COLOR_CODE);
        }

        private boolean isPanelGradeToken(String value) {
            return value != null && value.trim().matches(RegexConstants.AuctionSheet.Grade.PANEL_TOKEN_MATCH);
        }

        private boolean looksLikeChassis(String value) {
            return value != null && RegexConstants.Identifiers.CHASSIS_TOKEN
                    .matcher(value).find();
        }

        private boolean looksLikeModelCode(String value) {
            return value != null && RegexConstants.Identifiers.MODEL_CODE.matcher(value).find();
        }

        private String colorFromCode(String code) {
            if (code == null) {
                return null;
            }
            String key = code.toUpperCase();
            if (key.matches(RegexConstants.AuctionSheet.Color.WHITE)) {
                return "White";
            }
            if (key.matches(RegexConstants.AuctionSheet.Color.BLACK)) {
                return "Black";
            }
            if (key.matches(RegexConstants.AuctionSheet.Color.SILVER)) {
                return "Silver";
            }
            return null;
        }

        private boolean isIgnoredColorCode(String code) {
            return code.matches(RegexConstants.AuctionSheet.Color.IGNORED);
        }

        private void fillPanelGradesFromNoise(AuctionParseResult result, String text) {
            if (result.getFields().get("exteriorGrade") != null && result.getFields().get("interiorGrade") != null) {
                return;
            }
            Matcher pair = RegexConstants.AuctionSheet.Grade.PAIR.matcher(text);
            if (pair.find()) {
                if (result.getFields().get("exteriorGrade") == null) {
                    result.put("exteriorGrade", pair.group(1).toUpperCase());
                }
                if (result.getFields().get("interiorGrade") == null) {
                    result.put("interiorGrade", pair.group(2).toUpperCase());
                }
            }
        }

        private String cleanValue(String value) {
            if (value == null) {
                return null;
            }
            String cleaned = value.replaceAll(RegexConstants.Text.PIPE, " ").trim();
            String[] stop = new String[] {"車台番号", "車名", "型式", "年式", "走行", "カラー", "外装色", "内装色",
                    "排気量", "シフト", "燃料", "評価点", "出品番号", "ドア形状", "乗車定員", "グレード", "車検",
                    "車歴", "エアコン", "諸元", "セールスポイント", "純正装備", "リサイクル",
                    "Chassis number", "Vehicle name", "Car name", "Model code", "First registration",
                    "Mileage", "Exterior color", "Interior color", "Displacement", "Engine size",
                    "Transmission", "Fuel", "Evaluation", "Lot number", "Door shape",
                    "Seating capacity", "Grade", "History", "Air conditioner", "Specifications"};
            String lower = cleaned.toLowerCase();
            for (String s : stop) {
                int idx = lower.indexOf(s.toLowerCase());
                if (idx > 0) {
                    cleaned = cleaned.substring(0, idx);
                    lower = cleaned.toLowerCase();
                }
            }
            return cleaned.trim();
        }

        private boolean isFieldLabel(String value) {
            if (value == null) {
                return false;
            }
            String trimmed = value.trim();
            String[] labels = new String[] {"車台番号", "車名", "型式", "年式", "走行", "カラー", "外装色", "内装色",
                    "排気量", "シフト", "燃料", "評価点", "出品番号", "ドア形状", "乗車定員", "グレード", "車検",
                    "車歴", "エアコン", "諸元", "セールスポイント", "純正装備", "リサイクル", "登録番号",
                    "最大積載", "輸入車", "保証書", "検査員", "注意事項",
                    "Chassis number", "Vehicle name", "Car name", "Model name", "Model code",
                    "First registration", "Mileage", "Exterior color", "Interior color",
                    "Displacement", "Engine size", "Transmission", "Fuel", "Evaluation",
                    "Lot number", "Door shape", "Seating capacity", "Grade", "History",
                    "Air conditioner", "Specifications"};
            String lower = trimmed.toLowerCase();
            for (String label : labels) {
                String key = label.toLowerCase();
                if (lower.equals(key) || lower.startsWith(key)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isSpecToken(String value) {
            if (value == null) {
                return false;
            }
            String token = value.trim().toUpperCase().split(RegexConstants.Text.WHITESPACE)[0].replace(".", "");
            return token.matches(RegexConstants.AuctionSheet.Grade.SPEC_TOKEN);
        }

        private boolean hasContent(String value) {
            return value != null && !value.replaceAll(RegexConstants.Text.LABEL_PUNCT, "").isEmpty();
        }

        private String digitsOnly(String value) {
            if (value == null) {
                return null;
            }
            String digits = value.replaceAll(RegexConstants.Text.DIGITS_ONLY, "");
            return digits.isEmpty() ? null : digits;
        }

        private String translateMaker(String value) {
            return translator.translateJaToEn(value);
        }

        private String translateColor(String value) {
            return translator.translateJaToEn(value);
        }

        private String translateTransmission(String value) {
            String source = value == null ? "" : value.toUpperCase();
            if (source.contains("CVT") || (value != null && value.contains("無段"))) {
                return "CVT";
            }
            if (source.contains("IAT")) {
                return "IAT";
            }
            if (source.contains("F.AT") || source.contains("FAT")) {
                return "F.AT";
            }
            if (source.contains("AT") || (value != null && (value.contains("オート") || value.contains("自動")))) {
                return "AT";
            }
            if (source.contains("MT") || (value != null && (value.contains("マニュアル") || value.contains("手動")))) {
                return "MT";
            }
            return value;
        }

        private String translateFuel(String value) {
            return translator.translateJaToEn(value);
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }

        private String firstNonNull(String first, String second) {
            if (first != null && !first.trim().isEmpty()) {
                return first;
            }
            return second;
        }
    }
}
