package com.carsale.erp.importpipeline.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Setter
@Getter
@Entity
@Table(name = "vehicle_photos")
public class VehiclePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column()
    private String originalName;

    @Column(length = 120)
    private String storedName;

    @Column(length = 80)
    private String contentType;

}
