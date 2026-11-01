package com.carsale.erp.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class StandardReportRangeTest {

    @Test
    void includesEveryRowWhenNoPeriodIsSet() {
        assertTrue(StandardReportService.inRange("2026-01-15", "", ""));
        assertTrue(StandardReportService.inRange(null, "", ""));
    }

    @Test
    void keepsRowsWithoutADateDuringAPeriod() {
        assertTrue(StandardReportService.inRange(null, "2026-01-01", "2026-01-31"));
        assertTrue(StandardReportService.inRange("—", "2026-01-01", "2026-01-31"));
    }

    @Test
    void filtersDatesOutsideThePeriod() {
        assertTrue(StandardReportService.inRange("2026-01-15", "2026-01-01", "2026-01-31"));
        assertFalse(StandardReportService.inRange("2025-12-31", "2026-01-01", "2026-01-31"));
        assertFalse(StandardReportService.inRange("2026-02-01", "2026-01-01", "2026-01-31"));
    }

    @Test
    void stockPeriodDropsVehiclesWithoutAStockDate() {
        assertTrue(StandardReportService.inStockPeriod("2026-10-03", "", ""));
        assertTrue(StandardReportService.inStockPeriod(null, "", ""));
        assertTrue(StandardReportService.inStockPeriod("2026-10-03", "2026-10-01", "2026-10-31"));
        assertFalse(StandardReportService.inStockPeriod("2026-09-26", "2026-10-01", "2026-10-31"));
        assertFalse(StandardReportService.inStockPeriod(null, "2026-10-01", "2026-10-31"));
    }

    @Test
    void countsDaysFromTheListingDateBeforeYardArrival() {
        String listed = LocalDate.now().minusDays(61).toString();
        String arrived = LocalDate.now().minusDays(91).toString();
        assertEquals(Integer.valueOf(61), StandardReportService.daysInStock(listed, arrived));
        assertEquals(Integer.valueOf(91), StandardReportService.daysInStock(null, arrived));
        assertEquals(Integer.valueOf(60), StandardReportService.daysInStock(LocalDate.now().minusDays(60).toString(), null));
    }
}
