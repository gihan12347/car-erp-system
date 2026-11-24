package com.carsale.erp.shared.dto;

import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.repository.PipelineProgress;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class ImportProgress implements PipelineProgress {
    private final boolean auctionReady;
    private final boolean preshipReady;
    private final boolean equipmentReady;
    private final boolean jevicReady;
    private final boolean coiReady;
    private final boolean standardsReady;
    private final boolean exportReady;
    private final boolean photosReady;
    private final boolean gradeReady;
    private final boolean worksheetReady;
    private final Set<String> skippedStages;

    public ImportProgress(
            boolean auctionReady,
            boolean preshipReady,
            boolean equipmentReady,
            boolean jevicReady,
            boolean coiReady,
            boolean standardsReady,
            boolean exportReady,
            boolean photosReady,
            boolean gradeReady,
            boolean worksheetReady
    ) {
        this(
                auctionReady,
                preshipReady,
                equipmentReady,
                jevicReady,
                coiReady,
                standardsReady,
                exportReady,
                photosReady,
                gradeReady,
                worksheetReady,
                Collections.emptySet()
        );
    }

    @Override
    public boolean hasAnyCompletedStage() {
        return PipelineStageUtils.hasAnyCompletedStage(PipelineStageUtils.IMPORT_STAGE_KEYS, this);
    }

    @Override
    public int completedCount() {
        return PipelineStageUtils.completedCount(PipelineStageUtils.IMPORT_STAGE_KEYS, this);
    }

    @Override
    public boolean isPipelineCompleted() {
        return PipelineStageUtils.isPipelineCompleted(PipelineStageUtils.IMPORT_STAGE_KEYS, this);
    }

    public boolean isStageFilled(String stageKey) {
        if (FlowStage.PRESHIP.getStageKey().equals(stageKey)) {
            return preshipReady;
        }
        if (FlowStage.EQUIPMENT.getStageKey().equals(stageKey)) {
            return equipmentReady;
        }
        if (FlowStage.JEVIC.getStageKey().equals(stageKey)) {
            return jevicReady;
        }
        if (FlowStage.COI.getStageKey().equals(stageKey)) {
            return coiReady;
        }
        if (FlowStage.STANDARDS.getStageKey().equals(stageKey)) {
            return standardsReady;
        }
        if (FlowStage.EXPORT.getStageKey().equals(stageKey)) {
            return exportReady;
        }
        if (FlowStage.GRADE.getStageKey().equals(stageKey)) {
            return gradeReady;
        }
        if (FlowStage.PHOTOS.getStageKey().equals(stageKey)) {
            return photosReady;
        }
        if (FlowStage.WORKSHEET.getStageKey().equals(stageKey)) {
            return worksheetReady;
        }
        return auctionReady;
    }

    public boolean isStageSkipped(String stageKey) {
        return stageKey != null && skippedStages.contains(stageKey) && !isStageFilled(stageKey);
    }

    @Override
    public boolean isStageComplete(String stageKey) {
        return isStageFilled(stageKey) || (stageKey != null && skippedStages.contains(stageKey));
    }

    public String firstIncompleteStageKey(List<String> keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            if (PipelineStageUtils.isIncomplete(key, this)) {
                return key;
            }
        }
        return null;
    }
}
