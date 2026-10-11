package com.carsale.erp.shared.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Setter
@Getter
@Entity
@Table(name = "pipeline_flows")
public class PipelineFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flow_key", nullable = false, unique = true, length = 40)
    private String flowKey;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 160)
    private String description;

    @Column(length = 40)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "flow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<PipelineStage> stages = new ArrayList<>();

    public void addStage(PipelineStage stage) {
        if (stage == null) {
            return;
        }
        stages.add(stage);
        stage.setFlow(this);
    }
}
