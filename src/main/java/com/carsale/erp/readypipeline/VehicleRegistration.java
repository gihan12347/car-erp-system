package com.carsale.erp.readypipeline;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "vehicle_registrations")
public class VehicleRegistration {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(name = "registration_no", length = 40)
    private String registrationNo;

    @Column(name = "registered_owner", length = 120)
    private String registeredOwner;

    @Column(name = "registered_date", length = 40)
    private String registeredDate;

    @Column(name = "rmv_office", length = 80)
    private String rmvOffice;

    @Column(name = "file_no", length = 40)
    private String fileNo;

    @Column(name = "revenue_license_no", length = 40)
    private String revenueLicenseNo;

    @Column(name = "revenue_license_expiry", length = 40)
    private String revenueLicenseExpiry;

    @Column(name = "insurance_company", length = 80)
    private String insuranceCompany;

    @Column(name = "insurance_policy_no", length = 40)
    private String insurancePolicyNo;

    @Column(name = "insurance_expiry", length = 40)
    private String insuranceExpiry;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean completed;

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getRegisteredOwner() {
        return registeredOwner;
    }

    public void setRegisteredOwner(String registeredOwner) {
        this.registeredOwner = registeredOwner;
    }

    public String getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }

    public String getRmvOffice() {
        return rmvOffice;
    }

    public void setRmvOffice(String rmvOffice) {
        this.rmvOffice = rmvOffice;
    }

    public String getFileNo() {
        return fileNo;
    }

    public void setFileNo(String fileNo) {
        this.fileNo = fileNo;
    }

    public String getRevenueLicenseNo() {
        return revenueLicenseNo;
    }

    public void setRevenueLicenseNo(String revenueLicenseNo) {
        this.revenueLicenseNo = revenueLicenseNo;
    }

    public String getRevenueLicenseExpiry() {
        return revenueLicenseExpiry;
    }

    public void setRevenueLicenseExpiry(String revenueLicenseExpiry) {
        this.revenueLicenseExpiry = revenueLicenseExpiry;
    }

    public String getInsuranceCompany() {
        return insuranceCompany;
    }

    public void setInsuranceCompany(String insuranceCompany) {
        this.insuranceCompany = insuranceCompany;
    }

    public String getInsurancePolicyNo() {
        return insurancePolicyNo;
    }

    public void setInsurancePolicyNo(String insurancePolicyNo) {
        this.insurancePolicyNo = insurancePolicyNo;
    }

    public String getInsuranceExpiry() {
        return insuranceExpiry;
    }

    public void setInsuranceExpiry(String insuranceExpiry) {
        this.insuranceExpiry = insuranceExpiry;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
