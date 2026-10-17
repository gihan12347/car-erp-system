package com.carsale.erp.shared.document.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.dto.SaleProgress;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.shared.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdCardParser implements DocumentParser {

    private static final Map<String, String> TYPE_TO_FIELD = new LinkedHashMap<>();
    private static final Pattern NIC = Pattern.compile("\\b(\\d{9}[VvXx]|\\d{12})\\b");
    private static final Pattern[][] LINE_LABELS = {
            line("fullName", "(?:Full\\s+name|Name)"),
            line("nicNo", "(?:NIC|N\\.I\\.C\\.?|ID\\s*(?:No|Number)|Identity\\s*(?:No|Number)|National\\s+Identity)"),
            line("address", "Address"),
            line("dateOfBirth", "(?:Date\\s+of\\s+birth|DOB|D\\.O\\.B\\.?)"),
            line("gender", "(?:Gender|Sex)")
    };

    static {
        TYPE_TO_FIELD.put("full_name", "fullName");
        TYPE_TO_FIELD.put("name", "fullName");
        TYPE_TO_FIELD.put("nic_no", "nicNo");
        TYPE_TO_FIELD.put("nic", "nicNo");
        TYPE_TO_FIELD.put("id_no", "nicNo");
        TYPE_TO_FIELD.put("address", "address");
        TYPE_TO_FIELD.put("date_of_birth", "dateOfBirth");
        TYPE_TO_FIELD.put("gender", "gender");
        TYPE_TO_FIELD.put("sex", "gender");
    }

    private final String processorId;

    public IdCardParser(
            @Value("${app.ocr.documentAi.idCardProcessorId:}") String processorId
    ) {
        this.processorId = processorId == null ? "" : processorId.trim();
    }

    @Override
    public AuctionParseResult parsePage(String text) {
        AuctionParseResult result = AuctionParseResult.builder().build();
        result.setRawText(text);
        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("The identity card was empty.");
            return result;
        }
        String[] lines = text.replace('\r', '\n').split("\n");
        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            for (Pattern[] labeled : LINE_LABELS) {
                Matcher matcher = labeled[0].matcher(trimmed);
                if (!matcher.find()) {
                    continue;
                }
                String field = labeled[1].pattern();
                if (result.getFields().containsKey(field)) {
                    continue;
                }
                String value = clean(matcher.group(1));
                if (value != null) {
                    result.put(field, value);
                }
            }
        }
        if (!result.getFields().containsKey("nicNo")) {
            Matcher nic = NIC.matcher(text);
            if (nic.find()) {
                result.put("nicNo", nic.group(1).toUpperCase());
            }
        }
        Utils.finish(result, "the identity card");
        return result;
    }

    @Override
    public AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi) {
        if (documentAi == null) {
            AuctionParseResult empty = AuctionParseResult.builder().build();
            empty.setSuccess(false);
            empty.setMessage("Document AI returned no result.");
            return empty;
        }
        AuctionParseResult result = parsePage(documentAi.getText());
        List<SaleProgress.DocumentAiEntity> entities = documentAi.getEntities();
        if (entities != null) {
            for (SaleProgress.DocumentAiEntity entity : entities) {
                if (entity == null) {
                    continue;
                }
                String field = Utils.mapType(entity.getType(), TYPE_TO_FIELD);
                String value = clean(entity.getValue());
                if (field != null && value != null) {
                    result.put(field, value);
                }
            }
        }
        Utils.finish(result, "Document AI (identity card)");
        return result;
    }

    @Override
    public String getProcessorId() {
        return processorId;
    }

    @Override
    public String getDocumentName() {
        return "identity card";
    }

    @Override
    public String getOcrLanguage() {
        return "eng";
    }

    private static Pattern[] line(String field, String label) {
        return new Pattern[] {
                Pattern.compile("(?i)" + label + "\\s*[:.]?\\s+([^\\n]{1,180})"),
                Pattern.compile(field)
        };
    }

    static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        value = value.replaceAll(RegexConstants.Text.LEADING_PUNCT, "")
                .replaceAll(RegexConstants.Text.TRAILING_PUNCT, "")
                .trim();
        return value.isEmpty() ? null : value;
    }
}
