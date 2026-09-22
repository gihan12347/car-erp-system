package com.carsale.erp.importpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.ImportProgressService.ImportProgress;

class ImportProgressServiceTest {

    @Test
    void importIsCompleteWhenAllNineStagesAreDone() {
        ImportProgress pending = new ImportProgress(false, false, false, false, false, false, false, false, false);
        ImportProgress auctionOnly = new ImportProgress(true, false, false, false, false, false, false, false, false);
        ImportProgress twoOfNine = new ImportProgress(true, true, false, false, false, false, false, false, false);
        ImportProgress threeOfNine = new ImportProgress(true, true, true, false, false, false, false, false, false);
        ImportProgress fourOfNine = new ImportProgress(true, true, true, true, false, false, false, false, false);
        ImportProgress fiveOfNine = new ImportProgress(true, true, true, true, true, false, false, false, false);
        ImportProgress sixOfNine = new ImportProgress(true, true, true, true, true, true, false, false, false);
        ImportProgress sevenOfNine = new ImportProgress(true, true, true, true, true, true, true, false, false);
        ImportProgress eightOfNine = new ImportProgress(true, true, true, true, true, true, true, true, false);
        ImportProgress complete = new ImportProgress(true, true, true, true, true, true, true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isPipelineCompleted()).isFalse();
        assertThat(auctionOnly.completedCount()).isEqualTo(1);
        assertThat(auctionOnly.isPipelineCompleted()).isFalse();
        assertThat(twoOfNine.completedCount()).isEqualTo(2);
        assertThat(twoOfNine.isPipelineCompleted()).isFalse();
        assertThat(threeOfNine.completedCount()).isEqualTo(3);
        assertThat(threeOfNine.isPipelineCompleted()).isFalse();
        assertThat(fourOfNine.completedCount()).isEqualTo(4);
        assertThat(fourOfNine.isPipelineCompleted()).isFalse();
        assertThat(fiveOfNine.completedCount()).isEqualTo(5);
        assertThat(fiveOfNine.isPipelineCompleted()).isFalse();
        assertThat(sixOfNine.completedCount()).isEqualTo(6);
        assertThat(sixOfNine.isPipelineCompleted()).isFalse();
        assertThat(sevenOfNine.completedCount()).isEqualTo(7);
        assertThat(sevenOfNine.isPipelineCompleted()).isFalse();
        assertThat(eightOfNine.completedCount()).isEqualTo(8);
        assertThat(eightOfNine.isPipelineCompleted()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(9);
        assertThat(complete.isPipelineCompleted()).isTrue();
        assertThat(complete.hasAnyCompletedStage()).isTrue();
        assertThat(sevenOfNine.isStageComplete("auction")).isTrue();
        assertThat(sevenOfNine.isStageComplete("grade")).isFalse();
        assertThat(sevenOfNine.isStageComplete("photos")).isFalse();
        assertThat(eightOfNine.isStageComplete("photos")).isTrue();
        assertThat(eightOfNine.isStageComplete("grade")).isFalse();
        assertThat(sevenOfNine.firstIncompleteStageKey(java.util.Arrays.asList(
                "auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "grade", "photos"
        ))).isEqualTo("grade");
        assertThat(eightOfNine.firstIncompleteStageKey(java.util.Arrays.asList(
                "auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "grade", "photos"
        ))).isEqualTo("grade");
        assertThat(complete.firstIncompleteStageKey(java.util.Arrays.asList(
                "auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "grade", "photos"
        ))).isNull();
    }
}
