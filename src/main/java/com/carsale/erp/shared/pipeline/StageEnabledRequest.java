package com.carsale.erp.shared.pipeline;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StageEnabledRequest {

    private Long flowId;
    private String stageKey;
    private boolean enabled;
}
