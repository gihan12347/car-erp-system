package com.carsale.erp.importpipeline.exportcert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.shared.document.document.ExportCertificateParser;
import com.carsale.erp.shared.ocr.DocumentAiClient;
import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

class ExportCertificateParserTest {

    private final ExportCertificateParser parser = new ExportCertificateParser("322628bd2494c32d");

    @Test
    void parsePageMapsDocumentAiEntitiesAndNormalizesDates() {
        List<DocumentAiClient.DocumentAiEntity> entities = new ArrayList<>();
        entities.add(entity("date", "2025-02-27T00:00:00"));
        entities.add(entity("export_scheduled_day", "26/08/2025"));
        entities.add(entity("first_registration_date", "01/2025"));
        entities.add(entity("registration_date", "2025-02-27"));
        entities.add(entity("director_general_land_transport_branch", "Kanto District Transport Bureau"));
        entities.add(entity("motor_vehicle_registration_no", "Toyama 583 ro 9037"));
        entities.add(entity("chassis_no", "JF5-1141982"));
        entities.add(entity("classified_no", "0002"));
        entities.add(entity("specification_no", "20749"));
        entities.add(entity("type_of_vehicle", "Light Vehicle"));
        entities.add(entity("form_of_vehicle", "Station Wagon"));
        entities.add(entity("usage", "Passenger"));
        entities.add(entity("private_or_commercial", "Private"));
        entities.add(entity("type_of_fuel", "Petrol"));
        entities.add(entity("displacement", "0.65 L"));
        entities.add(entity("seating_capacity", "4"));
        entities.add(entity("max_carrying_capacity", "-"));
        entities.add(entity("weight_of_vehicle", "920kg"));
        entities.add(entity("gross_weight_of_vehicle", "1140kg"));
        entities.add(entity("length", "339cm"));
        entities.add(entity("width", "147cm"));
        entities.add(entity("height", "179cm"));
        entities.add(entity("weight_of_front_front_axel", "550kg"));
        entities.add(entity("weight_of_front_rear_axel", "-"));
        entities.add(entity("weight_of_rear_front_axel", "-"));
        entities.add(entity("weight_of_rear_rear_axel", "370kg"));
        entities.add(entity("name_of_owner", "Same as user"));
        entities.add(entity("address_of_owner", "Same as user address"));
        entities.add(entity("name_of_user", "M.D.K Corporation Ltd."));
        entities.add(entity("address_of_user", "678-1 Tsubatae, Imizu City, Toyama Prefecture"));
        entities.add(entity("parking_place", "Same as user address"));

        AuctionParseResult result = parser.parsePage(
                new DocumentAiClient.DocumentAiResult("raw export certificate text", entities));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields()).containsEntry("issueDate", "2025-02-27");
        assertThat(result.getFields()).containsEntry("exportScheduledDate", "2025-08-26");
        assertThat(result.getFields()).containsEntry("firstRegDate", "2025-01");
        assertThat(result.getFields()).containsEntry("registrationDate", "2025-02-27");
        assertThat(result.getFields()).containsEntry("directorGeneralLandTransportBranch", "Kanto District Transport Bureau");
        assertThat(result.getFields()).containsEntry("registrationNo", "Toyama 583 ro 9037");
        assertThat(result.getFields()).containsEntry("chassisVin", "JF5-1141982");
        assertThat(result.getFields()).containsEntry("classificationNo", "0002");
        assertThat(result.getFields()).containsEntry("specificationNo", "20749");
        assertThat(result.getFields()).containsEntry("vehicleClassification", "Light Vehicle");
        assertThat(result.getFields()).containsEntry("bodyType", "Station Wagon");
        assertThat(result.getFields()).containsEntry("useType", "Passenger");
        assertThat(result.getFields()).containsEntry("purpose", "Private");
        assertThat(result.getFields()).containsEntry("fuelType", "Petrol");
        assertThat(result.getFields()).containsEntry("engineCapacity", "0.65 L");
        assertThat(result.getFields()).containsEntry("seatingCapacity", "4");
        assertThat(result.getFields()).containsEntry("weightKg", "920kg");
        assertThat(result.getFields()).containsEntry("grossWeightKg", "1140kg");
        assertThat(result.getFields()).containsEntry("lengthCm", "339cm");
        assertThat(result.getFields()).containsEntry("widthCm", "147cm");
        assertThat(result.getFields()).containsEntry("heightCm", "179cm");
        assertThat(result.getFields()).containsEntry("frontAxleWeight", "550kg");
        assertThat(result.getFields()).containsEntry("rearAxleWeight", "370kg");
        assertThat(result.getFields()).containsEntry("ownerName", "Same as user");
        assertThat(result.getFields()).containsEntry("userName", "M.D.K Corporation Ltd");
        assertThat(result.getFields()).containsEntry("localityOfUse", "Same as user address");
        assertThat(parser.getProcessorId()).isEqualTo("322628bd2494c32d");
    }

    @Test
    void parsePageDoesNotUseJapaneseOcrMapping() {
        String text = "輸出予定届出証明書\n車台番号 JF5-1141982\n整理番号 5051215834190374\n";

        AuctionParseResult result = parser.parsePage(text);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFields()).doesNotContainKey("chassisVin");
        assertThat(result.getFields()).doesNotContainKey("documentType");
    }

    @Test
    void toIsoDateAndMonthNormalizeCommonFormats() {
        assertThat(ExportCertificateParser.toIsoDate("2025-02-27T00:00:00")).isEqualTo("2025-02-27");
        assertThat(ExportCertificateParser.toIsoDate("27/02/2025")).isEqualTo("2025-02-27");
        assertThat(ExportCertificateParser.toIsoMonth("01/2025")).isEqualTo("2025-01");
        assertThat(ExportCertificateParser.toIsoMonth("2025-01-15")).isEqualTo("2025-01");
    }

    private static DocumentAiClient.DocumentAiEntity entity(String type, String value) {
        return new DocumentAiClient.DocumentAiEntity(type, value, 0.99);
    }
}
