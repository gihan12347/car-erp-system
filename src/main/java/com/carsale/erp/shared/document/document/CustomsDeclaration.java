package com.carsale.erp.shared.document.document;

import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import com.carsale.erp.shared.regex.RegexConstants;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

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
                String field = CustomsDocumentParserUtils.mapType(entity.getType(), TYPE_TO_FIELD);
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

    @Override
    public String getOcrLanguage() {
        return "";
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
