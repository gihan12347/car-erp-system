package com.carsale.erp.shared.vehicle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VehicleFieldNormalizerTest {

    @Test
    void transmissionCodesBecomeAutomaticOrManual() {
        assertThat(VehicleFieldNormalizer.displayTransmission("F.AT")).isEqualTo("Automatic");
        assertThat(VehicleFieldNormalizer.displayTransmission("AT")).isEqualTo("Automatic");
        assertThat(VehicleFieldNormalizer.displayTransmission("IAT")).isEqualTo("Automatic");
        assertThat(VehicleFieldNormalizer.displayTransmission("CVT")).isEqualTo("Automatic");
        assertThat(VehicleFieldNormalizer.displayTransmission("MT")).isEqualTo("Manual");
        assertThat(VehicleFieldNormalizer.displayTransmission("automatic")).isEqualTo("Automatic");
        assertThat(VehicleFieldNormalizer.displayTransmission("manual")).isEqualTo("Manual");
        assertThat(VehicleFieldNormalizer.displayTransmission(null)).isNull();
    }

    @Test
    void normalizeStoresAutomaticOrManualOnTheVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setTransmission("F.AT");
        VehicleFieldNormalizer.normalize(vehicle);
        assertThat(vehicle.getTransmission()).isEqualTo("Automatic");
        assertThat(vehicle.getTransmissionLabel()).isEqualTo("Automatic");
    }
}
