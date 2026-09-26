package com.carsale.erp.customspipeline.document;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.AssessmentNotice;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentNoticeParseTest {

    private final AssessmentNotice parser = new AssessmentNotice("");

    @Test
    void parsesRealOcrWithAmountsAboveCodesAndBrokenCommas() {
        AuctionParseResult result = parser.parsePage(REAL_OCR);

        assertTrue(result.isSuccess());
        assertEquals("354,867", result.getFields().get("assessmentTaxCid"));
        assertEquals("177,434", result.getFields().get("assessmentTaxSur"));
        assertEquals("1,992,000", result.getFields().get("assessmentTaxXid"));
        assertEquals("805,692", result.getFields().get("assessmentTaxVat"));
        assertEquals("15,000", result.getFields().get("assessmentTaxVel"));
        assertEquals("3,346,743", result.getFields().get("assessmentTotalAssessed"));
        assertEquals("3,346,743", result.getFields().get("assessmentTotalPaid"));
        assertEquals("1,200", result.getFields().get("assessmentTaxOtc"));
        assertEquals("250", result.getFields().get("assessmentTaxCom"));
        assertEquals("300", result.getFields().get("assessmentTaxExm"));
    }

    @Test
    void parsesCleanItemTaxesTable() {
        AuctionParseResult result = parser.parsePage(
                "Global taxes\n"
                        + "OTC Over Time Charges 1,200\n"
                        + "COM Computer Fee 250\n"
                        + "EXM Other Receipts-Examination Fees 300\n"
                        + "Item taxes\n"
                        + "CID Customs Import Duty 354,867\n"
                        + "SUR Surcharge 177,434\n"
                        + "XID Excise Duty 1,992,000\n"
                        + "VAT Value Added Tax 805,692\n"
                        + "VEL Vehicle Entitlement Levy 15,000\n"
                        + "Total assessed amount for the declaration 3,346,743\n"
                        + "Total amount paid: 3,346,743\n"
        );

        assertEquals("354,867", result.getFields().get("assessmentTaxCid"));
        assertEquals("177,434", result.getFields().get("assessmentTaxSur"));
        assertEquals("1,992,000", result.getFields().get("assessmentTaxXid"));
        assertEquals("805,692", result.getFields().get("assessmentTaxVat"));
        assertEquals("15,000", result.getFields().get("assessmentTaxVel"));
        assertEquals("3,346,743", result.getFields().get("assessmentTotalAssessed"));
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
    void mapsTaxesWhenDocumentAiReturnsTextButNoEntities() {
        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult(COLUMNAR_OCR, java.util.Collections.emptyList()));

        assertTrue(result.isSuccess());
        assertEquals("645,990", result.getFields().get("assessmentTaxCid"));
        assertEquals("322,995", result.getFields().get("assessmentTaxSur"));
        assertEquals("1,992,000", result.getFields().get("assessmentTaxXid"));
        assertEquals("1,172,507", result.getFields().get("assessmentTaxVat"));
        assertEquals("15,000", result.getFields().get("assessmentTaxVel"));
        assertEquals("1,200", result.getFields().get("assessmentTaxOtc"));
        assertEquals("250", result.getFields().get("assessmentTaxCom"));
        assertEquals("300", result.getFields().get("assessmentTaxExm"));
        assertEquals("4,150,242", result.getFields().get("assessmentTotalAssessed"));
        assertEquals("171,182", result.getFields().get("assessmentTotalPaid"));
    }

    private static DocumentAiClient.DocumentAiEntity entity(String type, String value) {
        return new DocumentAiClient.DocumentAiEntity(type, value, 1.0d);
    }

    /** Exact OCR shape from the user's Assessment Notice scan. */
    private static final String REAL_OCR =
            "inistry of Finance\n"
            + "Customs Headquarters\n"
            + "Assessment Notice\n"
            + "Customs office: Hambanthota Import Office - Sea 2025 HBIM1 | 1798\n"
            + "Declaration reference\n"
            + "Global taxes\n"
            + "Tax code        Tax description Tax code\n"
            + "OTC     over Time Charges       1,200\n"
            + "COM     Computer Fee    250\n"
            + "300]\n"
            + "EXM     other Receipts-Examination Fees\n"
            + "Item taxes\n"
            + "Tax     code    Tax description Tax value\n"
            + "354,867\n"
            + "CID     Customs Import Duty\n"
            + "177,434\n"
            + "SUR     Surcharge       1,992, 000\n"
            + "XID     Excise (Special Prov.) Duty\n"
            + "VAT     Value Added Tax 805, 692\n"
            + "VEL     Vehicle Entitlement Levy        15,000\n"
            + "Total assessed amount for the declaration       3, 346,743\n"
            + "Total   amount paid:    3, 346,743\n";

    /** Columnar OCR from Document AI text when the CUSDEC processor is used. */
    private static final String COLUMNAR_OCR =
            "Ministry of Finance\n"
            + "Customs Headquarters\n"
            + "Assessment Notice\n"
            + "ASYCUDA.\n"
            + "Customs office: Hambanthota Import Office Sea\n"
            + "Declaration reference\n"
            + "2025 HBIM1 | 1802\n"
            + "Global taxes\n"
            + "Tax code\n"
            + "Tax description\n"
            + "Tax code\n"
            + "OTC\n"
            + "Over Time Charges\n"
            + "1,200\n"
            + "COM\n"
            + "Computer Fee\n"
            + "250\n"
            + "EXM\n"
            + "Other Receipts-Examination Fees\n"
            + "300\n"
            + "10/04/2025 A 1801\n"
            + "Consignee\n"
            + "4093862707000\n"
            + "1.00\n"
            + "COLOMBO MOTOR TRADING COMPANY\n"
            + "Item taxes\n"
            + "Tax code\n"
            + "Tax description\n"
            + "Tax value\n"
            + "CID\n"
            + "Customs Import Duty\n"
            + "645,990\n"
            + "SUR\n"
            + "Surcharge\n"
            + "322,995\n"
            + "XID\n"
            + "Excise (Special Prov.) Duty\n"
            + "1,992,000\n"
            + "VAT\n"
            + "Value Added Tax\n"
            + "1,172,507\n"
            + "VEL\n"
            + "Vehicle Entitlement Levy\n"
            + "15,000\n"
            + "Total assessed amount for the declaration\n"
            + "4,150,242\n"
            + "Total amount paid:\n"
            + "171,182\n";
}
