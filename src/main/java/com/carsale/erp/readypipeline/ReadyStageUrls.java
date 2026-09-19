package com.carsale.erp.readypipeline;

import java.util.List;

import com.carsale.erp.readypipeline.SaleListingService.SaleProgress;
import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

public final class ReadyStageUrls {

    private ReadyStageUrls() {
    }

    public static int clampStageIndex(Integer requested, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        int max = Math.max(0, keys.size() - 1);
        if (requested == null) {
            return 0;
        }
        if (requested < 0) {
            return 0;
        }
        if (requested > max) {
            return max;
        }
        return requested;
    }

    public static String redirectAfterListingSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.READY.getCurrentBase(), keys, FlowStage.LISTING.getStageKey());
    }

    public static String redirectAfterRegistrationSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(
                chassisNo, FlowPipeline.READY.getCurrentBase(), keys, FlowStage.REGISTRATION.getStageKey());
    }

    public static NavLinks editLinks(String chassisNo, int stageIndex, SaleProgress status, List<PipelineStage> keys) {
        NavLinks view = viewLinks(chassisNo, stageIndex, status, keys, FlowPipeline.READY);
        String encoded = PipelineStageUtils.encode(chassisNo);
        String prevUrl = null;
        if (stageIndex > 0) {
            String prevKey = PipelineStageService.stageKeyAt(keys, stageIndex - 1);
            if (FlowStage.DETAILS.getStageKey().equals(prevKey) || status.isStageComplete(prevKey)) {
                prevUrl = PipelineStageUtils.viewUrl(FlowPipeline.READY.getCurrentBase() + encoded, prevKey);
            } else {
                prevUrl = PipelineStageService.editUrlFor(encoded, prevKey);
            }
        }
        return new NavLinks(view.getStageIndex(), view.getStageLabel(), prevUrl,
                view.getNextUrl(), view.getNextLabel(), null);
    }
}
