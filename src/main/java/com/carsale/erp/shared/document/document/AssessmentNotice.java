package com.carsale.erp.shared.document.document;

import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Sri Lanka Customs Assessment Notice (ASYCUDA).
 * OCR often places the tax value on the line above the tax code, and inserts
 * spaces after thousands separators (e.g. "805, 692", "3, 346,743").
 */
@Component
public class AssessmentNotice implements DocumentParser {

    private static final String[] ITEM_CODES = {"CID", "SUR", "XID", "VAT", "VEL"};
    private static final String[] GLOBAL_CODES = {"OTC", "COM", "EXM"};

    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<>();
    private static final Map<String, String> TAX_CODE_TO_FIELD = new LinkedHashMap<>();
    private final String processorId;

    static {
        TYPE_TO_FIELD.put("total_amount_paid", "assessmentTotalPaid");
        TYPE_TO_FIELD.put("totalamountpaid", "assessmentTotalPaid");
        TYPE_TO_FIELD.put("assessment_total_paid", "assessmentTotalPaid");
        TYPE_TO_FIELD.put("total_assessed_amount", "assessmentTotalAssessed");
        TYPE_TO_FIELD.put("totalassessedamount", "assessmentTotalAssessed");
        TYPE_TO_FIELD.put("assessment_total_assessed", "assessmentTotalAssessed");
        TYPE_TO_FIELD.put("total_assessed", "assessmentTotalAssessed");
        TYPE_TO_FIELD.put("assessment_office", "assessmentOffice");
        TYPE_TO_FIELD.put("assessment_notice_ref", "assessmentNoticeRef");
        TYPE_TO_FIELD.put("assessment_model", "assessmentModel");
        TYPE_TO_FIELD.put("assessment_packages", "assessmentPackages");
        TYPE_TO_FIELD.put("assessment_customs_reference", "assessmentCustomsReference");
        TYPE_TO_FIELD.put("assessment_declarant_reference", "assessmentDeclarantReference");
        TYPE_TO_FIELD.put("assessment_reference", "assessmentReference");
        TYPE_TO_FIELD.put("assessment_declarant_id", "assessmentDeclarantId");
        TYPE_TO_FIELD.put("assessment_declarant_name", "assessmentDeclarantName");
        TYPE_TO_FIELD.put("assessment_declarant_address", "assessmentDeclarantAddress");
        TYPE_TO_FIELD.put("assessment_declarant_cha_exp", "assessmentDeclarantChaExp");
        TYPE_TO_FIELD.put("assessment_consignee_id", "assessmentConsigneeId");
        TYPE_TO_FIELD.put("assessment_consignee_name", "assessmentConsigneeName");
        TYPE_TO_FIELD.put("assessment_consignee_address", "assessmentConsigneeAddress");
        TYPE_TO_FIELD.put("assessment_tax_otc", "assessmentTaxOtc");
        TYPE_TO_FIELD.put("assessment_tax_com", "assessmentTaxCom");
        TYPE_TO_FIELD.put("assessment_tax_exm", "assessmentTaxExm");
        TYPE_TO_FIELD.put("assessment_tax_cid", "assessmentTaxCid");
        TYPE_TO_FIELD.put("assessment_tax_sur", "assessmentTaxSur");
        TYPE_TO_FIELD.put("assessment_tax_xid", "assessmentTaxXid");
        TYPE_TO_FIELD.put("assessment_tax_vat", "assessmentTaxVat");
        TYPE_TO_FIELD.put("assessment_tax_vel", "assessmentTaxVel");

        TAX_CODE_TO_FIELD.put("CID", "assessmentTaxCid");
        TAX_CODE_TO_FIELD.put("SUR", "assessmentTaxSur");
        TAX_CODE_TO_FIELD.put("XID", "assessmentTaxXid");
        TAX_CODE_TO_FIELD.put("VAT", "assessmentTaxVat");
        TAX_CODE_TO_FIELD.put("VEL", "assessmentTaxVel");
        TAX_CODE_TO_FIELD.put("OTC", "assessmentTaxOtc");
        TAX_CODE_TO_FIELD.put("COM", "assessmentTaxCom");
        TAX_CODE_TO_FIELD.put("EXM", "assessmentTaxExm");
    }

