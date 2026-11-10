package com.carsale.erp.importpipeline.auction;

import java.util.Arrays;
import java.util.Map;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.WorkingSheet;
import com.carsale.erp.shared.dto.SaleProgress;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSheetParserTest {

    private final WorkingSheet.AuctionSheetParser parser = new WorkingSheet.AuctionSheetParser(text -> text);

    @Test
    void parsesHandwrittenRaizeHybridSheet() {
        String ocr = ""
                + "コンパクトコーナー\n"
                + "8313\n"
                + "排気量 1,200cc\n"
                + "型式 5AA-A202A\n"
                + "初度登録 R7/4月\n"
                + "車名 ライズ\n"
                + "グレード ハイブリッドZ\n"
                + "4WD\n"
                + "評価点 S\n"
                + "内装 A\n"
                + "走行 10 km\n"
                + "外装色 W25\n"
                + "車台番号 A202A-0082833\n"
                + "シフト CVT\n"
                + "燃料 ガソリン\n"
                + "乗車定員 5\n"
                + "ドア形状 5\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("A202A-0082833", fields.get("chassisNo"));
        assertEquals("8313", fields.get("lotNo"));
        assertEquals("April 2025", fields.get("year"));
        assertEquals("Toyota", fields.get("make"));
        assertEquals("Toyota Raize", fields.get("model"));
        assertEquals("Hybrid Z", fields.get("grade"));
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("A", fields.get("interiorGrade"));
        assertEquals("10 km", fields.get("mileage"));
        assertEquals("1,200 cc", fields.get("engineSize"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("5AA-A202A", fields.get("modelCode"));
        assertEquals("W25", fields.get("colorCode"));
        assertEquals("White", fields.get("color"));
        assertEquals("CVT", fields.get("transmission"));
        assertEquals("5 people", fields.get("seats"));
        assertEquals("5-door", fields.get("bodyStyle"));
    }

    @Test
    void documentAiFillsFirstAndExistingParserFillsOnlyEmptyFields() {
        String ocr = ""
                + "8313\n"
                + "排気量 1,200cc\n"
                + "型式 5AA-A202A\n"
                + "初度登録 R7/4月\n"
                + "車名 ライズ\n"
                + "グレード ハイブリッドZ\n"
                + "評価点 S\n"
                + "走行 10 km\n"
                + "車台番号 A202A-0082833\n"
                + "燃料 ガソリン\n";
        DocumentAiClient.DocumentAiResult documentAi = new DocumentAiClient.DocumentAiResult(
                ocr,
                Arrays.asList(
                        new SaleProgress.DocumentAiEntity("chassis_number", "ZZZ-1111111", 0.99d),
                        new SaleProgress.DocumentAiEntity("vehicle_name", "Sheet Name", 0.99d),
                        new SaleProgress.DocumentAiEntity("mileage", "55", 0.99d),
                        new SaleProgress.DocumentAiEntity("fuel_type", "Gasoline", 0.99d),
                        new SaleProgress.DocumentAiEntity("first_registration_year", "2020", 0.99d),
                        new SaleProgress.DocumentAiEntity("first_registration_month", "3", 0.99d),
                        new SaleProgress.DocumentAiEntity("lot_number", "9999", 0.2d)
                )
        );

        Map<String, String> fields = parser.parsePage(documentAi).getFields();

        assertEquals("ZZZ-1111111", fields.get("chassisNo"));
        assertEquals("Sheet Name", fields.get("model"));
        assertEquals("55 km", fields.get("mileage"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("March 2020", fields.get("year"));
        assertEquals("8313", fields.get("lotNo"));
        assertEquals("1,200 cc", fields.get("engineSize"));
    }

    @Test
    void correctsIncompleteDocumentAiReadingsFromTheSheet() {
        String ocr = ""
                + "出品番号 63\n"
                + "排気量 3,400cc\n"
                + "型式 DBA-GRJ20W\n"
                + "初度登録 20/3月\n"
                + "グレード アスリート 3.5\n"
                + "走行 97,020 km\n"
                + "車台番号 200 116\n"
                + "燃料 ガソリン\n"
                + "カラーNo 062\n";
        DocumentAiClient.DocumentAiResult documentAi = new DocumentAiClient.DocumentAiResult(
                ocr,
                Arrays.asList(
                        new SaleProgress.DocumentAiEntity("first_registration_year", "2", 0.9d),
                        new SaleProgress.DocumentAiEntity("fuel_type", "ガソリン", 0.9d),
                        new SaleProgress.DocumentAiEntity("model_code", "DBA-GREJO", 0.9d),
                        new SaleProgress.DocumentAiEntity("chassis_number", "200 116", 0.9d),
                        new SaleProgress.DocumentAiEntity("lot_number", "63", 0.9d)
                )
        );
        WorkingSheet.AuctionSheetParser misTranslator = new WorkingSheet.AuctionSheetParser(text -> "Gunnon");

        Map<String, String> fields = misTranslator.parsePage(documentAi).getFields();

        assertEquals("March 2008", fields.get("year"));
        assertEquals("Gasoline", fields.get("fuel"));
        assertEquals("DBA-GRJ20W", fields.get("modelCode"));
        assertEquals("97,020 km", fields.get("mileage"));
        assertEquals("062", fields.get("colorCode"));
        assertEquals("200 116", fields.get("chassisNo"));
        assertEquals("Athlete 3.5", fields.get("grade"));
    }

    @Test
    void recoversCommonHandwritingOcrNoise() {
        String ocr = ""
                + "出品番号 8313\n"
                + "排気量 1,200 cc\n"
                + "型式 SAA-A2O2A\n"
                + "初度登録 R7/4\n"
                + "車名 ライス\n"
                + "グレード ハイプリッドＺ\n"
                + "評価点\n"
                + "S\n"
                + "内装\n"
                + "A\n"
                + "走行 10km\n"
                + "カラー W25\n"
                + "車台番号 A2O2A-0082833\n";

        AuctionParseResult result = parser.parsePage(ocr);
        assertTrue(result.isSuccess());
        Map<String, String> fields = result.getFields();

        assertEquals("A202A-0082833", fields.get("chassisNo"));
        assertEquals("8313", fields.get("lotNo"));
        assertEquals("April 2025", fields.get("year"));
        assertEquals("Toyota Raize", fields.get("model"));
        assertEquals("Hybrid Z", fields.get("grade"));
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("A", fields.get("interiorGrade"));
        assertEquals("10 km", fields.get("mileage"));
        assertEquals("1,200 cc", fields.get("engineSize"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("5AA-A202A", fields.get("modelCode"));
        assertEquals("W25", fields.get("colorCode"));
        assertEquals("White", fields.get("color"));
    }

    @Test
    void correctsLiveOcrMistakesFromRaizeSheet() {
        String ocr = ""
                + "8313\n"
                + "1,200 cc\n"
                + "2AA-A202A\n"
                + "2WD 4WD\n"
                + "走行 10 km\n"
                + "燃料 ガソリン\n"
                + "車歴 B )\n"
                + "A202A-0082833\n"
                + "W25\n"
                + "S\n"
                + "A\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("5AA-A202A", fields.get("modelCode"));
        assertEquals("A202A-0082833", fields.get("chassisNo"));
        assertEquals("Toyota Raize", fields.get("model"));
        assertEquals("Hybrid Z", fields.get("grade"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("5-door", fields.get("bodyStyle"));
        assertEquals("5 people", fields.get("seats"));
        assertEquals("1,200 cc", fields.get("engineSize"));
        assertEquals("10 km", fields.get("mileage"));
        assertEquals("W25", fields.get("colorCode"));
        assertEquals("White", fields.get("color"));
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("A", fields.get("interiorGrade"));
        assertEquals("CVT", fields.get("transmission"));
        assertEquals(null, fields.get("history"));
    }

    @Test
    void readsGradesWhenLabelsAreMissing() {
        String ocr = ""
                + "8313\n"
                + "5AA-A202A\n"
                + "PS PW ABS\n"
                + "S A\n"
                + "W25\n"
                + "A202A-0082833\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("A", fields.get("interiorGrade"));
    }

    @Test
    void readsPanelGradesFromGarbledLabels() {
        String ocr = ""
                + "評価 S\n"
                + "外装 B\n"
                + "内 A\n"
                + "A202A-0082833\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("B", fields.get("exteriorGrade"));
        assertEquals("A", fields.get("interiorGrade"));
    }

    @Test
    void parsesKgaaAquaSheet() {
        String ocr = ""
                + "KGAA 京都オートオークション 出品票\n"
                + "出品番号\n"
                + "1013\n"
                + "初度登録年月\n"
                + "24年11月\n"
                + "車名\n"
                + "アクア\n"
                + "グレード\n"
                + "S\n"
                + "ドア形状\n"
                + "5HB\n"
                + "評価点\n"
                + "R\n"
                + "型式\n"
                + "DBA-NHP10\n"
                + "排気量\n"
                + "1500 CC\n"
                + "車台番号\n"
                + "NHP10-6110240\n"
                + "シフト\n"
                + "F.AT\n"
                + "燃料\n"
                + "ガソリン\n"
                + "走行\n"
                + "9 8 6 8 5 km\n"
                + "外装色\n"
                + "ブルー\n"
                + "カラーNo.\n"
                + "8T7\n"
                + "内装色\n"
                + "グレー\n"
                + "乗車定員\n"
                + "5\n"
                + "冷房\n"
                + "AC\n"
                + "車歴\n"
                + "自家用\n"
                + "外装\n"
                + "B\n"
                + "内装\n"
                + "B\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("NHP10-6110240", fields.get("chassisNo"));
        assertEquals("1013", fields.get("lotNo"));
        assertEquals("DBA-NHP10", fields.get("modelCode"));
        assertEquals("November 2012", fields.get("year"));
        assertEquals("Toyota", fields.get("make"));
        assertEquals("Toyota Aqua", fields.get("model"));
        assertEquals("S", fields.get("grade"));
        assertEquals("R", fields.get("auctionGrade"));
        assertEquals("B", fields.get("exteriorGrade"));
        assertEquals("B", fields.get("interiorGrade"));
        assertEquals("98,685 km", fields.get("mileage"));
        assertEquals("1,500 cc", fields.get("engineSize"));
        assertEquals("Gasoline", fields.get("fuel"));
        assertEquals("F.AT", fields.get("transmission"));
        assertEquals("AC", fields.get("acType"));
        assertEquals("5-door", fields.get("bodyStyle"));
        assertEquals("5 people", fields.get("seats"));
        assertEquals("8T7", fields.get("colorCode"));
        assertEquals("Blue", fields.get("color"));
        assertEquals("Private", fields.get("history"));
    }

    @Test
    void parsesEnglishAuctionSheet() {
        String ocr = ""
                + "Lot number 8313\n"
                + "Displacement 1,200cc\n"
                + "Model code 5AA-A202A\n"
                + "First registration April 2025\n"
                + "Vehicle name Raize\n"
                + "Grade Hybrid Z\n"
                + "Evaluation S\n"
                + "Interior A\n"
                + "Mileage 10 km\n"
                + "Exterior color W25\n"
                + "Chassis number A202A-0082833\n"
                + "Shift CVT\n"
                + "Fuel Gasoline\n"
                + "Seating capacity 5\n"
                + "Doors 5\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("A202A-0082833", fields.get("chassisNo"));
        assertEquals("8313", fields.get("lotNo"));
        assertEquals("April 2025", fields.get("year"));
        assertEquals("Toyota Raize", fields.get("model"));
        assertEquals("Hybrid Z", fields.get("grade"));
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("A", fields.get("interiorGrade"));
        assertEquals("10 km", fields.get("mileage"));
        assertEquals("1,200 cc", fields.get("engineSize"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("5AA-A202A", fields.get("modelCode"));
        assertEquals("W25", fields.get("colorCode"));
        assertEquals("White", fields.get("color"));
        assertEquals("CVT", fields.get("transmission"));
        assertEquals("5 people", fields.get("seats"));
        assertEquals("5-door", fields.get("bodyStyle"));
    }

    @Test
    void readsCrownAthleteWhenTheNameAndModelCodeAreMisread() {
        String ocr = ""
                + "一般コーナー\n"
                + "出品番号 63\n"
                + "排気量 3500\n"
                + "型式 DBA-GRS206\n"
                + "評価点 4.5\n"
                + "グレード\n"
                + "アスリート 3.5\n"
                + "初度登録年月 車名 2WD 4WD\n"
                + "20/3月 フラウレ\n"
                + "内装 B\n"
                + "シフト FAT\n"
                + "車台番号 200 116\n"
                + "燃料 ガソリン\n"
                + "エアB\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("GRS204-200116", fields.get("chassisNo"));
        assertEquals("63", fields.get("lotNo"));
        assertEquals("March 2008", fields.get("year"));
        assertEquals("Toyota", fields.get("make"));
        assertEquals("Toyota Crown", fields.get("model"));
        assertEquals("Sedan", fields.get("bodyStyle"));
        assertEquals("Athlete 3.5", fields.get("grade"));
        assertEquals("4.5", fields.get("auctionGrade"));
        assertEquals("B", fields.get("interiorGrade"));
        assertEquals(null, fields.get("exteriorGrade"));
        assertEquals("DBA-GRS204", fields.get("modelCode"));
        assertEquals("3,500 cc", fields.get("engineSize"));
        assertEquals("Gasoline", fields.get("fuel"));
        assertEquals("F.AT", fields.get("transmission"));
    }

    @Test
    void replacesDocumentAiFrauleReadingForTheCrownSheet() {
        String ocr = ""
                + "出品番号 63\n"
                + "排気量 3500\n"
                + "型式 DBA-GRS206\n"
                + "初度登録年月\n"
                + "20/3月\n"
                + "車名 フラウレ\n"
                + "グレード アスリート 3.5\n"
                + "評価点 4.5\n"
                + "内装 B\n"
                + "車台番号 200 116\n";
        DocumentAiClient.DocumentAiResult documentAi = new DocumentAiClient.DocumentAiResult(
                ocr,
                Arrays.asList(
                        new SaleProgress.DocumentAiEntity("chassis_number", "200 116", 0.9d),
                        new SaleProgress.DocumentAiEntity("vehicle_name", "Fraule", 0.9d),
                        new SaleProgress.DocumentAiEntity("model_code", "DBA-GRS206", 0.9d),
                        new SaleProgress.DocumentAiEntity("exterior_grade", "B", 0.9d),
                        new SaleProgress.DocumentAiEntity("interior_grade", "B", 0.9d),
                        new SaleProgress.DocumentAiEntity("lot_number", "63", 0.9d)
                )
        );

        Map<String, String> fields = parser.parsePage(documentAi).getFields();

        assertEquals("GRS204-200116", fields.get("chassisNo"));
        assertEquals("Toyota Crown", fields.get("model"));
        assertEquals("March 2008", fields.get("year"));
        assertEquals("Sedan", fields.get("bodyStyle"));
        assertEquals("Athlete 3.5", fields.get("grade"));
        assertEquals("4.5", fields.get("auctionGrade"));
        assertEquals("B", fields.get("interiorGrade"));
        assertEquals(null, fields.get("exteriorGrade"));
        assertEquals("DBA-GRS204", fields.get("modelCode"));
        assertEquals("63", fields.get("lotNo"));
    }

    @Test
    void readsPriusUssSheet() {
        String ocr = ""
                + "一般コーナー\n"
                + "出品番号 364\n"
                + "排気量 1500\n"
                + "型式 DAA-NHW20\n"
                + "評価点 4.5\n"
                + "初度登録年月 19/9月\n"
                + "車名 プリウス\n"
                + "グレード Sツーリングセレクション\n"
                + "シフト AT\n"
                + "冷房 AC\n"
                + "走行 30,885 km\n"
                + "カラーNo 070\n"
                + "外装色 シルバー\n"
                + "燃料 ガソリン\n"
                + "車台番号 330815\n"
                + "内装 B\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("NHW20-330815", fields.get("chassisNo"));
        assertEquals("364", fields.get("lotNo"));
        assertEquals("September 2007", fields.get("year"));
        assertEquals("Toyota Prius", fields.get("model"));
        assertEquals("S Touring Selection", fields.get("grade"));
        assertEquals("4.5", fields.get("auctionGrade"));
        assertEquals("B", fields.get("interiorGrade"));
        assertEquals("DAA-NHW20", fields.get("modelCode"));
        assertEquals("1,500 cc", fields.get("engineSize"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("AT", fields.get("transmission"));
        assertEquals("AC", fields.get("acType"));
        assertEquals("30,885 km", fields.get("mileage"));
        assertEquals("070", fields.get("colorCode"));
        assertEquals("White", fields.get("color"));
    }

    @Test
    void readsAlphardPrintedSheet() {
        String ocr = ""
                + "TOYOTA ALPHARD AYH30W\n"
                + "出品番号 10\n"
                + "初度登録 R3年8月\n"
                + "車名 アルファード ハイブリッド\n"
                + "ドア形状 5W\n"
                + "グレード SR Cパッケージ\n"
                + "4WD\n"
                + "評価点 S\n"
                + "外装 A\n"
                + "内装 A\n"
                + "車歴 自家用\n"
                + "排気量 2500 cc\n"
                + "燃料 ガソリン\n"
                + "型式 6AA-AYH30W\n"
                + "走行 3 km\n"
                + "シフト IAT\n"
                + "エアコン AAC\n"
                + "外装色 パール\n"
                + "カラーNo 070\n"
                + "内装色 クロ\n"
                + "乗車定員 7\n"
                + "車台番号 AYH30-\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("10", fields.get("lotNo"));
        assertEquals("August 2021", fields.get("year"));
        assertEquals("Toyota Alphard", fields.get("model"));
        assertEquals("5-door", fields.get("bodyStyle"));
        assertEquals("SR C Package", fields.get("grade"));
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("A", fields.get("exteriorGrade"));
        assertEquals("A", fields.get("interiorGrade"));
        assertEquals("Private", fields.get("history"));
        assertEquals("2,500 cc", fields.get("engineSize"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("6AA-AYH30W", fields.get("modelCode"));
        assertEquals("3 km", fields.get("mileage"));
        assertEquals("IAT", fields.get("transmission"));
        assertEquals("AAC", fields.get("acType"));
        assertEquals("White", fields.get("color"));
        assertEquals("070", fields.get("colorCode"));
        assertEquals("7 people", fields.get("seats"));
    }

    @Test
    void readsWagonRPrintedSheet() {
        String ocr = ""
                + "出品番号 30080\n"
                + "初度登録 R8\n"
                + "年 3 月\n"
                + "車名 ワゴンR\n"
                + "ドア形状 5W\n"
                + "グレード ハイブリッド ZX\n"
                + "評価点 S\n"
                + "外装 A\n"
                + "内装 A\n"
                + "車歴 自家用\n"
                + "排気量 660 cc\n"
                + "燃料 ガソリン\n"
                + "型式 5AA-MH95S\n"
                + "走行 7 km\n"
                + "シフト IAT\n"
                + "エアコン AAC\n"
                + "外装色 パール\n"
                + "カラーNo ZVR\n"
                + "乗車定員 4 人\n"
                + "車台番号 MH95S-304069\n"
                + "長さ 339\n"
                + "幅 147\n"
                + "高さ 165\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("MH95S-304069", fields.get("chassisNo"));
        assertEquals("30080", fields.get("lotNo"));
        assertEquals("March 2026", fields.get("year"));
        assertEquals("Suzuki Wagon R", fields.get("model"));
        assertEquals("5-door", fields.get("bodyStyle"));
        assertEquals("Hybrid ZX", fields.get("grade"));
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("A", fields.get("exteriorGrade"));
        assertEquals("A", fields.get("interiorGrade"));
        assertEquals("Private", fields.get("history"));
        assertEquals("660 cc", fields.get("engineSize"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("5AA-MH95S", fields.get("modelCode"));
        assertEquals("7 km", fields.get("mileage"));
        assertEquals("IAT", fields.get("transmission"));
        assertEquals("AAC", fields.get("acType"));
        assertEquals("White", fields.get("color"));
        assertEquals("ZVR", fields.get("colorCode"));
        assertEquals("4 people", fields.get("seats"));
        assertEquals("339 cm", fields.get("lengthCm"));
        assertEquals("147 cm", fields.get("widthCm"));
        assertEquals("165 cm", fields.get("heightCm"));
    }

    @Test
    void readsCorollaAxioPrintedSheet() {
        String ocr = ""
                + "出品番号 2744\n"
                + "初度登録 H28 年 7 月\n"
                + "車名 カローラアクシオ\n"
                + "ドア形状 4SD\n"
                + "グレード ハイブリッド\n"
                + "評価点 4\n"
                + "外装 C\n"
                + "内装 B\n"
                + "車歴 レンタ\n"
                + "排気量 1500 cc\n"
                + "燃料 ガソリン\n"
                + "型式 DAA-NKE165\n"
                + "走行 38,731 km\n"
                + "シフト FAT\n"
                + "エアコン AC\n"
                + "外装色 パール\n"
                + "カラーNo 070\n"
                + "内装色 グレー\n"
                + "車台番号 7137412\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("NKE165-7137412", fields.get("chassisNo"));
        assertEquals("2744", fields.get("lotNo"));
        assertEquals("July 2016", fields.get("year"));
        assertEquals("Toyota Corolla Axio", fields.get("model"));
        assertEquals("4-door", fields.get("bodyStyle"));
        assertEquals("Hybrid", fields.get("grade"));
        assertEquals("4", fields.get("auctionGrade"));
        assertEquals("C", fields.get("exteriorGrade"));
        assertEquals("B", fields.get("interiorGrade"));
        assertEquals("Rental", fields.get("history"));
        assertEquals("1,500 cc", fields.get("engineSize"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("DAA-NKE165", fields.get("modelCode"));
        assertEquals("38,731 km", fields.get("mileage"));
        assertEquals("F.AT", fields.get("transmission"));
        assertEquals("AC", fields.get("acType"));
        assertEquals("White", fields.get("color"));
        assertEquals("070", fields.get("colorCode"));
    }

    @Test
    void readsRoomyPrintedSheet() {
        String ocr = ""
                + "出品番号 36497\n"
                + "初度登録 R6 年 6 月\n"
                + "車名 ルーミー\n"
                + "ドア形状 5ハコ\n"
                + "グレード G\n"
                + "評価点 5\n"
                + "外装 A\n"
                + "内装 B\n"
                + "車歴 レンタ\n"
                + "排気量 1000 cc\n"
                + "燃料 ガソリン\n"
                + "型式 5BA-M900A\n"
                + "走行 16,137 km\n"
                + "シフト IAT\n"
                + "エアコン AAC\n"
                + "外装色 パール\n"
                + "カラーNo W24\n"
                + "乗車定員 5 人\n"
                + "車台番号 M900A-1122703\n"
                + "長さ 370 幅 167 高さ 173\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("M900A-1122703", fields.get("chassisNo"));
        assertEquals("36497", fields.get("lotNo"));
        assertEquals("June 2024", fields.get("year"));
        assertEquals("Toyota Roomy", fields.get("model"));
        assertEquals("5-door", fields.get("bodyStyle"));
        assertEquals("G", fields.get("grade"));
        assertEquals("5", fields.get("auctionGrade"));
        assertEquals("A", fields.get("exteriorGrade"));
        assertEquals("B", fields.get("interiorGrade"));
        assertEquals("Rental", fields.get("history"));
        assertEquals("1,000 cc", fields.get("engineSize"));
        assertEquals("Gasoline", fields.get("fuel"));
        assertEquals("5BA-M900A", fields.get("modelCode"));
        assertEquals("16,137 km", fields.get("mileage"));
        assertEquals("IAT", fields.get("transmission"));
        assertEquals("AAC", fields.get("acType"));
        assertEquals("White", fields.get("color"));
        assertEquals("W24", fields.get("colorCode"));
        assertEquals("5 people", fields.get("seats"));
        assertEquals("370 cm", fields.get("lengthCm"));
        assertEquals("167 cm", fields.get("widthCm"));
        assertEquals("173 cm", fields.get("heightCm"));
    }

    @Test
    void readsRaizeSheetAsPrinted() {
        String ocr = ""
                + "コンパクトコーナー\n"
                + "出品番号 8313\n"
                + "排気量 1,200cc\n"
                + "型式 5AA-A202A\n"
                + "初度登録 R7/4月\n"
                + "車名 ライズ\n"
                + "5D\n"
                + "グレード ハイブリッドZ\n"
                + "評価点 S\n"
                + "内装 A\n"
                + "走行 10 km\n"
                + "外装色 パール\n"
                + "カラーNo W25\n"
                + "車台番号 A202A-0082833\n"
                + "シフト FAT\n"
                + "冷房 AAC\n"
                + "燃料 ガソリン\n"
                + "乗車定員 5\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("A202A-0082833", fields.get("chassisNo"));
        assertEquals("8313", fields.get("lotNo"));
        assertEquals("April 2025", fields.get("year"));
        assertEquals("Toyota Raize", fields.get("model"));
        assertEquals("5-door", fields.get("bodyStyle"));
        assertEquals("Hybrid Z", fields.get("grade"));
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("A", fields.get("interiorGrade"));
        assertEquals("10 km", fields.get("mileage"));
        assertEquals("Hybrid", fields.get("fuel"));
        assertEquals("5AA-A202A", fields.get("modelCode"));
        assertEquals("White", fields.get("color"));
        assertEquals("W25", fields.get("colorCode"));
        assertEquals("F.AT", fields.get("transmission"));
        assertEquals("AAC", fields.get("acType"));
        assertEquals("5 people", fields.get("seats"));
    }
}
