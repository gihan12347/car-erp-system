package com.carsale.erp.preparationpipeline;

import java.util.List;

import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.pipeline.FlowPipeline;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;

import static com.carsale.erp.shared.pipeline.PipelineStageService.editUrlOrFallback;

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

    public static String redirectAfterWorkshopSave(String chassisNo, PreparationProgress status, List<String> keys) {
        return redirectAfterStage(chassisNo, status, keys, FlowStage.WORKSHOP.getStageKey());
    }

    public static String redirectAfterYardSave(String chassisNo, PreparationProgress status, List<String> keys) {
        return redirectAfterStage(chassisNo, status, keys, FlowStage.YARD.getStageKey());
    }

    public static String redirectAfterInspectionSave(String chassisNo, PreparationProgress status, List<String> keys) {
        return redirectAfterStage(chassisNo, status, keys, FlowStage.INSPECTION.getStageKey());
    }

    private static String redirectAfterStage(
            String chassisNo,
            PreparationProgress status,
            List<String> keys,
            String currentKey
    ) {
        String encoded = encode(chassisNo);
        String viewBase = FlowPipeline.PREP.getCurrentBase() + encoded;
        int currentIndex = keys.indexOf(currentKey);

        if (status.isPipelineCompleted()) {
            return completedRedirect(viewBase, encoded, currentIndex, keys.size());
        }
        if (currentIndex >= 0 && currentIndex < keys.size() - 1) {
            return nextStageRedirect(encoded, viewBase, keys.get(currentIndex + 1), currentIndex + 1, status);
        }
        return firstIncompleteRedirect(encoded, viewBase, status, keys);
    }

    private static String completedRedirect(String viewBase, String encoded, int currentIndex, int size) {
        if (currentIndex >= 0 && currentIndex < size - 1) {
            return viewUrl(viewBase, currentIndex + 1);
        }
        return FlowPipeline.READY.getCurrentBase() + encoded;
    }

    private static String nextStageRedirect(
            String encoded,
            String viewBase,
            String nextKey,
            int nextIndex,
            PreparationProgress status
    ) {
        if (FlowStage.YARD.getStageKey().equals(nextKey) && !status.isCanEnterYard()) {
            return editUrlOrFallback(encoded, FlowStage.WORKSHOP.getStageKey(), viewBase);
        }
        if (status.isStageComplete(nextKey)) {
            return viewUrl(viewBase, nextIndex);
        }
        return editUrlOrFallback(encoded, nextKey, viewBase);
    }

    private static String firstIncompleteRedirect(
            String encoded,
            String viewBase,
            PreparationProgress status,
            List<String> keys
    ) {
        String incomplete = status.firstIncompleteStageKey(keys);
        return incomplete != null ? editUrlOrFallback(encoded, incomplete, viewBase) : viewBase;
    }

    private static String viewUrl(String viewBase, int stageIndex) {
        return viewBase + "?stage=" + stageIndex;
    }

    public static boolean isStageComplete(String stageKey, PreparationProgress status) {
        return status.isStageComplete(stageKey);
    }

    public static String encode(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, java.nio.charset.StandardCharsets.UTF_8);
    }
}
