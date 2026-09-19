package com.carsale.erp.shared.pipeline;

import java.util.ArrayList;
import java.util.List;

public class StageReorderResult {

    private boolean success;
    private String message;
    private List<PipelineStage> stages = new ArrayList<>();

    public static StageReorderResult ok(String message, List<PipelineStage> stages) {
        StageReorderResult result = new StageReorderResult();
        result.success = true;
        result.message = message;
        result.stages = stages;
        return result;
    }

    public static StageReorderResult fail(String message) {
        StageReorderResult result = new StageReorderResult();
        result.success = false;
        result.message = message;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<PipelineStage> getStages() {
        return stages;
    }

    public void setStages(List<PipelineStage> stages) {
        this.stages = stages;
    }
}
