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
@Table(name = "export_certificates")
public class ExportCertificate {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 16)
    private String documentType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String certificateNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String arrangementNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String issueDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String registrationNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String registrationDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String firstRegDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String chassisVin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String make;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String model;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String engineModel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String vehicleClassification;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String useType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bodyType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String seatingCapacity;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String maxCarry;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String weightKg;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String grossWeightKg;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String lengthCm;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String widthCm;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String heightCm;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String engineCapacity;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String fuelType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String specificationNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String classificationNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String frontAxleWeight;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rearAxleWeight;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String frWeight;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rfWeight;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String userName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String userAddress;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ownerName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ownerAddress;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String localityOfUse;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exportScheduledDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String directorGeneralLandTransportBranch;

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
