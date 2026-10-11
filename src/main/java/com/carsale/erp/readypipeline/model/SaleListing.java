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
@Table(name = "sale_listings")
public class SaleListing {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 40)
    private String askingPrice;

    @Column(length = 40)
    private String advertisedPrice;

    @Column(length = 40)
    private String saleCondition;

    @Column(length = 40)
    private String listedOn;

    @Column(name = "sale_code", length = 40)
    private String saleCode;

    @Column(name = "sale_location", length = 80)
    private String saleLocation;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String listingNotes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean listed;

    @Column(nullable = false)
    private boolean sold;

    @Column(name = "assignment_complete", nullable = false)
    private boolean assignmentComplete;

}
