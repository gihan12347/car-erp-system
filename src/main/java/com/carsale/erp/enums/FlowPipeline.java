package com.carsale.erp.enums;

/**
 * Seed catalog for pipeline_flows. Existing database rows are not overwritten on startup.
 */
public enum FlowPipeline {
    IMPORT("IMPORT", "Import pipeline", "Auction lot, pre-shipment, and equipment condition", "fa-route", 0),
    CUSTOMS("CUSTOMS", "Customs clearance pipeline", "JEVIC, CUSDEC, and assessment notice", "fa-passport", 1),
    PREP("PREP", "Preparation pipeline", "Workshop jobs and yard allocation", "fa-wrench", 2),
    READY("READY", "Ready for sale pipeline", "Price and list the vehicle", "fa-flag-checkered", 3);

    private final String flowKey;
    private final String title;
    private final String description;
    private final String icon;
    private final int sortOrder;

    FlowPipeline(String flowKey, String title, String description, String icon, int sortOrder) {
        this.flowKey = flowKey;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }

    public String getFlowKey() {
        return flowKey;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
