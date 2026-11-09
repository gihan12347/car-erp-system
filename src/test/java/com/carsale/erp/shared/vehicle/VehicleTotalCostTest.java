package com.carsale.erp.shared.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.carsale.erp.customspipeline.model.CustomsDocument;
import com.carsale.erp.preparationpipeline.model.WorkshopJob;
import com.carsale.erp.preparationpipeline.model.WorkshopJobLine;

class VehicleTotalCostTest {

    @Test
    void totalIsLandingPlusCusdecPlusRepairPlusAssessment() {
        CustomsDocument customs = new CustomsDocument();
        customs.setLandingCostLkr("1,000.50");
        customs.setValueNcy("200");
        customs.setAssessmentTotalAssessed("50");

        WorkshopJob job = new WorkshopJob();
        WorkshopJobLine line = new WorkshopJobLine();
        line.setJobSummary("Paint");
        line.setActualCost("25");
        job.getLines().add(line);

        VehicleTotalCost cost = VehicleTotalCost.of(customs, job);

        assertEquals(new BigDecimal("1000.50"), cost.getLanding());
        assertEquals(new BigDecimal("200"), cost.getCusdec());
        assertEquals(new BigDecimal("50"), cost.getAssessment());
        assertEquals(new BigDecimal("25"), cost.getRepair());
        assertEquals(new BigDecimal("1275.50"), cost.getTotal());
    }

    @Test
    void repairUsesTheEstimateWhenActualIsMissing() {
        WorkshopJobLine line = new WorkshopJobLine();
        line.setJobSummary("Dent");
        line.setEstimatedCost("40");

        assertEquals(new BigDecimal("40"), VehicleTotalCost.lineCost(line));
    }

    @Test
    void partialPartsStillProduceATotal() {
        CustomsDocument customs = new CustomsDocument();
        customs.setLandingCostLkr("100");

        VehicleTotalCost cost = VehicleTotalCost.of(customs, null);

        assertEquals(new BigDecimal("100"), cost.getTotal());
        assertTrue(cost.isMissingRequired());
        assertNull(VehicleTotalCost.of(null, null).getTotal());
    }
}
