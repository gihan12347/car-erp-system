package com.carsale.erp.importpipeline.standards;

import static org.assertj.core.api.Assertions.assertThat;

import com.carsale.erp.shared.document.document.StandardsCertificateDoc;
import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

class StandardsCertificateDocParserTest {

    @Test
    void parseExtractsEmissionSafetyAndVehicleFields() {
        String text = "National Environment Act No.47 of 1980\n"
                + "complies with the emission standards specified in\n"
                + "Schedule III                    Schedule V ✓\n"
                + "CO 1.15 g/km    HC N/A\n"
                + "NMHC 0.025 g/km HC+NOx N/A\n"
                + "NOx 0.013 g/km  THC N/A\n"
                + "PM -            CH4 N/A\n"
                + "Smoke N/A\n"
                + "Safety Measures / Standards specified in the Gazette No.2107/45 dated 25.01.2019\n"
                + "[✓] Three point seat belts for driver and front passengers\n"
                + "[✓] Minimum two point seat belts for other passengers\n"
                + "Air Bags: [✓] Driver  [✓] Front Passenger\n"
                + "ABS: [✓]\n"
                + "Make HONDA\n"
                + "Model 6BA-JF5\n"
                + "Chassis Number JF5-1141982\n"
                + "Place of Inspection East Japan Area\n"
                + "Date of Inspection 05 March 2025\n"
                + "Remarks\n";

        AuctionParseResult result = new StandardsCertificateDoc().parsePage(text);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields()).containsEntry("scheduleType", "V");
        assertThat(result.getFields()).containsEntry("emissionCo", "1.15");
        assertThat(result.getFields()).containsEntry("emissionNmhc", "0.025");
        assertThat(result.getFields()).containsEntry("emissionNox", "0.013");
        assertThat(result.getFields()).containsEntry("emissionPm", "-");
        assertThat(result.getFields()).containsEntry("emissionHc", "N/A");
        assertThat(result.getFields()).containsEntry("emissionHcNox", "N/A");
        assertThat(result.getFields()).containsEntry("emissionThc", "N/A");
        assertThat(result.getFields()).containsEntry("emissionCh4", "N/A");
        assertThat(result.getFields()).containsEntry("emissionSmoke", "N/A");
        assertThat(result.getFields()).containsEntry("threePointSeatBelts", "true");
        assertThat(result.getFields()).containsEntry("twoPointSeatBelts", "true");
        assertThat(result.getFields()).containsEntry("driverAirbag", "true");
        assertThat(result.getFields()).containsEntry("passengerAirbag", "true");
        assertThat(result.getFields()).containsEntry("absFitted", "true");
        assertThat(result.getFields()).containsEntry("make", "HONDA");
        assertThat(result.getFields()).containsEntry("model", "6BA-JF5");
        assertThat(result.getFields()).containsEntry("chassisVin", "JF5-1141982");
        assertThat(result.getFields()).containsEntry("placeOfInspection", "East Japan Area");
        assertThat(result.getFields()).containsEntry("inspectionDate", "05 March 2025");
    }
}
