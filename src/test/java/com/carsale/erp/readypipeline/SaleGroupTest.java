package com.carsale.erp.readypipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.util.SaleGroup;
import org.junit.jupiter.api.Test;

import com.carsale.erp.readypipeline.controller.ReadyForSaleController.SaleRow;
import com.carsale.erp.readypipeline.service.SaleListingService.SaleProgress;
import com.carsale.erp.shared.vehicle.Vehicle;

class SaleGroupTest {

    @Test
    void groupsVehiclesBySaleAndKeepsEmptyConfiguredSales() {
        SaleLocation showroom = location("SL-01", "Showroom", "Front", 15, 2);
        SaleLocation outdoor = location("SL-02", "Outdoor lot", "Street", 30, 0);

        List<SaleGroup> groups = SaleGroup.group(
                Arrays.asList(showroom, outdoor),
                Arrays.asList(
                        row("SL-01", true, false),
                        row("sl-01", false, true),
                        row(null, false, false)
                )
        );

        assertEquals(3, groups.size());
        assertEquals("SL-01", groups.get(0).getSaleCode());
        assertEquals(2, groups.get(0).getVehicleCount());
        assertEquals(1, groups.get(0).getListedCount());
        assertEquals(1, groups.get(0).getSoldCount());
        assertEquals(2, groups.get(0).getOccupied());
        assertEquals("", groups.get(0).getFillState());
        assertEquals("SL-02", groups.get(1).getSaleCode());
        assertEquals(0, groups.get(1).getVehicleCount());
        assertEquals(SaleGroup.UNASSIGNED_KEY, groups.get(2).getSaleCode());
        assertEquals(1, groups.get(2).getVehicleCount());
        assertTrue(groups.get(2).isUnassigned());
    }

    @Test
    void matchesFiltersBySaleCodeOrUnassigned() {
        SaleRow assigned = row("SL-01", true, false);
        SaleRow blank = row(null, false, false);

        assertTrue(SaleGroup.matches(assigned, null));
        assertTrue(SaleGroup.matches(assigned, "sl-01"));
        assertFalse(SaleGroup.matches(assigned, "SL-02"));
        assertTrue(SaleGroup.matches(blank, SaleGroup.UNASSIGNED_KEY));
        assertFalse(SaleGroup.matches(assigned, SaleGroup.UNASSIGNED_KEY));
    }

    @Test
    void saleRowTabsFollowSoldThenRegistration() {
        SaleRow unregistered = row("SL-01", true, false);
        assertEquals("unregistered", unregistered.getTab());

        SaleRow registered = new SaleRow(
                unregistered.getVehicle(),
                new SaleProgress(true, true, true, false),
                unregistered.getListing()
        );
        assertEquals("registered", registered.getTab());

        SaleRow sold = row("SL-01", false, true);
        assertEquals("sold", sold.getTab());
    }

    @Test
    void emptyRowsStillReturnConfiguredGroups() {
        SaleLocation showroom = location("SL-01", "Showroom", "Front", 15, 0);
        List<SaleGroup> groups = SaleGroup.group(Collections.singletonList(showroom), Collections.emptyList());
        assertEquals(1, groups.size());
        assertEquals(0, groups.get(0).getVehicleCount());
    }

    private static SaleLocation location(String code, String name, String place, int capacity, int occupied) {
        SaleLocation sale = new SaleLocation();
        sale.setSaleCode(code);
        sale.setSaleName(name);
        sale.setLocation(place);
        sale.setCapacity(capacity);
        sale.setOccupied(occupied);
        return sale;
    }

    private static SaleRow row(String saleCode, boolean listed, boolean sold) {
        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNo("CH-" + (saleCode == null ? "NONE" : saleCode));
        SaleListing listing = new SaleListing();
        listing.setSaleCode(saleCode);
        listing.setListed(listed);
        listing.setSold(sold);
        return new SaleRow(vehicle, new SaleProgress(true, listed || sold, false, sold), listing);
    }
}
