package com.carsale.erp.shared.pipeline;

public enum FlowPipeline {
    IMPORT("IMPORT", "Import pipeline", "Auction, pre-shipment, equipment, JEVIC, inspection, standards, export, grade, and photos", "fa-route", 0, "/auction/"),
    CUSTOMS("CUSTOMS", "Customs clearance pipeline", "Bill of lading, CUSDEC, assessment notice, and working sheet", "fa-passport", 1, "/customs/"),
    PREP("PREP", "Preparation pipeline", "Inspection, workshop, and yard", "fa-wrench", 2, "/workshop-yard/"),
    READY("READY", "Sale", "Unregistered, registered, and sold vehicles", "fa-tags", 3, "/ready-for-sale/");

    private final String flowKey;
    private final String title;
    private final String description;
    private final String icon;
    private final int sortOrder;
    private final String currentBase;

    FlowPipeline(String flowKey, String title, String description,
                 String icon, int sortOrder, String currentBase) {
        this.flowKey = flowKey;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.currentBase = currentBase;
    }

    public String getCurrentBase() {
        return currentBase;
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

    public static FlowPipeline findPipelineBySortOrder(int order) {
        for (FlowPipeline pipeline : FlowPipeline.values()) {
            if (pipeline.sortOrder == order) {
                return pipeline;
            }
        }
        return null;
    }

    public static FlowPipeline findPipelineByBasePath(String currentBase) {
        for (FlowPipeline pipeline : FlowPipeline.values()) {
            if (currentBase.equalsIgnoreCase(pipeline.getCurrentBase())) {
                return pipeline;
            }
        }
        return null;
    }
}
