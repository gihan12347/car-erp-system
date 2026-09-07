package com.carsale.erp.shared.ocr;

import com.carsale.erp.importpipeline.auction.AuctionSheetEnglish;

@FunctionalInterface
public interface JapaneseTextTranslator {

    /**
     * Translate Japanese (or mixed) text to English. Values with no Japanese
     * characters are returned unchanged.
     */
    String translateJaToEn(String text);

    /**
     * Translate a full auction sheet in one call. Codes such as chassis and
     * color numbers are kept as-is so fields can be read from the English text.
     */
    default String translateDocument(String text) {
        return AuctionSheetEnglish.translateSheet(text, this);
    }
}
