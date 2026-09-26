package com.carsale.erp.importpipeline.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

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

    @Column(length = 255)
    private String documentOriginalName;

    @Column(length = 120)
    private String documentStoredName;

    @Column(length = 80)
    private String documentContentType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getCertificateNo() {
        return certificateNo;
    }

    public void setCertificateNo(String certificateNo) {
        this.certificateNo = certificateNo;
    }

    public String getArrangementNo() {
        return arrangementNo;
    }

    public void setArrangementNo(String arrangementNo) {
        this.arrangementNo = arrangementNo;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getFirstRegDate() {
        return firstRegDate;
    }

    public void setFirstRegDate(String firstRegDate) {
        this.firstRegDate = firstRegDate;
    }

    public String getChassisVin() {
        return chassisVin;
    }

    public void setChassisVin(String chassisVin) {
        this.chassisVin = chassisVin;
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

    public String getEngineModel() {
        return engineModel;
    }

    public void setEngineModel(String engineModel) {
        this.engineModel = engineModel;
    }

    public String getVehicleClassification() {
        return vehicleClassification;
    }

    public void setVehicleClassification(String vehicleClassification) {
        this.vehicleClassification = vehicleClassification;
    }

    public String getUseType() {
        return useType;
    }

    public void setUseType(String useType) {
        this.useType = useType;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(String seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public String getMaxCarry() {
        return maxCarry;
    }

    public void setMaxCarry(String maxCarry) {
        this.maxCarry = maxCarry;
    }

    public String getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(String weightKg) {
        this.weightKg = weightKg;
    }

    public String getGrossWeightKg() {
        return grossWeightKg;
    }

    public void setGrossWeightKg(String grossWeightKg) {
        this.grossWeightKg = grossWeightKg;
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

    public String getEngineCapacity() {
        return engineCapacity;
    }

    public void setEngineCapacity(String engineCapacity) {
        this.engineCapacity = engineCapacity;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getSpecificationNo() {
        return specificationNo;
    }

    public void setSpecificationNo(String specificationNo) {
        this.specificationNo = specificationNo;
    }

    public String getClassificationNo() {
        return classificationNo;
    }

    public void setClassificationNo(String classificationNo) {
        this.classificationNo = classificationNo;
    }

    public String getFrontAxleWeight() {
        return frontAxleWeight;
    }

    public void setFrontAxleWeight(String frontAxleWeight) {
        this.frontAxleWeight = frontAxleWeight;
    }

    public String getRearAxleWeight() {
        return rearAxleWeight;
    }

    public void setRearAxleWeight(String rearAxleWeight) {
        this.rearAxleWeight = rearAxleWeight;
    }

    public String getFrWeight() {
        return frWeight;
    }

    public void setFrWeight(String frWeight) {
        this.frWeight = frWeight;
    }

    public String getRfWeight() {
        return rfWeight;
    }

    public void setRfWeight(String rfWeight) {
        this.rfWeight = rfWeight;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }

    public void setOwnerAddress(String ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    public String getLocalityOfUse() {
        return localityOfUse;
    }

    public void setLocalityOfUse(String localityOfUse) {
        this.localityOfUse = localityOfUse;
    }

    public String getExportScheduledDate() {
        return exportScheduledDate;
    }

    public void setExportScheduledDate(String exportScheduledDate) {
        this.exportScheduledDate = exportScheduledDate;
    }

    public String getDirectorGeneralLandTransportBranch() {
        return directorGeneralLandTransportBranch;
    }

    public void setDirectorGeneralLandTransportBranch(String directorGeneralLandTransportBranch) {
        this.directorGeneralLandTransportBranch = directorGeneralLandTransportBranch;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }
}
