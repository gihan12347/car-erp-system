package com.carsale.erp.preparationpipeline.dto;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.preparationpipeline.model.YardBay;
import com.carsale.erp.preparationpipeline.util.Yard;
import lombok.Getter;

@Getter
public final class YardLane {

    private final YardBay yard;
    private final List<YardVehicleCard> cards;

    public YardLane(YardBay yard, List<YardVehicleCard> cards) {
        this.yard = yard;
        this.cards = cards == null ? new ArrayList<YardVehicleCard>() : cards;
    }

    public boolean isUnassigned() {
        return yard == null;
    }

    public String getCode() {
        return yard == null ? "unassigned" : yard.getBayCode();
    }

    public String getTitle() {
        if (yard == null) {
            return "Unassigned vehicles";
        }
        return Yard.yardTitle(yard);
    }

    public String getLocation() {
        if (yard == null || yard.getLocation() == null || yard.getLocation().trim().isEmpty()) {
            return null;
        }
        return yard.getLocation().trim();
    }

    public int getCount() {
        return cards.size();
    }

    public int getCapacity() {
        return yard == null ? 0 : yard.getCapacity();
    }

    public int getRemaining() {
        return yard == null ? 0 : yard.getRemaining();
    }

    public boolean isFull() {
        return yard != null && yard.isFull();
    }

    public String getFillState() {
        if (yard == null) {
            return cards.isEmpty() ? "is-empty" : "is-open";
        }
        return yard.getFillState();
    }
}
