package com.carsale.erp.preparationpipeline.yard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.carsale.erp.preparationpipeline.model.YardRecord;
import com.carsale.erp.preparationpipeline.util.Yard;
import com.carsale.erp.preparationpipeline.util.YardAllocation;
import com.carsale.erp.shared.vehicle.Vehicle;
import org.junit.jupiter.api.Test;

class YardAllocationTest {

    @Test
    void blankOrUnknownBayGoesToUnassignedLane() {
        Set<String> yards = new HashSet<String>(Arrays.asList("YD-01", "YD-02"));
        YardRecord blank = new YardRecord();
        blank.setBayNo("  ");
        YardRecord unknown = new YardRecord();
        unknown.setBayNo("YD-99");
        YardRecord assigned = new YardRecord();
        assigned.setBayNo("yd-01");

        assertThat(YardAllocation.laneKey(blank, yards)).isEqualTo(YardAllocation.UNASSIGNED_KEY);
        assertThat(YardAllocation.isUnassigned(unknown, yards)).isTrue();
        assertThat(YardAllocation.laneKey(assigned, yards)).isEqualTo("YD-01");
        assertThat(YardAllocation.isUnassigned(assigned, yards)).isFalse();
    }

    @Test
    void makeModelPrefersCombinedNameWithoutDuplicatingMake() {
        Vehicle raize = new Vehicle();
        raize.setChassisNo("A202A-0082833");
        raize.setMake("Toyota");
        raize.setModel("Toyota Raize");
        assertThat(Yard.makeModel(raize)).isEqualTo("Toyota Raize");

        Vehicle aqua = new Vehicle();
        aqua.setChassisNo("NHP10");
        aqua.setMake("Toyota");
        aqua.setModel("Aqua");
        assertThat(Yard.makeModel(aqua)).isEqualTo("Toyota Aqua");
    }

    @Test
    void requireSpaceAllowsWhenCapacityIsEnough() {
        YardAllocation.requireSpace(3, 3);
        YardAllocation.requireSpace(2, 0);
    }

    @Test
    void requireSpaceRejectsWhenSelectionExceedsRemaining() {
        try {
            YardAllocation.requireSpace(2, 3);
            throw new AssertionError("expected capacity error");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).contains("2 spaces left");
            assertThat(ex.getMessage()).contains("3");
        }
    }
}
