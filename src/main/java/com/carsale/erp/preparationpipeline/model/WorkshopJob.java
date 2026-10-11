package com.carsale.erp.preparationpipeline.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Setter
@Getter
@Entity
@Table(name = "workshop_jobs")
public class WorkshopJob {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 40)
    private String jobStatus = "PENDING";

    @Column()
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

    @Column(nullable = false)
    private boolean completed;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<WorkshopJobLine> lines = new ArrayList<>();

    public List<WorkshopJobLine> getLines() {
        if (lines == null) {
            lines = new ArrayList<>();
        }
        return lines;
    }

}
