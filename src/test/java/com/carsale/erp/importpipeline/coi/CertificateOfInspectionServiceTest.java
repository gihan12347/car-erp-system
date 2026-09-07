package com.carsale.erp.importpipeline.coi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

class CertificateOfInspectionServiceTest {

    @Test
    void remapParseResultMapsCertificateOfInspectionFields() {
        AuctionParseResult parsed = new AuctionParseResult();
        parsed.setSuccess(true);
        parsed.setRawText("raw");
        parsed.put("jevicCertificateNo", "LK1-A000705");
        parsed.put("jevicIssueDate", "07/03/2025");
        parsed.put("jevicLocation", "East Japan Area");
        parsed.put("jevicMake", "HONDA");
        parsed.put("jevicModel", "6BA-JF5");
        parsed.put("jevicEngineCapacity", "650");
        parsed.put("jevicFirstRegistration", "202501");
        parsed.put("jevicChassisVin", "JF5-1141982");
        parsed.put("jevicEngineNo", "S07B-6236116");
        parsed.put("jevicCurrentOdometer", "5 km");
        parsed.put("jevicInspectionDate", "05/03/2025");
        parsed.put("jevicRemarks", "Year of Manufacture:2024");

        CertificateOfInspectionService service = new CertificateOfInspectionService(null, null, null);
        AuctionParseResult remapped = service.remapParseResult(parsed);

        assertThat(remapped.isSuccess()).isTrue();
        assertThat(remapped.getRawText()).isEqualTo("raw");
        assertThat(remapped.getFields()).containsEntry("certificateNo", "LK1-A000705");
        assertThat(remapped.getFields()).containsEntry("issueDate", "07/03/2025");
        assertThat(remapped.getFields()).containsEntry("inspectionBranch", "East Japan Area");
        assertThat(remapped.getFields()).containsEntry("make", "HONDA");
        assertThat(remapped.getFields()).containsEntry("model", "6BA-JF5");
        assertThat(remapped.getFields()).containsEntry("engineCapacity", "650");
        assertThat(remapped.getFields()).containsEntry("firstRegistration", "202501");
        assertThat(remapped.getFields()).containsEntry("chassisVin", "JF5-1141982");
        assertThat(remapped.getFields()).containsEntry("engineNo", "S07B-6236116");
        assertThat(remapped.getFields()).containsEntry("inspectedMileage", "5 km");
        assertThat(remapped.getFields()).containsEntry("inspectionDate", "05/03/2025");
        assertThat(remapped.getFields()).containsEntry("remarks", "Year of Manufacture:2024");
    }
}
