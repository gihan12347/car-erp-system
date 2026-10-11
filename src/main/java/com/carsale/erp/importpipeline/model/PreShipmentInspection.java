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
@Table(name = "pre_shipment_inspections")
public class PreShipmentInspection {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 40)
    private String certificateReference;

    @Column(length = 120)
    private String documentTitle;

    @Column(length = 40)
    private String bvNumber;

    @Column(length = 40)
    private String certificateDate;

    @Column(length = 40)
    private String pageInfo;

    @Column(length = 80)
    private String documentControlNumber;

    @Column(length = 120)
    private String inspectionOrgName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String inspectionOrgAddress;

    @Column(length = 40)
    private String inspectionOrgTel;

    @Column(length = 40)
    private String inspectionOrgFax;

    @Column(length = 120)
    private String inspectionOrgEmail;

    @Column()
    private String placeOfInspection;

    @Column(length = 40)
    private String dateOfInspection;

    @Column(length = 120)
    private String applicantName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String applicantAddress;

    @Column(length = 40)
    private String applicantTel;

    @Column(length = 40)
    private String applicantFax;

    @Column(length = 120)
    private String applicantEmail;

    @Column(length = 80)
    private String vehicleType;

    @Column(length = 80)
    private String make;

    @Column(length = 80)
    private String model;

    @Column(length = 120)
    private String commonName;

    @Column(length = 40)
    private String manufactureGrade;

    @Column(length = 40)
    private String preshipAuctionGrade;

    @Column(length = 80)
    private String bodyColour;

    @Column(length = 40)
    private String fuelType;

    @Column(length = 40)
    private String firstRegistration;

    @Column(length = 40)
    private String inspectionMileage;

    @Column(length = 40)
    private String engineCapacity;

    @Column(length = 80)
    private String engineModel;

    @Column(length = 40)
    private String engineNo;

    @Column(length = 40)
    private String drivingSystem;

    @Column(length = 20)
    private String accidentMarksOnChassis;

    @Column(length = 40)
    private String chassisCondition;

    @Column(length = 80)
    private String fullModelNo;

    @Column(length = 20)
    private String yearOfManufacture;

    @Column(length = 120)
    private String tyreSize;

    @Column(length = 120)
    private String wheelBase;

    @Column(length = 120)
    private String grossVehicleMass;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    @Column()
    private String documentOriginalName;

    @Column(length = 120)
    private String documentStoredName;

    @Column(length = 80)
    private String documentContentType;

}
