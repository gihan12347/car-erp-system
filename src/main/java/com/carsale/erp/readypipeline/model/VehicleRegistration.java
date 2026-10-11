package com.carsale.erp.readypipeline.model;

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

}
