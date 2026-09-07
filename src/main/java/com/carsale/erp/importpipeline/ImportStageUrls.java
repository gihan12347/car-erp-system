package com.carsale.erp.importpipeline;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.carsale.erp.shared.pipeline.*;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.importpipeline.ImportProgressService.ImportProgress;

import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

public final class ImportStageUrls {

    private ImportStageUrls() {
    }

    public static String pipelineHubUrl(String chassisNo, ImportProgress status, List<String> keys) {
        String encoded = encode(chassisNo);
        if (status.isPipelineCompleted()) {
            return viewBase(encoded);
        }
        if (!status.hasAnyCompletedStage()) {
            return firstIncompleteEditUrl(encoded, status, keys);
        }
        return viewBase(encoded);
    }

    public static String redirectAfterNewVehicle(String chassisNo, ImportProgress status, List<String> keys) {
        String encoded = encode(chassisNo);
        if (status.isPipelineCompleted()) {
            return "/customs/" + encoded;
        }
        return firstIncompleteEditUrl(encoded, status, keys);
    }

    public static int resolveStageIndex(List<PipelineStage> stages, String requested) {
        if (requested == null) {
            return 0;
        }
        String value = requested.trim();
        for (int i = 0; i < stages.size(); i++) {
            if (value.equalsIgnoreCase(stages.get(i).getStageKey())) {
                return i;
            }
        }
        return 0;
    }

    public static String stageKeyAt(List<PipelineStage> stages, int index) {
        if (stages == null || stages.isEmpty()) {
            return FlowStage.AUCTION.getStageKey();
        }
        if (index < 0) {
            return stages.get(0).getStageKey();
        }
        if (index >= stages.size()) {
            return stages.get(stages.size() - 1).getStageKey();
        }
        return stages.get(index).getStageKey();
    }

    public static String getStageKeyBySortOrder(List<PipelineStage> stages, int index) {
        return stageKeyAt(stages, index);
    }

    private static String viewUrl(String viewBase, String stageKey) {
        if (stageKey == null || stageKey.trim().isEmpty()) {
            return viewBase;
        }
        return viewBase + "?stage=" + stageKey;
    }

    public static Map<Integer, String> getStageKeyBySortOrder(List<PipelineStage> stages) {
        Map<Integer, String> map = new TreeMap<>();
        if (stages == null) {
            return map;
        }
        for (PipelineStage stage : stages) {
            map.put(stage.getSortOrder(), stage.getStageKey());
        }
        return map;
    }

    public static NavLinks editLinks(String chassisNo, int stageIndex, ImportProgress status, List<PipelineStage> keys) {
        NavLinks view = viewLinks(chassisNo, stageIndex, status, keys, FlowPipeline.IMPORT);
        return new NavLinks(view.getStageIndex(), view.getStageLabel(), view.getPrevUrl(),
                view.getNextUrl(), view.getNextLabel(), null);
    }

    public static String redirectAfterAuctionSave(String chassisNo, ImportProgress status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, FlowStage.AUCTION.getStageKey());
    }

    public static String redirectAfterPreshipSave(String chassisNo, ImportProgress status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, FlowStage.PRESHIP.getStageKey());
    }

    public static String redirectAfterEquipmentSave(String chassisNo, ImportProgress status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, FlowStage.EQUIPMENT.getStageKey());
    }

    public static String redirectAfterOdometerSave(String chassisNo, ImportProgress status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, FlowStage.JEVIC.getStageKey());
    }

    public static String redirectAfterCoiSave(String chassisNo, ImportProgress status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, FlowStage.COI.getStageKey());
    }

    public static String redirectAfterStandardsSave(String chassisNo, ImportProgress status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, FlowStage.STANDARDS.getStageKey());
    }

    public static String redirectAfterExportSave(String chassisNo, ImportProgress status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, FlowStage.EXPORT.getStageKey());
    }

    public static String redirectAfterPhotosSave(String chassisNo, ImportProgress status, List<String> keys) {
        return redirectAfterStageSave(chassisNo, status, keys, FlowStage.PHOTOS.getStageKey());
    }

    private static String redirectAfterStageSave(
            String chassisNo,
            ImportProgress status,
            List<String> keys,
            String currentKey
    ) {
        String encoded = encode(chassisNo);
        if (status.isPipelineCompleted()) {
            return "/customs/" + encoded;
        }
        return redirectToNext(encoded, status, keys, currentKey, viewBase(encoded));
    }

    private static String redirectToNext(
            String encoded,
            ImportProgress status,
            List<String> keys,
            String currentKey,
            String viewBase
    ) {
        int index = keys.indexOf(currentKey);
        for (int i = index + 1; i < keys.size(); i++) {
            String nextKey = keys.get(i);
            if (isStageComplete(nextKey, status)) {
                return viewUrl(viewBase, nextKey);
            }
            return editUrlFor(encoded, nextKey);
        }
        for (String key : keys) {
            if (!isStageComplete(key, status)) {
                return editUrlFor(encoded, key);
            }
        }
        return viewBase;
    }

    public static boolean isStageComplete(String stageKey, PipelineProgress status) {
        return status.isStageComplete(stageKey);
    }

    public static String editUrlFor(String encodedChassis, String stageKey) {
        return PipelineStageService.editUrlFor(encodedChassis, stageKey);
    }


    private static String firstIncompleteEditUrl(String encoded, ImportProgress status, List<String> keys) {
        List<String> ordered = keys;
        if (ordered == null || ordered.isEmpty()) {
            ordered = java.util.Arrays.asList(
                    FlowStage.AUCTION.getStageKey(),
                    FlowStage.PRESHIP.getStageKey(),
                    FlowStage.EQUIPMENT.getStageKey(),
                    FlowStage.JEVIC.getStageKey(),
                    FlowStage.COI.getStageKey(),
                    FlowStage.STANDARDS.getStageKey(),
                    FlowStage.EXPORT.getStageKey(),
                    FlowStage.PHOTOS.getStageKey()
            );
        }
        for (String key : ordered) {
            if (!isStageComplete(key, status)) {
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
