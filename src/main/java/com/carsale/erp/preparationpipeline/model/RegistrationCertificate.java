package com.carsale.erp.preparationpipeline.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

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

    @Column(length = 255)
    private String documentOriginalName;

    @Column(length = 120)
    private String documentStoredName;

    @Column(length = 80)
    private String documentContentType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    public String getChassisNo() { return chassisNo; }
    public void setChassisNo(String chassisNo) { this.chassisNo = chassisNo; }

    public String getAbsoluteOwner() { return absoluteOwner; }
    public void setAbsoluteOwner(String absoluteOwner) { this.absoluteOwner = absoluteOwner; }

    public String getClassOfVehicle() { return classOfVehicle; }
    public void setClassOfVehicle(String classOfVehicle) { this.classOfVehicle = classOfVehicle; }

    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }

    public String getConditionsSpecialNotes() { return conditionsSpecialNotes; }
    public void setConditionsSpecialNotes(String conditionsSpecialNotes) { this.conditionsSpecialNotes = conditionsSpecialNotes; }

    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public String getCrNo() { return crNo; }
    public void setCrNo(String crNo) { this.crNo = crNo; }

    public String getCurrentOwnerAddressIdNo() { return currentOwnerAddressIdNo; }
    public void setCurrentOwnerAddressIdNo(String currentOwnerAddressIdNo) { this.currentOwnerAddressIdNo = currentOwnerAddressIdNo; }

    public String getCylinderCapacity() { return cylinderCapacity; }
    public void setCylinderCapacity(String cylinderCapacity) { this.cylinderCapacity = cylinderCapacity; }

    public String getDateOfFirstRegistration() { return dateOfFirstRegistration; }
    public void setDateOfFirstRegistration(String dateOfFirstRegistration) { this.dateOfFirstRegistration = dateOfFirstRegistration; }

    public String getEngineNo() { return engineNo; }
    public void setEngineNo(String engineNo) { this.engineNo = engineNo; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getLength() { return length; }
    public void setLength(String length) { this.length = length; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getManufacturesDescription() { return manufacturesDescription; }
    public void setManufacturesDescription(String manufacturesDescription) { this.manufacturesDescription = manufacturesDescription; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getOverHang() { return overHang; }
    public void setOverHang(String overHang) { this.overHang = overHang; }

    public String getPreviousOwners() { return previousOwners; }
    public void setPreviousOwners(String previousOwners) { this.previousOwners = previousOwners; }

    public String getPrintedDate() { return printedDate; }
    public void setPrintedDate(String printedDate) { this.printedDate = printedDate; }

    public String getProvincialCouncil() { return provincialCouncil; }
    public void setProvincialCouncil(String provincialCouncil) { this.provincialCouncil = provincialCouncil; }

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public String getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(String seatingCapacity) { this.seatingCapacity = seatingCapacity; }

    public String getStatusWhenRegistered() { return statusWhenRegistered; }
    public void setStatusWhenRegistered(String statusWhenRegistered) { this.statusWhenRegistered = statusWhenRegistered; }

    public String getTaxationClass() { return taxationClass; }
    public void setTaxationClass(String taxationClass) { this.taxationClass = taxationClass; }

    public String getTaxesPayable() { return taxesPayable; }
    public void setTaxesPayable(String taxesPayable) { this.taxesPayable = taxesPayable; }

    public String getTypeOfBody() { return typeOfBody; }
    public void setTypeOfBody(String typeOfBody) { this.typeOfBody = typeOfBody; }

    public String getTyreSizeDual() { return tyreSizeDual; }
    public void setTyreSizeDual(String tyreSizeDual) { this.tyreSizeDual = tyreSizeDual; }

    public String getTyreSizeFront() { return tyreSizeFront; }
    public void setTyreSizeFront(String tyreSizeFront) { this.tyreSizeFront = tyreSizeFront; }

    public String getTyreSizeRear() { return tyreSizeRear; }
    public void setTyreSizeRear(String tyreSizeRear) { this.tyreSizeRear = tyreSizeRear; }

    public String getTyreSizeSingle() { return tyreSizeSingle; }
    public void setTyreSizeSingle(String tyreSizeSingle) { this.tyreSizeSingle = tyreSizeSingle; }

    public String getWeightGross() { return weightGross; }
    public void setWeightGross(String weightGross) { this.weightGross = weightGross; }

    public String getWeightUnladen() { return weightUnladen; }
    public void setWeightUnladen(String weightUnladen) { this.weightUnladen = weightUnladen; }

    public String getWheelBase() { return wheelBase; }
    public void setWheelBase(String wheelBase) { this.wheelBase = wheelBase; }

    public String getWidth() { return width; }
    public void setWidth(String width) { this.width = width; }

    public String getYearOfManufacture() { return yearOfManufacture; }
    public void setYearOfManufacture(String yearOfManufacture) { this.yearOfManufacture = yearOfManufacture; }

    public String getMileage() { return mileage; }
    public void setMileage(String mileage) { this.mileage = mileage; }

    public String getDocumentOriginalName() { return documentOriginalName; }
    public void setDocumentOriginalName(String documentOriginalName) { this.documentOriginalName = documentOriginalName; }

    public String getDocumentStoredName() { return documentStoredName; }
    public void setDocumentStoredName(String documentStoredName) { this.documentStoredName = documentStoredName; }

    public String getDocumentContentType() { return documentContentType; }
    public void setDocumentContentType(String documentContentType) { this.documentContentType = documentContentType; }

    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }

}
