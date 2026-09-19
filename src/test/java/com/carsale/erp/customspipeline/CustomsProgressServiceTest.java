package com.carsale.erp.customspipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.customspipeline.CustomsProgressService.CustomsProgress;

class CustomsProgressServiceTest {

    @Test
    void clearanceIsCompleteWhenBlDeclarationAssessmentAndWorksheetAreDone() {
        CustomsProgress pending = new CustomsProgress(false, false, false, false);
        CustomsProgress blOnly = new CustomsProgress(true, false, false, false);
        CustomsProgress twoOfFour = new CustomsProgress(true, true, false, false);
        CustomsProgress threeOfFour = new CustomsProgress(true, true, true, false);
        CustomsProgress complete = new CustomsProgress(true, true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isClearanceComplete()).isFalse();
        assertThat(blOnly.completedCount()).isEqualTo(1);
        assertThat(blOnly.isClearanceComplete()).isFalse();
        assertThat(twoOfFour.completedCount()).isEqualTo(2);
        assertThat(twoOfFour.isClearanceComplete()).isFalse();
        assertThat(threeOfFour.completedCount()).isEqualTo(3);
        assertThat(threeOfFour.isClearanceComplete()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(4);
        assertThat(complete.isClearanceComplete()).isTrue();
        assertThat(pending.firstIncompleteStageKey(java.util.Arrays.asList("bl", "declaration", "assessment", "worksheet")))
                .isEqualTo("bl");
        assertThat(threeOfFour.firstIncompleteStageKey(java.util.Arrays.asList("bl", "declaration", "assessment", "worksheet")))
                .isEqualTo("worksheet");
        assertThat(complete.firstIncompleteStageKey(java.util.Arrays.asList("bl", "declaration", "assessment", "worksheet")))
                .isNull();
    }
}
