package com.carsale.erp.shared.document.document;

import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Parses Sri Lanka Customs Assessment Notice (ASYCUDA).
 * OCR often places the tax value on the line above the tax code, and inserts
 * spaces after thousands separators (e.g. "805, 692", "3, 346,743").
 */
@Component
public class AssessmentNotice implements DocumentParser {

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
        result.setRawText(text);
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The assessment notice was empty.");
            return result;
        }
        result.setSuccess(false);
        result.setMessage("The document was read, but fields could not be mapped. Please fill them manually.");
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
