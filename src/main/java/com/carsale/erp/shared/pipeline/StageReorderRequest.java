package com.carsale.erp.shared.pipeline;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class StageReorderRequest {
    private Long flowId;
    private List<String> stageKeys = new ArrayList<>();
}
