package com.carsale.erp.importpipeline.preshipment;

import com.carsale.erp.shared.vehicle.Vehicle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

class PreShipmentParserTest {

    private final PreShipmentParser parser = new PreShipmentParser();

    @Test
    void parsesSampleBvCertificateFields() {
        AuctionParseResult result = parser.parse(SAMPLE_TEXT);

        assertTrue(result.isSuccess());
        assertEquals("007028J", result.getFields().get("certificateReference"));
        assertEquals("PRE-SHIPMENT INSPECTION CERTIFICATE", result.getFields().get("documentTitle"));
        assertEquals("1/3", result.getFields().get("pageInfo"));
        assertEquals("31-Jul-25", result.getFields().get("certificateDate"));
        assertEquals("SRL2025811-25", result.getFields().get("bvNumber"));
        assertEquals("BUREAU VERITAS", result.getFields().get("inspectionOrgName"));
        assertEquals("+81 6 4790 7035", result.getFields().get("inspectionOrgTel"));
        assertEquals("+81 6 4790 7060", result.getFields().get("inspectionOrgFax"));
        assertEquals("yuri.kashihara@bureauveritas.com", result.getFields().get("inspectionOrgEmail"));
        assertEquals("KAN-DE (NAGOYA) TRADING CO., LTD. IN AMA-GUN AICHI, JAPAN", result.getFields().get("placeOfInspection"));
        assertEquals("22-Jul-25", result.getFields().get("dateOfInspection"));
        assertEquals("KAN-DE (NAGOYA) TRADING CO., LTD.", result.getFields().get("applicantName"));
        assertEquals("+81 52 351 9943", result.getFields().get("applicantTel"));
        assertEquals("ito@kan-de.co.jp", result.getFields().get("applicantEmail"));
        assertEquals("STATION WAGON", result.getFields().get("vehicleType"));
        assertEquals("TOYOTA", result.getFields().get("make"));
        assertEquals("3BA-TRJ250W", result.getFields().get("model"));
        assertEquals("LAND CRUISER 250", result.getFields().get("commonName"));
        assertEquals("PEARL WHITE", result.getFields().get("bodyColour"));
        assertEquals("GASOLINE", result.getFields().get("fuelType"));
        assertEquals("May-2025", result.getFields().get("firstRegistration"));
        assertEquals("32km", result.getFields().get("inspectionMileage"));
        assertEquals("2,690cc", result.getFields().get("engineCapacity"));
        assertEquals("TRJ250-0025161", result.getFields().get("chassisNo"));
        assertEquals("2TR-2705714", result.getFields().get("engineNo"));
        assertEquals("4WD", result.getFields().get("drivingSystem"));
        assertEquals("No", result.getFields().get("accidentMarksOnChassis"));
        assertEquals("Good", result.getFields().get("chassisCondition"));
        assertEquals("3BA-TRJ250W-GZTTK", result.getFields().get("fullModelNo"));
        assertEquals("2025", result.getFields().get("yearOfManufacture"));
    }

    @Test
    void parsesTableLayoutVehicleParticulars() {
        AuctionParseResult result = parser.parse(TABLE_SAMPLE_TEXT);

        assertTrue(result.isSuccess());
        assertEquals("STATION WAGON", result.getFields().get("vehicleType"));
        assertEquals("TOYOTA", result.getFields().get("make"));
        assertEquals("3BA-TRJ250W", result.getFields().get("model"));
        assertEquals("LAND CRUISER 250", result.getFields().get("commonName"));
        assertEquals("Not Applicable", result.getFields().get("manufactureGrade"));
        assertEquals("Not Applicable", result.getFields().get("preshipAuctionGrade"));
        assertEquals("PEARL WHITE", result.getFields().get("bodyColour"));
        assertEquals("32km", result.getFields().get("inspectionMileage"));
        assertEquals("TRJ250-0025161", result.getFields().get("chassisNo"));
        assertEquals("3BA-TRJ250W-GZTTK", result.getFields().get("fullModelNo"));
    }

    @Test
    void parsesMultilineVehicleParticulars() {
        AuctionParseResult result = parser.parse(MULTILINE_SAMPLE_TEXT);

        assertEquals("STATION WAGON", result.getFields().get("vehicleType"));
        assertEquals("TOYOTA", result.getFields().get("make"));
        assertEquals("4WD", result.getFields().get("drivingSystem"));
        assertEquals("Good", result.getFields().get("chassisCondition"));
    }