    public AssessmentNotice(@Value("${app.ocr.documentAi.assessmentNoticeProcessorId:}") String processorId) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("assessment notice was empty.");
            return result;
        }
        // "805, 692" / "1,992, 000" / "3, 346,743" → normal thousands grouping
        String normalized = fixBrokenCommas(CustomsDocumentParserUtils.normalize(text));

        result.put("assessmentOffice", extractAssessmentOffice(normalized));
        result.put("assessmentNoticeRef", extractAssessmentNoticeRef(normalized));
        result.put("assessmentModel", extractAssessmentModel(normalized));
        result.put("assessmentCustomsReference", extractLabeledDateRef(normalized, "Customs reference"));
        result.put("assessmentDeclarantReference", extractDeclarantReference(normalized));
        result.put("assessmentReference", extractLabeledDateRef(normalized, "Assessment reference"));
        result.put("assessmentPackages", extractAssessmentPackages(normalized));

        String[] declarant = extractAssessmentParty(normalized, "Declarant", "Consignee");
        result.put("assessmentDeclarantId", declarant[0]);
        result.put("assessmentDeclarantName", declarant[1]);
        result.put("assessmentDeclarantAddress", declarant[2]);
        result.put("assessmentDeclarantChaExp", extractChaExpiry(normalized));

        String[] consignee = extractAssessmentParty(normalized, "Consignee", "Global taxes");
        result.put("assessmentConsigneeId", consignee[0]);
        result.put("assessmentConsigneeName", consignee[1]);
        result.put("assessmentConsigneeAddress", consignee[2]);

        Map<String, String> globalTaxes = extractTaxTable(normalized, RegexConstants.Assessment.GLOBAL_TAXES_SECTION, GLOBAL_CODES);
        result.put("assessmentTaxOtc", globalTaxes.get("OTC"));
        result.put("assessmentTaxCom", globalTaxes.get("COM"));
        result.put("assessmentTaxExm", globalTaxes.get("EXM"));

        Map<String, String> itemTaxes = extractTaxTable(normalized, RegexConstants.Assessment.ITEM_TAXES_SECTION, ITEM_CODES);
        result.put("assessmentTaxCid", itemTaxes.get("CID"));
        result.put("assessmentTaxSur", itemTaxes.get("SUR"));
        result.put("assessmentTaxXid", itemTaxes.get("XID"));
        result.put("assessmentTaxVat", itemTaxes.get("VAT"));
        result.put("assessmentTaxVel", itemTaxes.get("VEL"));

        result.put("assessmentTotalAssessed", extractLabeledAmount(normalized,
                "Total assessed amount for the declaration", "Total assessed amount"));
        result.put("assessmentTotalPaid", extractLabeledAmount(normalized,
                "Total amount paid", "Total   amount paid"));

        CustomsDocumentParserUtils.finish(result, "Assessment notice");
        return result;
    }

    @Override
    public AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi) {
        AuctionParseResult result = new AuctionParseResult();

        if (documentAi == null) {
            result.setSuccess(false);
            result.setMessage("Document AI returned no result.");
            return result;
        }
        result.setRawText(documentAi.getText());
        List<DocumentAiClient.DocumentAiEntity> entities = documentAi.getEntities();
        String pendingCode = null;
        String pendingValue = null;
        if (entities != null) {
            for (DocumentAiClient.DocumentAiEntity entity : entities) {
                if (entity == null) {
                    continue;
                }
                String type = leafType(entity.getType());
                if (isTaxGroupStart(type)) {
                    flushTax(result, pendingCode, pendingValue);
                    pendingCode = null;
                    pendingValue = null;
                    continue;
                }
                TaxParseResult taxResult = handleTaxType(
                        type,
                        entity,
                        result,
                        pendingCode,
                        pendingValue
                );
                if (taxResult.isHandled()) {
                    pendingCode = taxResult.getPendingCode();
                    pendingValue = taxResult.getPendingValue();
                    continue;
                }
                String field = mapType(entity.getType());
                if (field != null) {
                    result.put(
                            field,
                            moneyField(field)
                                    ? cleanMoney(entity.getValue())
                                    : cleanText(entity.getValue())
                    );
                }
            }
            flushTax(result, pendingCode, pendingValue);
        }

        fillMissingFromText(result, documentAi.getText());
        CustomsDocumentParserUtils.finish(result, "Document AI (assessment notice)");

        return result;
    }

    @Override
    public String getProcessorId() {
        return this.processorId;
    }

    @Override
    public String getDocumentName() {
        return "assessment notice";
    }

    private static String fixBrokenCommas(String text) {
        if (text == null) {
            return null;
        }
        // Collapse spaces after thousands separators produced by OCR
        return text.replaceAll(RegexConstants.Amounts.THOUSANDS_SPACE, ",");
    }

    private static Map<String, String> extractTaxTable(String text, Pattern sectionPattern, String[] codes) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String code : codes) {
            values.put(code, null);
        }

        String section = firstGroup(sectionPattern, text);
        String search = section != null ? section : text;

        // Primary: line-oriented parse (handles amount-above-code OCR)
        Map<String, String> fromLines = parseTaxLines(search, codes);
        for (String code : codes) {
            values.put(code, fromLines.get(code));
        }

        // Fallback: amount immediately after each code until the next code
        for (String code : codes) {
            if (values.get(code) == null) {
                values.put(code, extractCodeAmount(search, code, codes));
            }
        }

        return values;
    }

    /**
     * OCR layout seen on Assessment Notices:
     * <pre>
     * 354,867
     * CID  Customs Import Duty
     * 177,434
     * SUR  Surcharge   1,992,000   ← value before code is SUR; trailing amount is next (XID)
     * XID  Excise ...
     * VAT  Value Added Tax 805,692
     * </pre>
     */
    private static Map<String, String> parseTaxLines(String section, String[] allowedCodes) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String code : allowedCodes) {
            values.put(code, null);
        }

        String pendingAmount = null;
        String pendingCode = null;
        String[] lines = section.split(RegexConstants.Text.NEWLINE);
        for (String s : lines) {
            String line = s.trim();
            if (line.isEmpty()) {
                continue;
            }
            // Strip trailing junk like "300]"
            line = line.replaceAll(RegexConstants.Text.TRAILING_LINE_JUNK, "").trim();

            List<String> codesOnLine = codesOnLine(line, allowedCodes);
            List<String> amountsOnLine = amountsOnLine(line);

            if (codesOnLine.isEmpty()) {
                if (!amountsOnLine.isEmpty()) {
                    String amount = amountsOnLine.get(amountsOnLine.size() - 1);
                    if (pendingCode != null && values.get(pendingCode) == null) {
                        values.put(pendingCode, amount);
                        pendingCode = null;
                    } else {
                        pendingAmount = amount;
                    }
                }
                continue;
            }

            String code = codesOnLine.get(0).toUpperCase(Locale.ROOT);
            if (!values.containsKey(code) || values.get(code) != null) {
                // Unknown or already filled — keep pending for a later code if needed
                if (!amountsOnLine.isEmpty()) {
                    pendingAmount = amountsOnLine.get(amountsOnLine.size() - 1);
                }
                continue;
            }

            if (pendingAmount != null) {
                values.put(code, pendingAmount);
                pendingAmount = null;
                pendingCode = null;
                // Trailing amount on this line belongs to the next tax code
                if (!amountsOnLine.isEmpty()) {
                    pendingAmount = amountsOnLine.get(amountsOnLine.size() - 1);
                }
            } else if (!amountsOnLine.isEmpty()) {
                values.put(code, amountsOnLine.get(amountsOnLine.size() - 1));
                pendingCode = null;
            } else {
                pendingCode = code;
            }
        }

        return values;
    }

    private static List<String> codesOnLine(String line, String[] allowedCodes) {
        List<String> found = new ArrayList<>();
        Matcher matcher = RegexConstants.Assessment.CODE_ON_LINE.matcher(line);
        while (matcher.find()) {
            String code = matcher.group(1).toUpperCase(Locale.ROOT);
            for (String allowed : allowedCodes) {
                if (allowed.equals(code)) {
                    found.add(code);
                    break;
                }
            }
        }
        return found;
    }

    private static List<String> amountsOnLine(String line) {
        List<String> found = new ArrayList<>();
        Matcher matcher = RegexConstants.Amounts.GROUPED_PATTERN.matcher(line);
        while (matcher.find()) {
            String amount = normalizeAmount(matcher.group(1));
            if (amount != null) {
                found.add(amount);
            }
        }
        return found;
    }

    private static String extractCodeAmount(String text, String code, String[] allCodes) {
        String boundary = joinCodes(allCodes);
        Pattern pattern = RegexConstants.Assessment.snippetAfterTaxCode(code, boundary);
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return lastAmount(matcher.group(1));
    }

    private static String joinCodes(String[] codes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < codes.length; i++) {
            if (i > 0) {
                out.append('|');
            }
            out.append(Pattern.quote(codes[i]));
        }
        return out.toString();
    }

    private static String lastAmount(String snippet) {
        if (snippet == null || snippet.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = RegexConstants.Amounts.GROUPED_PATTERN.matcher(snippet);
        String last = null;
        while (matcher.find()) {
            last = normalizeAmount(matcher.group(1));
        }
        return last;
    }

    private static String extractLabeledAmount(String text, String... labels) {
        for (String label : labels) {
            Pattern pattern = RegexConstants.Assessment.amountAfterQuotedLabel(label);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return normalizeAmount(matcher.group(1));
            }
        }
        // OCR sometimes splits "Total amount paid"
        Matcher paid = RegexConstants.Assessment.TOTAL_AMOUNT_PAID.matcher(text);
        if (paid.find()) {
            return normalizeAmount(paid.group(1));
        }
        return null;
    }

    private static String normalizeAmount(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll(RegexConstants.Text.NON_DIGIT, "");
        if (digits.length() <= 2) {
            return null;
        }
        return formatThousands(digits);
    }

    private static String formatThousands(String digits) {
        int length = digits.length();
        if (length < 4) {
            return digits;
        }
        StringBuilder result = new StringBuilder();
        int lead = length % 3;
        if (lead == 0) {
            lead = 3;
        }
        result.append(digits, 0, lead);
        for (int i = lead; i < length; i += 3) {
            result.append(',').append(digits, i, i + 3);
        }
        return result.toString();
    }

    private static String firstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractAssessmentOffice(String text) {
        Matcher matcher = RegexConstants.Assessment.IMPORT_OFFICE.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return CustomsDocumentParserUtils.extractLabel(text, "Customs Office", "Office");
    }

    private static String extractAssessmentNoticeRef(String text) {
        Matcher matcher = RegexConstants.Assessment.NOTICE_REF.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return null;
    }

    private static String extractAssessmentModel(String text) {
        Matcher matcher = RegexConstants.Assessment.MODEL_IM.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll(RegexConstants.Text.WHITESPACE, " ").toUpperCase(Locale.ROOT);
        }
        return CustomsDocumentParserUtils.extractLabel(text, "Model");
    }

    private static String extractLabeledDateRef(String text, String label) {
        Matcher matcher = RegexConstants.Assessment.customsRefAfterQuotedLabel(label).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        matcher = RegexConstants.Assessment.customsRefNearQuotedLabel(label).matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1));
        }
        return null;
    }

    private static String extractDeclarantReference(String text) {
        Matcher matcher = RegexConstants.Assessment.DECLARANT_REFERENCE.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1).replaceAll(RegexConstants.Text.WHITESPACE, " "));
        }
        matcher = RegexConstants.Assessment.HASH_REFERENCE.matcher(text);
        if (matcher.find()) {
            return CustomsDocumentParserUtils.clean(matcher.group(1).replaceAll(RegexConstants.Text.WHITESPACE, " "));
        }
        return null;
    }

    private static String extractAssessmentPackages(String text) {
        Matcher matcher = RegexConstants.Assessment.PACKAGES.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractChaExpiry(String text) {
        Matcher matcher = RegexConstants.Assessment.CHA_EXP.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String[] extractAssessmentParty(String text, String heading, String stopHeading) {
        String[] empty = new String[] { null, null, null };
        Pattern blockPattern = RegexConstants.Assessment.sectionBetweenHeadings(heading, stopHeading);
        Matcher blockMatcher = blockPattern.matcher(text);
        if (!blockMatcher.find()) {
            return empty;
        }
        String block = blockMatcher.group();
        String id = null;
        Matcher idMatcher = RegexConstants.Assessment.PARTY_ID.matcher(block);
        if (idMatcher.find()) {
            id = idMatcher.group(1);
        }
        String name = CustomsDocumentParserUtils.extractLabel(block, "Name");
        if (name != null && (name.equalsIgnoreCase(heading) || name.toUpperCase(Locale.ROOT).startsWith("ID"))) {
            name = null;
        }
        if (name == null) {
            Matcher nameMatcher = RegexConstants.Assessment.PARTY_NAME.matcher(block);
            while (nameMatcher.find()) {
                String candidate = CustomsDocumentParserUtils.clean(nameMatcher.group(1));
                if (candidate == null) {
                    continue;
                }
                String upper = candidate.toUpperCase(Locale.ROOT);
                if (upper.equals(heading.toUpperCase(Locale.ROOT))
                        || upper.startsWith("ID")
                        || upper.startsWith("CHA")
                        || upper.contains("TAX")
                        || upper.contains("REFERENCE")) {
                    continue;
                }
                name = candidate;
                break;
            }
        }
        String address = CustomsDocumentParserUtils.extractLabel(block, "Address");
        if (address == null) {
            Matcher addressMatcher = RegexConstants.Assessment.PARTY_ADDRESS.matcher(block);
            if (addressMatcher.find()) {
                address = CustomsDocumentParserUtils.clean(addressMatcher.group(1));
            }
        }
        return new String[] { id, name, address };
    }

    public static String mapType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        String key = typeKey(type);
        String mapped = TYPE_TO_FIELD.get(key);
        if (mapped != null) {
            return mapped;
        }
        int slash = key.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < key.length()) {
            return TYPE_TO_FIELD.get(key.substring(slash + 1));
        }
        return null;
    }

    public static String cleanMoney(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        value = value.replaceAll(RegexConstants.Amounts.CURRENCY_LKR, "").trim();
        value = value.replaceAll(RegexConstants.Text.CURRENCY_CHARS, "");
        if (value.isEmpty()) {
            return null;
        }
        if (value.indexOf(',') >= 0) {
            if (value.matches(RegexConstants.Amounts.TRAILING_ZERO_DECIMALS)) {
                value = value.substring(0, value.lastIndexOf('.'));
            }
            return value;
        }
        int dot = value.lastIndexOf('.');
        String intPart = (dot >= 0 ? value.substring(0, dot) : value).replaceAll(RegexConstants.Text.NON_DIGIT, "");
        String decPart = dot >= 0 ? value.substring(dot + 1).replaceAll(RegexConstants.Text.NON_DIGIT, "") : "";
        if (intPart.isEmpty()) {
            return null;
        }
        String formatted = formatThousands(intPart);
        if (decPart.isEmpty() || decPart.matches(RegexConstants.Amounts.ALL_ZEROS)) {
            return formatted;
        }
        return formatted + "." + decPart;
    }

    public static String extractTaxCode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = RegexConstants.Assessment.CODE_ON_LINE.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static void flushTax(AuctionParseResult result, String code, String value) {
        if (code == null || value == null) {
            return;
        }
        String field = TAX_CODE_TO_FIELD.get(code.toUpperCase(Locale.ROOT));
        if (field != null) {
            result.put(field, value);
        }
    }

    private static boolean isTaxGroupStart(String type) {
        return "item_tax".equals(type) || "itemtax".equals(type)
                || "global_tax".equals(type) || "globaltax".equals(type);
    }

    private static boolean moneyField(String field) {
        return field != null && (field.startsWith("assessmentTax")
                || "assessmentTotalAssessed".equals(field)
                || "assessmentTotalPaid".equals(field));
    }

    private static String cleanText(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        return value.isEmpty() ? null : value;
    }

    private void fillMissingFromText(AuctionParseResult result, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        AuctionParseResult fromText = parsePage(text);
        if (fromText.getFields() == null || fromText.getFields().isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : fromText.getFields().entrySet()) {
            if (!result.getFields().containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static String leafType(String type) {
        String key = typeKey(type);
        int slash = key.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < key.length()) {
            return key.substring(slash + 1);
        }
        return key;
    }

    private static String typeKey(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "";
        }
        return type.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private TaxParseResult handleTaxType(
            String type,
            DocumentAiClient.DocumentAiEntity entity,
            AuctionParseResult result,
            String pendingCode,
            String pendingValue) {

        switch (type) {
            case "tax_code":
            case "taxcode":
                if (pendingCode != null && pendingValue != null) {
                    flushTax(result, pendingCode, pendingValue);
                    pendingValue = null;
                }

                pendingCode = extractTaxCode(entity.getValue());
                return new TaxParseResult(true, pendingCode, pendingValue);

            case "tax_value":
            case "taxvalue":
                pendingValue = cleanMoney(entity.getValue());

                if (pendingCode != null && pendingValue != null) {
                    flushTax(result, pendingCode, pendingValue);
                    pendingCode = null;
                    pendingValue = null;
                }

                return new TaxParseResult(true, pendingCode, pendingValue);

            case "tax_description":
            case "taxdescription":
            case "totals":
                return new TaxParseResult(true, pendingCode, pendingValue);

            default:
                return new TaxParseResult(false, pendingCode, pendingValue);
        }
    }

    private static class TaxParseResult {

        private final boolean handled;
        private final String pendingCode;
        private final String pendingValue;

        TaxParseResult(boolean handled, String pendingCode, String pendingValue) {
            this.handled = handled;
            this.pendingCode = pendingCode;
            this.pendingValue = pendingValue;
        }

        public boolean isHandled() {
            return handled;
        }

        public String getPendingCode() {
            return pendingCode;
        }

        public String getPendingValue() {
            return pendingValue;
        }
    }
}
