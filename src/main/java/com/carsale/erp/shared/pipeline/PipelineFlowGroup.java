package com.carsale.erp.shared.pipeline;

import com.carsale.erp.shared.model.PipelineStage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PipelineFlowGroup {
    private final Long id;
    private final String title;
    private final String description;
    private final String icon;
    private final List<PipelineStage> stages;
}
