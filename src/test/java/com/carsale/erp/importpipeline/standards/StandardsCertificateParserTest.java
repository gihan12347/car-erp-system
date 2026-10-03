package com.carsale.erp.importpipeline.standards;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.StandardsCertificateParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

class StandardsCertificateParserTest {

    private final StandardsCertificateParser parser = new StandardsCertificateParser("standards-processor");

    @Test
    void parsePageMapsDocumentAiEntitiesAndCheckboxes() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<DocumentAiClient.DocumentAiEntity>();
        entities.add(entity("certificate_number", "LK1-S000705"));
        entities.add(entity("co", "1.15"));
        entities.add(entity("nmhc", "0.025"));
        entities.add(entity("nox", "0.013"));
        entities.add(entity("pm", "-"));
        entities.add(entity("hc", "N/A"));
        entities.add(entity("hc_plus_nox", "N/A"));
        entities.add(entity("thc", "N/A"));
        entities.add(entity("ch4", "N/A"));
        entities.add(entity("smoke", "N/A"));
        entities.add(entity("abs", "true"));
        entities.add(entity("air_bags_driver", "checked"));
        entities.add(entity("air_bags_front_passenger", "false"));
        entities.add(entity("seat_belts_driver_front", "☑"));
        entities.add(entity("seat_belts_other", "no"));
        entities.add(entity("remarks", "Year of Manufacture:2024"));
        entities.add(entity("make", "HONDA"));

        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult("raw standards certificate", entities));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields()).containsEntry("certificateNo", "LK1-S000705");
        assertThat(result.getFields()).containsEntry("emissionCo", "1.15");
        assertThat(result.getFields()).containsEntry("emissionNmhc", "0.025");
        assertThat(result.getFields()).containsEntry("emissionNox", "0.013");
        assertThat(result.getFields()).containsEntry("emissionPm", "-");
        assertThat(result.getFields()).containsEntry("emissionHc", "N/A");
        assertThat(result.getFields()).containsEntry("emissionHcNox", "N/A");
        assertThat(result.getFields()).containsEntry("emissionThc", "N/A");
        assertThat(result.getFields()).containsEntry("emissionCh4", "N/A");
        assertThat(result.getFields()).containsEntry("emissionSmoke", "N/A");
        assertThat(result.getFields()).containsEntry("absFitted", "true");
        assertThat(result.getFields()).containsEntry("driverAirbag", "true");
        assertThat(result.getFields()).containsEntry("passengerAirbag", "false");
        assertThat(result.getFields()).containsEntry("threePointSeatBelts", "true");
        assertThat(result.getFields()).containsEntry("twoPointSeatBelts", "false");
        assertThat(result.getFields()).containsEntry("remarks", "Year of Manufacture:2024");
        assertThat(result.getFields()).doesNotContainKey("make");
        assertThat(parser.getProcessorId()).isEqualTo("standards-processor");
    }

    @Test
    void parsePageDoesNotUseRegexFallback() {
        AuctionParseResult result = parser.parsePage("CO 1.15 g/km\nMake HONDA\n");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEmpty();
        assertThat(result.getFields()).isEmpty();
    }

    private static DocumentAiClient.DocumentAiEntity entity(String type, String value) {
        return new DocumentAiClient.DocumentAiEntity(type, value, 0.99);
    }
}
