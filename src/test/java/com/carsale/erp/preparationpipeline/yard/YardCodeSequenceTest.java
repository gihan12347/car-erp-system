package com.carsale.erp.preparationpipeline.yard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import com.carsale.erp.preparationpipeline.service.YardBayService;
import org.junit.jupiter.api.Test;

class YardCodeSequenceTest {

    @Test
    void startsAtYd01WhenNoYardsExist() {
        assertEquals("YD-01", YardBayService.nextYardCode(Collections.<String>emptyList()));
    }

    @Test
    void incrementsFromHighestYdCode() {
        assertEquals("YD-04", YardBayService.nextYardCode(Arrays.asList("YD-01", "YD-03", "YD-02")));
    }

    @Test
    void ignoresNonYdCodes() {
        assertEquals("YD-02", YardBayService.nextYardCode(Arrays.asList("A-01", "YD-01", "MAIN")));
    }

    @Test
    void padsSingleDigits() {
        assertEquals("YD-02", YardBayService.formatYardCode(2));
        assertEquals("YD-10", YardBayService.formatYardCode(10));
        assertEquals("YD-100", YardBayService.formatYardCode(100));
    }
}
