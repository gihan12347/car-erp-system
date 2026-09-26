package com.carsale.erp.shared.document.document;

import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Section C invoice amounts from Sri Lanka Customs Goods Declaration (CUSDEC).
 * Reads only the TOTAL INVOICE AMOUNT block — not tax totals or website / A/V figures.
 */
@Component
public class CustomsDeclaration implements DocumentParser {
    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<>();
    private final String processorId;

    static {
        TYPE_TO_FIELD.put("exchange_rate", "exchangeRate");
        TYPE_TO_FIELD.put("exchangerate", "exchangeRate");
        TYPE_TO_FIELD.put("total_invoice_amount", "invoiceTotal");
        TYPE_TO_FIELD.put("totalinvoiceamount", "invoiceTotal");
        TYPE_TO_FIELD.put("invoice_total", "invoiceTotal");
        TYPE_TO_FIELD.put("invoice_fob", "invoiceFob");
        TYPE_TO_FIELD.put("invoicefob", "invoiceFob");
        TYPE_TO_FIELD.put("fob_cif", "invoiceFob");
        TYPE_TO_FIELD.put("invoice_freight", "invoiceFreight");
        TYPE_TO_FIELD.put("invoicefreight", "invoiceFreight");
        TYPE_TO_FIELD.put("freight", "invoiceFreight");
        TYPE_TO_FIELD.put("invoice_insurance", "invoiceInsurance");
        TYPE_TO_FIELD.put("invoiceinsurance", "invoiceInsurance");
        TYPE_TO_FIELD.put("insurance", "invoiceInsurance");
        TYPE_TO_FIELD.put("invoice_other", "invoiceOther");
        TYPE_TO_FIELD.put("invoiceother", "invoiceOther");
        TYPE_TO_FIELD.put("other", "invoiceOther");
        TYPE_TO_FIELD.put("value_ncy", "valueNcy");
        TYPE_TO_FIELD.put("valuency", "valueNcy");
    }

