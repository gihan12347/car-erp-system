package com.carsale.erp.controller;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.carsale.erp.entity.PipelineStage;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.dto.NavLinks;
import com.carsale.erp.service.ImportPipelineService.ImportStatus;
import com.carsale.erp.service.PipelineStageService;

final class ImportStageNavigation {

    private ImportStageNavigation() {
    }

    static int clampStageIndex(Integer requested, ImportStatus status, List<String> keys) {
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

    static int resolveStartStageIndex(ImportStatus status, List<String> keys) {
        if (status.isImportComplete() || keys.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (!isReady(keys.get(i), status)) {
                return i;
            }
        }
        return 0;
    }

    static String pipelineHubUrl(String chassisNo, ImportStatus status) {
        return pipelineHubUrl(chassisNo, status, null);
    }

    static String pipelineHubUrl(String chassisNo, ImportStatus status, List<String> keys) {
        String encoded = encode(chassisNo);
        if (status.isImportComplete()) {
            return viewBase(encoded);
        }
        if (!status.isAnyReady()) {
            return firstIncompleteEditUrl(encoded, status, keys);
        }
        return viewBase(encoded);
    }

    static String redirectAfterNewVehicle(String chassisNo, ImportStatus status, List<String> keys) {
        String encoded = encode(chassisNo);
        if (status.isImportComplete()) {
            return "/customs/" + encoded;
        }
        return firstIncompleteEditUrl(encoded, status, keys);
    }

    static NavLinks viewLinks(String chassisNo, int stageIndex, ImportStatus status, List<PipelineStage> stages) {
        String encoded = encode(chassisNo);
        String viewBase = "/auction/" + encoded;
        String key = getStageKeyBySortOrder(stages, stageIndex);
        String prevUrl = stageIndex > 0 ? viewBase + "?stage=" + (stageIndex - 1) : null;
        String nextUrl;
        String nextLabel = "Next";
        if (stageIndex < stages.size() - 1) {
            String nextKey = getStageKeyBySortOrder(stages, stageIndex + 1);
            if (isReady(nextKey, status)) {
                nextUrl = viewBase + "?stage=" + (stageIndex + 1);
                nextLabel = "Next · " + titleFor(nextKey);
            } else {
                nextUrl = editUrlFor(encoded, nextKey);
                nextLabel = "Continue " + titleFor(nextKey).toLowerCase();
            }
        } else if (status.isImportComplete()) {
            nextUrl = "/customs/" + encoded;
            nextLabel = "Customs clearance pipeline";
        } else {
            nextUrl = null;
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

    static String getStageKeyBySortOrder(List<PipelineStage> stages, int sortOrder) {
        return stages.stream()
                .filter(stage -> stage.getSortOrder() == sortOrder)
                .map(PipelineStage::getStageKey)
                .findFirst()
                .orElse(null);
    }

    static Map<Integer, String> getStageKeyBySortOrder(List<PipelineStage> stages) {
        Map<Integer, String> map = new TreeMap<>();
        if (stages == null) {
            return map;
        }
        for (PipelineStage stage : stages) {
            map.put(stage.getSortOrder(), stage.getStageKey());
        }
        return map;
    }

    static NavLinks editLinks(String chassisNo, int stageIndex, ImportStatus status, List<PipelineStage> keys) {
        NavLinks view = viewLinks(chassisNo, stageIndex, status, keys);
        return new NavLinks(view.getStageIndex(), view.getStageLabel(), view.getPrevUrl(),
                view.getNextUrl(), view.getNextLabel(), null);
    }

    static String redirectAfterAuctionSave(String chassisNo, ImportStatus status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, PipelineStageService.STAGE_AUCTION);
    }

    static String redirectAfterPreshipSave(String chassisNo, ImportStatus status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, PipelineStageService.STAGE_PRESHIP);
    }

    static String redirectAfterEquipmentSave(String chassisNo, ImportStatus status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, PipelineStageService.STAGE_EQUIPMENT);
    }

    private static String redirectAfterStageSave(
            String chassisNo,
            ImportStatus status,
            List<String> keys,
            String currentKey
    ) {
        String encoded = encode(chassisNo);
        if (status.isImportComplete()) {
            return "/customs/" + encoded;
        }
        return redirectToNext(encoded, status, keys, currentKey, viewBase(encoded));
    }

    private static String redirectToNext(
            String encoded,
            ImportStatus status,
            List<String> keys,
            String currentKey,
            String viewBase
    ) {
        int index = keys.indexOf(currentKey);
        for (int i = index + 1; i < keys.size(); i++) {
            String nextKey = keys.get(i);
            if (isReady(nextKey, status)) {
                return viewBase + "?stage=" + i;
            }
            return editUrlFor(encoded, nextKey);
        }
        for (int i = 0; i < keys.size(); i++) {
            if (!isReady(keys.get(i), status)) {
                return editUrlFor(encoded, keys.get(i));
            }
        }
        return viewBase;
    }

    static boolean isReady(String stageKey, ImportStatus status) {
        if (PipelineStageService.STAGE_PRESHIP.equals(stageKey)) {
            return status.isPreshipReady();
        }
        if (PipelineStageService.STAGE_EQUIPMENT.equals(stageKey)) {
            return status.isEquipmentReady();
        }
        return status.isAuctionReady();
    }

    static String editUrlFor(String encodedChassis, String stageKey) {
        if (PipelineStageService.STAGE_PRESHIP.equals(stageKey)) {
            return "/shipping/" + encodedChassis + "?hub=1";
        }
        if (PipelineStageService.STAGE_EQUIPMENT.equals(stageKey)) {
            return "/equipment/" + encodedChassis + "?hub=1";
        }
        return "/auction/" + encodedChassis + "/edit?hub=1";
    }

    static String titleFor(String stageKey) {
        if (PipelineStageService.STAGE_PRESHIP.equals(stageKey)) {
            return "Pre-shipment";
        }
        if (PipelineStageService.STAGE_EQUIPMENT.equals(stageKey)) {
            return "Equipment condition";
        }
        return "Auction lot";
    }

    static String keyAt(List<String> keys, int index) {
        if (keys == null || keys.isEmpty()) {
            return PipelineStageService.STAGE_AUCTION;
        }
        if (index < 0) {
            return keys.get(0);
        }
        if (index >= keys.size()) {
            return keys.get(keys.size() - 1);
        }
        return keys.get(index);
    }

    private static String firstIncompleteEditUrl(String encoded, ImportStatus status, List<String> keys) {
        List<String> ordered = keys;
        if (ordered == null || ordered.isEmpty()) {
            ordered = java.util.Arrays.asList(
                    PipelineStageService.STAGE_AUCTION,
                    PipelineStageService.STAGE_PRESHIP,
                    PipelineStageService.STAGE_EQUIPMENT
            );
        }
        for (int i = 0; i < ordered.size(); i++) {
            String key = ordered.get(i);
            if (!isReady(key, status)) {
                return editUrlFor(encoded, key);
            }
        }
        return viewBase(encoded);
    }

    private static String viewBase(String encoded) {
        return "/auction/" + encoded;
    }

    private static String encode(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, java.nio.charset.StandardCharsets.UTF_8);
    }
}
