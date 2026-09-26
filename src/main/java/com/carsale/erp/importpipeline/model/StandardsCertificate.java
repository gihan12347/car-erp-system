package com.carsale.erp.importpipeline.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "standards_certificates")
public class StandardsCertificate {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 8)
    private String scheduleType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionCo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionNmhc;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionNox;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionPm;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionHc;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionHcNox;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionThc;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionCh4;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emissionSmoke;

    @Column(name = "three_point_seat_belts", nullable = false)
    private boolean threePointSeatBelts;

    @Column(name = "two_point_seat_belts", nullable = false)
    private boolean twoPointSeatBelts;

    @Column(name = "driver_airbag", nullable = false)
    private boolean driverAirbag;

    @Column(name = "passenger_airbag", nullable = false)
    private boolean passengerAirbag;

    @Column(name = "abs_fitted", nullable = false)
    private boolean absFitted;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String make;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String model;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String chassisVin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String placeOfInspection;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String inspectionDate;

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

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getEmissionCo() {
        return emissionCo;
    }

    public void setEmissionCo(String emissionCo) {
        this.emissionCo = emissionCo;
    }

    public String getEmissionNmhc() {
        return emissionNmhc;
    }

    public void setEmissionNmhc(String emissionNmhc) {
        this.emissionNmhc = emissionNmhc;
    }

    public String getEmissionNox() {
        return emissionNox;
    }

    public void setEmissionNox(String emissionNox) {
        this.emissionNox = emissionNox;
    }

    public String getEmissionPm() {
        return emissionPm;
    }

    public void setEmissionPm(String emissionPm) {
        this.emissionPm = emissionPm;
    }

    public String getEmissionHc() {
        return emissionHc;
    }

    public void setEmissionHc(String emissionHc) {
        this.emissionHc = emissionHc;
    }

    public String getEmissionHcNox() {
        return emissionHcNox;
    }

    public void setEmissionHcNox(String emissionHcNox) {
        this.emissionHcNox = emissionHcNox;
    }

    public String getEmissionThc() {
        return emissionThc;
    }

    public void setEmissionThc(String emissionThc) {
        this.emissionThc = emissionThc;
    }

    public String getEmissionCh4() {
        return emissionCh4;
    }

    public void setEmissionCh4(String emissionCh4) {
        this.emissionCh4 = emissionCh4;
    }

    public String getEmissionSmoke() {
        return emissionSmoke;
    }

    public void setEmissionSmoke(String emissionSmoke) {
        this.emissionSmoke = emissionSmoke;
    }

    public boolean isThreePointSeatBelts() {
        return threePointSeatBelts;
    }

    public void setThreePointSeatBelts(boolean threePointSeatBelts) {
        this.threePointSeatBelts = threePointSeatBelts;
    }

    public boolean isTwoPointSeatBelts() {
        return twoPointSeatBelts;
    }

    public void setTwoPointSeatBelts(boolean twoPointSeatBelts) {
        this.twoPointSeatBelts = twoPointSeatBelts;
    }

    public boolean isDriverAirbag() {
        return driverAirbag;
    }

    public void setDriverAirbag(boolean driverAirbag) {
        this.driverAirbag = driverAirbag;
    }

    public boolean isPassengerAirbag() {
        return passengerAirbag;
    }

    public void setPassengerAirbag(boolean passengerAirbag) {
        this.passengerAirbag = passengerAirbag;
    }

    public boolean isAbsFitted() {
        return absFitted;
    }

    public void setAbsFitted(boolean absFitted) {
        this.absFitted = absFitted;
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

    public String getChassisVin() {
        return chassisVin;
    }

    public void setChassisVin(String chassisVin) {
        this.chassisVin = chassisVin;
    }

    public String getPlaceOfInspection() {
        return placeOfInspection;
    }

    public void setPlaceOfInspection(String placeOfInspection) {
        this.placeOfInspection = placeOfInspection;
    }

    public String getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(String inspectionDate) {
        this.inspectionDate = inspectionDate;
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
