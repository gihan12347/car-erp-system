package com.carsale.erp.importpipeline.model;

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
@Table(name = "inspection_certificates")
public class InspectionCertificate {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String certificateNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String inspectionBranch;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String make;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String model;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String engineCapacity;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String firstRegistration;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String chassisVin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String engineNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String inspectedMileage;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String inspectionDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column()
    private String documentOriginalName;

    @Column(length = 120)
    private String documentStoredName;

    @Column(length = 80)
    private String documentContentType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

}
