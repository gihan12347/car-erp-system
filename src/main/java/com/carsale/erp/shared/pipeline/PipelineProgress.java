package com.carsale.erp.shared.pipeline;

public interface PipelineProgress {
    public boolean hasAnyCompletedStage();
    public int completedCount();
    public boolean isStageComplete(String stageKey);
    public boolean isPipelineCompleted();
}
