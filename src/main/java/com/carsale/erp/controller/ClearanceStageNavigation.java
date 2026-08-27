package com.carsale.erp.controller;

import java.util.List;

import org.springframework.web.util.UriUtils;

import com.carsale.erp.dto.NavLinks;
import com.carsale.erp.service.ClearancePipelineService.ClearanceStatus;
import com.carsale.erp.service.PipelineStageService;

final class ClearanceStageNavigation {

    private ClearanceStageNavigation() {
    }

    static int clampStageIndex(Integer requested, ClearanceStatus status, List<String> keys) {
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

    static int resolveStartStageIndex(ClearanceStatus status, List<String> keys) {
        if (status.isClearanceComplete() || keys.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (isReady(keys.get(i), status)) {
                return i;
            }
        }
        return 0;
    }

    static NavLinks viewLinks(String chassisNo, int stageIndex, List<String> keys) {
        String encoded = encode(chassisNo);
        String viewBase = "/customs/" + encoded;
        String key = keyAt(keys, stageIndex);
        String prevUrl = stageIndex > 0 ? viewBase + "?stage=" + (stageIndex - 1) : null;
        String nextUrl;
        String nextLabel = "Next";
        if (stageIndex < keys.size() - 1) {
            String nextKey = keys.get(stageIndex + 1);
            nextUrl = viewBase + "?stage=" + (stageIndex + 1);
            nextLabel = "Next · " + titleFor(nextKey);
        } else {
            nextUrl = "/workshop-yard/" + encoded;
            nextLabel = "Next · Preparation pipeline";
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

    static String redirectAfterClearanceSave(String chassisNo, ClearanceStatus status, List<String> keys) {
        String encoded = encode(chassisNo);
        String viewBase = "/customs/" + encoded;
        if (status.isClearanceComplete()) {
            int last = Math.max(0, keys.size() - 1);
            return viewBase + "?stage=" + last;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (isReady(keys.get(i), status)) {
                return viewBase + "?stage=" + i;
            }
        }
        return viewBase;
    }

    static String editUrlFor(String encodedChassis, String stageKey) {
        String base = "/customs/" + encodedChassis + "/edit";
        if (PipelineStageService.STAGE_DECLARATION.equals(stageKey)) {
            return base + "?tab=2";
        }
        if (PipelineStageService.STAGE_ASSESSMENT.equals(stageKey)) {
            return base + "?tab=3";
        }
        return base;
    }

    static boolean isReady(String stageKey, ClearanceStatus status) {
        if (PipelineStageService.STAGE_DECLARATION.equals(stageKey)) {
            return !status.isDeclarationReady();
        }
        if (PipelineStageService.STAGE_ASSESSMENT.equals(stageKey)) {
            return !status.isAssessmentReady();
        }
        return !status.isJevicReady();
    }

    static String titleFor(String stageKey) {
        if (PipelineStageService.STAGE_DECLARATION.equals(stageKey)) {
            return "Customs declaration";
        }
        if (PipelineStageService.STAGE_ASSESSMENT.equals(stageKey)) {
            return "Assessment notice";
        }
        return "JEVIC certificate";
    }

    static String keyAt(List<String> keys, int index) {
        if (keys == null || keys.isEmpty()) {
            return PipelineStageService.STAGE_JEVIC;
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
