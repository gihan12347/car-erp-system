package com.carsale.erp.preparationpipeline.inspection;

public class InspectionFailRequest {

    private Long lineId;
    private String itemKey;
    private String itemTitle;
    private String notes;
    private boolean catalogItem;

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public String getItemKey() {
        return itemKey;
    }

    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isCatalogItem() {
        return catalogItem;
    }

    public void setCatalogItem(boolean catalogItem) {
        this.catalogItem = catalogItem;
    }
}
