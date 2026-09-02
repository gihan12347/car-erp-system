package com.carsale.erp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InspectionItemServiceTest {

    @Test
    void slugNormalizesTitle() {
        assertThat(InspectionItemService.slug("Air conditioning")).isEqualTo("air-conditioning");
        assertThat(InspectionItemService.slug("  Body / exterior  ")).isEqualTo("body-exterior");
        assertThat(InspectionItemService.slug("!!!")).isEqualTo("item");
    }
}
