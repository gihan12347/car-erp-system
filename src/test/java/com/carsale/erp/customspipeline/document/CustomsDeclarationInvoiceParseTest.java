package com.carsale.erp.customspipeline.document;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.CustomsDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomsDeclarationInvoiceParseTest {

    private final CustomsDeclaration parser = new CustomsDeclaration("");

    @Test
    void parsesInlineInvoiceSectionFromNoisyOcr() {
        AuctionParseResult result = parser.parsePage(INLINE_OCR);

        assertTrue(result.isSuccess());
        assertEquals("1,486,650.00", result.getFields().get("invoiceFob"));
        assertEquals("76,970.04", result.getFields().get("invoiceFreight"));
        assertEquals("5,000.00", result.getFields().get("invoiceInsurance"));
        assertNull(result.getFields().get("invoiceOther"));
        assertEquals("1,568,620.04", result.getFields().get("invoiceTotal"));
        assertEquals("2.0591", result.getFields().get("exchangeRate"));
        assertEquals("3,229,945.52", result.getFields().get("valueNcy"));
    }

    @Test
    void parsesColumnarInvoiceSectionFromGoogleVisionOcr() {
        AuctionParseResult result = parser.parsePage(COLUMNAR_OCR);

        assertTrue(result.isSuccess());
        assertEquals("1,486,650.00", result.getFields().get("invoiceFob"));
        assertEquals("76,970.04", result.getFields().get("invoiceFreight"));
        assertEquals("5,000.00", result.getFields().get("invoiceInsurance"));
        assertNull(result.getFields().get("invoiceOther"));
        assertEquals("1,568,620.04", result.getFields().get("invoiceTotal"));
        assertEquals("2.0591", result.getFields().get("exchangeRate"));
        assertEquals("3,229,945.52", result.getFields().get("valueNcy"));
    }

    @Test
    void ignoresWebsiteAndTaxFiguresOutsideSectionC() {
        AuctionParseResult result = parser.parsePage(NOISY_FULL_OCR);

        assertEquals("1,486,650.00", result.getFields().get("invoiceFob"));
        assertEquals("76,970.04", result.getFields().get("invoiceFreight"));
        assertEquals("5,000.00", result.getFields().get("invoiceInsurance"));
        assertNull(result.getFields().get("invoiceOther"));
        assertEquals("1,568,620.04", result.getFields().get("invoiceTotal"));
        assertEquals("2.0591", result.getFields().get("exchangeRate"));
        assertEquals("3,229,945.52", result.getFields().get("valueNcy"));
    }

    private static final String INLINE_OCR =
            "CALCULATION OF TAXES\n"
            + "CID     3,229,946       20%     645,990\n"
            + "Total   4,150,242\n"
            + "50.     c.      (TOTAL INVOICE AMOUNT) CURRENCY\n"
            + "F00/CIF 1,486,650.00 JPY\n"
            + "FREIGHT 76,970.04 JPY\n"
            + "INSURANCE       5,000.00 PY\n"
            + "OTHER\n"
            + "51.     TOTAL   1.568,620.04 jPY\n"
            + "23. Exchange Rate\n"
            + "2.0591\n"
            + "46. Value (NCY)   3,229,945.52\n"
            + "B/L: FRT: USD 526.28 X 301.1496 / 2.0591 = JPY 76,970.04\n";

    private static final String COLUMNAR_OCR =
            "22. Currency And Total Amount Invoiced\n"
            + "JPY\n"
            + "1,568,620.04\n"
            + "23. Exchange Rate\n"
            + "2.0591\n"
            + "46. Value (NCY)\n"
            + "3,229,945.52\n"
            + "CALCULATION OF TAXES\n"
            + "Total\n"
            + "4,150,242\n"
            + "50.\n"
            + "C.\n"
            + "(TOTAL INVOICE AMOUNT)\n"
            + "CURRENCY\n"
            + "FOB/CIF\n"
            + "1,486,650.00\n"
            + "FREIGHT\n"
            + "INSURANCE\n"
            + "51.\n"
            + "52.\n"
            + "OTHER\n"
            + "TOTAL\n"
            + "JPY\n"
            + "76,970.04 JPY\n"
            + "5,000.00 JPY\n"
            + "1,568,620.04 JPY\n"
            + "B/L: FRT: USD 526.28 X 301.1496 / 2.0591-JPY 76,970.04\n"
            + "53. COLOMBO MOTOR TRADING COMPANY\n"
            + "do hereby affirm\n";

    private static final String NOISY_FULL_OCR =
            "22. Currency And Total Amount Invoiced JPY 1,568,620.04\n"
            + "23. Exchange Rate 2.0591\n"
            + "WEBSITE VALUE: 1,923,900.00/110 X 100\n"
            + "A/V: JPY 1,749,000.00 X 85%\n"
            + "A/V: JPY FOB: 1,486,650.00, FRT:,INS:\n"
            + "I/V: JPY FOB: 1,310,000.00, FRT: 45,000.00,INS: 5,000.00\n"
            + "B/L: FRT: USD 526.28 X 301.1496 / 2.0591 = JPY 76,970.04\n"
            + "46. Value (NCY) 3,229,945.52\n"
            + "CALCULATION OF TAXES\n"
            + "CID 3,229,946 20% 645,990\n"
            + "Total 4,150,242\n"
            + "(TOTAL INVOICE AMOUNT) CURRENCY\n"
            + "FOB/CIF 1,486,650.00 JPY\n"
            + "FREIGHT 76,970.04 JPY\n"
            + "INSURANCE 5,000.00 JPY\n"
            + "OTHER -\n"
            + "TOTAL 1,568,620.04 JPY\n"
            + "Declaration Submitted By\n";
}
