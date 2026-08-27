package com.carsale.erp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(unique = true, length = 20)
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

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getStockNo() {
        return stockNo;
    }

    public void setStockNo(String stockNo) {
        this.stockNo = stockNo;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getMileage() {
        return mileage;
    }

    public void setMileage(String mileage) {
        this.mileage = mileage;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getEngineSize() {
        return engineSize;
    }

    public void setEngineSize(String engineSize) {
        this.engineSize = engineSize;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getFuel() {
        return fuel;
    }

    public void setFuel(String fuel) {
        this.fuel = fuel;
    }

    public String getDoors() {
        return doors;
    }

    public void setDoors(String doors) {
        this.doors = doors;
    }

    public String getSeats() {
        return seats;
    }

    public void setSeats(String seats) {
        this.seats = seats;
    }

    public String getAuctionHouse() {
        return auctionHouse;
    }

    public void setAuctionHouse(String auctionHouse) {
        this.auctionHouse = auctionHouse;
    }

    public String getLotNo() {
        return lotNo;
    }

    public void setLotNo(String lotNo) {
        this.lotNo = lotNo;
    }

    public String getAuctionGrade() {
        return auctionGrade;
    }

    public void setAuctionGrade(String auctionGrade) {
        this.auctionGrade = auctionGrade;
    }

    public String getInspection() {
        return inspection;
    }

    public void setInspection(String inspection) {
        this.inspection = inspection;
    }

    public String getRegistrationMonth() {
        return registrationMonth;
    }

    public void setRegistrationMonth(String registrationMonth) {
        this.registrationMonth = registrationMonth;
    }

    public String getExteriorGrade() {
        return exteriorGrade;
    }

    public void setExteriorGrade(String exteriorGrade) {
        this.exteriorGrade = exteriorGrade;
    }

    public String getInteriorGrade() {
        return interiorGrade;
    }

    public void setInteriorGrade(String interiorGrade) {
        this.interiorGrade = interiorGrade;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public String getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(String listingStatus) {
        this.listingStatus = listingStatus;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }

    public String getDriveSystem() {
        return driveSystem;
    }

    public void setDriveSystem(String driveSystem) {
        this.driveSystem = driveSystem;
    }

    public String getBodyStyle() {
        return bodyStyle;
    }

    public void setBodyStyle(String bodyStyle) {
        this.bodyStyle = bodyStyle;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getInteriorColor() {
        return interiorColor;
    }

    public void setInteriorColor(String interiorColor) {
        this.interiorColor = interiorColor;
    }

    public String getAcType() {
        return acType;
    }

    public void setAcType(String acType) {
        this.acType = acType;
    }

    public String getRecycleFee() {
        return recycleFee;
    }

    public void setRecycleFee(String recycleFee) {
        this.recycleFee = recycleFee;
    }

    public String getLengthCm() {
        return lengthCm;
    }

    public void setLengthCm(String lengthCm) {
        this.lengthCm = lengthCm;
    }

    public String getWidthCm() {
        return widthCm;
    }

    public void setWidthCm(String widthCm) {
        this.widthCm = widthCm;
    }

    public String getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(String heightCm) {
        this.heightCm = heightCm;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getSalesPoints() {
        return salesPoints;
    }

    public void setSalesPoints(String salesPoints) {
        this.salesPoints = salesPoints;
    }

    public String getInspectorNotes() {
        return inspectorNotes;
    }

    public void setInspectorNotes(String inspectorNotes) {
        this.inspectorNotes = inspectorNotes;
    }

    public VehicleStage getStage() {
        return stage;
    }

    public void setStage(VehicleStage stage) {
        this.stage = stage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public String getSheetOriginalName() {
        return sheetOriginalName;
    }

    public void setSheetOriginalName(String sheetOriginalName) {
        this.sheetOriginalName = sheetOriginalName;
    }

    public String getSheetStoredName() {
        return sheetStoredName;
    }

    public void setSheetStoredName(String sheetStoredName) {
        this.sheetStoredName = sheetStoredName;
    }

    public String getSheetContentType() {
        return sheetContentType;
    }

    public void setSheetContentType(String sheetContentType) {
        this.sheetContentType = sheetContentType;
    }
}
