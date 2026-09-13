package com.carsale.erp.customspipeline.document;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentNoticeParseTest {

    private final AssessmentNotice parser = new AssessmentNotice();

    @Test
    void parsesRealOcrWithAmountsAboveCodesAndBrokenCommas() {
        AuctionParseResult result = parser.parsePage(REAL_OCR);

        assertTrue(result.isSuccess());
        assertEquals("354,867", result.getFields().get("assessmentTaxCid"));
        assertEquals("177,434", result.getFields().get("assessmentTaxSur"));
        assertEquals("1,992,000", result.getFields().get("assessmentTaxXid"));
        assertEquals("805,692", result.getFields().get("assessmentTaxVat"));
        assertEquals("15,000", result.getFields().get("assessmentTaxVel"));
        assertEquals("3,346,743", result.getFields().get("assessmentTotalAssessed"));
        assertEquals("3,346,743", result.getFields().get("assessmentTotalPaid"));
        assertEquals("1,200", result.getFields().get("assessmentTaxOtc"));
        assertEquals("250", result.getFields().get("assessmentTaxCom"));
        assertEquals("300", result.getFields().get("assessmentTaxExm"));
    }

    @Test
    void parsesCleanItemTaxesTable() {
        AuctionParseResult result = parser.parsePage(
                "Global taxes\n"
                        + "OTC Over Time Charges 1,200\n"
                        + "COM Computer Fee 250\n"
                        + "EXM Other Receipts-Examination Fees 300\n"
                        + "Item taxes\n"
                        + "CID Customs Import Duty 354,867\n"
                        + "SUR Surcharge 177,434\n"
                        + "XID Excise Duty 1,992,000\n"
                        + "VAT Value Added Tax 805,692\n"
                        + "VEL Vehicle Entitlement Levy 15,000\n"
                        + "Total assessed amount for the declaration 3,346,743\n"
                        + "Total amount paid: 3,346,743\n"
        );

        assertEquals("354,867", result.getFields().get("assessmentTaxCid"));
        assertEquals("177,434", result.getFields().get("assessmentTaxSur"));
        assertEquals("1,992,000", result.getFields().get("assessmentTaxXid"));
        assertEquals("805,692", result.getFields().get("assessmentTaxVat"));
        assertEquals("15,000", result.getFields().get("assessmentTaxVel"));
        assertEquals("3,346,743", result.getFields().get("assessmentTotalAssessed"));
    }

    /** Exact OCR shape from the user's Assessment Notice scan. */
    private static final String REAL_OCR =
            "inistry of Finance\n"
            + "Customs Headquarters\n"
            + "Assessment Notice\n"
            + "Customs office: Hambanthota Import Office - Sea 2025 HBIM1 | 1798\n"
            + "Declaration reference\n"
            + "Global taxes\n"
            + "Tax code        Tax description Tax code\n"
            + "OTC     over Time Charges       1,200\n"
            + "COM     Computer Fee    250\n"
            + "300]\n"
            + "EXM     other Receipts-Examination Fees\n"
            + "Item taxes\n"
            + "Tax     code    Tax description Tax value\n"
            + "354,867\n"
            + "CID     Customs Import Duty\n"
            + "177,434\n"
            + "SUR     Surcharge       1,992, 000\n"
            + "XID     Excise (Special Prov.) Duty\n"
            + "VAT     Value Added Tax 805, 692\n"
            + "VEL     Vehicle Entitlement Levy        15,000\n"
            + "Total assessed amount for the declaration       3, 346,743\n"
            + "Total   amount paid:    3, 346,743\n";
}
