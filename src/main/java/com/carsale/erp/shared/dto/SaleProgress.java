package com.carsale.erp.shared.dto;

import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.repository.PipelineProgress;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;

@Getter
public class SaleProgress implements PipelineProgress {
    private final boolean detailsReady;
    private final boolean listingReady;
    private final boolean registrationReady;
    private final boolean sold;
    private final Set<String> skippedStages;

    public SaleProgress(boolean detailsReady, boolean listingReady, boolean registrationReady, boolean sold) {
        this(detailsReady, listingReady, registrationReady, sold, Collections.emptySet());
    }

    public SaleProgress(
            boolean detailsReady,
            boolean listingReady,
            boolean registrationReady,
            boolean sold,
            Set<String> skippedStages
    ) {
        this.detailsReady = detailsReady;
        this.listingReady = listingReady;
        this.registrationReady = registrationReady;
        this.sold = sold;
        this.skippedStages = skippedStages == null ? Collections.emptySet() : skippedStages;
    }

    @Override
    public boolean hasAnyCompletedStage() {
        return completedCount() > 0;
    }

    @Override
    public int completedCount() {
        int count = 0;
        for (String key : PipelineStageUtils.READY_STAGE_KEYS) {
            if (isStageComplete(key)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isPipelineCompleted() {
        for (String key : PipelineStageUtils.READY_STAGE_KEYS) {
            if (!isStageComplete(key)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStageFilled(String stageKey) {
        if (FlowStage.DETAILS.getStageKey().equals(stageKey)) {
            return detailsReady;
        }
        if (FlowStage.LISTING.getStageKey().equals(stageKey)) {
            return listingReady;
        }
        return FlowStage.REGISTRATION.getStageKey().equals(stageKey) && registrationReady;
    }

    public boolean isStageSkipped(String stageKey) {
        return stageKey != null && skippedStages.contains(stageKey) && !isStageFilled(stageKey);
    }

    @Override
    public boolean isStageComplete(String stageKey) {
        return isStageFilled(stageKey) || (stageKey != null && skippedStages.contains(stageKey));
    }

    public String firstIncompleteStageKey(java.util.List<String> keys) {
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

    @Getter
    @AllArgsConstructor
    public static class DocumentAiEntity {
        private final String type;
        private final String value;
        private final Double confidence;
    }
}
