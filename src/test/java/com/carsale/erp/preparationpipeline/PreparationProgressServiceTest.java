package com.carsale.erp.preparationpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;

class PreparationProgressServiceTest {

    @Test
    void prepIsCompleteWhenInspectionWorkshopAndYardAreDone() {
        PreparationProgress pending = new PreparationProgress(false, false, false, false);
        PreparationProgress workshopOnly = new PreparationProgress(true, false, false, true);
        PreparationProgress yardOnly = new PreparationProgress(false, true, false, false);
        PreparationProgress inspectionOnly = new PreparationProgress(false, false, true, false);
        PreparationProgress workshopAndYard = new PreparationProgress(true, true, false, true);
        PreparationProgress complete = new PreparationProgress(true, true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isPipelineCompleted()).isFalse();
        assertThat(workshopOnly.completedCount()).isEqualTo(1);
        assertThat(workshopOnly.isPipelineCompleted()).isFalse();
        assertThat(yardOnly.completedCount()).isEqualTo(1);
        assertThat(inspectionOnly.completedCount()).isEqualTo(1);
        assertThat(workshopAndYard.completedCount()).isEqualTo(2);
        assertThat(workshopAndYard.isPipelineCompleted()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(3);
        assertThat(complete.isPipelineCompleted()).isTrue();
        assertThat(workshopOnly.isCanEnterYard()).isTrue();
        assertThat(pending.isCanEnterYard()).isFalse();
        assertThat(pending.firstIncompleteStageKey(java.util.Arrays.asList("inspection", "workshop", "yard")))
                .isEqualTo("inspection");
        assertThat(complete.firstIncompleteStageKey(java.util.Arrays.asList("inspection", "workshop", "yard")))
                .isNull();
    }
}
