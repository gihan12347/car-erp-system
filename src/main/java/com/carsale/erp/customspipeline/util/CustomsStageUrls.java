package com.carsale.erp.customspipeline.util;

import java.util.List;

import com.carsale.erp.customspipeline.service.CustomsProgressService.CustomsProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.utils.PipelineStageUtils;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

public final class CustomsStageUrls {

    public static String redirectAfterBillOfLadingSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.CUSTOMS.getCurrentBase(), keys, FlowStage.BILL_OF_LADING.getStageKey());
    }

    public static String redirectAfterDeclarationSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.CUSTOMS.getCurrentBase(), keys, FlowStage.DECLARATION.getStageKey());
    }

    public static String redirectAfterAssessmentSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.CUSTOMS.getCurrentBase(), keys, FlowStage.ASSESSMENT.getStageKey());
    }

    public static String redirectAfterWorksheetSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.CUSTOMS.getCurrentBase(), keys, FlowStage.WORKSHEET.getStageKey());
    }

    public static NavLinks editLinks(String chassisNo, int stageIndex, CustomsProgress status, List<PipelineStage> keys) {
        NavLinks view = viewLinks(chassisNo, stageIndex, status, keys, FlowPipeline.CUSTOMS);
        return new NavLinks(view.getStageIndex(), view.getStageLabel(), view.getPrevUrl(),
                view.getNextUrl(), view.getNextLabel(), null);
    }

    public static String editUrlFor(String encodedChassis, String stageKey) {
        return PipelineStageService.editUrlFor(encodedChassis, stageKey);
    }
}
