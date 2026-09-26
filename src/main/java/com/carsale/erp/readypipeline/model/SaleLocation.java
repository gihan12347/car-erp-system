package com.carsale.erp.readypipeline.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "sale_locations")
public class SaleLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_code", nullable = false, length = 40, unique = true)
    private String saleCode;

    @Column(name = "sale_name", length = 80)
    private String saleName;

    @Column(nullable = false)
    private int capacity = 1;

    @Column(name = "location_name", nullable = false, length = 80)
    private String location;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Transient
    private int occupied;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSaleCode() {
        return saleCode;
    }

    public void setSaleCode(String saleCode) {
        this.saleCode = saleCode;
    }

    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getOccupied() {
        return occupied;
    }

    public void setOccupied(int occupied) {
        this.occupied = Math.max(0, occupied);
    }

    public int getRemaining() {
        return Math.max(0, capacity - occupied);
    }

    public boolean isFull() {
        return getRemaining() <= 0;
    }

    public boolean hasFreeSlotFor(boolean alreadyAssignedHere) {
        int used = alreadyAssignedHere ? Math.max(0, occupied - 1) : occupied;
        return used < capacity;
    }

    public int getFillPercent() {
        if (capacity <= 0) {
            return occupied > 0 ? 100 : 0;
        }
        int percent = (int) Math.round(occupied * 100.0 / capacity);
        return Math.min(100, Math.max(0, percent));
    }

    public String getFillState() {
        if (isFull()) {
            return "is-full";
        }
        if (getRemaining() <= 3) {
            return "is-low";
        }
        return "";
    }
}
