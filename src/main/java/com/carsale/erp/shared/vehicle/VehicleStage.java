package com.carsale.erp.shared.vehicle;

public enum VehicleStage {
    PURCHASED("Purchased"),
    SHIPPING("Shipping"),
    CUSTOMS("Customs"),
    WORKSHOP("Workshop"),
    READY("Sale"),
    RESERVED("Reserved"),
    SOLD("Sold");

    private final String displayName;

    VehicleStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