    @Test
    void parsesUserReportedRowsWithoutRowTenPrefix() {
        AuctionParseResult result = parser.parse(USER_ROWS_TEXT);

        assertEquals("32km", result.getFields().get("inspectionMileage"));
        assertEquals("2,690cc", result.getFields().get("engineCapacity"));
        assertEquals("TRJ250-0025161", result.getFields().get("chassisNo"));
        assertEquals("2TR-2705714", result.getFields().get("engineNo"));
        assertEquals("4WD", result.getFields().get("drivingSystem"));
        assertEquals("No", result.getFields().get("accidentMarksOnChassis"));
        assertEquals("Good", result.getFields().get("chassisCondition"));
    }

    @Test
    void parsesBvParenthesisCertificateFormat() {
        AuctionParseResult result = parser.parse(BV_PAREN_FORMAT_TEXT);

        assertEquals("32km", result.getFields().get("inspectionMileage"));
        assertEquals("2,690cc", result.getFields().get("engineCapacity"));
        assertEquals("TRJ250-0025161", result.getFields().get("chassisNo"));
        assertEquals("2TR-2705714", result.getFields().get("engineNo"));
        assertEquals("4WD", result.getFields().get("drivingSystem"));
        assertEquals("No", result.getFields().get("accidentMarksOnChassis"));
        assertEquals("Good", result.getFields().get("chassisCondition"));
        assertEquals("3BA-TRJ250W-GZTTK", result.getFields().get("fullModelNo"));
        assertEquals("2025", result.getFields().get("yearOfManufacture"));
    }

    @Test
    void parsesBvParenthesisSplitLines() {
        AuctionParseResult result = parser.parse(BV_PAREN_SPLIT_TEXT);

        assertEquals("2,690cc", result.getFields().get("engineCapacity"));
        assertEquals("2TR-2705714", result.getFields().get("engineNo"));
        assertEquals("4WD", result.getFields().get("drivingSystem"));
        assertEquals("Good", result.getFields().get("chassisCondition"));
    }

    private static final String BV_PAREN_FORMAT_TEXT =
            "3. PARTICULARS OF SECOND-HAND MOTOR VEHICLE\n"
            + "(1) Type of vehicle: STATION WAGON\n"
            + "(2) Make: TOYOTA\n"
            + "(3) Model: 3BA-TRJ250W\n"
            + "(4) Commonly called (emblem reading): LAND CRUISER 250\n"
            + "(5) Manufacture Grade (emblem reading): Not Applicable\n"
            + "(6) Auction Grade: Not Applicable\n"
            + "(7) Body colour: PEARL WHITE\n"
            + "(8) Fuel type: GASOLINE\n"
            + "(9) Year/month of first registration: May-2025\n"
            + "(10) Inspection mileage (odometer reading): 32km\n"
            + "(11) Engine capacity: 2,690cc\n"
            + "(12) Chassis No. (original): TRJ250-0025161\n"
            + "(13) Engine No.: 2TR-2705714\n"
            + "(14) Driving system: 4WD\n"
            + "(15) Marks of accident on chassis (by visual check): No\n"
            + "(16) Condition of chassis: Good\n"
            + "Full Model No. 3BA-TRJ250W-GZTTK\n"
            + "Year of Manufacture: 2025\n";

    private static final String BV_PAREN_SPLIT_TEXT =
            "Particulars of Second-Hand Motor Vehicle\n"
            + "(11) Engine capacity:\n"
            + "2,690cc\n"
            + "(13) Engine No.:\n"
            + "2TR-2705714\n"
            + "(14) Driving system:\n"
            + "4WD\n"
            + "(16) Condition of chassis:\n"
            + "Good\n";

    private static final String USER_ROWS_TEXT =
            "Particulars of Second-Hand Motor Vehicle\n"
            + "9\tYear/month of first registration\tMay-2025\n"
            + "Inspection mileage (odometer reading)\t32km\n"
            + "11\tEngine capacity\t2,690cc\n"
            + "12\tChassis No. (original)\tTRJ250-0025161\n"
            + "13\tEngine No.\t2TR-2705714\n"
            + "14\tDriving system\t4WD\n"
            + "15\tMarks of accident on chassis (by visual check)\tNo\n"
            + "16\tCondition of chassis\tGood\n";

