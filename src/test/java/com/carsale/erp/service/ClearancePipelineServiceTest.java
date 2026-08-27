package com.carsale.erp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.service.ClearancePipelineService.ClearanceStatus;

class ClearancePipelineServiceTest {

    @Test
    void clearanceIsCompleteOnlyWhenAllThreeDocumentsAreDone() {
        ClearanceStatus pending = new ClearanceStatus(false, false, false);
        ClearanceStatus jevicOnly = new ClearanceStatus(true, false, false);
        ClearanceStatus twoOfThree = new ClearanceStatus(true, true, false);
        ClearanceStatus complete = new ClearanceStatus(true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isClearanceComplete()).isFalse();
        assertThat(jevicOnly.completedCount()).isEqualTo(1);
        assertThat(twoOfThree.completedCount()).isEqualTo(2);
        assertThat(twoOfThree.isClearanceComplete()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(3);
        assertThat(complete.isClearanceComplete()).isTrue();
    }
}
