package com.carsale.erp.shared.pipeline;

import java.util.ArrayList;
import java.util.List;

public class StageReorderRequest {

    private Long flowId;
    private List<String> stageKeys = new ArrayList<String>();

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public List<String> getStageKeys() {
        return stageKeys;
    }

    public void setStageKeys(List<String> stageKeys) {
        this.stageKeys = stageKeys;
    }
}
