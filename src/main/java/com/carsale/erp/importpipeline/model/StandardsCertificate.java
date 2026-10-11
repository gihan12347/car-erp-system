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
@Table(name = "standards_certificates")
public class StandardsCertificate {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 8)
    private String scheduleType;

    @Lob
    @Column(name = "certificate_no", columnDefinition = "TEXT")
    private String certificateNo;

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
