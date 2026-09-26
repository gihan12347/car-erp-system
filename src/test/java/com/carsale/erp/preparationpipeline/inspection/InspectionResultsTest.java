package com.carsale.erp.preparationpipeline.inspection;

import static org.assertj.core.api.Assertions.assertThat;

import com.carsale.erp.preparationpipeline.dto.InspectionResults;
import org.junit.jupiter.api.Test;

class InspectionResultsTest {

    @Test
    void optionsAreOkNoAndNa() {
        assertThat(InspectionResults.options()).containsExactly("OK", "NO", "N/A");
    }

    @Test
    void canonicalizesNoValues() {
        assertThat(InspectionResults.isNo("no")).isTrue();
        assertThat(InspectionResults.isNo("NO")).isTrue();
        assertThat(InspectionResults.isNo("n")).isTrue();
        assertThat(InspectionResults.isNo("OK")).isFalse();
        assertThat(InspectionResults.isNo("N/A")).isFalse();
    }

    @Test
    void jobSummaryUsesItemTitle() {
        assertThat(InspectionResults.jobSummary("Brakes")).isEqualTo("Inspection fail: Brakes");
        assertThat(InspectionResults.jobSummary("  ")).isEqualTo("Inspection fail");
    }

    @Test
    void displayShowsNoInSentenceCase() {
        assertThat(InspectionResults.display("NO")).isEqualTo("No");
        assertThat(InspectionResults.display("ok")).isEqualTo("OK");
        assertThat(InspectionResults.display("na")).isEqualTo("N/A");
    }
}
