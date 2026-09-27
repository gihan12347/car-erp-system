package com.carsale.erp.customspipeline.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.WorkingSheet;
import com.carsale.erp.shared.ocr.DocumentAiClient;

class WorkingSheetDocumentAiTest {

    private final WorkingSheet parser = new WorkingSheet("118d57fe35792578");

    @Test
    void processorIdIsTheWorkingSheetExtractor() {
        assertThat(parser.getProcessorId()).isEqualTo("118d57fe35792578");
    }

    @Test
    void mapsDocumentAiEntitiesFromTheMotorVehicleSheet() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<DocumentAiClient.DocumentAiEntity>();
        entities.add(entity("document_identifier", "F5-1141982-AE"));
        entities.add(entity("hs_code", "8703.21.69"));
        entities.add(entity("type_of_vehicle", "01 UNIT USED HONDA N BOX CUSTOM PETROL ST/WAGON"));
        entities.add(entity("reference_no", ""));
        entities.add(entity("name_of_vessel", "GUARDIAN LEADER"));
        entities.add(entity("chassis_nos", "JF5-1141982"));
        entities.add(entity("agents_fob_amount", "1,749,000.00"));
        entities.add(entity("agents_fob_calculation", "1,923,900.00/110 x 100"));
        entities.add(entity("invoiced_fob", "1,310,000.00"));
        entities.add(entity("invoiced_freight", "45,000.00"));
        entities.add(entity("invoiced_insurance", "5,000.00"));
        entities.add(entity("bl_freight_details", "USD 526.28 @ 301.1496 = JPY @ 2.0591 = 76,970.04"));
        entities.add(entity("date_of_bl", "2025-03-14T00:00:00"));
        entities.add(entity("date_of_manufacture", "2024-11-11"));
        entities.add(entity("age_difference_for_icl", "0-4-3"));
        entities.add(entity("date_of_1st_registration", "2025-01-15"));
        entities.add(entity("website_value", "1,749,000.00"));
        entities.add(entity("fifteen_percent_of_value", "262,350.00"));
        entities.add(entity("fob_value_85_percent", "1,486,650.00"));
        entities.add(entity("fob_value_currency", "JPY"));
        entities.add(entity("lc_no", "ABLOLCS002250017"));
        entities.add(entity("lc_amount", "1,360,000.00"));
        entities.add(entity("lc_bank", "AMANA BANK LIMITED"));
        entities.add(entity("importer_name", "AMBC MOTOR TRADING COMPANY"));
        entities.add(entity("lc_date_of_issue", "17/02/2025"));
        entities.add(entity("declarant_company", "WIT CLEARING & FORWARDING CO"));
        entities.add(entity("fob_for_fiscal_levies", "1,486,650.00"));
        entities.add(entity("freight_charges_for_fiscal_levies", "76,970.04"));
        entities.add(entity("insurance_charges_for_fiscal_levies", "5,000.00"));
        entities.add(entity("value_of_options_for_fiscal_levies", "0.00"));
        entities.add(entity("total_value_for_fiscal_levies", "1,568,620.04"));
        entities.add(entity("total_value_for_fiscal_levies_currency", "JPY"));

        AuctionParseResult result = parser.parsePage(new DocumentAiClient.DocumentAiResult("raw", entities));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields())
                .containsEntry("worksheetRef", "F5-1141982-AE")
                .containsEntry("worksheetHsCode", "8703.21.69")
                .containsEntry("worksheetChassisNo", "JF5-1141982")
                .containsEntry("worksheetAgentsFob", "1,749,000.00")
                .containsEntry("worksheetAgentsFobCalc", "1,923,900.00/110 x 100")
                .containsEntry("worksheetBlFreightCalc", "USD 526.28 @ 301.1496 = JPY @ 2.0591 = 76,970.04")
                .containsEntry("worksheetBlFreightAmount", "76,970.04")
                .containsEntry("worksheetBlDate", "2025-03-14")
                .containsEntry("worksheetAgeDifference", "0 years 4 months 3 days")
                .containsEntry("worksheetFobValue85Currency", "JPY")
                .containsEntry("worksheetLcImporter", "AMBC MOTOR TRADING COMPANY")
                .containsEntry("worksheetClearingAgent", "WIT CLEARING & FORWARDING CO")
                .containsEntry("worksheetFiscalTotal", "1,568,620.04")
                .containsEntry("worksheetFiscalTotalCurrency", "JPY");
        assertThat(result.getFields()).doesNotContainKey("worksheetReferenceNo");
    }

    @Test
    void mapsSecondSheetAgeAndChassis() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<DocumentAiClient.DocumentAiEntity>();
        entities.add(entity("chassis_nos", "LA350A-0041575"));
        entities.add(entity("age_difference_for_icl", "2 years 1 month 29 days"));
        entities.add(entity("agents_fob_amount", "932,000.00"));
        entities.add(entity("bl_freight_details", "USD 441.03 @ 301.1496 = JPY @ 2.0591 = 64,501.97"));

        AuctionParseResult result = parser.parsePage(new DocumentAiClient.DocumentAiResult("raw", entities));

        assertThat(result.getFields())
                .containsEntry("worksheetChassisNo", "LA350A-0041575")
                .containsEntry("worksheetAgeDifference", "2 years 1 month 29 days")
                .containsEntry("worksheetAgentsFob", "932,000.00")
                .containsEntry("worksheetBlFreightAmount", "64,501.97");
    }

    private static DocumentAiClient.DocumentAiEntity entity(String type, String value) {
        return new DocumentAiClient.DocumentAiEntity(type, value, 0.99);
    }
}
