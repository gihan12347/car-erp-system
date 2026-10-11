package com.carsale.erp.shared.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(
        name = "pipeline_stages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pipeline_stages_flow_id_stage",
                columnNames = {"flow_id", "stage_key"}
        )
)
public class PipelineStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "flow_id",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pipeline_stages_flow")
    )
    private PipelineFlow flow;

    @Column(name = "stage_key", nullable = false, length = 40)
    private String stageKey;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 120)
    private String subtitle;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

}
