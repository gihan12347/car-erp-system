package com.carsale.erp.shared.document.document;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StandardsCertificateParser implements DocumentParser {

    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<>();
    private static final Set<String> CHECKBOX_FIELDS = new HashSet<>();

    static {
        TYPE_TO_FIELD.put("abs", "absFitted");
        TYPE_TO_FIELD.put("air_bags_driver", "driverAirbag");
        TYPE_TO_FIELD.put("air_bags_front_passenger", "passengerAirbag");
        TYPE_TO_FIELD.put("certificate_number", "certificateNo");
        TYPE_TO_FIELD.put("ch4", "emissionCh4");
        TYPE_TO_FIELD.put("co", "emissionCo");
        TYPE_TO_FIELD.put("hc", "emissionHc");
        TYPE_TO_FIELD.put("hc_plus_nox", "emissionHcNox");
        TYPE_TO_FIELD.put("nmhc", "emissionNmhc");
        TYPE_TO_FIELD.put("nox", "emissionNox");
        TYPE_TO_FIELD.put("pm", "emissionPm");
        TYPE_TO_FIELD.put("remarks", "remarks");
        TYPE_TO_FIELD.put("seat_belts_driver_front", "threePointSeatBelts");
        TYPE_TO_FIELD.put("seat_belts_other", "twoPointSeatBelts");
        TYPE_TO_FIELD.put("smoke", "emissionSmoke");
        TYPE_TO_FIELD.put("thc", "emissionThc");

        CHECKBOX_FIELDS.add("absFitted");
        CHECKBOX_FIELDS.add("driverAirbag");
        CHECKBOX_FIELDS.add("passengerAirbag");
        CHECKBOX_FIELDS.add("threePointSeatBelts");
        CHECKBOX_FIELDS.add("twoPointSeatBelts");
    }

    private final String processorId;

    public StandardsCertificateParser(
            @Value("${app.ocr.documentAi.standardsCertificateProcessorId:}") String processorId
    ) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        return null;
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
                if (field == null || result.getFields().containsKey(field)) {
                    continue;
                }
                result.put(field, cleanDocumentAiValue(field, entity.getValue()));
            }
        }
        if (result.getFields().isEmpty()) {
            result.setSuccess(true);
            result.setMessage("");
            return result;
        }
        CustomsDocumentParserUtils.finish(result, "Document AI (standards certificate)");
        return result;
    }

    @Override
    public String getProcessorId() {
        return this.processorId;
    }

    @Override
    public String getDocumentName() {
        return "standards certificate";
    }

    @Override
    public String getOcrLanguage() {
        return "eng";
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

    static String cleanDocumentAiValue(String field, String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        String stripped = value.replaceAll(RegexConstants.Text.LEADING_PUNCT, "")
                .replaceAll(RegexConstants.Text.TRAILING_PUNCT, "")
                .trim();
        if (!stripped.isEmpty()) {
            value = stripped;
        }
        if (value.isEmpty()) {
            return null;
        }
        if (CHECKBOX_FIELDS.contains(field)) {
            return checkboxValue(value);
        }
        return value;
    }

    static String checkboxValue(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.equals("true")
                || value.equals("yes")
                || value.equals("y")
                || value.equals("1")
                || value.equals("checked")
                || value.indexOf('\u2713') >= 0
                || value.indexOf('\u2714') >= 0
                || value.indexOf('\u2611') >= 0) {
            return "true";
        }
        return "false";
    }
}
