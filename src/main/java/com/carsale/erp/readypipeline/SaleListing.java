package com.carsale.erp.readypipeline;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

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

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getAskingPrice() {
        return askingPrice;
    }

    public void setAskingPrice(String askingPrice) {
        this.askingPrice = askingPrice;
    }

    public String getAdvertisedPrice() {
        return advertisedPrice;
    }

    public void setAdvertisedPrice(String advertisedPrice) {
        this.advertisedPrice = advertisedPrice;
    }

    public String getSaleCondition() {
        return saleCondition;
    }

    public void setSaleCondition(String saleCondition) {
        this.saleCondition = saleCondition;
    }

    public String getListedOn() {
        return listedOn;
    }

    public void setListedOn(String listedOn) {
        this.listedOn = listedOn;
    }

    public String getSaleCode() {
        return saleCode;
    }

    public void setSaleCode(String saleCode) {
        this.saleCode = saleCode;
    }

    public String getSaleLocation() {
        return saleLocation;
    }

    public void setSaleLocation(String saleLocation) {
        this.saleLocation = saleLocation;
    }

    public String getListingNotes() {
        return listingNotes;
    }

    public void setListingNotes(String listingNotes) {
        this.listingNotes = listingNotes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isListed() {
        return listed;
    }

    public void setListed(boolean listed) {
        this.listed = listed;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public boolean isAssignmentComplete() {
        return assignmentComplete;
    }

    public void setAssignmentComplete(boolean assignmentComplete) {
        this.assignmentComplete = assignmentComplete;
    }
}
