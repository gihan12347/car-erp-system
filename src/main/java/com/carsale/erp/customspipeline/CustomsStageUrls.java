package com.carsale.erp.customspipeline;

import java.util.List;

import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.pipeline.NavLinks;
import com.carsale.erp.customspipeline.CustomsProgressService.CustomsProgress;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;

public final class CustomsStageUrls {

    private CustomsStageUrls() {
    }

    public static int clampStageIndex(Integer requested, CustomsProgress status, List<String> keys) {
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
        if (status.isClearanceComplete() || keys.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (isIncomplete(keys.get(i), status)) {
                return i;
            }
        }
        return 0;
    }

    public static String redirectAfterClearanceSave(String chassisNo, CustomsProgress status, List<String> keys) {
        String encoded = encode(chassisNo);
        String viewBase = "/customs/" + encoded;
        if (status.isClearanceComplete()) {
            int last = Math.max(0, keys.size() - 1);
            return viewBase + "?stage=" + last;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (isIncomplete(keys.get(i), status)) {
                return viewBase + "?stage=" + i;
            }
        }
        return viewBase;
    }

    public static String editUrlFor(String encodedChassis, String stageKey) {
        if (FlowStage.ASSESSMENT.getStageKey().equals(stageKey)) {
            return "/assessment/" + encodedChassis + "?hub=1";
        }
        if (FlowStage.WORKSHEET.getStageKey().equals(stageKey)) {
            return "/worksheet/" + encodedChassis + "?hub=1";
        }
        return "/declaration/" + encodedChassis + "?hub=1";
    }

    public static boolean isIncomplete(String stageKey, CustomsProgress status) {
        return !status.isStageComplete(stageKey);
    }

    public static String titleFor(String stageKey) {
        if (FlowStage.DECLARATION.getStageKey().equals(stageKey)) {
            return "Customs declaration";
        }
        if (FlowStage.WORKSHEET.getStageKey().equals(stageKey)) {
            return "Working sheet";
        }
        return "Assessment notice";
    }

    public static String keyAt(List<String> keys, int index) {
        if (keys == null || keys.isEmpty()) {
            return FlowStage.DECLARATION.getStageKey();
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
