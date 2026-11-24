package com.carsale.erp.shared.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PipelineStageEnabledProgressTest {

    @AfterEach
    void restoreEnabledLookup() {
        PipelineStageUtils.setStageEnabledLookup(null);
    }

    @Test
    void disabledIncompleteStageIsTreatedAsSkippedWithoutChangingProgress() {
        ImportProgress auctionOnly = new ImportProgress(
                true, false, false, false, false, false, false, false, false, false);
        List<String> keys = Arrays.asList(
                "auction", "preshipment", "equipment", "jevic", "coi", "standards", "export", "grade", "photos", "worksheet");

        assertThat(auctionOnly.isStageComplete("preshipment")).isFalse();
        assertThat(auctionOnly.firstIncompleteStageKey(keys)).isEqualTo("preshipment");
        assertThat(auctionOnly.isPipelineCompleted()).isFalse();

        PipelineStageUtils.setStageEnabledLookup(key -> !"preshipment".equals(key));

        assertThat(auctionOnly.isStageComplete("preshipment")).isFalse();
        assertThat(PipelineStageUtils.isIncomplete("preshipment", auctionOnly)).isFalse();
        assertThat(auctionOnly.firstIncompleteStageKey(keys)).isEqualTo("equipment");
        assertThat(auctionOnly.completedCount()).isEqualTo(1);
        assertThat(auctionOnly.isPipelineCompleted()).isFalse();
    }

    @Test
    void pipelineCompletesWhenRemainingIncompleteStagesAreDisabled() {
        ImportProgress auctionOnly = new ImportProgress(
                true, false, false, false, false, false, false, false, false, false);
        PipelineStageUtils.setStageEnabledLookup(key -> "auction".equals(key));

        assertThat(auctionOnly.isPipelineCompleted()).isTrue();
        assertThat(auctionOnly.firstIncompleteStageKey(PipelineStageUtils.IMPORT_STAGE_KEYS)).isNull();
        assertThat(auctionOnly.isStageFilled("preshipment")).isFalse();
        assertThat(auctionOnly.isStageComplete("preshipment")).isFalse();
    }
}