    public CustomsDeclaration(@Value("${app.ocr.documentAi.cusdecProcessorId:}") String processorId) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("customs declaration was empty.");
            return result;
        }
        String normalized = CustomsDocumentParserUtils.normalize(text);
        String section = invoiceSection(normalized);

        result.put("exchangeRate", extractExchangeRate(normalized));
        result.put("valueNcy", extractValueNcy(normalized));

        String fob = null;
        String freight = null;
        String insurance = null;
        String other = null;
        String total = null;

        if (section != null) {
            fob = money(RegexConstants.Declaration.LINE_FOB, section);
            freight = money(RegexConstants.Declaration.LINE_FREIGHT, section);
            insurance = money(RegexConstants.Declaration.LINE_INSURANCE, section);
            other = money(RegexConstants.Declaration.LINE_OTHER, section);
            total = money(RegexConstants.Declaration.LINE_TOTAL, section);

            String[] columnar = extractColumnar(section, fob, freight, insurance, other, total);
            fob = firstNonBlank(fob, columnar[0]);
            freight = firstNonBlank(freight, columnar[1]);
            insurance = firstNonBlank(insurance, columnar[2]);
            other = firstNonBlank(other, columnar[3]);
            total = firstNonBlank(total, columnar[4]);
        }

        if (freight == null) {
            freight = money(RegexConstants.Declaration.FREIGHT_BL, normalized);
        }
        if (total == null) {
            total = money(RegexConstants.Declaration.TOTAL_INVOICED, normalized);
        }

        result.put("invoiceFob", fob);
        result.put("invoiceFreight", freight);
        result.put("invoiceInsurance", insurance);
        result.put("invoiceOther", other);
        result.put("invoiceTotal", total);

        CustomsDocumentParserUtils.finish(result, "Customs declaration");
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
        if (entities != null) {
            for (DocumentAiClient.DocumentAiEntity entity : entities) {
                if (entity == null) {
                    continue;
                }
                String field = mapType(entity.getType());
                if (field == null) {
                    continue;
                }
                String value = cleanValue(field, entity.getValue());
                result.put(field, value);
            }
        }
        CustomsDocumentParserUtils.finish(result, "Document AI (customs declaration)");
        return result;
    }

    @Override
    public String getProcessorId() {
        return this.processorId;
    }

    @Override
    public String getDocumentName() {
        return "customs declaration";
    }

    private static String invoiceSection(String text) {
        Matcher matcher = RegexConstants.Declaration.INVOICE_SECTION.matcher(text);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        if (last != null && looksLikeInvoiceSection(last)) {
            return last;
        }
        Matcher fallback = RegexConstants.Declaration.SECTION_FALLBACK.matcher(text);
        String block = null;
        while (fallback.find()) {
            block = fallback.group();
        }
        return block;
    }

    private static boolean looksLikeInvoiceSection(String section) {
        String upper = section.toUpperCase();
        return upper.contains("FOB") || upper.contains("FREIGHT") || upper.contains("INSURANCE");
    }

    /**
     * Handles OCR that prints labels first, then amounts with JPY:
     * FOB/CIF, FREIGHT, INSURANCE, OTHER, TOTAL → then 76,970.04 JPY / 5,000.00 JPY / total.
     * OTHER stays empty when the form has a dash and only three amounts remain.
     */
    private static String[] extractColumnar(
            String section,
            String fob,
            String freight,
            String insurance,
            String other,
            String total
    ) {
        List<String> labels = new ArrayList<>();
        Matcher labelMatcher = RegexConstants.Declaration.LABEL_ONLY.matcher(section);
        while (labelMatcher.find()) {
            String kind = normalizeLabel(labelMatcher.group());
            if (!labels.contains(kind)) {
                labels.add(kind);
            }
        }

        List<String> amounts = new ArrayList<>();
        Matcher amountMatcher = RegexConstants.Declaration.ORPHAN_AMOUNT.matcher(section);
        while (amountMatcher.find()) {
            String value = normalizeMoney(amountMatcher.group(1));
            if (value != null) {
                amounts.add(value);
            }
        }

        // Drop amounts already captured on the same line as a label
        if (fob != null) {
            removeFirst(amounts, fob);
        }
        if (freight != null) {
            removeFirst(amounts, freight);
        }
        if (insurance != null) {
            removeFirst(amounts, insurance);
        }
        if (other != null) {
            removeFirst(amounts, other);
        }
        if (total != null) {
            removeFirst(amounts, total);
        }

        List<String> missing = new ArrayList<>();
        for (String label : labels) {
            if ("FOB".equals(label) && fob == null) {
                missing.add(label);
            } else if ("FREIGHT".equals(label) && freight == null) {
                missing.add(label);
            } else if ("INSURANCE".equals(label) && insurance == null) {
                missing.add(label);
            } else if ("TOTAL".equals(label) && total == null) {
                missing.add(label);
            }
            // OTHER is usually a dash on the form — only accept an explicit same-line amount
        }

        int index = 0;
        for (String label : missing) {
            if (index >= amounts.size()) {
                break;
            }
            String value = amounts.get(index++);
            if ("FOB".equals(label)) {
                fob = value;
            } else if ("FREIGHT".equals(label)) {
                freight = value;
            } else if ("INSURANCE".equals(label)) {
                insurance = value;
            } else if ("TOTAL".equals(label)) {
                total = value;
            }
        }

        if (total == null && !amounts.isEmpty()) {
            total = amounts.get(amounts.size() - 1);
        }

        return new String[] { fob, freight, insurance, other, total };
    }

    private static void removeFirst(List<String> amounts, String value) {
        for (int i = 0; i < amounts.size(); i++) {
            if (sameAmount(amounts.get(i), value)) {
                amounts.remove(i);
                return;
            }
        }
    }

    private static boolean sameAmount(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.replace(",", "").equals(right.replace(",", ""));
    }

    private static String normalizeLabel(String label) {
        String upper = label.toUpperCase().replace(" ", "");
        if (upper.contains("FOB") || upper.startsWith("F0")) {
            return "FOB";
        }
        if (upper.contains("FREIGHT")) {
            return "FREIGHT";
        }
        if (upper.contains("INSURANCE")) {
            return "INSURANCE";
        }
        if (upper.contains("OTHER")) {
            return "OTHER";
        }
        if (upper.contains("TOTAL")) {
            return "TOTAL";
        }
        return upper;
    }

    private static String extractExchangeRate(String text) {
        Matcher matcher = RegexConstants.Declaration.EXCHANGE_RATE.matcher(text);
        String value = null;
        while (matcher.find()) {
            value = matcher.group(1);
        }
        return value;
    }

    private static String extractValueNcy(String text) {
        return firstNonBlank(money(RegexConstants.Declaration.VALUE_NCY, text), money(RegexConstants.Declaration.VALUE_NCY_NEAR, text));
    }

    private static String money(Pattern pattern, String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return normalizeMoney(matcher.group(1));
        }
        return null;
    }

    private static String normalizeMoney(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        boolean hasDot = value.indexOf('.') >= 0;
        boolean hasComma = value.indexOf(',') >= 0;
        if (hasDot && hasComma) {
            int lastSep = Math.max(value.lastIndexOf('.'), value.lastIndexOf(','));
            String intPart = value.substring(0, lastSep).replaceAll(RegexConstants.Text.COMMA_OR_DOT, "");
            String decPart = value.substring(lastSep + 1).replaceAll(RegexConstants.Text.NON_DIGIT, "");
            if (intPart.isEmpty()) {
                return null;
            }
            return withThousands(intPart) + (decPart.isEmpty() ? "" : "." + decPart);
        }
        if (value.matches(RegexConstants.Amounts.EUROPEAN_MONEY)) {
            int lastDot = value.lastIndexOf('.');
            String intPart = value.substring(0, lastDot).replace(".", "");
            return withThousands(intPart) + value.substring(lastDot);
        }
        return value;
    }

    private static String withThousands(String digits) {
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                out.insert(0, ',');
            }
            out.insert(0, digits.charAt(i));
            count++;
        }
        return out.toString();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    static String mapType(String type) {

        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        String key = type.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
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

    static String cleanValue(String field, String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        value = value.replaceAll(RegexConstants.Amounts.CURRENCY_JPY, "").trim();
        value = value.replaceAll(RegexConstants.Text.LEADING_PUNCT, "").replaceAll(RegexConstants.Text.TRAILING_DASH, "").trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("exchangeRate".equals(field)) {
            return value.replace(",", "");
        }
        return value;
    }
}
