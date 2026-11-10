package com.carsale.erp.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class DashboardStockPageTest {

    @Test
    void pagesFourVehiclesAtATime() {
        List<String> rows = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(2, DashboardService.stockPageCount(rows.size()));
        assertEquals(Arrays.asList("a", "b", "c", "d"), DashboardService.stockPageSlice(rows, 1));
        assertEquals(Collections.singletonList("e"), DashboardService.stockPageSlice(rows, 2));
    }

    @Test
    void clampsPageOutsideTheRange() {
        List<String> rows = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("a", "b", "c", "d"), DashboardService.stockPageSlice(rows, 0));
        assertEquals(Collections.singletonList("e"), DashboardService.stockPageSlice(rows, 9));
        assertTrue(DashboardService.stockPageSlice(Collections.<String>emptyList(), 3).isEmpty());
        assertEquals(0, DashboardService.stockPageCount(0));
    }

    @Test
    void hidesPagerWhenOnePageFits() {
        assertTrue(DashboardService.stockPageNumbers(1, 1).isEmpty());
        assertEquals(Arrays.asList(1, 2), DashboardService.stockPageNumbers(1, 2));
    }

    @Test
    void stockAddDatePrefersYardArrivalOverTheSaleDate() {
        assertEquals("2026-03-04", DashboardService.stockAddDate("2026-03-04", "2026-11-09"));
        assertEquals("2026-11-09", DashboardService.stockAddDate(null, "2026/11/09"));
        assertEquals("2026-01-15", DashboardService.stockAddDate("—", "15/01/2026"));
        assertNull(DashboardService.stockAddDate("—", null));
    }

    @Test
    void totalCostPeriodUsesTheStockAddDate() {
        String today = LocalDate.now().toString();

        assertTrue(DashboardService.inStockAddPeriod(today, "today"));
        assertTrue(DashboardService.inStockAddPeriod(today, "all"));
        assertTrue(DashboardService.inStockAddPeriod(null, "all"));
        assertFalse(DashboardService.inStockAddPeriod(null, "month"));
        assertFalse(DashboardService.inStockAddPeriod("2001-01-01", "today"));
    }
}
