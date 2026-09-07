package com.carsale.erp.customspipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.customspipeline.CustomsProgressService.CustomsProgress;

class CustomsProgressServiceTest {

    @Test
    void clearanceIsCompleteWhenDeclarationAssessmentAndWorksheetAreDone() {
        CustomsProgress pending = new CustomsProgress(false, false, false);
        CustomsProgress declarationOnly = new CustomsProgress(true, false, false);
        CustomsProgress twoOfThree = new CustomsProgress(true, true, false);
        CustomsProgress complete = new CustomsProgress(true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isClearanceComplete()).isFalse();
        assertThat(declarationOnly.completedCount()).isEqualTo(1);
        assertThat(declarationOnly.isClearanceComplete()).isFalse();
        assertThat(twoOfThree.completedCount()).isEqualTo(2);
        assertThat(twoOfThree.isClearanceComplete()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(3);
        assertThat(complete.isClearanceComplete()).isTrue();
        assertThat(pending.firstIncompleteStageKey(java.util.Arrays.asList("declaration", "assessment", "worksheet")))
                .isEqualTo("declaration");
        assertThat(twoOfThree.firstIncompleteStageKey(java.util.Arrays.asList("declaration", "assessment", "worksheet")))
                .isEqualTo("worksheet");
        assertThat(complete.firstIncompleteStageKey(java.util.Arrays.asList("declaration", "assessment", "worksheet")))
                .isNull();
    }
}
