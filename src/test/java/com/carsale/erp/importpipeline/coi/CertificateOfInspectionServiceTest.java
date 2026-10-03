package com.carsale.erp.importpipeline.coi;

import static org.assertj.core.api.Assertions.assertThat;

import com.carsale.erp.importpipeline.model.InspectionCertificate;
import com.carsale.erp.importpipeline.service.CertificateOfInspectionService;
import org.junit.jupiter.api.Test;

class CertificateOfInspectionServiceTest {

    @Test
    void newBlankUsesAnIsoInspectionDate() {
        CertificateOfInspectionService service = new CertificateOfInspectionService(null, null, null);
        InspectionCertificate record = service.newBlank();

        assertThat(record.getInspectionDate()).matches("\\d{4}/\\d{2}/\\d{2}");
    }
}
