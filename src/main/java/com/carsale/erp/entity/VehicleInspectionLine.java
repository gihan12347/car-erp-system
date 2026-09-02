package com.carsale.erp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "vehicle_inspection_lines")
public class VehicleInspectionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chassis_no", nullable = false)
    private VehicleInspection inspection;

    @Column(name = "item_key", length = 80)
    private String itemKey;

    @Column(name = "item_title", nullable = false, length = 160)
    private String itemTitle;

    @Column(length = 12)
    private String result;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "catalog_item", nullable = false)
    private boolean catalogItem;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleInspection getInspection() {
        return inspection;
    }

    public void setInspection(VehicleInspection inspection) {
        this.inspection = inspection;
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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isCatalogItem() {
        return catalogItem;
    }

    public void setCatalogItem(boolean catalogItem) {
        this.catalogItem = catalogItem;
    }

    public boolean isBlankCustom() {
        return !catalogItem
                && isBlank(itemTitle)
                && isBlank(result)
                && isBlank(notes);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
