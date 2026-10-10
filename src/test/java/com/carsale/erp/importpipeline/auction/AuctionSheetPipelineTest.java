package com.carsale.erp.importpipeline.auction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.WorkingSheet;
import com.carsale.erp.shared.ocr.AuctionSheetImagePreparer;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.ocr.dto.DocumentAiEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSheetPipelineTest {

    private final WorkingSheet.AuctionSheetParser parser = new WorkingSheet.AuctionSheetParser(text -> text);

    @Test
    void usesAuctionDocumentAiProcessor() {
        assertEquals("bdd4eb40cbc32911", parser.getProcessorId());
    }

    @Test
    void documentAiTextBecomesEnglishFieldsAndFlagsUncertainty() {
        String text = ""
                + "KGAA 京都オートオークション 出品票\n"
                + "出品番号\n1013\n"
                + "初度登録年月\n24年11月\n"
                + "車名\nアクア\n"
                + "ドア形状\n5HB\n"
                + "評価点\nR\n"
                + "型式\nDBA-NHP10\n"
                + "排気量\n1500 CC\n"
                + "車台番号\nNHP10-6110240\n"
                + "シフト\nF.AT\n"
                + "燃料\nガソリン\n"
                + "走行\n9 8 6 8 5 km\n"
                + "外装色\nブルー\n"
                + "カラーNo.\n8T7\n"
                + "乗車定員\n5\n"
                + "冷房\nAC\n"
                + "車歴\n自家用\n"
                + "外装\nB\n"
                + "内装\nB\n";
        DocumentAiClient.DocumentAiResult document = new DocumentAiClient.DocumentAiResult(text, Arrays.asList(
                new DocumentAiEntity("grade", "S", 0.93d),
                new DocumentAiEntity("mileage", "1 km", 0.2d)
        ));

        AuctionParseResult result = parser.parsePage(document);

        assertTrue(result.isSuccess());
        assertEquals("Toyota Aqua", result.getFields().get("model"));
        assertEquals("S", result.getFields().get("grade"));
        assertEquals("98,685 km", result.getFields().get("mileage"));
        assertEquals("Blue", result.getFields().get("color"));
        assertEquals("November 2012", result.getFields().get("year"));
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("mileage")));
        assertTrue(result.getMessage().contains("Check:"));
    }

    @Test
    void straightensATiltedSheet() {
        BufferedImage sheet = new BufferedImage(420, 520, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 420, 520);
        graphics.setColor(Color.BLACK);
        for (int y = 40; y < 480; y += 36) {
            graphics.fillRect(24, y, 370, 3);
        }
        graphics.dispose();

        BufferedImage tilted = AuctionSheetImagePreparer.rotate(sheet, 6d);
        double correction = AuctionSheetImagePreparer.skewDegrees(tilted);
        assertTrue(Math.abs(correction + 6d) < 1.2d, "correction was " + correction);

        BufferedImage straightened = AuctionSheetImagePreparer.process(tilted, 1, 800);
        assertTrue(Math.abs(AuctionSheetImagePreparer.skewDegrees(straightened)) < 1.2d);
    }

    @Test
    void mapsScrambledKgaaTranslation() {
        String ocr = ""
                + "KCAA Kyoto Kyoto Auto Auction Listing Form\n"
                + "Listing Number\n"
                + "First Registration Date\n"
                + "SR\n"
                + "Vehicle Name\n"
                + "Grade\n"
                + "1013 2411 yen Aqua\n"
                + "Year\n"
                + "10,110\n"
                + "DAA-NHPIO\n"
                + "Mileage\n"
                + "Shift Air Conditioning Fuel\n"
                + "Gasoline/Diesel\n"
                + "Door/Shape\n"
                + "Manufacturer\n"
                + "Door/Shape\n"
                + "Evaluation Score\n"
                + "4WD 5HB\n"
                + "1500\n"
                + "99685 F.AT, One Owner\n"
                + "Exterior Color Blue\n"
                + "Color8T7, Interior Color Black, Gray\n"
                + "Chassis Number/HP10-6110240\n"
                + "Seating Capacity (5)\n"
                + "AC. Warranty and Instruction Manual Included\n"
                + "R707 BP\n"
                + "(Exterior)\n"
                + "(Contents)\n"
                + "R\n"
                + "b\n"
                + "b\n";

        Map<String, String> fields = parser.parsePage(ocr).getFields();

        assertEquals("NHP10-6110240", fields.get("chassisNo"));
        assertEquals("1013", fields.get("lotNo"));
        assertEquals("November 2012", fields.get("year"));
        assertEquals("Toyota Aqua", fields.get("model"));
        assertEquals("S", fields.get("grade"));
        assertEquals("R", fields.get("auctionGrade"));
        assertEquals("B", fields.get("exteriorGrade"));
        assertEquals("B", fields.get("interiorGrade"));
        assertEquals("DBA-NHP10", fields.get("modelCode"));
        assertEquals("Blue", fields.get("color"));
        assertEquals("5-door", fields.get("bodyStyle"));
    }

    @Test
    void emptyDocumentAiResultDoesNotThrow() {
        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult(" ", Collections.emptyList()));
        assertTrue(result.getMessage() != null && !result.getMessage().isEmpty());
    }
}
