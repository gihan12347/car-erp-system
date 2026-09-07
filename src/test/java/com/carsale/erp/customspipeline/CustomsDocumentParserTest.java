package com.carsale.erp.customspipeline;

import com.carsale.erp.shared.vehicle.Vehicle;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomsDocumentParserTest {

    private final CustomsDocumentParser parser = new CustomsDocumentParser();

    @Test
    void parsePage1_readsCertificateOfInspectionFields() {
        String ocr = ""
                + "Certificate No.: LK1-A000705\n"
                + "Date of Issue: 07/03/2025\n"
                + "CERTIFICATE OF INSPECTION\n"
                + "We hereby certify that the motor vehicle whose details are given below was "
                + "pre-shipment inspected by JEVIC COMPANY LIMITED for SRI LANKA, "
                + "as per the Import and Export (Control) Regulation, No. 02 of 2013.\n"
                + "Inspection Branch: East Japan Area\n"
                + "Inspected Motor Vehicle Particulars\n"
                + "Make: HONDA\n"
                + "Model: 6BA-JF5\n"
                + "Engine Capacity: 650\n"
                + "Year of First Registration: 202501\n"
                + "Chassis Number: JF5-1141982\n"
                + "Engine Number: S07B-6236116\n"
                + "Inspected Mileage (Odometer Reading): 5 km\n"
                + "Inspection Date: 05/03/2025\n"
                + "Remarks: Year of Manufacture:2024\n";

        Map<String, String> result = parser.parsePage1(ocr).getFields();

        assertEquals("LK1-A000705", result.get("jevicCertificateNo"));
        assertEquals("07/03/2025", result.get("jevicIssueDate"));
        assertEquals("East Japan Area", result.get("jevicLocation"));
        assertEquals("HONDA", result.get("jevicMake"));
        assertEquals("6BA-JF5", result.get("jevicModel"));
        assertEquals("650", result.get("jevicEngineCapacity"));
        assertEquals("202501", result.get("jevicFirstRegistration"));
        assertEquals("JF5-1141982", result.get("jevicChassisVin"));
        assertEquals("S07B-6236116", result.get("jevicEngineNo"));
        assertEquals("5 km", result.get("jevicCurrentOdometer"));
        assertEquals("05/03/2025", result.get("jevicInspectionDate"));
        assertEquals("Year of Manufacture:2024", result.get("jevicRemarks"));
    }

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

    @Test
    void parsePage4_readsWorkingSheetFields() {
        String ocr = ""
                + "F5-1141982-AE\n"
                + "WORKING SHEET FOR MOTOR VEHICLES\n"
                + "H.S. CODE 8703.21.69\n"
                + "TYPE OF VEHICLE 01 UNIT USED HONDA N BOX CUSTOM PETROL ST/WAGON\n"
                + "NAME OF VESSEL GUARDIAN LEADER\n"
                + "CHASSIS NOS JF5-1141982\n"
                + "Agents FOB 1,923,900.00 / 110 X 100 = 1,749,000.00\n"
                + "Invoiced FOB 1,310,000.00\n"
                + "Invoiced Freight 45,000.00\n"
                + "Invoiced Insurance 5,000.00\n"
                + "B/L Freight Calculation USD 526.28 @ 301.1496 = JPY @ 2.0591 = 76,970.04\n"
                + "Date of B/L 2025 3 14\n"
                + "Date of Manufacture 2024 11 11\n"
                + "Age Difference for I.C.L 0 4 3\n"
                + "Date of 1st Registration 2025 1 15\n"
                + "Website Value 1,749,000.00\n"
                + "15% of Value 262,350.00\n"
                + "FOB Value (85%) JPY 1,486,650.00\n"
                + "LC No ABLOLCS002250017\n"
                + "Amount 1,360,000.00\n"
                + "Bank AMANA BANK LIMITED\n"
                + "COLOMBO MOTOR TRADING COMPANY\n"
                + "Date of Issue 17/02/2025\n"
                + "Clearing Agent Mentioned WIT CLEARING & FORWARDING CO.\n"
                + "FOB for Fiscal Levies 1,486,650.00\n"
                + "Freight Charges for Fiscal Levies 76,970.04\n"
                + "Insurance Charges for Fiscal Levies 5,000.00\n"
                + "Value of Options for Fiscal Levies 0.00\n"
                + "Total Value for Fiscal Levies JPY 1,568,620.04\n";

        Map<String, String> result = parser.parsePage4(ocr).getFields();

        assertEquals("F5-1141982-AE", result.get("worksheetRef"));
        assertEquals("8703.21.69", result.get("worksheetHsCode"));
        assertEquals("01 UNIT USED HONDA N BOX CUSTOM PETROL ST/WAGON", result.get("worksheetVehicleType"));
        assertEquals("GUARDIAN LEADER", result.get("worksheetVesselName"));
        assertEquals("JF5-1141982", result.get("worksheetChassisNo"));
        assertEquals("1,749,000.00", result.get("worksheetAgentsFob"));
        assertEquals("1,310,000.00", result.get("worksheetInvoicedFob"));
        assertEquals("45,000.00", result.get("worksheetInvoicedFreight"));
        assertEquals("5,000.00", result.get("worksheetInvoicedInsurance"));
        assertEquals("76,970.04", result.get("worksheetBlFreightAmount"));
        assertEquals("14/03/2025", result.get("worksheetBlDate"));
        assertEquals("11/11/2024", result.get("worksheetManufactureDate"));
        assertEquals("0 years 4 months 3 days", result.get("worksheetAgeDifference"));
        assertEquals("15/01/2025", result.get("worksheetFirstRegistrationDate"));
        assertEquals("1,749,000.00", result.get("worksheetWebsiteValue"));
        assertEquals("262,350.00", result.get("worksheetFifteenPercent"));
        assertEquals("1,486,650.00", result.get("worksheetFobValue85"));
        assertEquals("ABLOLCS002250017", result.get("worksheetLcNo"));
        assertEquals("1,360,000.00", result.get("worksheetLcAmount"));
        assertEquals("AMANA BANK LIMITED", result.get("worksheetLcBank"));
        assertEquals("COLOMBO MOTOR TRADING COMPANY", result.get("worksheetLcImporter"));
        assertEquals("17/02/2025", result.get("worksheetLcIssueDate"));
        assertEquals("WIT CLEARING & FORWARDING CO.", result.get("worksheetClearingAgent"));
        assertEquals("1,486,650.00", result.get("worksheetFiscalFob"));
        assertEquals("76,970.04", result.get("worksheetFiscalFreight"));
        assertEquals("5,000.00", result.get("worksheetFiscalInsurance"));
        assertEquals("0.00", result.get("worksheetFiscalOptions"));
        assertEquals("1,568,620.04", result.get("worksheetFiscalTotal"));
    }

    @Test
    void parsePage4_readsWorkingSheetWhenLabelsAndValuesAreOnSeparateLines() {
        String ocr = ""
                + "F5-1141982-AE\n"
                + "WORKING SHEET FOR MOTOR VEHICLES\n"
                + "H.S. CODE\n"
                + "8703.21.69\n"
                + "TYPE OF VEHICLE\n"
                + "01 UNIT USED HONDA N BOX CUSTOM PETROL ST/WAGON\n"
                + "Reference No.\n"
                + "NAME OF VESSEL\n"
                + "GUARDIAN LEADER\n"
                + "CHASSIS NOS\n"
                + "JF5-1141982\n"
                + "Agents FOB\n"
                + "1,923,900.00 / 110 X 100 = 1,749,000.00\n"
                + "Invoiced FOB\n"
                + "1,310,000.00\n"
                + "Invoiced Freight\n"
                + "45,000.00\n"
                + "Invoiced Insurance\n"
                + "5,000.00\n"
                + "B/L Freight Calculation\n"
                + "USD 526.28 @ 301.1496 = JPY @ 2.0591 = 76,970.04\n"
                + "Date of B/L\n"
                + "2025 3 14\n"
                + "Date of Manufacture\n"
                + "2024 11 11\n"
                + "Age Difference for I.C.L\n"
                + "0 4 3\n"
                + "Date of 1st Registration\n"
                + "2025 1 15\n"
                + "Website Value\n"
                + "1,749,000.00\n"
                + "15% of Value\n"
                + "262,350.00\n"
                + "FOB Value (85%)\n"
                + "JPY 1,486,650.00\n"
                + "LC No\n"
                + "ABLOLCS002250017\n"
                + "Amount\n"
                + "1,360,000.00\n"
                + "Bank\n"
                + "AMANA BANK LIMITED\n"
                + "COLOMBO MOTOR TRADING COMPANY\n"
                + "Date of Issue\n"
                + "17/02/2025\n"
                + "Clearing Agent Mentioned\n"
                + "WIT CLEARING & FORWARDING CO.\n"
                + "FOB for Fiscal Levies\n"
                + "1,486,650.00\n"
                + "Freight Charges for Fiscal Levies\n"
                + "76,970.04\n"
                + "Insurance Charges for Fiscal Levies\n"
                + "5,000.00\n"
                + "Value of Options for Fiscal Levies\n"
                + "0.00\n"
                + "Total Value for Fiscal Levies\n"
                + "JPY 1,568,620.04\n";

        Map<String, String> result = parser.parsePage4(ocr).getFields();

        assertEquals("F5-1141982-AE", result.get("worksheetRef"));
        assertEquals("8703.21.69", result.get("worksheetHsCode"));
        assertEquals("01 UNIT USED HONDA N BOX CUSTOM PETROL ST/WAGON", result.get("worksheetVehicleType"));
        assertEquals("GUARDIAN LEADER", result.get("worksheetVesselName"));
        assertEquals("JF5-1141982", result.get("worksheetChassisNo"));
        assertEquals("1,749,000.00", result.get("worksheetAgentsFob"));
        assertEquals("1,310,000.00", result.get("worksheetInvoicedFob"));
        assertEquals("45,000.00", result.get("worksheetInvoicedFreight"));
        assertEquals("5,000.00", result.get("worksheetInvoicedInsurance"));
        assertEquals("76,970.04", result.get("worksheetBlFreightAmount"));
        assertEquals("14/03/2025", result.get("worksheetBlDate"));
        assertEquals("11/11/2024", result.get("worksheetManufactureDate"));
        assertEquals("0 years 4 months 3 days", result.get("worksheetAgeDifference"));
        assertEquals("15/01/2025", result.get("worksheetFirstRegistrationDate"));
        assertEquals("1,749,000.00", result.get("worksheetWebsiteValue"));
        assertEquals("262,350.00", result.get("worksheetFifteenPercent"));
        assertEquals("1,486,650.00", result.get("worksheetFobValue85"));
        assertEquals("ABLOLCS002250017", result.get("worksheetLcNo"));
        assertEquals("1,360,000.00", result.get("worksheetLcAmount"));
        assertEquals("AMANA BANK LIMITED", result.get("worksheetLcBank"));
        assertEquals("COLOMBO MOTOR TRADING COMPANY", result.get("worksheetLcImporter"));
        assertEquals("17/02/2025", result.get("worksheetLcIssueDate"));
        assertEquals("WIT CLEARING & FORWARDING CO.", result.get("worksheetClearingAgent"));
        assertEquals("1,486,650.00", result.get("worksheetFiscalFob"));
        assertEquals("1,568,620.04", result.get("worksheetFiscalTotal"));
        assertNull(result.get("worksheetOptionsValue"));
    }

    @Test
    void parsePage2_readsBlAwbNoWithoutSwallowingTheRestOfTheLine() {
        String ocr = ""
                + "Customs Reference Number: 1802\n"
                + "BL / AWB No. EGLV123456789012 Location of Goods COLOMBO PORT HS Code 8703.21.69 "
                + "Gross Mass 1280 Net Mass 1180 and other declaration boxes that OCR mashed onto one line\n";

        Map<String, String> result = parser.parsePage2(ocr).getFields();

        assertEquals("EGLV123456789012", result.get("blAwbNo"));
    }
}
