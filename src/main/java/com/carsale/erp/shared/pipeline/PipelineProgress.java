package com.carsale.erp.shared.pipeline;

public interface PipelineProgress {
    boolean hasAnyCompletedStage();
    int completedCount();
    boolean isStageComplete(String stageKey);
    boolean isPipelineCompleted();
}
