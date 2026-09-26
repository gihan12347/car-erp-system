package com.carsale.erp.customspipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.customspipeline.service.CustomsProgressService.CustomsProgress;

class CustomsProgressServiceTest {

    @Test
    void clearanceIsCompleteWhenBlDeclarationAssessmentAndWorksheetAreDone() {
        CustomsProgress pending = new CustomsProgress(false, false, false, false);
        CustomsProgress blOnly = new CustomsProgress(true, false, false, false);
        CustomsProgress twoOfFour = new CustomsProgress(true, true, false, false);
        CustomsProgress threeOfFour = new CustomsProgress(true, true, true, false);
        CustomsProgress complete = new CustomsProgress(true, true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isPipelineCompleted()).isFalse();
        assertThat(blOnly.completedCount()).isEqualTo(1);
        assertThat(blOnly.isPipelineCompleted()).isFalse();
        assertThat(twoOfFour.completedCount()).isEqualTo(2);
        assertThat(twoOfFour.isPipelineCompleted()).isFalse();
        assertThat(threeOfFour.completedCount()).isEqualTo(3);
        assertThat(threeOfFour.isPipelineCompleted()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(4);
        assertThat(complete.isPipelineCompleted()).isTrue();
        assertThat(pending.firstIncompleteStageKey(java.util.Arrays.asList("bl", "declaration", "assessment", "worksheet")))
                .isEqualTo("bl");
        assertThat(threeOfFour.firstIncompleteStageKey(java.util.Arrays.asList("bl", "declaration", "assessment", "worksheet")))
                .isEqualTo("worksheet");
        assertThat(complete.firstIncompleteStageKey(java.util.Arrays.asList("bl", "declaration", "assessment", "worksheet")))
                .isNull();
    }
}
