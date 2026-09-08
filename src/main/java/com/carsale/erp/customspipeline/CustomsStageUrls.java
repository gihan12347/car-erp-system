package com.carsale.erp.customspipeline;

import java.util.List;

import com.carsale.erp.customspipeline.CustomsProgressService.CustomsProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.utils.PipelineStageUtils;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

public final class CustomsStageUrls {

    private CustomsStageUrls() {
    }

    public static int clampStageIndex(Integer requested, CustomsProgress status, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        int max = Math.max(0, keys.size() - 1);
        if (requested == null) {
            return resolveStartStageIndex(status, keys);
        }
        if (requested < 0) {
            return 0;
        }
        if (requested > max) {
            return max;
        }
        return requested;
    }

    public static int resolveStartStageIndex(CustomsProgress status, List<String> keys) {
        if (status.isClearanceComplete() || keys == null || keys.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (isIncomplete(keys.get(i), status)) {
                return i;
            }
        }
        return 0;
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

    public static boolean isIncomplete(String stageKey, CustomsProgress status) {
        return !status.isStageComplete(stageKey);
    }

    public static String encode(String chassisNo) {
        return PipelineStageUtils.encode(chassisNo);
    }
}
