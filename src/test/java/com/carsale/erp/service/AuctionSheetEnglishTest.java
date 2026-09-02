package com.carsale.erp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSheetEnglishTest {

    @Test
    void convertDatesWithoutAWordMap() {
        assertEquals("Apr 2018", AuctionSheetEnglish.convert("平成30年4月", text -> text));
        assertEquals("May 2025", AuctionSheetEnglish.convert("令和7年5月", text -> text));
    }

    @Test
    void convertUsesTranslatorForJapanese() {
        String out = AuctionSheetEnglish.convert("ハイブリッドZ", text -> "Hybrid Z");
        assertEquals("Hybrid Z", out);
    }

    @Test
    void translateLineKeepsColorCodeOnTheEnglishLine() {
        String out = AuctionSheetEnglish.translateLine("外装色 W25", text -> "Exterior color White");
        assertEquals("Exterior color White W25", out);
    }

    @Test
    void translateDocumentUsesOneTranslatorCall() {
        int[] calls = new int[] {0};
        JapaneseTextTranslator translator = text -> {
            calls[0]++;
            return text
                    .replace("車名", "Vehicle name")
                    .replace("外装色", "Exterior color");
        };
        String out = translator.translateDocument("車名 ライズ\n外装色 W25\n評価点 S");
        assertEquals(1, calls[0]);
        assertTrue(out.contains("Vehicle name"));
        assertTrue(out.contains("Exterior color W25"));
    }

    @Test
    void translateDocumentKeepsLineBreaks() {
        JapaneseTextTranslator translator = text -> text == null ? null : text.replace("車名", "Vehicle name");
        String out = translator.translateDocument("車名 ライズ\n5AA-A202A");
        assertEquals("Vehicle name ライズ\n5AA-A202A", out);
    }

    @Test
    void containsJapaneseDetectsKanaAndKanji() {
        assertTrue(AuctionSheetEnglish.containsJapanese("ライズ"));
        assertTrue(AuctionSheetEnglish.containsJapanese("Hybrid ハイブリッド"));
        assertFalse(AuctionSheetEnglish.containsJapanese("Hybrid Z"));
    }
}
