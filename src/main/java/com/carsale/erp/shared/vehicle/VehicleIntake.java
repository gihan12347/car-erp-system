package com.carsale.erp.shared.vehicle;

/**
 * How a vehicle entered stock. Registered local purchases skip import and customs
 * and start in the preparation pipeline. Every other intake uses the import pipeline.
 */
public enum VehicleIntake {
    COMPANY_IMPORT("company"),
    PERSONAL_IMPORT("personal"),
    LOCAL_UNREGISTERED("local-unregistered"),
    LOCAL_REGISTERED("local-registered");

    private final String key;

    VehicleIntake(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public boolean startsInPreparation() {
        return this == LOCAL_REGISTERED;
    }
}
