package com.carsale.erp.shared.pipeline;

public enum StageLabels {
    AUCTION_LOT(0, "1 · Auction lot"),
    PRE_SHIPMENT_CERTIFICATE(1, "2 · Pre-shipment certificate"),
    EQUIPMENT_CONDITION(2, "3 · Equipment condition");

    final String description;
    final int index;
    StageLabels(int index, String description) {
        this.description = description;
        this.index = index;
    }

    public String getDescription() {
        return description;
    }

    public int getIndex() {
        return index;
    }
}
