package com.carsale.erp.importpipeline.gradesearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.GradeSearchParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

class GradeSearchParserTest {

    private final GradeSearchParser parser = new GradeSearchParser("c86ec486ccb7e7d7");

    @Test
    void parsePageMapsDocumentAiEntities() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<>();
        entities.add(entity("chassis_number", "JF5-1141982"));
        entities.add(entity("grade", "Hybrid Z"));

        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult("raw grade search text", entities));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields()).containsEntry("chassisNumber", "JF5-1141982");
        assertThat(result.getFields()).containsEntry("grade", "Hybrid Z");
        assertThat(parser.getProcessorId()).isEqualTo("c86ec486ccb7e7d7");
    }

    @Test
    void parsePageMapsChassisAliasesOnce() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<>();
        entities.add(entity("chassis_no", "ABC-123"));
        entities.add(entity("chassis", "SHOULD-SKIP"));
        entities.add(entity("chassis_number", "SHOULD-SKIP-TOO"));

        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult("raw", entities));

        assertThat(result.getFields()).containsEntry("chassisNumber", "ABC-123");
        assertThat(result.getFields()).doesNotContainValue("SHOULD-SKIP");
        assertThat(result.getFields()).doesNotContainValue("SHOULD-SKIP-TOO");
    }

    @Test
    void parsePageDoesNotUseRegexFallback() {
        AuctionParseResult result = parser.parsePage(
                "Chassis Number: JF5-1141982\nGrade: Hybrid Z\n");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFields()).isEmpty();
    }

    private static DocumentAiClient.DocumentAiEntity entity(String type, String value) {
        return new DocumentAiClient.DocumentAiEntity(type, value, 0.99);
    }
}
