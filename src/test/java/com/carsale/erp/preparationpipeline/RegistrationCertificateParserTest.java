package com.carsale.erp.preparationpipeline;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.document.RegistrationCertificateParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import com.carsale.erp.shared.ocr.RegistrationCertificateDocumentAi;
import com.carsale.erp.shared.ocr.dto.DocumentAiEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationCertificateParserTest {

    private final RegistrationCertificateParser parser = new RegistrationCertificateParser("8b79a197a34f84f7");

    @Test
    void readsEnglishLabelsFromCertificateText() {
        String text = ""
                + "CR No.: LOT FO 0642763\n"
                + "Registration No. SP KW-6743\n"
                + "Chassis No ZE3-1803941\n"
                + "Engine No DAA-MF4-2023415\n"
                + "Class of Vehicle MOTOR CAR\n"
                + "Make HONDA\n"
                + "Model GRACE\n"
                + "Colour PEARL WHITE\n"
                + "Fuel Type PETROL\n"
                + "Year of Manufacture 2012\n";
        AuctionParseResult result = parser.parsePage(text);
        assertTrue(result.isSuccess());
        assertEquals("LOT FO 0642763", result.getFields().get("crNo"));
        assertEquals("SP KW-6743", result.getFields().get("registrationNo"));
        assertEquals("ZE3-1803941", result.getFields().get("chassisNo"));
        assertEquals("HONDA", result.getFields().get("make"));
        assertEquals("PEARL WHITE", result.getFields().get("colour"));
        assertEquals("2012", result.getFields().get("yearOfManufacture"));
    }

    @Test
    void documentAiEntitiesOverrideText() {
        DocumentAiClient.DocumentAiResult documentAi = new DocumentAiClient.DocumentAiResult(
                "Make UNKNOWN",
                Arrays.asList(
                        new DocumentAiEntity("make", "HONDA", 0.94d),
                        new DocumentAiEntity("chassis_no", "ZE3-1803941", 0.91d),
                        new DocumentAiEntity("cr_no", "LOT FO 0642763", 0.88d)
                )
        );
        AuctionParseResult result = parser.parsePage(documentAi);
        assertEquals("HONDA", result.getFields().get("make"));
        assertEquals("ZE3-1803941", result.getFields().get("chassisNo"));
        assertEquals("LOT FO 0642763", result.getFields().get("crNo"));
        assertTrue(result.getMessage().contains("Document AI"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void schemaListsCertificateFieldsWithoutDescriptions() {
        Map<String, Object> options = RegistrationCertificateDocumentAi.processOptions();
        assertFalse(options.containsKey("ocrConfig"));
        Map<String, Object> schema = (Map<String, Object>) options.get("schemaOverride");
        List<Map<String, Object>> entityTypes = (List<Map<String, Object>>) schema.get("entityTypes");
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entityTypes.get(0).get("properties");
        assertEquals(36, properties.size());
        assertTrue(properties.stream().anyMatch(property -> "absolute_owner".equals(property.get("name"))));
        assertTrue(properties.stream().anyMatch(property -> "year_of_manufacture".equals(property.get("name"))));
        assertTrue(properties.stream().noneMatch(property -> property.containsKey("description")));
        assertEquals("8b79a197a34f84f7", parser.getProcessorId());
        assertEquals("registration certificate", parser.getDocumentName());
    }
}
