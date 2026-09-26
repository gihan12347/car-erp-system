package com.carsale.erp.readypipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import com.carsale.erp.readypipeline.service.SaleLocationService;
import org.junit.jupiter.api.Test;

class SaleCodeSequenceTest {

    @Test
    void startsAtSl01WhenNoSalesExist() {
        assertEquals("SL-01", SaleLocationService.nextSaleCode(Collections.<String>emptyList()));
    }

    @Test
    void incrementsFromHighestSlCode() {
        assertEquals("SL-04", SaleLocationService.nextSaleCode(Arrays.asList("SL-01", "SL-03", "SL-02")));
    }

    @Test
    void padsSingleDigits() {
        assertEquals("SL-02", SaleLocationService.formatSaleCode(2));
        assertEquals("SL-10", SaleLocationService.formatSaleCode(10));
        assertEquals("SL-100", SaleLocationService.formatSaleCode(100));
    }
}
