package com.carsale.erp.preparationpipeline.model;

import lombok.Getter;
import lombok.Setter;

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

@Setter
@Getter
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
