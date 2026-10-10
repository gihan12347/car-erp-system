package com.carsale.erp.importpipeline.auction;

import java.util.Map;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.WorkingSheet;
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
}
