package com.carsale.erp.shared.vehicle;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    /** Vehicle no stored as V0001, V0002, ... (column name kept as stock_no). */
    @Column(name = "stock_no", unique = true, length = 20)
    private String stockNo;

    @Column(length = 80)
    private String make;

    @Column(length = 80)
    private String model;

    @Column(length = 80)
    private String grade;

    @Column(length = 40)
    private String modelCode;

    @Column(name = "model_year", length = 40)
    private String year;

    @Column(length = 40)
    private String mileage;

    @Column(length = 40)
    private String color;

    @Column(length = 40)
    private String engineSize;

    @Column(length = 40)
    private String transmission;

    @Column(length = 40)
    private String fuel;

    @Column(length = 10)
    private String doors;

    @Column(length = 20)
    private String seats;

    @Column(length = 80)
    private String auctionHouse;

    @Column(length = 40)
    private String lotNo;

    @Column(length = 20)
    private String auctionGrade;

    @Column(length = 40)
    private String inspection;

    @Column(length = 10)
    private String registrationMonth;

    @Column(length = 10)
    private String exteriorGrade;

    @Column(length = 10)
    private String interiorGrade;

    @Column(length = 40)
    private String history;

    @Column(length = 40)
    private String listingStatus;

    @Column(length = 40)
    private String warranty;

    @Column(length = 20)
    private String driveSystem;

    @Column(length = 40)
    private String bodyStyle;

    @Column(length = 20)
    private String colorCode;

    @Column(length = 80)
    private String interiorColor;

    @Column(length = 20)
    private String acType;

    @Column(length = 40)
    private String recycleFee;

    @Column(name = "length_cm", length = 10)
    private String lengthCm;

    @Column(name = "width_cm", length = 10)
    private String widthCm;

    @Column(name = "height_cm", length = 10)
    private String heightCm;

    @Column(length = 500)
    private String equipment;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String salesPoints;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String inspectorNotes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VehicleStage stage = VehicleStage.PURCHASED;

    @Column(length = 500)
    private String notes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    @Column(length = 255)
    private String sheetOriginalName;

    @Column(length = 120)
    private String sheetStoredName;

    @Column(length = 80)
    private String sheetContentType;

    @Column(name = "skipped_stages", length = 255)
    private String skippedStages;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private VehicleIntake intake;

    @Column(name = "selected_pipelines", length = 40)
    private String selectedPipelines;

    public String getTransmissionLabel() {
        return VehicleFieldNormalizer.displayTransmission(transmission);
    }
}
