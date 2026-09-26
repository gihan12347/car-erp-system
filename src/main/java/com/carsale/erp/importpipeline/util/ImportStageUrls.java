package com.carsale.erp.importpipeline.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.carsale.erp.shared.pipeline.*;
import com.carsale.erp.shared.utils.PipelineStageUtils;

import com.carsale.erp.importpipeline.service.ImportProgressService.ImportProgress;
import static com.carsale.erp.shared.pipeline.PipelineStageService.viewLinks;

public final class ImportStageUrls {

    private ImportStageUrls() {
    }

    public static String pipelineHubUrl(String chassisNo, ImportProgress status, List<String> keys) {
        String encoded = PipelineStageUtils.encode(chassisNo);
        if (status.isPipelineCompleted()) {
            return "/auction/" + encoded;
        }
        if (!status.hasAnyCompletedStage()) {
            return firstIncompleteEditUrl(encoded, status, keys);
        }
        return "/auction/" + encoded;
    }

    public static String redirectAfterNewVehicle(String chassisNo, ImportProgress status, List<String> keys) {
        String encoded = PipelineStageUtils.encode(chassisNo);
        if (status.isPipelineCompleted()) {
            return "/customs/" + encoded;
        }
        return firstIncompleteEditUrl(encoded, status, keys);
    }

    public static String getStageKeyBySortOrder(List<PipelineStage> stages, int index) {
        String stageKeyAt = PipelineStageUtils.stageKeyAt(stages, index);
        return stageKeyAt == null ? FlowStage.AUCTION.getStageKey() : stageKeyAt;
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
        String encoded = PipelineStageUtils.encode(chassisNo);
        String prevUrl = null;
        if (stageIndex > 0) {
            String prevKey = PipelineStageService.stageKeyAt(keys, stageIndex - 1);
            prevUrl = editUrlFor(encoded, prevKey);
        }
        return new NavLinks(view.getStageIndex(), view.getStageLabel(), prevUrl,
                view.getNextUrl(), view.getNextLabel(), null);
    }

    public static String redirectAfterAuctionSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.AUCTION.getStageKey());
    }

    public static String redirectAfterPreshipSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.PRESHIP.getStageKey());
    }

    public static String redirectAfterEquipmentSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.EQUIPMENT.getStageKey());
    }

    public static String redirectAfterOdometerSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.JEVIC.getStageKey());
    }

    public static String redirectAfterCoiSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.COI.getStageKey());
    }

    public static String redirectAfterStandardsSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.STANDARDS.getStageKey());
    }

    public static String redirectAfterExportSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.EXPORT.getStageKey());
    }

    public static String redirectAfterGradeSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.GRADE.getStageKey());
    }

    public static String redirectAfterPhotosSave(String chassisNo, List<String> keys) {
        return PipelineStageUtils.redirectAfterStageSave(chassisNo, FlowPipeline.IMPORT.getCurrentBase(), keys, FlowStage.PHOTOS.getStageKey());
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
                    FlowStage.GRADE.getStageKey(),
                    FlowStage.PHOTOS.getStageKey()
            );
        }
        for (String key : ordered) {
            if (!isStageComplete(key, status)) {
                String url = editUrlFor(encoded, key);
                if (url != null) {
                    return url;
                }
            }
        }
        return "/auction/" + encoded;
    }

}
