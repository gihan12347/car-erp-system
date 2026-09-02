package com.carsale.erp.controller;

import java.util.List;

import org.springframework.web.util.UriUtils;

import com.carsale.erp.dto.NavLinks;
import com.carsale.erp.service.PipelineStageService;
import com.carsale.erp.service.PreparationPipelineService.PrepStatus;

final class ReadyStageNavigation {

    private ReadyStageNavigation() {
    }

    static int clampStageIndex(Integer requested, PrepStatus status, List<String> keys) {
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

    static int resolveStartStageIndex(PrepStatus status, List<String> keys) {
        if (status.isPrepComplete() || keys.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (!isReady(keys.get(i), status)) {
                return i;
            }
        }
        return 0;
    }

    static NavLinks viewLinks(String chassisNo, int stageIndex, PrepStatus status, List<String> keys) {
        String encoded = encode(chassisNo);
        String viewBase = "/workshop-yard/" + encoded;
        String key = keyAt(keys, stageIndex);
        String prevUrl = stageIndex > 0 ? viewBase + "?stage=" + (stageIndex - 1) : null;
        String nextUrl;
        String nextLabel = "Next";
        if (stageIndex < keys.size() - 1) {
            String nextKey = keys.get(stageIndex + 1);
            if (PipelineStageService.STAGE_YARD.equals(nextKey) && !status.isCanEnterYard()) {
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

    static String redirectAfterWorkshopSave(String chassisNo, PrepStatus status, List<String> keys) {
        return redirectAfterStage(chassisNo, status, keys, PipelineStageService.STAGE_WORKSHOP);
    }

    static String redirectAfterYardSave(String chassisNo, PrepStatus status, List<String> keys) {
        return redirectAfterStage(chassisNo, status, keys, PipelineStageService.STAGE_YARD);
    }

    static String redirectAfterInspectionSave(String chassisNo, PrepStatus status, List<String> keys) {
        return redirectAfterStage(chassisNo, status, keys, PipelineStageService.STAGE_INSPECTION);
    }

    private static String redirectAfterStage(String chassisNo, PrepStatus status, List<String> keys, String currentKey) {
        String encoded = encode(chassisNo);
        String viewBase = "/workshop-yard/" + encoded;
        if (status.isPrepComplete()) {
            int index = keys.indexOf(currentKey);
            if (index >= 0 && index < keys.size() - 1) {
                return viewBase + "?stage=" + (index + 1);
            }
            return "/ready-for-sale/" + encoded;
        }
        int index = keys.indexOf(currentKey);
        for (int i = index + 1; i < keys.size(); i++) {
            if (PipelineStageService.STAGE_YARD.equals(keys.get(i)) && !status.isCanEnterYard()) {
                return "/workshop/" + encoded;
            }
            if (isReady(keys.get(i), status)) {
                return viewBase + "?stage=" + i;
            }
            return editUrlFor(encoded, keys.get(i));
        }
        for (int i = 0; i < keys.size(); i++) {
            if (!isReady(keys.get(i), status)) {
                return editUrlFor(encoded, keys.get(i));
            }
        }
        return viewBase;
    }

    static boolean isReady(String stageKey, PrepStatus status) {
        if (PipelineStageService.STAGE_YARD.equals(stageKey)) {
            return status.isYardReady();
        }
        if (PipelineStageService.STAGE_INSPECTION.equals(stageKey)) {
            return status.isInspectionReady();
        }
        return status.isWorkshopReady();
    }

    static String editUrlFor(String encodedChassis, String stageKey) {
        if (PipelineStageService.STAGE_YARD.equals(stageKey)) {
            return "/yard/" + encodedChassis;
        }
        if (PipelineStageService.STAGE_INSPECTION.equals(stageKey)) {
            return "/inspection/" + encodedChassis;
        }
        return "/workshop/" + encodedChassis;
    }

    static String titleFor(String stageKey) {
        if (PipelineStageService.STAGE_YARD.equals(stageKey)) {
            return "Yard";
        }
        if (PipelineStageService.STAGE_INSPECTION.equals(stageKey)) {
            return "Inspection";
        }
        return "Workshop";
    }

    static String keyAt(List<String> keys, int index) {
        if (keys == null || keys.isEmpty()) {
            return PipelineStageService.STAGE_WORKSHOP;
        }
        if (index < 0) {
            return keys.get(0);
        }
        if (index >= keys.size()) {
            return keys.get(keys.size() - 1);
        }
        return keys.get(index);
    }

    static String encode(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, java.nio.charset.StandardCharsets.UTF_8);
    }
}
