package com.carsale.erp.customspipeline.document;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.CustomsDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomsDeclarationInvoiceParseTest {

    private final CustomsDeclaration parser = new CustomsDeclaration("");

    @Test
    void parsePageDoesNotUseRegexFallback() {
        AuctionParseResult result = parser.parsePage(
                "FOB/CIF 1,486,650.00 JPY\nFREIGHT 76,970.04 JPY\nTOTAL 1,568,620.04 JPY\n");

        assertFalse(result.isSuccess());
        assertTrue(result.getFields().isEmpty());
    }
}
