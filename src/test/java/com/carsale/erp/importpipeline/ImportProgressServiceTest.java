package com.carsale.erp.importpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.ImportProgressService.ImportProgress;

class ImportProgressServiceTest {

    @Test
    void importIsCompleteWhenAllEightStagesAreDone() {
        ImportProgress pending = new ImportProgress(false, false, false, false, false, false, false, false);
        ImportProgress auctionOnly = new ImportProgress(true, false, false, false, false, false, false, false);
        ImportProgress twoOfEight = new ImportProgress(true, true, false, false, false, false, false, false);
        ImportProgress threeOfEight = new ImportProgress(true, true, true, false, false, false, false, false);
        ImportProgress fourOfEight = new ImportProgress(true, true, true, true, false, false, false, false);
        ImportProgress fiveOfEight = new ImportProgress(true, true, true, true, true, false, false, false);
        ImportProgress sixOfEight = new ImportProgress(true, true, true, true, true, true, false, false);
        ImportProgress sevenOfEight = new ImportProgress(true, true, true, true, true, true, true, false);
        ImportProgress complete = new ImportProgress(true, true, true, true, true, true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isPipelineCompleted()).isFalse();
        assertThat(auctionOnly.completedCount()).isEqualTo(1);
        assertThat(auctionOnly.isPipelineCompleted()).isFalse();
        assertThat(twoOfEight.completedCount()).isEqualTo(2);
        assertThat(twoOfEight.isPipelineCompleted()).isFalse();
        assertThat(threeOfEight.completedCount()).isEqualTo(3);
        assertThat(threeOfEight.isPipelineCompleted()).isFalse();
        assertThat(fourOfEight.completedCount()).isEqualTo(4);
        assertThat(fourOfEight.isPipelineCompleted()).isFalse();
        assertThat(fiveOfEight.completedCount()).isEqualTo(5);
        assertThat(fiveOfEight.isPipelineCompleted()).isFalse();
        assertThat(sixOfEight.completedCount()).isEqualTo(6);
        assertThat(sixOfEight.isPipelineCompleted()).isFalse();
        assertThat(sevenOfEight.completedCount()).isEqualTo(7);
        assertThat(sevenOfEight.isPipelineCompleted()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(8);
        assertThat(complete.isPipelineCompleted()).isTrue();
        assertThat(complete.hasAnyCompletedStage()).isTrue();
        assertThat(sevenOfEight.isStageComplete("auction")).isTrue();
        assertThat(sevenOfEight.isStageComplete("photos")).isFalse();
        assertThat(sevenOfEight.firstIncompleteStageKey(java.util.Arrays.asList(
                "auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "photos"
        ))).isEqualTo("photos");
        assertThat(complete.firstIncompleteStageKey(java.util.Arrays.asList(
                "auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "photos"
        ))).isNull();
    }
}
