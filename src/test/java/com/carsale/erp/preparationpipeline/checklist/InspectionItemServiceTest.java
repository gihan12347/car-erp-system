package com.carsale.erp.preparationpipeline.checklist;

import static org.assertj.core.api.Assertions.assertThat;

import com.carsale.erp.preparationpipeline.service.InspectionItemService;
import org.junit.jupiter.api.Test;

class InspectionItemServiceTest {

    @Test
    void slugNormalizesTitle() {
        assertThat(InspectionItemService.slug("Air conditioning")).isEqualTo("air-conditioning");
        assertThat(InspectionItemService.slug("  Body / exterior  ")).isEqualTo("body-exterior");
        assertThat(InspectionItemService.slug("!!!")).isEqualTo("item");
    }
}
