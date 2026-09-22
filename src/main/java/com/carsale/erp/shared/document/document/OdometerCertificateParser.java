package com.carsale.erp.shared.document.document;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OdometerCertificateParser implements DocumentParser {

    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<>();
    private static final Set<String> DATE_FIELDS = new HashSet<>();

    static {
        TYPE_TO_FIELD.put("certificate_no", "jevicCertificateNo");
        TYPE_TO_FIELD.put("chassis_vin", "jevicChassisVin");
        TYPE_TO_FIELD.put("current_odometer_reading", "jevicCurrentOdometer");
        TYPE_TO_FIELD.put("date_of_inspection", "jevicInspectionDate");
        TYPE_TO_FIELD.put("date_of_issue", "jevicIssueDate");
        TYPE_TO_FIELD.put("location", "jevicLocation");
        TYPE_TO_FIELD.put("make", "jevicMake");
        TYPE_TO_FIELD.put("model", "jevicModel");

        DATE_FIELDS.add("jevicInspectionDate");
        DATE_FIELDS.add("jevicIssueDate");
    }

    private final String processorId;

    public OdometerCertificateParser(
            @Value("${app.ocr.documentAi.odometerCertificateProcessorId:}") String processorId
    ) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = new AuctionParseResult();
        result.setRawText(text);
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The odometer certificate was empty.");
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
        CustomsDocumentParserUtils.finish(result, "Document AI (odometer certificate)");
        return result;
    }

    @Override
    public String getProcessorId() {
        return this.processorId;
    }

    @Override
    public String getDocumentName() {
        return "odometer certificate";
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
        value = value.replaceAll(RegexConstants.Text.LEADING_PUNCT, "")
                .replaceAll(RegexConstants.Text.TRAILING_PUNCT, "")
                .trim();
        if (value.isEmpty()) {
            return null;
        }
        if (DATE_FIELDS.contains(field)) {
            return ExportCertificateParser.toIsoDate(value);
        }
        return value;
    }
}
