package com.carsale.erp.shared.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.vehicle.Vehicle;

class IntakePlanServiceTest {

    @Test
    void leavingAPipelineOutSkipsItsStages() {
        assertTrue(IntakePlanService.keysToSkip(false, true, true).containsAll(PipelineStageUtils.IMPORT_STAGE_KEYS));
        assertFalse(IntakePlanService.keysToSkip(false, true, true).contains(FlowStage.BILL_OF_LADING.getStageKey()));
        assertFalse(IntakePlanService.keysToSkip(false, true, true).contains(FlowStage.INSPECTION.getStageKey()));

        assertTrue(IntakePlanService.keysToSkip(true, false, true).contains(FlowStage.DECLARATION.getStageKey()));
        assertFalse(IntakePlanService.keysToSkip(true, false, true).contains(FlowStage.WORKSHEET.getStageKey()));
        assertFalse(IntakePlanService.keysToSkip(true, false, true).contains(FlowStage.AUCTION.getStageKey()));

        assertTrue(IntakePlanService.keysToSkip(true, true, false).containsAll(PipelineStageUtils.PREP_STAGE_KEYS));
        assertTrue(IntakePlanService.keysToSkip(false, false, false).contains(FlowStage.YARD.getStageKey()));
        assertTrue(IntakePlanService.keysToSkip(false, false, false).contains(FlowStage.ASSESSMENT.getStageKey()));
    }

    @Test
    void basicsStageAppearsOnlyWhenImportWasLeftOut() {
        assertEquals("-", IntakePlanService.encode(false, false, false));
        assertEquals("customs,prep", IntakePlanService.encode(false, true, true));

        Vehicle legacy = new Vehicle();
        assertFalse(IntakePlanService.usesBasics(legacy));

        Vehicle skippedImport = new Vehicle();
        skippedImport.setSelectedPipelines("customs");
        assertTrue(IntakePlanService.usesBasics(skippedImport));

        Vehicle withImport = new Vehicle();
        withImport.setSelectedPipelines("import,prep");
        assertFalse(IntakePlanService.usesBasics(withImport));
    }
}
