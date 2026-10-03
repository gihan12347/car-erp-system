package com.carsale.erp.importpipeline.preshipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.PreShipmentParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

class PreShipmentParserTest {

    private final PreShipmentParser parser = new PreShipmentParser("");

    @Test
    void parsePageDoesNotUseRegexFallback() {
        AuctionParseResult result = parser.parsePage(
                "Document No: 007028J\nMake: TOYOTA\nChassis No. (original): TRJ250-0025161\n");

        assertFalse(result.isSuccess());
        assertTrue(result.getFields().isEmpty());
    }

    @Test
    void parsesDocumentAiEntitiesIntoFormFields() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<DocumentAiClient.DocumentAiEntity>();
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "applicant_name", "KAN-DE (NAGOYA) TRADING CO., LTD.", 0.98));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "inspection_organization_name", "BUREAU VERITAS", 0.98));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "make", "TOYOTA", 0.97));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "chassis_no", "TRJ250-0025161", 0.99));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "tyre_size", "265/65R18", 0.9));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "tyre_size", "265/70R17", 0.9));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "auction_grade", "4.5", 0.9));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "commonly_called", "LAND CRUISER 250", 0.9));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "marks_of_accident_on_chassis", "No", 0.9));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "year_month_of_first_registration", "May-2025", 0.95));
        entities.add(new DocumentAiClient.DocumentAiEntity(
                "vehicle_details", "ignored parent", 0.5));

        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult("raw", entities));

        assertTrue(result.isSuccess());
        assertEquals("KAN-DE (NAGOYA) TRADING CO., LTD", result.getFields().get("applicantName"));
        assertEquals("BUREAU VERITAS", result.getFields().get("inspectionOrgName"));
        assertEquals("TOYOTA", result.getFields().get("make"));
        assertEquals("TRJ250-0025161", result.getFields().get("chassisNo"));
        assertEquals("265/65R18 / 265/70R17", result.getFields().get("tyreSize"));
        assertEquals("4.5", result.getFields().get("preshipAuctionGrade"));
        assertEquals("LAND CRUISER 250", result.getFields().get("commonName"));
        assertEquals("No", result.getFields().get("accidentMarksOnChassis"));
        assertEquals("2025-05", result.getFields().get("firstRegistration"));
        assertEquals("", parser.getProcessorId());
    }
}
