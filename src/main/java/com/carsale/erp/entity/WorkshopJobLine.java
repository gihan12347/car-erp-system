package com.carsale.erp.entity;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkshopJob getJob() {
        return job;
    }

    public void setJob(WorkshopJob job) {
        this.job = job;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getJobSummary() {
        return jobSummary;
    }

    public void setJobSummary(String jobSummary) {
        this.jobSummary = jobSummary;
    }

    public String getTechnician() {
        return technician;
    }

    public void setTechnician(String technician) {
        this.technician = technician;
    }

    public String getPartsUsed() {
        return partsUsed;
    }

    public void setPartsUsed(String partsUsed) {
        this.partsUsed = partsUsed;
    }

    public String getLaborHours() {
        return laborHours;
    }

    public void setLaborHours(String laborHours) {
        this.laborHours = laborHours;
    }

    public String getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(String estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public String getActualCost() {
        return actualCost;
    }

    public void setActualCost(String actualCost) {
        this.actualCost = actualCost;
    }

    public String getStartedOn() {
        return startedOn;
    }

    public void setStartedOn(String startedOn) {
        this.startedOn = startedOn;
    }

    public String getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(String completedOn) {
        this.completedOn = completedOn;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getInspectionItemKey() {
        return inspectionItemKey;
    }

    public void setInspectionItemKey(String inspectionItemKey) {
        this.inspectionItemKey = inspectionItemKey;
    }

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
