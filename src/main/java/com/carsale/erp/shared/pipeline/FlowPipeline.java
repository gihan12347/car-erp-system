package com.carsale.erp.shared.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FlowPipeline {
    IMPORT("IMPORT", "Import pipeline", "Auction, pre-shipment, equipment, JEVIC, inspection, standards, export, grade, photos, and working sheet", "fa-route", 0, "/auction/"),
    CUSTOMS("CUSTOMS", "Customs clearance pipeline", "Bill of lading, CUSDEC, and assessment notice", "fa-passport", 1, "/customs/"),
    PREP("PREP", "Preparation pipeline", "Inspection, workshop, and yard", "fa-wrench", 2, "/workshop-yard/"),
    READY("READY", "Sale", "Unregistered, registered, and sold vehicles", "fa-tags", 3, "/ready-for-sale/");

    private final String flowKey;
    private final String title;
    private final String description;
    private final String icon;
    private final int sortOrder;
    private final String currentBase;

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
