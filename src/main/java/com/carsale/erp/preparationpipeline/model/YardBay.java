package com.carsale.erp.preparationpipeline.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Getter
@Setter
@Entity
@Table(name = "yard_bays")
public class YardBay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bay_code", nullable = false, length = 40, unique = true)
    private String bayCode;

    @Column(name = "yard_name", length = 80)
    private String yardName;

    @Column(nullable = false)
    private int capacity = 1;

    @Column(name = "section", nullable = false, length = 80)
    private String location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(length = 160)
    private String notes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Transient
    private int occupied;

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
