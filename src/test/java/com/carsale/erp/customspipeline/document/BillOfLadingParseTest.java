package com.carsale.erp.customspipeline.document;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.document.BillOfLading;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillOfLadingParseTest {

    private final BillOfLading parser = new BillOfLading("");

    @Test
    void parsesBlNoAndIssueDateFromNyKBillOfLadingOcr() {
        AuctionParseResult result = parser.parsePage(SAMPLE_OCR);

        assertTrue(result.isSuccess());
        assertEquals("NYK8182204251", result.getFields().get("blNo"));
        assertEquals("14 MAR 2025", result.getFields().get("dateOfBlIssue"));
    }

    @Test
    void doesNotUseProformaInvoiceDateAsBlIssueDate() {
        AuctionParseResult result = parser.parsePage(SAMPLE_OCR);

        assertEquals("14 MAR 2025", result.getFields().get("dateOfBlIssue"));
    }

    @Test
    void mapsDocumentAiEntities() {
        List<DocumentAiClient.DocumentAiEntity> entities = Arrays.asList(
                entity("bl_no", "NYK8182204251"),
                entity("date_of_bl_issue", "14 MAR 2025")
        );

        AuctionParseResult result = parser.parsePage(new DocumentAiClient.DocumentAiResult("raw", entities));

        assertTrue(result.isSuccess());
        assertEquals("NYK8182204251", result.getFields().get("blNo"));
        assertEquals("14 MAR 2025", result.getFields().get("dateOfBlIssue"));
    }

    @Test
    void fillsMissingFieldsFromDocumentAiText() {
        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult(SAMPLE_OCR, Collections.emptyList()));

        assertTrue(result.isSuccess());
        assertEquals("NYK8182204251", result.getFields().get("blNo"));
        assertEquals("14 MAR 2025", result.getFields().get("dateOfBlIssue"));
    }

    @Test
    void mapsDocumentAiTypeAliases() {
        assertEquals("blNo", BillOfLading.mapType("bl_no"));
        assertEquals("dateOfBlIssue", BillOfLading.mapType("date_of_bl_issue"));
        assertEquals("NYK8182204251", BillOfLading.cleanValue("blNo", "NYK 8182204251"));
    }

    private static DocumentAiClient.DocumentAiEntity entity(String type, String value) {
        return new DocumentAiClient.DocumentAiEntity(type, value, 1.0d);
    }

    private static final String SAMPLE_OCR =
            "FIRST ORIGINAL\n"
            + "JAPAN FORWARDING AGENCY LTD.\n"
            + "S/O No. YOKSHBA-632\n"
            + "B/L No. NYK8182204251\n"
            + "BILL OF LADING\n"
            + "(NON NEGOTIABLE UNLESS CONSIGNED TO ORDER)\n"
            + "AS PER PROFORMA INVOICE NO: 2100\n"
            + "DATED : 12.02.2025\n"
            + "DATE OF ISSUE 2025-02-17\n"
            + "Place of B(s)/L Issue Dated\n"
            + "TOKYO, JAPAN 14 MAR 2025\n"
            + "NIPPON YUSEN KAISHA as Carrier\n";
}
