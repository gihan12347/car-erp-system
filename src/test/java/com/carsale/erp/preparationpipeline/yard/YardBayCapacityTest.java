package com.carsale.erp.preparationpipeline.yard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YardBayCapacityTest {

    @Test
    void remainingDropsWhenVehiclesOccupySlots() {
        YardBay yard = new YardBay();
        yard.setCapacity(10);
        yard.setOccupied(3);

        assertEquals(7, yard.getRemaining());
        assertEquals(30, yard.getFillPercent());
        assertFalse(yard.isFull());
        assertTrue(yard.hasFreeSlotFor(false));
    }

    @Test
    void remainingIsZeroWhenFull() {
        YardBay yard = new YardBay();
        yard.setCapacity(2);
        yard.setOccupied(2);

        assertEquals(0, yard.getRemaining());
        assertTrue(yard.isFull());
        assertFalse(yard.hasFreeSlotFor(false));
        assertTrue(yard.hasFreeSlotFor(true));
    }

    @Test
    void remainingDoesNotGoNegativeWhenOverCapacity() {
        YardBay yard = new YardBay();
        yard.setCapacity(2);
        yard.setOccupied(5);

        assertEquals(0, yard.getRemaining());
        assertEquals(100, yard.getFillPercent());
        assertTrue(yard.isFull());
        assertFalse(yard.hasFreeSlotFor(false));
    }
}
