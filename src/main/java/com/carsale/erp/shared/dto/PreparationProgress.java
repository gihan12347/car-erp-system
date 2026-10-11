package com.carsale.erp.shared.dto;

import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.repository.PipelineProgress;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
public class PreparationProgress implements PipelineProgress {
    private final boolean workshopReady;
    private final boolean yardReady;
    private final boolean inspectionReady;
    private final boolean canEnterYard;
    private final boolean saleReady;
    private final Set<String> skippedStages;

    public PreparationProgress(boolean workshopReady, boolean yardReady, boolean inspectionReady, boolean canEnterYard, boolean saleReady) {
        this(workshopReady, yardReady, inspectionReady, canEnterYard, saleReady, Collections.emptySet());
    }

    public PreparationProgress(
            boolean workshopReady,
            boolean yardReady,
            boolean inspectionReady,
            boolean canEnterYard,
            boolean saleReady,
            Set<String> skippedStages
    ) {
        this.workshopReady = workshopReady;
        this.yardReady = yardReady;
        this.inspectionReady = inspectionReady;
        this.canEnterYard = canEnterYard;
        this.saleReady = saleReady;
        this.skippedStages = skippedStages == null ? Collections.emptySet() : skippedStages;
    }

    @Override
    public boolean hasAnyCompletedStage() {
        return completedCount() > 0;
    }

    @Override
    public int completedCount() {
        int count = 0;
        for (String key : PipelineStageUtils.PREP_STAGE_KEYS) {
            if (isStageComplete(key)) {
                count++;
            }
        }
        return count;
    }

    public boolean isPipelineCompleted() {
        for (String key : PipelineStageUtils.PREP_STAGE_KEYS) {
            if (!isStageComplete(key)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStageFilled(String stageKey) {
        if (FlowStage.INSPECTION.getStageKey().equals(stageKey)) {
            return inspectionReady;
        }
        if (FlowStage.WORKSHOP.getStageKey().equals(stageKey)) {
            return workshopReady;
        }
        if (FlowStage.YARD.getStageKey().equals(stageKey)) {
            return yardReady;
        }
        if (FlowStage.SALE.getStageKey().equals(stageKey)) {
            return saleReady;
        }
        return false;
    }

    public boolean isStageSkipped(String stageKey) {
        return stageKey != null && skippedStages.contains(stageKey) && !isStageFilled(stageKey);
    }

    public boolean isStageComplete(String stageKey) {
        return isStageFilled(stageKey) || (stageKey != null && skippedStages.contains(stageKey));
    }

    public String firstIncompleteStageKey(List<String> keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            if (!isStageComplete(key)) {
                return key;
            }
        }
        return null;
    }
}