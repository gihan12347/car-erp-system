package com.carsale.erp.readypipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carsale.erp.readypipeline.model.SaleLocation;
import org.junit.jupiter.api.Test;

class SaleLocationCapacityTest {

    @Test
    void remainingDropsWhenVehiclesOccupySlots() {
        SaleLocation sale = new SaleLocation();
        sale.setCapacity(10);
        sale.setOccupied(3);

        assertEquals(7, sale.getRemaining());
        assertEquals(30, sale.getFillPercent());
        assertFalse(sale.isFull());
        assertTrue(sale.hasFreeSlotFor(false));
    }

    @Test
    void remainingReturnsWhenSoldVehicleFreesASlot() {
        SaleLocation sale = new SaleLocation();
        sale.setCapacity(4);
        sale.setOccupied(4);
        assertEquals(0, sale.getRemaining());
        assertTrue(sale.isFull());

        sale.setOccupied(3);
        assertEquals(1, sale.getRemaining());
        assertFalse(sale.isFull());
        assertTrue(sale.hasFreeSlotFor(false));
    }
}
