package com.carsale.erp.preparationpipeline;

import java.util.List;

import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;

public final class PreparationStageUrls {

    private PreparationStageUrls() {
    }

    public static int clampStageIndex(Integer requested, PreparationProgress status, List<String> keys) {
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
        if (status.isPipelineCompleted() || keys.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (!isStageComplete(keys.get(i), status)) {
                return i;
            }
        }
        return 0;
    }

    public static NavLinks viewLinks(String chassisNo, int stageIndex, PreparationProgress status, List<String> keys) {
        String encoded = encode(chassisNo);
        String viewBase = "/workshop-yard/" + encoded;
        String key = keyAt(keys, stageIndex);
        String prevUrl = stageIndex > 0 ? viewBase + "?stage=" + (stageIndex - 1) : null;
        String nextUrl;
        String nextLabel;
        if (stageIndex < keys.size() - 1) {
            String nextKey = keys.get(stageIndex + 1);
            if (FlowStage.YARD.getStageKey().equals(nextKey) && !status.isCanEnterYard()) {
                nextUrl = null;
                nextLabel = "Complete all workshop jobs to open Yard";
            } else {
                nextUrl = viewBase + "?stage=" + (stageIndex + 1);
                nextLabel = "Next · " + titleFor(nextKey);
            }
        } else {
            nextUrl = "/ready-for-sale/" + encoded;
            nextLabel = "Next · Ready for sale pipeline";
        }
        return new NavLinks(
                stageIndex,
                (stageIndex + 1) + " · " + titleFor(key),
                prevUrl,
                nextUrl,
                nextLabel,
                editUrlFor(encoded, key)
        );
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

    private static String redirectAfterStage(String chassisNo, PreparationProgress status, List<String> keys, String currentKey) {
        String encoded = encode(chassisNo);
        String viewBase = "/workshop-yard/" + encoded;
        if (status.isPipelineCompleted()) {
            int index = keys.indexOf(currentKey);
            if (index >= 0 && index < keys.size() - 1) {
                return viewBase + "?stage=" + (index + 1);
            }
            return "/ready-for-sale/" + encoded;
        }
        int index = keys.indexOf(currentKey);
        for (int i = index + 1; i < keys.size(); i++) {
            if (FlowStage.YARD.getStageKey().equals(keys.get(i)) && !status.isCanEnterYard()) {
                return "/workshop/" + encoded;
            }
            if (isStageComplete(keys.get(i), status)) {
                return viewBase + "?stage=" + i;
            }
            return editUrlFor(encoded, keys.get(i));
        }
        for (String key : keys) {
            if (!isStageComplete(key, status)) {
                return editUrlFor(encoded, key);
            }
        }
        return viewBase;
    }

    public static boolean isStageComplete(String stageKey, PreparationProgress status) {
        return status.isStageComplete(stageKey);
    }

    public static String editUrlFor(String encodedChassis, String stageKey) {
        if (FlowStage.YARD.getStageKey().equals(stageKey)) {
            return "/yard/" + encodedChassis;
        }
        if (FlowStage.INSPECTION.getStageKey().equals(stageKey)) {
            return "/inspection/" + encodedChassis;
        }
        return "/workshop/" + encodedChassis;
    }

    public static String titleFor(String stageKey) {
        if (FlowStage.YARD.getStageKey().equals(stageKey)) {
            return "Yard";
        }
        if (FlowStage.INSPECTION.getStageKey().equals(stageKey)) {
            return "Inspection";
        }
        return "Workshop";
    }

    public static String shortTitleFor(String stageKey) {
        return titleFor(stageKey);
    }

    public static String keyAt(List<String> keys, int index) {
        if (keys == null || keys.isEmpty()) {
            return FlowStage.WORKSHOP.getStageKey();
        }
        if (index < 0) {
            return keys.get(0);
        }
        if (index >= keys.size()) {
            return keys.get(keys.size() - 1);
        }
        return keys.get(index);
    }

    public static String encode(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, java.nio.charset.StandardCharsets.UTF_8);
    }
}
