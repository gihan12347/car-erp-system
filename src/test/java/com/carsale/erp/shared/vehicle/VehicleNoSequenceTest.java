package com.carsale.erp.shared.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class VehicleNoSequenceTest {

    @Test
    void formatsFourDigitVehicleNumbers() {
        assertEquals("V0001", VehicleService.formatVehicleNo(1));
        assertEquals("V0002", VehicleService.formatVehicleNo(2));
        assertEquals("V0010", VehicleService.formatVehicleNo(10));
        assertEquals("V0100", VehicleService.formatVehicleNo(100));
        assertEquals("V1000", VehicleService.formatVehicleNo(1000));
    }

    @Test
    void readsSuffixFromPaddedAndLegacyNumbers() {
        assertEquals(1, VehicleService.parseVehicleNoSuffix("V0001"));
        assertEquals(2, VehicleService.parseVehicleNoSuffix("V0002"));
        assertEquals(1, VehicleService.parseVehicleNoSuffix("V001"));
        assertEquals(12, VehicleService.parseVehicleNoSuffix("V12"));
        assertEquals(0, VehicleService.parseVehicleNoSuffix(null));
        assertEquals(0, VehicleService.parseVehicleNoSuffix("LOT-1"));
    }

    @Test
    void sortsLatestVehicleNumbersFirst() {
        Vehicle older = vehicle("AAA", "V0001");
        Vehicle newer = vehicle("BBB", "V0012");
        Vehicle newest = vehicle("CCC", "V0100");
        List<Vehicle> sorted = VehicleService.newestFirst(Arrays.asList(older, newest, newer));
        assertEquals("V0100", sorted.get(0).getStockNo());
        assertEquals("V0012", sorted.get(1).getStockNo());
        assertEquals("V0001", sorted.get(2).getStockNo());
    }

    private static Vehicle vehicle(String chassisNo, String stockNo) {
        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNo(chassisNo);
        vehicle.setStockNo(stockNo);
        return vehicle;
    }
}
