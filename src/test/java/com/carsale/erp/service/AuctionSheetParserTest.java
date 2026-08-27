package com.carsale.erp.service;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.carsale.erp.dto.AuctionParseResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSheetParserTest {

    private final AuctionSheetParser parser = new AuctionSheetParser();

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

        Map<String, String> fields = parser.parse(ocr).getFields();

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

        AuctionParseResult result = parser.parse(ocr);
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

        Map<String, String> fields = parser.parse(ocr).getFields();

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

        Map<String, String> fields = parser.parse(ocr).getFields();
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

        Map<String, String> fields = parser.parse(ocr).getFields();
        assertEquals("S", fields.get("auctionGrade"));
        assertEquals("B", fields.get("exteriorGrade"));
        assertEquals("A", fields.get("interiorGrade"));
    }
}
