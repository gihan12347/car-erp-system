package com.carsale.erp.entity;

public enum VehicleStage {
    PURCHASED("Purchased"),
    SHIPPING("Shipping"),
    CUSTOMS("Customs"),
    WORKSHOP("Workshop"),
    READY("Ready for sale"),
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