    private static final String TABLE_SAMPLE_TEXT =
            "Document No.\t007028J\n"
            + "Particulars of Applicant\n"
            + "3. PARTICULARS OF SECOND-HAND MOTOR VEHICLE\n"
            + "No.\tExact Attribute\tValue\n"
            + "1\tType of vehicle\tSTATION WAGON\n"
            + "2\tMake\tTOYOTA\n"
            + "3\tModel\t3BA-TRJ250W\n"
            + "4\tCommonly called (emblem reading)\tLAND CRUISER 250\n"
            + "5\tManufacture Grade (emblem reading)\tNot Applicable\n"
            + "6\tAuction Grade\tNot Applicable\n"
            + "7\tBody colour\tPEARL WHITE\n"
            + "8\tFuel type\tGASOLINE\n"
            + "9\tYear/month of first registration\tMay-2025\n"
            + "10\tInspection mileage (odometer reading)\t32km\n"
            + "11\tEngine capacity\t2,690cc\n"
            + "12\tChassis No. (original)\tTRJ250-0025161\n"
            + "13\tEngine No.\t2TR-2705714\n"
            + "14\tDriving system\t4WD\n"
            + "15\tMarks of accident on chassis (by visual check)\tNo\n"
            + "16\tCondition of chassis\tGood\n"
            + "Full Model No.\t3BA-TRJ250W-GZTTK\n"
            + "Year of Manufacture\t2025\n";

    private static final String MULTILINE_SAMPLE_TEXT =
            "Particulars of Second-Hand Motor Vehicle\n"
            + "No.\nExact Attribute\nValue\n"
            + "1\nType of vehicle\nSTATION WAGON\n"
            + "2\nMake\nTOYOTA\n"
            + "14\nDriving system\n4WD\n"
            + "16\nCondition of chassis\nGood\n";

    private static final String SAMPLE_TEXT =
            "Document No. 007028J\n"
            + "Document Title PRE-SHIPMENT INSPECTION CERTIFICATE\n"
            + "Page 1/3\n"
            + "Date 31-Jul-25\n"
            + "BV No. SRL2025811 -25\n"
            + "Inspection Organisation BUREAU VERITAS\n"
            + "Name of Inspection Organisation\n"
            + "(a) Name of inspection organisation BUREAU VERITAS\n"
            + "(b) Address Osaka U2 Bldg. 4F, 2-4-7, Uchihonmachi, Chuo-ku, Osaka, Japan 540-0026\n"
            + "Tel. No. +81 6 4790 7035\n"
            + "Fax No. +81 6 4790 7060\n"
            + "Email yuri.kashihara@bureauveritas.com\n"
            + "Place of Inspection KAN-DE (NAGOYA) TRADING CO., LTD. IN AMA-GUN AICHI, JAPAN\n"
            + "Date of Inspection 22-Jul-25\n"
            + "Particulars of Applicant\n"
            + "(a) Name KAN-DE (NAGOYA) TRADING CO., LTD.\n"
            + "(b) Address 3-190, Nagara-cho, Nakagawa-ku, Nagoya, Aichi, Japan 454-0815\n"
            + "Tel. No. +81 52 351 9943\n"
            + "Fax No. +81 52 351 9944\n"
            + "Email ito@kan-de.co.jp\n"
            + "Particulars of Second-Hand Motor Vehicle\n"
            + "1. Type of vehicle STATION WAGON\n"
            + "2. Make TOYOTA\n"
            + "3. Model 3BA-TRJ250W\n"
            + "4. Commonly called (emblem reading) LAND CRUISER 250\n"
            + "5. Manufacture Grade (emblem reading) Not Applicable\n"
            + "6. Auction Grade Not Applicable\n"
            + "7. Body colour PEARL WHITE\n"
            + "8. Fuel type GASOLINE\n"
            + "9. Year/month of first registration May-2025\n"
            + "10. Inspection mileage (odometer reading) 32km\n"
            + "11. Engine capacity 2,690cc\n"
            + "12. Chassis No. (original) TRJ250-0025161\n"
            + "13. Engine No. 2TR-2705714\n"
            + "14. Driving system 4WD\n"
            + "15. Marks of accident on chassis (by visual check) No\n"
            + "16. Condition of chassis Good\n"
            + "Full Model No. 3BA-TRJ250W-GZTTK\n"
            + "Year of Manufacture 2025\n";
}
