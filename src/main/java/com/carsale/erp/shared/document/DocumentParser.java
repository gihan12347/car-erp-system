package com.carsale.erp.shared.document;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.ocr.DocumentAiClient;

public interface DocumentParser {
    AuctionParseResult parsePage(String text);
    AuctionParseResult parsePage(DocumentAiClient.DocumentAiResult documentAi);
    String getProcessorId();
    String getDocumentName();

    default String getOcrLanguage() {
        return null;
    }
}
