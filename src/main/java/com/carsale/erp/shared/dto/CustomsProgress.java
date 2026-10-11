package com.carsale.erp.shared.dto;

import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.repository.PipelineProgress;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
public class CustomsProgress implements PipelineProgress {
    private final boolean blReady;
    private final boolean declarationReady;
    private final boolean assessmentReady;
    private final boolean worksheetReady;
    private final Set<String> skippedStages;

    public CustomsProgress(boolean blReady, boolean declarationReady, boolean assessmentReady, boolean worksheetReady) {
        this(blReady, declarationReady, assessmentReady, worksheetReady, Collections.emptySet());
    }

    public CustomsProgress(
            boolean blReady,
            boolean declarationReady,
            boolean assessmentReady,
            boolean worksheetReady,
            Set<String> skippedStages
    ) {
        this.blReady = blReady;
        this.declarationReady = declarationReady;
        this.assessmentReady = assessmentReady;
        this.worksheetReady = worksheetReady;
        this.skippedStages = skippedStages == null ? Collections.emptySet() : skippedStages;
    }

    @Override
    public boolean hasAnyCompletedStage() {
        return completedCount() > 0;
    }

    public int completedCount() {
        int count = 0;
        for (String key : PipelineStageUtils.CUSTOMS_STAGE_KEYS) {
            if (isStageComplete(key)) {
                count++;
            }
        }
        return count;
    }

    public boolean isStageFilled(String stageKey) {
        if (FlowStage.BILL_OF_LADING.getStageKey().equals(stageKey)) {
            return blReady;
        }
        if (FlowStage.ASSESSMENT.getStageKey().equals(stageKey)) {
            return assessmentReady;
        }
        if (FlowStage.WORKSHEET.getStageKey().equals(stageKey)) {
            return worksheetReady;
        }
        return FlowStage.DECLARATION.getStageKey().equals(stageKey) && declarationReady;
    }

    public boolean isStageSkipped(String stageKey) {
        return stageKey != null && skippedStages.contains(stageKey) && !isStageFilled(stageKey);
    }

    public boolean isStageComplete(String stageKey) {
        return isStageFilled(stageKey) || (stageKey != null && skippedStages.contains(stageKey));
    }

    @Override
    public boolean isPipelineCompleted() {
        for (String key : PipelineStageUtils.CUSTOMS_STAGE_KEYS) {
            if (!isStageComplete(key)) {
                return false;
            }
        }
        return true;
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
