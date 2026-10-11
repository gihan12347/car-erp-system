package com.carsale.erp.shared.repository;

public interface PipelineProgress {
    boolean hasAnyCompletedStage();
    int completedCount();
    boolean isStageComplete(String stageKey);
    boolean isPipelineCompleted();
}
