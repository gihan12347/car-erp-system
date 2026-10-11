package com.carsale.erp.preparationpipeline.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Setter
@Getter
@Entity
@Table(name = "workshop_job_lines")
public class WorkshopJobLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chassis_no", nullable = false)
    private WorkshopJob job;

    @Column(length = 40)
    private String jobStatus = "PENDING";

    @Column(length = 255)
    private String jobSummary;

    @Column(length = 80)
    private String technician;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String partsUsed;

    @Column(length = 20)
    private String laborHours;

    @Column(length = 40)
    private String estimatedCost;

    @Column(length = 40)
    private String actualCost;

    @Column(length = 40)
    private String startedOn;

    @Column(length = 40)
    private String completedOn;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "inspection_item_key", length = 80)
    private String inspectionItemKey;

    public boolean isEmpty() {
        return isBlank(jobSummary)
                && isBlank(technician)
                && isBlank(partsUsed)
                && isBlank(laborHours)
                && isBlank(estimatedCost)
                && isBlank(actualCost)
                && isBlank(startedOn)
                && isBlank(completedOn)
                && isBlank(notes)
                && isBlank(inspectionItemKey)
                && (isBlank(jobStatus) || "PENDING".equals(jobStatus));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
