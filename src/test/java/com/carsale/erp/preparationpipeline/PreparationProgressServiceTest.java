package com.carsale.erp.preparationpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;

class PreparationProgressServiceTest {

    @Test
    void prepIsCompleteWhenInspectionWorkshopYardAndSaleAreDone() {
        PreparationProgress pending = new PreparationProgress(false, false, false, false, false);
        PreparationProgress workshopOnly = new PreparationProgress(true, false, false, true, false);
        PreparationProgress yardOnly = new PreparationProgress(false, true, false, false, false);
        PreparationProgress inspectionOnly = new PreparationProgress(false, false, true, false, false);
        PreparationProgress workshopAndYard = new PreparationProgress(true, true, false, true, false);
        PreparationProgress withoutSale = new PreparationProgress(true, true, true, true, false);
        PreparationProgress complete = new PreparationProgress(true, true, true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isPipelineCompleted()).isFalse();
        assertThat(workshopOnly.completedCount()).isEqualTo(1);
        assertThat(workshopOnly.isPipelineCompleted()).isFalse();
        assertThat(yardOnly.completedCount()).isEqualTo(1);
        assertThat(inspectionOnly.completedCount()).isEqualTo(1);
        assertThat(workshopAndYard.completedCount()).isEqualTo(2);
        assertThat(workshopAndYard.isPipelineCompleted()).isFalse();
        assertThat(withoutSale.completedCount()).isEqualTo(3);
        assertThat(withoutSale.isPipelineCompleted()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(4);
        assertThat(complete.isPipelineCompleted()).isTrue();
        assertThat(workshopOnly.isCanEnterYard()).isTrue();
        assertThat(pending.isCanEnterYard()).isFalse();
        assertThat(pending.firstIncompleteStageKey(java.util.Arrays.asList("inspection", "workshop", "yard", "sale")))
                .isEqualTo("inspection");
        assertThat(withoutSale.firstIncompleteStageKey(java.util.Arrays.asList("inspection", "workshop", "yard", "sale")))
                .isEqualTo("sale");
        assertThat(complete.firstIncompleteStageKey(java.util.Arrays.asList("inspection", "workshop", "yard", "sale")))
                .isNull();
    }
}
