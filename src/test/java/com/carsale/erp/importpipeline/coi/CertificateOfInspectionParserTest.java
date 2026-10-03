package com.carsale.erp.importpipeline.coi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.CertificateOfInspectionParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

class CertificateOfInspectionParserTest {

    private final CertificateOfInspectionParser parser = new CertificateOfInspectionParser("43cd71a403f5b8f7");

    @Test
    void parsePageMapsDocumentAiEntitiesAndNormalizesInspectionDate() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<>();
        entities.add(entity("certificate_number", "LK1-A000705"));
        entities.add(entity("chassis_number", "JF5-1141982"));
        entities.add(entity("engine_capacity", "650"));
        entities.add(entity("engine_number", "S07B-6236116"));
        entities.add(entity("inspected_mileage", "5 km"));
        entities.add(entity("inspection_branch", "East Japan Area"));
        entities.add(entity("inspection_date", "2025-03-05T00:00:00"));
        entities.add(entity("make", "HONDA"));
        entities.add(entity("model", "6BA-JF5"));
        entities.add(entity("remarks", "Year of Manufacture:2024"));
        entities.add(entity("year_of_first_registration", "202501"));
        entities.add(entity("date_of_issue", "07/03/2025"));

        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult("raw certificate of inspection text", entities));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields()).containsEntry("certificateNo", "LK1-A000705");
        assertThat(result.getFields()).containsEntry("chassisVin", "JF5-1141982");
        assertThat(result.getFields()).containsEntry("chassisNo", "JF5-1141982");
        assertThat(result.getFields()).containsEntry("engineCapacity", "650");
        assertThat(result.getFields()).containsEntry("engineNo", "S07B-6236116");
        assertThat(result.getFields()).containsEntry("inspectedMileage", "5 km");
        assertThat(result.getFields()).containsEntry("inspectionBranch", "East Japan Area");
        assertThat(result.getFields()).containsEntry("inspectionDate", "2025/03/05");
        assertThat(result.getFields()).containsEntry("make", "HONDA");
        assertThat(result.getFields()).containsEntry("model", "6BA-JF5");
        assertThat(result.getFields()).containsEntry("remarks", "Year of Manufacture:2024");
        assertThat(result.getFields()).containsEntry("firstRegistration", "202501");
        assertThat(result.getFields()).doesNotContainKey("issueDate");
        assertThat(parser.getProcessorId()).isEqualTo("43cd71a403f5b8f7");
    }

    @Test
    void slashDateShowsYearThenMonthThenDay() {
        assertThat(CertificateOfInspectionParser.toSlashDate("05/03/2025")).isEqualTo("2025/05/03");
        assertThat(CertificateOfInspectionParser.toSlashDate("2025-05-03")).isEqualTo("2025/05/03");
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
