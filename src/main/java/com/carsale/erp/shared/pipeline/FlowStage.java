package com.carsale.erp.shared.pipeline;

public enum FlowStage {
    AUCTION("auction", "/auction/", "Auction lot", "Auction"),
    PRESHIP("preshipment", "/shipping/", "Pre-shipment", "Pre-ship"),
    EQUIPMENT("equipment", "/equipment/", "Equipment condition", "Equip"),
    JEVIC("jevic", "/jevic/", "Odometer certificate", "JEVIC"),
    COI("coi", "/coi/", "Certificate of inspection", "COI"),
    STANDARDS("standards", "/standards/", "Standards certificate", "Standards"),
    EXPORT("export", "/export/", "Export certificate", "Export"),
    GRADE("grade", "/grade/", "Grade search", "Grade"),
    PHOTOS("photos", "/photos/", "Vehicle images", "Images"),
    DECLARATION("declaration", "/declaration/", "Customs declaration", "CUSDEC"),
    BILL_OF_LADING("bl", "/bl/", "Bill of lading", "B/L"),
    ASSESSMENT("assessment", "/assessment/", "Assessment notice", "Assessment"),
    WORKSHEET("worksheet", "/worksheet/", "Working sheet", "Worksheet"),
    WORKSHOP("workshop", "/workshop/", "Workshop", "Workshop"),
    INSPECTION("inspection", "/inspection/", "Inspection", "Inspection"),
    YARD("yard", "/yard/", "Yard", "Yard"),
    SALE("sale", "/sale/", "Sale", "Sale"),
    DETAILS("details", "/ready-for-sale/", "Details", "Details"),
    LISTING("listing", "/listing/", "Listing", "Listing"),
    REGISTRATION("registration", "/registration/", "Registration", "Reg");

    private final String stageKey;
    private final String urlPath;
    private final String title;
    private final String shortTitle;

    FlowStage(String stageKey, String urlPath, String title, String shortTitle) {
        this.stageKey = stageKey;
        this.urlPath = urlPath;
        this.title = title;
        this.shortTitle = shortTitle;
    }

    public String getStageKey() {
        return stageKey;
    }

    public String getTitle() {
        return title;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public String editUrl(String encodedChassis) {
         return urlPath + encodedChassis + "?hub=1";
    }

    public static FlowStage fromKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String key = value.trim();
        for (FlowStage stage : values()) {
            if (stage.stageKey.equalsIgnoreCase(key)) {
                return stage;
            }
        }
        return null;
    }
}
