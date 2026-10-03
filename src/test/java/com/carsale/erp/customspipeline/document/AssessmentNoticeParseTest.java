package com.carsale.erp.customspipeline.document;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.AssessmentNotice;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentNoticeParseTest {

    private final AssessmentNotice parser = new AssessmentNotice("");

    @Test
    void parsePageDoesNotUseRegexFallback() {
        AuctionParseResult result = parser.parsePage(
                "CID Customs Import Duty 354,867\n"
                        + "Total assessed amount for the declaration 3,346,743\n"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getFields().isEmpty());
    }

    @Test
    void mapsDocumentAiItemTaxesAndTotals() {
        List<DocumentAiClient.DocumentAiEntity> entities = Arrays.asList(
                entity("item_tax", "CID Customs Import Duty 354,867"),
                entity("tax_code", "CID"),
                entity("tax_description", "Customs Import Duty"),
                entity("tax_value", "354867 LKR"),
                entity("item_tax", "SUR Surcharge 177,434"),
                entity("tax_code", "SUR"),
                entity("tax_description", "Surcharge"),
                entity("tax_value", "177,434"),
                entity("item_tax", "XID Excise Duty 1,992,000"),
                entity("tax_code", "XID"),
                entity("tax_value", "1992000"),
                entity("item_tax", "VAT Value Added Tax 805,692"),
                entity("tax_code", "VAT"),
                entity("tax_value", "805,692.00"),
                entity("item_tax", "VEL Vehicle Entitlement Levy 15,000"),
                entity("tax_code", "VEL"),
                entity("tax_value", "15000"),
                entity("totals", "Total assessed 3,346,743 Total amount paid 3,346,743"),
                entity("total_amount_paid", "3,346,743 LKR"),
                entity("total_assessed_amount", "3346743")
        );

        AuctionParseResult result = parser.parsePage(new DocumentAiClient.DocumentAiResult("raw", entities));

        assertTrue(result.isSuccess());
        assertEquals("354,867", result.getFields().get("assessmentTaxCid"));
        assertEquals("177,434", result.getFields().get("assessmentTaxSur"));
        assertEquals("1,992,000", result.getFields().get("assessmentTaxXid"));
        assertEquals("805,692", result.getFields().get("assessmentTaxVat"));
        assertEquals("15,000", result.getFields().get("assessmentTaxVel"));
        assertEquals("3,346,743", result.getFields().get("assessmentTotalPaid"));
        assertEquals("3,346,743", result.getFields().get("assessmentTotalAssessed"));
    }

    @Test
    void mapsDocumentAiTaxValueBeforeTaxCode() {
        List<DocumentAiClient.DocumentAiEntity> entities = Arrays.asList(
                entity("item_tax", "CID 354,867"),
                entity("tax_value", "354,867"),
                entity("tax_code", "CID")
        );

        AuctionParseResult result = parser.parsePage(new DocumentAiClient.DocumentAiResult("", entities));

        assertEquals("354,867", result.getFields().get("assessmentTaxCid"));
    }

    @Test
    void mapsDocumentAiTypeAliases() {
        assertEquals("assessmentTotalPaid", AssessmentNotice.mapType("total_amount_paid"));
        assertEquals("assessmentTotalAssessed", AssessmentNotice.mapType("total_assessed_amount"));
        assertEquals("354,867", AssessmentNotice.cleanMoney("354867 LKR"));
        assertEquals("CID", AssessmentNotice.extractTaxCode("CID Customs Import Duty"));
    }

    @Test
    void doesNotMapTaxesFromDocumentAiTextWhenEntitiesAreMissing() {
        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult(
                        "CID Customs Import Duty 354,867\nTotal assessed amount 3,346,743\n",
                        java.util.Collections.emptyList()));

        assertFalse(result.isSuccess());
        assertTrue(result.getFields().isEmpty());
    }

    private static DocumentAiClient.DocumentAiEntity entity(String type, String value) {
        return new DocumentAiClient.DocumentAiEntity(type, value, 1.0d);
    }
}
