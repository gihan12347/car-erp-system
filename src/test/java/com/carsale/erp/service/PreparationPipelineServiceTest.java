package com.carsale.erp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.service.PreparationPipelineService.PrepStatus;

class PreparationPipelineServiceTest {

    @Test
    void prepIsCompleteWhenInspectionWorkshopAndYardAreDone() {
        PrepStatus pending = new PrepStatus(false, false, false, false);
        PrepStatus workshopOnly = new PrepStatus(true, false, false, true);
        PrepStatus yardOnly = new PrepStatus(false, true, false, false);
        PrepStatus inspectionOnly = new PrepStatus(false, false, true, false);
        PrepStatus workshopAndYard = new PrepStatus(true, true, false, true);
        PrepStatus complete = new PrepStatus(true, true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isPrepComplete()).isFalse();
        assertThat(workshopOnly.completedCount()).isEqualTo(1);
        assertThat(workshopOnly.isPrepComplete()).isFalse();
        assertThat(yardOnly.completedCount()).isEqualTo(1);
        assertThat(inspectionOnly.completedCount()).isEqualTo(1);
        assertThat(workshopAndYard.completedCount()).isEqualTo(2);
        assertThat(workshopAndYard.isPrepComplete()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(3);
        assertThat(complete.isPrepComplete()).isTrue();
        assertThat(workshopOnly.isCanEnterYard()).isTrue();
        assertThat(pending.isCanEnterYard()).isFalse();
    }
}
