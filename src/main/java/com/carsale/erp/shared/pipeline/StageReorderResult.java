package com.carsale.erp.shared.pipeline;

import com.carsale.erp.shared.model.PipelineStage;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
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

}
