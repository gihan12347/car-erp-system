package com.carsale.erp.preparationpipeline;

import java.util.List;

import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;
import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

public final class PreparationStageUrls {

    private PreparationStageUrls() {
    }

    public static int clampStageIndex(Integer requested, PreparationProgress status, List<String> keys) {
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

    public static int resolveStartStageIndex(PreparationProgress status, List<String> keys) {
        if (status.isPipelineCompleted() || keys == null || keys.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (!isStageComplete(keys.get(i), status)) {
                return i;
            }
        }
        return 0;
    }

    public static String redirectAfterInspectionSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.PREP.getCurrentBase(), keys, FlowStage.INSPECTION.getStageKey());
    }

    public static String redirectAfterWorkshopSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.PREP.getCurrentBase(), keys, FlowStage.WORKSHOP.getStageKey());
    }

    public static String redirectAfterYardSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.PREP.getCurrentBase(), keys, FlowStage.YARD.getStageKey());
    }

    public static String redirectAfterSaleSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.PREP.getCurrentBase(), keys, FlowStage.SALE.getStageKey());
    }

    public static NavLinks editLinks(String chassisNo, int stageIndex, PreparationProgress status, List<PipelineStage> keys) {
        NavLinks view = viewLinks(chassisNo, stageIndex, status, keys, FlowPipeline.PREP);
        String encoded = PipelineStageUtils.encode(chassisNo);
        String prevUrl = null;
        if (stageIndex > 0) {
            String prevKey = PipelineStageService.stageKeyAt(keys, stageIndex - 1);
            prevUrl = editUrlFor(encoded, prevKey);
        }
        return new NavLinks(view.getStageIndex(), view.getStageLabel(), prevUrl,
                view.getNextUrl(), view.getNextLabel(), null);
    }

    public static String editUrlFor(String encodedChassis, String stageKey) {
        return PipelineStageService.editUrlFor(encodedChassis, stageKey);
    }

    public static boolean isStageComplete(String stageKey, PreparationProgress status) {
        return status.isStageComplete(stageKey);
    }

    public static String encode(String chassisNo) {
        return PipelineStageUtils.encode(chassisNo);
    }
}
