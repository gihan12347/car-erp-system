package com.carsale.erp.shared.document.document;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts B/L number and date of B(s)/L issue from a NYK / ocean bill of lading.
 * Landing cost and exchange rate are entered manually on the form.
 */
@Component
public class BillOfLading implements DocumentParser {

    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<>();
    private final String processorId;

    static {
        TYPE_TO_FIELD.put("bl_no", "blNo");
        TYPE_TO_FIELD.put("blno", "blNo");
        TYPE_TO_FIELD.put("bl_number", "blNo");
        TYPE_TO_FIELD.put("blnumber", "blNo");
        TYPE_TO_FIELD.put("b_l_no", "blNo");
        TYPE_TO_FIELD.put("b_l_number", "blNo");
        TYPE_TO_FIELD.put("bill_of_lading_no", "blNo");
        TYPE_TO_FIELD.put("bill_of_lading_number", "blNo");
        TYPE_TO_FIELD.put("date_of_bl_issue", "dateOfBlIssue");
        TYPE_TO_FIELD.put("dateofblissue", "dateOfBlIssue");
        TYPE_TO_FIELD.put("date_of_b_l_issue", "dateOfBlIssue");
        TYPE_TO_FIELD.put("date_of_bs_l_issue", "dateOfBlIssue");
        TYPE_TO_FIELD.put("bl_issue_date", "dateOfBlIssue");
    }

    public BillOfLading(@Value("${app.ocr.documentAi.billOfLandingProcessorId:}") String processorId) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("bill of lading was empty.");
            return result;
        }
        fillFromText(result, CustomsDocumentParserUtils.normalize(text));
        CustomsDocumentParserUtils.finish(result, "Bill of lading");
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
                result.put(field, cleanValue(field, entity.getValue()));
            }
        }
        fillMissingFromText(result, documentAi.getText());
        CustomsDocumentParserUtils.finish(result, "Document AI (bill of lading)");
        return result;
    }

    @Override
    public String getProcessorId() {
        return this.processorId;
    }

    @Override
    public String getDocumentName() {
        return "bill of landing";
    }

    public static String mapType(String type) {
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

    public static String cleanValue(String field, String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        value = value.replaceAll(RegexConstants.Text.LEADING_PUNCT, "").replaceAll(RegexConstants.Text.TRAILING_PUNCT, "").trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("blNo".equals(field)) {
            return cleanBlNo(value);
        }
        return value;
    }

    private static void fillFromText(AuctionParseResult result, String text) {
        result.put("blNo", extractBlNo(text));
        result.put("dateOfBlIssue", extractDateOfBlIssue(text));
    }

    private static void fillMissingFromText(AuctionParseResult result, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String normalized = CustomsDocumentParserUtils.normalize(text);
        if (!result.getFields().containsKey("blNo")) {
            result.put("blNo", extractBlNo(normalized));
        }
        if (!result.getFields().containsKey("dateOfBlIssue")) {
            result.put("dateOfBlIssue", extractDateOfBlIssue(normalized));
        }
    }

    static String extractBlNo(String text) {
        if (text == null) {
            return null;
        }
        Matcher labeled = RegexConstants.OceanBill.NO_LABELED.matcher(text);
        if (labeled.find()) {
            return cleanBlNo(labeled.group(1));
        }
        Matcher nyk = RegexConstants.OceanBill.NO_NYK.matcher(text);
        if (nyk.find()) {
            return cleanBlNo(nyk.group(1));
        }
        return null;
    }

    static String extractDateOfBlIssue(String text) {
        if (text == null) {
            return null;
        }
        String labeled = firstGroup(RegexConstants.OceanBill.DATE_OF_BL_ISSUE, text);
        if (labeled != null) {
            return cleanDate(labeled);
        }
        String afterPlace = firstGroup(RegexConstants.OceanBill.DATE_AFTER_PLACE_OF_ISSUE, text);
        if (afterPlace != null) {
            return cleanDate(afterPlace);
        }
        String afterPlaceNumeric = firstGroup(RegexConstants.OceanBill.DATE_AFTER_PLACE_OF_ISSUE_NUMERIC, text);
        if (afterPlaceNumeric != null) {
            return cleanDate(afterPlaceNumeric);
        }
        String dated = firstGroup(RegexConstants.OceanBill.DATE_AFTER_DATED, text);
        if (dated != null) {
            return cleanDate(dated);
        }
        Matcher monthName = RegexConstants.Dates.MONTH_NAME_DATE.matcher(text);
        String last = null;
        while (monthName.find()) {
            last = monthName.group(1);
        }
        return cleanDate(last);
    }

    private static String firstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && !group.trim().isEmpty()) {
                    return group;
                }
            }
        }
        return null;
    }

    private static String cleanBlNo(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll(RegexConstants.Text.COMPACT_PUNCT, "").toUpperCase(Locale.ROOT);
        if (cleaned.length() < 6) {
            return null;
        }
        return cleaned;
    }

    private static String cleanDate(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        cleaned = cleaned.replaceAll(RegexConstants.Text.LEADING_PUNCT, "").replaceAll(RegexConstants.Text.TRAILING_PUNCT, "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
