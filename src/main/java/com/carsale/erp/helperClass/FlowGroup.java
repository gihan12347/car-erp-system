package com.carsale.erp.helperClass;

import java.util.List;

import com.carsale.erp.entity.PipelineStage;

public class FlowGroup {
    private final Long id;
    private final String title;
    private final String description;
    private final String icon;
    private final List<PipelineStage> stages;

    public FlowGroup(Long id, String title, String description, String icon, List<PipelineStage> stages) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.stages = stages;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public List<PipelineStage> getStages() {
        return stages;
    }
}
