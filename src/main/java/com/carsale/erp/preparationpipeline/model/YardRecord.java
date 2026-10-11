package com.carsale.erp.preparationpipeline.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Setter
@Getter
@Entity
@Table(name = "yard_records")
public class YardRecord {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 40)
    private String bayNo;

    @Column(length = 80)
    private String yardSection;

    @Column(length = 40)
    private String arrivalDate;

    @Column(length = 80)
    private String keysLocation;

    @Column(length = 40)
    private String inspectionStatus = "PENDING";

    @Column(length = 80)
    private String inspector;

    @Column(length = 40)
    private String inspectionDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String conditionNotes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean completed;

}
