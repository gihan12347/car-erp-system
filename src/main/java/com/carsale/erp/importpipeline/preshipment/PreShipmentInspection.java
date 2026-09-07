package com.carsale.erp.importpipeline.preshipment;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

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

    @Column(length = 255)
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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    @Column(length = 255)
    private String documentOriginalName;

    @Column(length = 120)
    private String documentStoredName;

    @Column(length = 80)
    private String documentContentType;

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getCertificateReference() {
        return certificateReference;
    }

    public void setCertificateReference(String certificateReference) {
        this.certificateReference = certificateReference;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getBvNumber() {
        return bvNumber;
    }

    public void setBvNumber(String bvNumber) {
        this.bvNumber = bvNumber;
    }

    public String getCertificateDate() {
        return certificateDate;
    }

    public void setCertificateDate(String certificateDate) {
        this.certificateDate = certificateDate;
    }

    public String getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(String pageInfo) {
        this.pageInfo = pageInfo;
    }

    public String getInspectionOrgName() {
        return inspectionOrgName;
    }

    public void setInspectionOrgName(String inspectionOrgName) {
        this.inspectionOrgName = inspectionOrgName;
    }

    public String getInspectionOrgAddress() {
        return inspectionOrgAddress;
    }

    public void setInspectionOrgAddress(String inspectionOrgAddress) {
        this.inspectionOrgAddress = inspectionOrgAddress;
    }

    public String getInspectionOrgTel() {
        return inspectionOrgTel;
    }

    public void setInspectionOrgTel(String inspectionOrgTel) {
        this.inspectionOrgTel = inspectionOrgTel;
    }

    public String getInspectionOrgFax() {
        return inspectionOrgFax;
    }

    public void setInspectionOrgFax(String inspectionOrgFax) {
        this.inspectionOrgFax = inspectionOrgFax;
    }

    public String getInspectionOrgEmail() {
        return inspectionOrgEmail;
    }

    public void setInspectionOrgEmail(String inspectionOrgEmail) {
        this.inspectionOrgEmail = inspectionOrgEmail;
    }

    public String getPlaceOfInspection() {
        return placeOfInspection;
    }

    public void setPlaceOfInspection(String placeOfInspection) {
        this.placeOfInspection = placeOfInspection;
    }

    public String getDateOfInspection() {
        return dateOfInspection;
    }

    public void setDateOfInspection(String dateOfInspection) {
        this.dateOfInspection = dateOfInspection;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantAddress() {
        return applicantAddress;
    }

    public void setApplicantAddress(String applicantAddress) {
        this.applicantAddress = applicantAddress;
    }

    public String getApplicantTel() {
        return applicantTel;
    }

    public void setApplicantTel(String applicantTel) {
        this.applicantTel = applicantTel;
    }

    public String getApplicantFax() {
        return applicantFax;
    }

    public void setApplicantFax(String applicantFax) {
        this.applicantFax = applicantFax;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
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

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getManufactureGrade() {
        return manufactureGrade;
    }

    public void setManufactureGrade(String manufactureGrade) {
        this.manufactureGrade = manufactureGrade;
    }

    public String getPreshipAuctionGrade() {
        return preshipAuctionGrade;
    }

    public void setPreshipAuctionGrade(String preshipAuctionGrade) {
        this.preshipAuctionGrade = preshipAuctionGrade;
    }

    public String getBodyColour() {
        return bodyColour;
    }

    public void setBodyColour(String bodyColour) {
        this.bodyColour = bodyColour;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getFirstRegistration() {
        return firstRegistration;
    }

    public void setFirstRegistration(String firstRegistration) {
        this.firstRegistration = firstRegistration;
    }

    public String getInspectionMileage() {
        return inspectionMileage;
    }

    public void setInspectionMileage(String inspectionMileage) {
        this.inspectionMileage = inspectionMileage;
    }

    public String getEngineCapacity() {
        return engineCapacity;
    }

    public void setEngineCapacity(String engineCapacity) {
        this.engineCapacity = engineCapacity;
    }

    public String getEngineNo() {
        return engineNo;
    }

    public void setEngineNo(String engineNo) {
        this.engineNo = engineNo;
    }

    public String getDrivingSystem() {
        return drivingSystem;
    }

    public void setDrivingSystem(String drivingSystem) {
        this.drivingSystem = drivingSystem;
    }

    public String getAccidentMarksOnChassis() {
        return accidentMarksOnChassis;
    }

    public void setAccidentMarksOnChassis(String accidentMarksOnChassis) {
        this.accidentMarksOnChassis = accidentMarksOnChassis;
    }

    public String getChassisCondition() {
        return chassisCondition;
    }

    public void setChassisCondition(String chassisCondition) {
        this.chassisCondition = chassisCondition;
    }

    public String getFullModelNo() {
        return fullModelNo;
    }

    public void setFullModelNo(String fullModelNo) {
        this.fullModelNo = fullModelNo;
    }

    public String getYearOfManufacture() {
        return yearOfManufacture;
    }

    public void setYearOfManufacture(String yearOfManufacture) {
        this.yearOfManufacture = yearOfManufacture;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public String getDocumentOriginalName() {
        return documentOriginalName;
    }

    public void setDocumentOriginalName(String documentOriginalName) {
        this.documentOriginalName = documentOriginalName;
    }

    public String getDocumentStoredName() {
        return documentStoredName;
    }

    public void setDocumentStoredName(String documentStoredName) {
        this.documentStoredName = documentStoredName;
    }

    public String getDocumentContentType() {
        return documentContentType;
    }

    public void setDocumentContentType(String documentContentType) {
        this.documentContentType = documentContentType;
    }
}
