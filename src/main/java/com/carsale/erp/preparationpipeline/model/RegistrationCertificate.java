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
@Table(name = "registration_certificates")
public class RegistrationCertificate {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String absoluteOwner;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String classOfVehicle;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String colour;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String conditionsSpecialNotes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String countryOfOrigin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String crNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String currentOwnerAddressIdNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String cylinderCapacity;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String dateOfFirstRegistration;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String engineNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String fuelType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String height;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String length;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String make;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String manufacturesDescription;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String model;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String overHang;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String previousOwners;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String printedDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String provincialCouncil;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String registrationNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String seatingCapacity;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String statusWhenRegistered;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String taxationClass;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String taxesPayable;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String typeOfBody;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tyreSizeDual;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tyreSizeFront;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tyreSizeRear;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tyreSizeSingle;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String weightGross;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String weightUnladen;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String wheelBase;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String width;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String yearOfManufacture;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String mileage;

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
