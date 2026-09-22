package com.carsale.erp.importpipeline.odometer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.document.OdometerCertificateParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

class OdometerCertificateParserTest {

    private final OdometerCertificateParser parser = new OdometerCertificateParser("f15abb638c6327b7");

    @Test
    void parsePageMapsDocumentAiEntitiesAndNormalizesDates() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<>();
        entities.add(entity("certificate_no", "LK1-A000705"));
        entities.add(entity("chassis_vin", "JF5-1141982"));
        entities.add(entity("current_odometer_reading", "5 km"));
        entities.add(entity("date_of_inspection", "05/03/2025"));
        entities.add(entity("date_of_issue", "2025-03-07T00:00:00"));
        entities.add(entity("location", "Nagoya"));
        entities.add(entity("make", "Honda"));
        entities.add(entity("model", "N-BOX"));

        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult("raw odometer certificate text", entities));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields()).containsEntry("jevicCertificateNo", "LK1-A000705");
        assertThat(result.getFields()).containsEntry("jevicChassisVin", "JF5-1141982");
        assertThat(result.getFields()).containsEntry("jevicCurrentOdometer", "5 km");
        assertThat(result.getFields()).containsEntry("jevicInspectionDate", "2025-03-05");
        assertThat(result.getFields()).containsEntry("jevicIssueDate", "2025-03-07");
        assertThat(result.getFields()).containsEntry("jevicLocation", "Nagoya");
        assertThat(result.getFields()).containsEntry("jevicMake", "Honda");
        assertThat(result.getFields()).containsEntry("jevicModel", "N-BOX");
        assertThat(parser.getProcessorId()).isEqualTo("f15abb638c6327b7");
    }

    @Test
    void parsePageDoesNotUseRegexFallback() {
        AuctionParseResult result = parser.parsePage(
                "Certificate No: LK1-A000705\nChassis Number: JF5-1141982\nDate of Issue: 07/03/2025\n");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFields()).isEmpty();
    }

    private static DocumentAiClient.DocumentAiEntity entity(String type, String value) {
        return new DocumentAiClient.DocumentAiEntity(type, value, 0.99);
    }
}
