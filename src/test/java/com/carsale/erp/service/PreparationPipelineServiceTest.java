package com.carsale.erp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.service.PreparationPipelineService.PrepStatus;

class PreparationPipelineServiceTest {

    @Test
    void prepIsCompleteOnlyWhenWorkshopAndYardAreDone() {
        PrepStatus pending = new PrepStatus(false, false);
        PrepStatus workshopOnly = new PrepStatus(true, false);
        PrepStatus yardOnly = new PrepStatus(false, true);
        PrepStatus complete = new PrepStatus(true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isPrepComplete()).isFalse();
        assertThat(workshopOnly.completedCount()).isEqualTo(1);
        assertThat(workshopOnly.isPrepComplete()).isFalse();
        assertThat(yardOnly.completedCount()).isEqualTo(1);
        assertThat(complete.completedCount()).isEqualTo(2);
        assertThat(complete.isPrepComplete()).isTrue();
    }
}
