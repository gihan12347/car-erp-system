package com.carsale.erp.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
