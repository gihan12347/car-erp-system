package com.carsale.erp.service;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClearanceParserTest {

    private final ClearanceParser parser = new ClearanceParser();

    @Test
    void parsePage1_handlesStackedLabelsAndBlankReadingRows() {
        String ocr = ""
                + "Chassis / VIN # : M900A-1218085\n"
                + "Make : TOYOTA\n"
                + "Model : ROOMY\n"
                + "Date of Inspection : 25/09/2025\n"
                + "Location :\n"
                + "Certificate No :\n"
                + "Date of Issue :\n"
                + "East Japan Area\n"
                + "LK1-A00154397\n"
                + "03/10/2025\n"
                + "Current Odometer Reading : 14 km\n"
                + "Auction Reading / Date : -\n"
                + "Dealer Reading / Date : -\n"
                + "De-Registration Reading / Date : -\n";

        Map<String, String> result = parser.parsePage1(ocr).getFields();

        assertEquals("M900A-1218085", result.get("jevicChassisVin"));
        assertEquals("TOYOTA", result.get("jevicMake"));
        assertEquals("ROOMY", result.get("jevicModel"));
        assertEquals("25/09/2025", result.get("jevicInspectionDate"));
        assertEquals("East Japan Area", result.get("jevicLocation"));
        assertEquals("LK1-A00154397", result.get("jevicCertificateNo"));
        assertEquals("03/10/2025", result.get("jevicIssueDate"));
        assertEquals("14 km", result.get("jevicCurrentOdometer"));
        assertNull(result.get("jevicAuctionReadingDate"));
        assertNull(result.get("jevicDealerReadingDate"));
        assertNull(result.get("jevicDeregistrationReadingDate"));
    }

    @Test
    void parsePage1_rejectsNextFieldLabelsAsValues() {
        String ocr = ""
                + "Location : Certificate No : Date of Issue :\n"
                + "East Japan Area LK1-A00154397 03/10/2025\n"
                + "Current Odometer Reading : Auction Reading / Date :\n"
                + "14 km\n"
                + "Auction Reading / Date : Dealer Reading / Date :\n"
                + "Dealer Reading / Date : De-Registration Reading / Date :\n";

        Map<String, String> result = parser.parsePage1(ocr).getFields();

        assertEquals("East Japan Area", result.get("jevicLocation"));
        assertEquals("LK1-A00154397", result.get("jevicCertificateNo"));
        assertEquals("03/10/2025", result.get("jevicIssueDate"));
        assertEquals("14 km", result.get("jevicCurrentOdometer"));
        assertNull(result.get("jevicAuctionReadingDate"));
        assertNull(result.get("jevicDealerReadingDate"));
        assertNull(result.get("jevicDeregistrationReadingDate"));
    }

    @Test
    void parsePage1_doesNotConfuseLocationsWordInIntro() {
        String ocr = ""
                + "This certificate records the odometer readings at the time of vehicle inspection, "
                + "and other times / locations as applicable.\n"
                + "Chassis / VIN # : M900A-1218085\n"
                + "Make : TOYOTA\n"
                + "Model : ROOMY\n"
                + "Date of Inspection : 25/09/2025\n"
                + "Location : East Japan Area\n"
                + "Certificate No : LK1-A00154397\n"
                + "Date of Issue : 03/10/2025\n"
                + "Current Odometer Reading : 14 km\n"
                + "Auction Reading / Date : -\n";

        Map<String, String> result = parser.parsePage1(ocr).getFields();

        assertEquals("East Japan Area", result.get("jevicLocation"));
        assertEquals("14 km", result.get("jevicCurrentOdometer"));
    }

    @Test
    void parsePage1_findsOdometerWhenValueIsOnNextLineAfterOtherLabels() {
        String ocr = ""
                + "Location : East Japan Area\n"
                + "Certificate No : LK1-A00154397\n"
                + "Date of Issue : 03/10/2025\n"
                + "Current Odometer Reading : Auction Reading / Date :\n"
                + "14 km\n";

        Map<String, String> result = parser.parsePage1(ocr).getFields();

        assertEquals("East Japan Area", result.get("jevicLocation"));
        assertEquals("14 km", result.get("jevicCurrentOdometer"));
    }

    @Test
    void parsePage3_readsAsycudaAssessmentNotice() {
        String ocr = ""
                + "Ministry of Finance Customs Headquarters\n"
                + "ASYCUDA Assessment Notice\n"
                + "Hambanthota Import Office - Sea\n"
                + "2025 HBIM1 I 1802\n"
                + "Model IM 4\n"
                + "Customs reference 10/04/2025 I 1802\n"
                + "Declarant reference 2025 #26\n"
                + "Assessment reference 10/04/2025 A 1801\n"
                + "Packages 1.00\n"
                + "Declarant\n"
                + "ID 7224028122525-1\n"
                + "WIT CLEARING & FORWARDING CO\n"
                + "448/N/24 EHELAGAHAHENA UDUGAMPOLA\n"
                + "CHA EXP 31/12/2025\n"
                + "Consignee\n"
                + "ID 4093862707000\n"
                + "COLOMBO MOTOR TRADING COMPANY\n"
                + "NO 614, MARADANA ROAD, COLOMBO 10\n"
                + "Global taxes\n"
                + "OTC Over Time Charges 1,200\n"
                + "COM Computer Fee 250\n"
                + "EXM Other Receipts-Examination Fees 300\n"
                + "Item taxes\n"
                + "CID Customs Import Duty 645,990\n"
                + "SUR Surcharge 322,995\n"
                + "XID Excise (Special Prov.) Duty 1,992,000\n"
                + "VAT Value Added Tax 1,172,507\n"
                + "VEL Vehicle Entitlement Levy 15,000\n"
                + "Total assessed amount for the declaration 4,150,242\n"
                + "Total amount paid 171,182\n";

        Map<String, String> result = parser.parsePage3(ocr).getFields();

        assertEquals("Hambanthota Import Office - Sea", result.get("assessmentOffice"));
        assertEquals("2025 HBIM1 I 1802", result.get("assessmentNoticeRef"));
        assertEquals("IM 4", result.get("assessmentModel"));
        assertEquals("10/04/2025 I 1802", result.get("assessmentCustomsReference"));
        assertEquals("2025 #26", result.get("assessmentDeclarantReference"));
        assertEquals("10/04/2025 A 1801", result.get("assessmentReference"));
        assertEquals("1.00", result.get("assessmentPackages"));
        assertEquals("7224028122525-1", result.get("assessmentDeclarantId"));
        assertEquals("WIT CLEARING & FORWARDING CO", result.get("assessmentDeclarantName"));
        assertEquals("448/N/24 EHELAGAHAHENA UDUGAMPOLA", result.get("assessmentDeclarantAddress"));
        assertEquals("31/12/2025", result.get("assessmentDeclarantChaExp"));
        assertEquals("4093862707000", result.get("assessmentConsigneeId"));
        assertEquals("COLOMBO MOTOR TRADING COMPANY", result.get("assessmentConsigneeName"));
        assertEquals("NO 614, MARADANA ROAD, COLOMBO 10", result.get("assessmentConsigneeAddress"));
        assertEquals("1,200", result.get("assessmentTaxOtc"));
        assertEquals("250", result.get("assessmentTaxCom"));
        assertEquals("300", result.get("assessmentTaxExm"));
        assertEquals("645,990", result.get("assessmentTaxCid"));
        assertEquals("322,995", result.get("assessmentTaxSur"));
        assertEquals("1,992,000", result.get("assessmentTaxXid"));
        assertEquals("1,172,507", result.get("assessmentTaxVat"));
        assertEquals("15,000", result.get("assessmentTaxVel"));
        assertEquals("4,150,242", result.get("assessmentTotalAssessed"));
        assertEquals("171,182", result.get("assessmentTotalPaid"));
    }

    @Test
    void parsePage3_keepsFullCidAmountWhenOcrDropsOrBreaksTheComma() {
        String ocr = ""
                + "Item taxes\n"
                + "CID Customs Import Duty 645990\n"
                + "SUR Surcharge 322,995\n";

        Map<String, String> result = parser.parsePage3(ocr).getFields();
        assertEquals("645,990", result.get("assessmentTaxCid"));

        ocr = ""
                + "Item taxes\n"
                + "CID Customs Import Duty 645 990\n"
                + "SUR Surcharge 322,995\n";
        result = parser.parsePage3(ocr).getFields();
        assertEquals("645,990", result.get("assessmentTaxCid"));

        ocr = ""
                + "Item taxes\n"
                + "CID Customs Import Duty 645.990\n"
                + "SUR Surcharge 322,995\n";
        result = parser.parsePage3(ocr).getFields();
        assertEquals("645,990", result.get("assessmentTaxCid"));

        ocr = ""
                + "Item taxes\n"
                + "CID Customs Import Duty 645\n"
                + "990\n"
                + "SUR Surcharge 322,995\n";
        result = parser.parsePage3(ocr).getFields();
        assertEquals("645,990", result.get("assessmentTaxCid"));
        assertEquals("322,995", result.get("assessmentTaxSur"));

        ocr = ""
                + "Item taxes\n"
                + "CID Customs Import Duty 645,990 SUR Surcharge 322,995 XID Excise 1,992,000\n";
        result = parser.parsePage3(ocr).getFields();
        assertEquals("645,990", result.get("assessmentTaxCid"));
        assertEquals("322,995", result.get("assessmentTaxSur"));
        assertEquals("1,992,000", result.get("assessmentTaxXid"));
    }
}
