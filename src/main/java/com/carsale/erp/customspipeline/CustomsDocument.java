package com.carsale.erp.customspipeline;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

@Entity(name = "CustomsDocument")
@Table(name = "clearance_documents")
@SecondaryTables({
        @SecondaryTable(
                name = "clearance_assessment_notices",
                pkJoinColumns = @PrimaryKeyJoinColumn(name = "chassis_no")
        ),
        @SecondaryTable(
                name = "clearance_jevic_inspections",
                pkJoinColumns = @PrimaryKeyJoinColumn(name = "chassis_no")
        ),
        @SecondaryTable(
                name = "clearance_working_sheets",
                pkJoinColumns = @PrimaryKeyJoinColumn(name = "chassis_no")
        ),
        @SecondaryTable(
                name = "clearance_bills_of_lading",
                pkJoinColumns = @PrimaryKeyJoinColumn(name = "chassis_no")
        )
})
@org.hibernate.annotations.Tables({
        @org.hibernate.annotations.Table(appliesTo = "clearance_assessment_notices", optional = true),
        @org.hibernate.annotations.Table(appliesTo = "clearance_jevic_inspections", optional = true),
        @org.hibernate.annotations.Table(appliesTo = "clearance_working_sheets", optional = true),
        @org.hibernate.annotations.Table(appliesTo = "clearance_bills_of_lading", optional = true)
})
public class CustomsDocument {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    /* Page 1 — JEVIC certificate of inspection */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicCertificateNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicChassisVin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicMake;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicModel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicLocation;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicInspectionDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicIssueDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicCurrentOdometer;

    /* COI extras live on clearance_jevic_inspections — clearance_documents is at InnoDB row-size limit */
    @Lob
    @Column(table = "clearance_jevic_inspections", columnDefinition = "TEXT")
    private String jevicEngineCapacity;

    @Lob
    @Column(table = "clearance_jevic_inspections", columnDefinition = "TEXT")
    private String jevicFirstRegistration;

    @Lob
    @Column(table = "clearance_jevic_inspections", columnDefinition = "TEXT")
    private String jevicEngineNo;

    @Lob
    @Column(table = "clearance_jevic_inspections", columnDefinition = "TEXT")
    private String jevicRemarks;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicAuctionReading;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicAuctionReadingDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicDealerReading;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicDealerReadingDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicDeregistrationReading;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicDeregistrationReadingDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicAuthorizedBy;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicOrgAddress;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicTel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicWebsite;

    @Column(length = 255)
    private String page1OriginalName;

    @Column(length = 120)
    private String page1StoredName;

    @Column(length = 80)
    private String page1ContentType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrTextPage1;

    /* Page 2 — Sri Lanka customs declaration (CUSDEC) */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String customsReference;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarationType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarationPages;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarationLists;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarationItems;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String totalPackages;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exporterName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exporterAddress;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String consigneeName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String consigneeAddress;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String consigneeTin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarantName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarantAddress;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarantTin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String countryLastConsignment;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tradingCountry;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String countryExport;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String countryDestination;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String countryOrigin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String vesselFlight;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String deliveryTerms;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String voyageNoDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String placeLoadingDischarging;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String currencyInvoiced;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String totalAmountInvoiced;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exchangeRate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String paymentTerms;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bankCode;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bankName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bankBranch;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bankReference;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String locationOfGoods;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String hsCode;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String grossMassKg;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String netMassKg;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String blAwbNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String goodsDescription;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String modelSpec;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String yearOfManufacture;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String dateOfRegistration;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String clearanceChassisNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String clearanceEngineNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String itemPrice;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String valueNcy;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String engineCapacityCc;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String taxCid;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String taxSur;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String taxVat;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String taxXid;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String taxVel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String taxOther;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String totalTaxAmount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String invoiceFob;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String invoiceFreight;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String invoiceInsurance;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String invoiceOther;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String invoiceTotal;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarantDate;

    @Column(length = 255)
    private String page2OriginalName;

    @Column(length = 120)
    private String page2StoredName;

    @Column(length = 80)
    private String page2ContentType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrTextPage2;

    /* Page 3 — ASYCUDA assessment notice (own table: clearance_documents is at InnoDB row-size limit) */
    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentOffice;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentNoticeRef;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentModel;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentCustomsReference;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentDeclarantReference;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentReference;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentPackages;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentDeclarantId;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentDeclarantName;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentDeclarantAddress;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentDeclarantChaExp;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentConsigneeId;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentConsigneeName;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentConsigneeAddress;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTaxOtc;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTaxCom;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTaxExm;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTaxCid;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTaxSur;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTaxXid;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTaxVat;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTaxVel;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTotalAssessed;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String assessmentTotalPaid;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String page3OriginalName;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String page3StoredName;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String page3ContentType;

    @Lob
    @Column(table = "clearance_assessment_notices", columnDefinition = "TEXT")
    private String ocrTextPage3;

    /* Page 4 — Working sheet for motor vehicles */
    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetRef;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetHsCode;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetVehicleType;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetReferenceNo;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetVesselName;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetChassisNo;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetAgentsFob;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetInvoicedFob;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetAgentsFreight;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetInvoicedFreight;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetAgentsInsurance;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetInvoicedInsurance;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetOptionsValue;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetBlFreightCalc;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetBlFreightAmount;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetBlDate;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetManufactureDate;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetAgeDifference;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFirstRegistrationDate;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetWebsiteValue;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetLocalTaxes;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFifteenPercent;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFobValue85;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetLcNo;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetLcAmount;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetLcBank;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetLcImporter;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetLcIssueDate;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetLcExpiryDate;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetLcAmendmentDate;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetClearingAgent;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFiscalFob;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFiscalFreight;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFiscalInsurance;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFiscalOptions;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFiscalTotal;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String page4OriginalName;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String page4StoredName;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String page4ContentType;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String ocrTextPage4;

    /* Page 5 — Bill of lading (own table: clearance_documents is at InnoDB row-size limit) */
    @Lob
    @Column(name = "bl_no", table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String blNo;

    @Lob
    @Column(name = "date_of_bl_issue", table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String dateOfBlIssue;

    @Lob
    @Column(name = "landing_cost_usd", table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String landingCostUsd;

    @Lob
    @Column(name = "bl_exchange_rate", table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String blExchangeRate;

    @Lob
    @Column(name = "landing_cost_lkr", table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String landingCostLkr;

    @Lob
    @Column(table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String page5OriginalName;

    @Lob
    @Column(table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String page5StoredName;

    @Lob
    @Column(table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String page5ContentType;

    @Lob
    @Column(table = "clearance_bills_of_lading", columnDefinition = "TEXT")
    private String ocrTextPage5;

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getJevicCertificateNo() {
        return jevicCertificateNo;
    }

    public void setJevicCertificateNo(String jevicCertificateNo) {
        this.jevicCertificateNo = jevicCertificateNo;
    }

    public String getJevicChassisVin() {
        return jevicChassisVin;
    }

    public void setJevicChassisVin(String jevicChassisVin) {
        this.jevicChassisVin = jevicChassisVin;
    }

    public String getJevicMake() {
        return jevicMake;
    }

    public void setJevicMake(String jevicMake) {
        this.jevicMake = jevicMake;
    }

    public String getJevicModel() {
        return jevicModel;
    }

    public void setJevicModel(String jevicModel) {
        this.jevicModel = jevicModel;
    }

    public String getJevicLocation() {
        return jevicLocation;
    }

    public void setJevicLocation(String jevicLocation) {
        this.jevicLocation = jevicLocation;
    }

    public String getJevicInspectionDate() {
        return jevicInspectionDate;
    }

    public void setJevicInspectionDate(String jevicInspectionDate) {
        this.jevicInspectionDate = jevicInspectionDate;
    }

    public String getJevicIssueDate() {
        return jevicIssueDate;
    }

    public void setJevicIssueDate(String jevicIssueDate) {
        this.jevicIssueDate = jevicIssueDate;
    }

    public String getJevicCurrentOdometer() {
        return jevicCurrentOdometer;
    }

    public void setJevicCurrentOdometer(String jevicCurrentOdometer) {
        this.jevicCurrentOdometer = jevicCurrentOdometer;
    }

    public String getJevicEngineCapacity() {
        return jevicEngineCapacity;
    }

    public void setJevicEngineCapacity(String jevicEngineCapacity) {
        this.jevicEngineCapacity = jevicEngineCapacity;
    }

    public String getJevicFirstRegistration() {
        return jevicFirstRegistration;
    }

    public void setJevicFirstRegistration(String jevicFirstRegistration) {
        this.jevicFirstRegistration = jevicFirstRegistration;
    }

    public String getJevicEngineNo() {
        return jevicEngineNo;
    }

    public void setJevicEngineNo(String jevicEngineNo) {
        this.jevicEngineNo = jevicEngineNo;
    }

    public String getJevicRemarks() {
        return jevicRemarks;
    }

    public void setJevicRemarks(String jevicRemarks) {
        this.jevicRemarks = jevicRemarks;
    }

    public String getJevicAuctionReading() {
        return jevicAuctionReading;
    }

    public void setJevicAuctionReading(String jevicAuctionReading) {
        this.jevicAuctionReading = jevicAuctionReading;
    }

    public String getJevicAuctionReadingDate() {
        return jevicAuctionReadingDate;
    }

    public void setJevicAuctionReadingDate(String jevicAuctionReadingDate) {
        this.jevicAuctionReadingDate = jevicAuctionReadingDate;
    }

    public String getJevicDealerReading() {
        return jevicDealerReading;
    }

    public void setJevicDealerReading(String jevicDealerReading) {
        this.jevicDealerReading = jevicDealerReading;
    }

    public String getJevicDealerReadingDate() {
        return jevicDealerReadingDate;
    }

    public void setJevicDealerReadingDate(String jevicDealerReadingDate) {
        this.jevicDealerReadingDate = jevicDealerReadingDate;
    }

    public String getJevicDeregistrationReading() {
        return jevicDeregistrationReading;
    }

    public void setJevicDeregistrationReading(String jevicDeregistrationReading) {
        this.jevicDeregistrationReading = jevicDeregistrationReading;
    }

    public String getJevicDeregistrationReadingDate() {
        return jevicDeregistrationReadingDate;
    }

    public void setJevicDeregistrationReadingDate(String jevicDeregistrationReadingDate) {
        this.jevicDeregistrationReadingDate = jevicDeregistrationReadingDate;
    }

    public String getJevicAuthorizedBy() {
        return jevicAuthorizedBy;
    }

    public void setJevicAuthorizedBy(String jevicAuthorizedBy) {
        this.jevicAuthorizedBy = jevicAuthorizedBy;
    }

    public String getJevicOrgAddress() {
        return jevicOrgAddress;
    }

    public void setJevicOrgAddress(String jevicOrgAddress) {
        this.jevicOrgAddress = jevicOrgAddress;
    }

    public String getJevicTel() {
        return jevicTel;
    }

    public void setJevicTel(String jevicTel) {
        this.jevicTel = jevicTel;
    }

    public String getJevicWebsite() {
        return jevicWebsite;
    }

    public void setJevicWebsite(String jevicWebsite) {
        this.jevicWebsite = jevicWebsite;
    }

    public String getPage1OriginalName() {
        return page1OriginalName;
    }

    public void setPage1OriginalName(String page1OriginalName) {
        this.page1OriginalName = page1OriginalName;
    }

    public String getPage1StoredName() {
        return page1StoredName;
    }

    public void setPage1StoredName(String page1StoredName) {
        this.page1StoredName = page1StoredName;
    }

    public String getPage1ContentType() {
        return page1ContentType;
    }

    public void setPage1ContentType(String page1ContentType) {
        this.page1ContentType = page1ContentType;
    }

    public String getOcrTextPage1() {
        return ocrTextPage1;
    }

    public void setOcrTextPage1(String ocrTextPage1) {
        this.ocrTextPage1 = ocrTextPage1;
    }

    public String getCustomsReference() {
        return customsReference;
    }

    public void setCustomsReference(String customsReference) {
        this.customsReference = customsReference;
    }

    public String getDeclarationType() {
        return declarationType;
    }

    public void setDeclarationType(String declarationType) {
        this.declarationType = declarationType;
    }

    public String getDeclarationPages() {
        return declarationPages;
    }

    public void setDeclarationPages(String declarationPages) {
        this.declarationPages = declarationPages;
    }

    public String getDeclarationLists() {
        return declarationLists;
    }

    public void setDeclarationLists(String declarationLists) {
        this.declarationLists = declarationLists;
    }

    public String getDeclarationItems() {
        return declarationItems;
    }

    public void setDeclarationItems(String declarationItems) {
        this.declarationItems = declarationItems;
    }

    public String getTotalPackages() {
        return totalPackages;
    }

    public void setTotalPackages(String totalPackages) {
        this.totalPackages = totalPackages;
    }

    public String getExporterName() {
        return exporterName;
    }

    public void setExporterName(String exporterName) {
        this.exporterName = exporterName;
    }

    public String getExporterAddress() {
        return exporterAddress;
    }

    public void setExporterAddress(String exporterAddress) {
        this.exporterAddress = exporterAddress;
    }

    public String getConsigneeName() {
        return consigneeName;
    }

    public void setConsigneeName(String consigneeName) {
        this.consigneeName = consigneeName;
    }

    public String getConsigneeAddress() {
        return consigneeAddress;
    }

    public void setConsigneeAddress(String consigneeAddress) {
        this.consigneeAddress = consigneeAddress;
    }

    public String getConsigneeTin() {
        return consigneeTin;
    }

    public void setConsigneeTin(String consigneeTin) {
        this.consigneeTin = consigneeTin;
    }

    public String getDeclarantName() {
        return declarantName;
    }

    public void setDeclarantName(String declarantName) {
        this.declarantName = declarantName;
    }

    public String getDeclarantAddress() {
        return declarantAddress;
    }

    public void setDeclarantAddress(String declarantAddress) {
        this.declarantAddress = declarantAddress;
    }

    public String getDeclarantTin() {
        return declarantTin;
    }

    public void setDeclarantTin(String declarantTin) {
        this.declarantTin = declarantTin;
    }

    public String getCountryLastConsignment() {
        return countryLastConsignment;
    }

    public void setCountryLastConsignment(String countryLastConsignment) {
        this.countryLastConsignment = countryLastConsignment;
    }

    public String getTradingCountry() {
        return tradingCountry;
    }

    public void setTradingCountry(String tradingCountry) {
        this.tradingCountry = tradingCountry;
    }

    public String getCountryExport() {
        return countryExport;
    }

    public void setCountryExport(String countryExport) {
        this.countryExport = countryExport;
    }

    public String getCountryDestination() {
        return countryDestination;
    }

    public void setCountryDestination(String countryDestination) {
        this.countryDestination = countryDestination;
    }

    public String getCountryOrigin() {
        return countryOrigin;
    }

    public void setCountryOrigin(String countryOrigin) {
        this.countryOrigin = countryOrigin;
    }

    public String getVesselFlight() {
        return vesselFlight;
    }

    public void setVesselFlight(String vesselFlight) {
        this.vesselFlight = vesselFlight;
    }

    public String getDeliveryTerms() {
        return deliveryTerms;
    }

    public void setDeliveryTerms(String deliveryTerms) {
        this.deliveryTerms = deliveryTerms;
    }

    public String getVoyageNoDate() {
        return voyageNoDate;
    }

    public void setVoyageNoDate(String voyageNoDate) {
        this.voyageNoDate = voyageNoDate;
    }

    public String getPlaceLoadingDischarging() {
        return placeLoadingDischarging;
    }

    public void setPlaceLoadingDischarging(String placeLoadingDischarging) {
        this.placeLoadingDischarging = placeLoadingDischarging;
    }

    public String getCurrencyInvoiced() {
        return currencyInvoiced;
    }

    public void setCurrencyInvoiced(String currencyInvoiced) {
        this.currencyInvoiced = currencyInvoiced;
    }

    public String getTotalAmountInvoiced() {
        return totalAmountInvoiced;
    }

    public void setTotalAmountInvoiced(String totalAmountInvoiced) {
        this.totalAmountInvoiced = totalAmountInvoiced;
    }

    public String getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(String exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public String getLocationOfGoods() {
        return locationOfGoods;
    }

    public void setLocationOfGoods(String locationOfGoods) {
        this.locationOfGoods = locationOfGoods;
    }

    public String getHsCode() {
        return hsCode;
    }

    public void setHsCode(String hsCode) {
        this.hsCode = hsCode;
    }

    public String getGrossMassKg() {
        return grossMassKg;
    }

    public void setGrossMassKg(String grossMassKg) {
        this.grossMassKg = grossMassKg;
    }

    public String getNetMassKg() {
        return netMassKg;
    }

    public void setNetMassKg(String netMassKg) {
        this.netMassKg = netMassKg;
    }

    public String getBlAwbNo() {
        return blAwbNo;
    }

    public void setBlAwbNo(String blAwbNo) {
        this.blAwbNo = blAwbNo;
    }

    public String getGoodsDescription() {
        return goodsDescription;
    }

    public void setGoodsDescription(String goodsDescription) {
        this.goodsDescription = goodsDescription;
    }

    public String getModelSpec() {
        return modelSpec;
    }

    public void setModelSpec(String modelSpec) {
        this.modelSpec = modelSpec;
    }

    public String getYearOfManufacture() {
        return yearOfManufacture;
    }

    public void setYearOfManufacture(String yearOfManufacture) {
        this.yearOfManufacture = yearOfManufacture;
    }

    public String getDateOfRegistration() {
        return dateOfRegistration;
    }

    public void setDateOfRegistration(String dateOfRegistration) {
        this.dateOfRegistration = dateOfRegistration;
    }

    public String getClearanceChassisNo() {
        return clearanceChassisNo;
    }

    public void setClearanceChassisNo(String clearanceChassisNo) {
        this.clearanceChassisNo = clearanceChassisNo;
    }

    public String getClearanceEngineNo() {
        return clearanceEngineNo;
    }

    public void setClearanceEngineNo(String clearanceEngineNo) {
        this.clearanceEngineNo = clearanceEngineNo;
    }

    public String getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(String itemPrice) {
        this.itemPrice = itemPrice;
    }

    public String getValueNcy() {
        return valueNcy;
    }

    public void setValueNcy(String valueNcy) {
        this.valueNcy = valueNcy;
    }

    public String getEngineCapacityCc() {
        return engineCapacityCc;
    }

    public void setEngineCapacityCc(String engineCapacityCc) {
        this.engineCapacityCc = engineCapacityCc;
    }

    public String getTaxCid() {
        return taxCid;
    }

    public void setTaxCid(String taxCid) {
        this.taxCid = taxCid;
    }

    public String getTaxSur() {
        return taxSur;
    }

    public void setTaxSur(String taxSur) {
        this.taxSur = taxSur;
    }

    public String getTaxVat() {
        return taxVat;
    }

    public void setTaxVat(String taxVat) {
        this.taxVat = taxVat;
    }

    public String getTaxXid() {
        return taxXid;
    }

    public void setTaxXid(String taxXid) {
        this.taxXid = taxXid;
    }

    public String getTaxVel() {
        return taxVel;
    }

    public void setTaxVel(String taxVel) {
        this.taxVel = taxVel;
    }

    public String getTaxOther() {
        return taxOther;
    }

    public void setTaxOther(String taxOther) {
        this.taxOther = taxOther;
    }

    public String getTotalTaxAmount() {
        return totalTaxAmount;
    }

    public void setTotalTaxAmount(String totalTaxAmount) {
        this.totalTaxAmount = totalTaxAmount;
    }

    public String getInvoiceFob() {
        return invoiceFob;
    }

    public void setInvoiceFob(String invoiceFob) {
        this.invoiceFob = invoiceFob;
    }

    public String getInvoiceFreight() {
        return invoiceFreight;
    }

    public void setInvoiceFreight(String invoiceFreight) {
        this.invoiceFreight = invoiceFreight;
    }

    public String getInvoiceInsurance() {
        return invoiceInsurance;
    }

    public void setInvoiceInsurance(String invoiceInsurance) {
        this.invoiceInsurance = invoiceInsurance;
    }

    public String getInvoiceOther() {
        return invoiceOther;
    }

    public void setInvoiceOther(String invoiceOther) {
        this.invoiceOther = invoiceOther;
    }

    public String getInvoiceTotal() {
        return invoiceTotal;
    }

    public void setInvoiceTotal(String invoiceTotal) {
        this.invoiceTotal = invoiceTotal;
    }

    public String getDeclarantDate() {
        return declarantDate;
    }

    public void setDeclarantDate(String declarantDate) {
        this.declarantDate = declarantDate;
    }

    public String getPage2OriginalName() {
        return page2OriginalName;
    }

    public void setPage2OriginalName(String page2OriginalName) {
        this.page2OriginalName = page2OriginalName;
    }

    public String getPage2StoredName() {
        return page2StoredName;
    }

    public void setPage2StoredName(String page2StoredName) {
        this.page2StoredName = page2StoredName;
    }

    public String getPage2ContentType() {
        return page2ContentType;
    }

    public void setPage2ContentType(String page2ContentType) {
        this.page2ContentType = page2ContentType;
    }

    public String getOcrTextPage2() {
        return ocrTextPage2;
    }

    public void setOcrTextPage2(String ocrTextPage2) {
        this.ocrTextPage2 = ocrTextPage2;
    }

    public String getAssessmentOffice() {
        return assessmentOffice;
    }

    public void setAssessmentOffice(String assessmentOffice) {
        this.assessmentOffice = assessmentOffice;
    }

    public String getAssessmentNoticeRef() {
        return assessmentNoticeRef;
    }

    public void setAssessmentNoticeRef(String assessmentNoticeRef) {
        this.assessmentNoticeRef = assessmentNoticeRef;
    }

    public String getAssessmentModel() {
        return assessmentModel;
    }

    public void setAssessmentModel(String assessmentModel) {
        this.assessmentModel = assessmentModel;
    }

    public String getAssessmentCustomsReference() {
        return assessmentCustomsReference;
    }

    public void setAssessmentCustomsReference(String assessmentCustomsReference) {
        this.assessmentCustomsReference = assessmentCustomsReference;
    }

    public String getAssessmentDeclarantReference() {
        return assessmentDeclarantReference;
    }

    public void setAssessmentDeclarantReference(String assessmentDeclarantReference) {
        this.assessmentDeclarantReference = assessmentDeclarantReference;
    }

    public String getAssessmentReference() {
        return assessmentReference;
    }

    public void setAssessmentReference(String assessmentReference) {
        this.assessmentReference = assessmentReference;
    }

    public String getAssessmentPackages() {
        return assessmentPackages;
    }

    public void setAssessmentPackages(String assessmentPackages) {
        this.assessmentPackages = assessmentPackages;
    }

    public String getAssessmentDeclarantId() {
        return assessmentDeclarantId;
    }

    public void setAssessmentDeclarantId(String assessmentDeclarantId) {
        this.assessmentDeclarantId = assessmentDeclarantId;
    }

    public String getAssessmentDeclarantName() {
        return assessmentDeclarantName;
    }

    public void setAssessmentDeclarantName(String assessmentDeclarantName) {
        this.assessmentDeclarantName = assessmentDeclarantName;
    }

    public String getAssessmentDeclarantAddress() {
        return assessmentDeclarantAddress;
    }

    public void setAssessmentDeclarantAddress(String assessmentDeclarantAddress) {
        this.assessmentDeclarantAddress = assessmentDeclarantAddress;
    }

    public String getAssessmentDeclarantChaExp() {
        return assessmentDeclarantChaExp;
    }

    public void setAssessmentDeclarantChaExp(String assessmentDeclarantChaExp) {
        this.assessmentDeclarantChaExp = assessmentDeclarantChaExp;
    }

    public String getAssessmentConsigneeId() {
        return assessmentConsigneeId;
    }

    public void setAssessmentConsigneeId(String assessmentConsigneeId) {
        this.assessmentConsigneeId = assessmentConsigneeId;
    }

    public String getAssessmentConsigneeName() {
        return assessmentConsigneeName;
    }

    public void setAssessmentConsigneeName(String assessmentConsigneeName) {
        this.assessmentConsigneeName = assessmentConsigneeName;
    }

    public String getAssessmentConsigneeAddress() {
        return assessmentConsigneeAddress;
    }

    public void setAssessmentConsigneeAddress(String assessmentConsigneeAddress) {
        this.assessmentConsigneeAddress = assessmentConsigneeAddress;
    }

    public String getAssessmentTaxOtc() {
        return assessmentTaxOtc;
    }

    public void setAssessmentTaxOtc(String assessmentTaxOtc) {
        this.assessmentTaxOtc = assessmentTaxOtc;
    }

    public String getAssessmentTaxCom() {
        return assessmentTaxCom;
    }

    public void setAssessmentTaxCom(String assessmentTaxCom) {
        this.assessmentTaxCom = assessmentTaxCom;
    }

    public String getAssessmentTaxExm() {
        return assessmentTaxExm;
    }

    public void setAssessmentTaxExm(String assessmentTaxExm) {
        this.assessmentTaxExm = assessmentTaxExm;
    }

    public String getAssessmentTaxCid() {
        return assessmentTaxCid;
    }

    public void setAssessmentTaxCid(String assessmentTaxCid) {
        this.assessmentTaxCid = assessmentTaxCid;
    }

    public String getAssessmentTaxSur() {
        return assessmentTaxSur;
    }

    public void setAssessmentTaxSur(String assessmentTaxSur) {
        this.assessmentTaxSur = assessmentTaxSur;
    }

    public String getAssessmentTaxXid() {
        return assessmentTaxXid;
    }

    public void setAssessmentTaxXid(String assessmentTaxXid) {
        this.assessmentTaxXid = assessmentTaxXid;
    }

    public String getAssessmentTaxVat() {
        return assessmentTaxVat;
    }

    public void setAssessmentTaxVat(String assessmentTaxVat) {
        this.assessmentTaxVat = assessmentTaxVat;
    }

    public String getAssessmentTaxVel() {
        return assessmentTaxVel;
    }

    public void setAssessmentTaxVel(String assessmentTaxVel) {
        this.assessmentTaxVel = assessmentTaxVel;
    }

    public String getAssessmentTotalAssessed() {
        return assessmentTotalAssessed;
    }

    public void setAssessmentTotalAssessed(String assessmentTotalAssessed) {
        this.assessmentTotalAssessed = assessmentTotalAssessed;
    }

    public String getAssessmentTotalPaid() {
        return assessmentTotalPaid;
    }

    public void setAssessmentTotalPaid(String assessmentTotalPaid) {
        this.assessmentTotalPaid = assessmentTotalPaid;
    }

    public String getPage3OriginalName() {
        return page3OriginalName;
    }

    public void setPage3OriginalName(String page3OriginalName) {
        this.page3OriginalName = page3OriginalName;
    }

    public String getPage3StoredName() {
        return page3StoredName;
    }

    public void setPage3StoredName(String page3StoredName) {
        this.page3StoredName = page3StoredName;
    }

    public String getPage3ContentType() {
        return page3ContentType;
    }

    public void setPage3ContentType(String page3ContentType) {
        this.page3ContentType = page3ContentType;
    }

    public String getOcrTextPage3() {
        return ocrTextPage3;
    }

    public void setOcrTextPage3(String ocrTextPage3) {
        this.ocrTextPage3 = ocrTextPage3;
    }

    public String getWorksheetRef() {
        return worksheetRef;
    }

    public void setWorksheetRef(String worksheetRef) {
        this.worksheetRef = worksheetRef;
    }

    public String getWorksheetHsCode() {
        return worksheetHsCode;
    }

    public void setWorksheetHsCode(String worksheetHsCode) {
        this.worksheetHsCode = worksheetHsCode;
    }

    public String getWorksheetVehicleType() {
        return worksheetVehicleType;
    }

    public void setWorksheetVehicleType(String worksheetVehicleType) {
        this.worksheetVehicleType = worksheetVehicleType;
    }

    public String getWorksheetReferenceNo() {
        return worksheetReferenceNo;
    }

    public void setWorksheetReferenceNo(String worksheetReferenceNo) {
        this.worksheetReferenceNo = worksheetReferenceNo;
    }

    public String getWorksheetVesselName() {
        return worksheetVesselName;
    }

    public void setWorksheetVesselName(String worksheetVesselName) {
        this.worksheetVesselName = worksheetVesselName;
    }

    public String getWorksheetChassisNo() {
        return worksheetChassisNo;
    }

    public void setWorksheetChassisNo(String worksheetChassisNo) {
        this.worksheetChassisNo = worksheetChassisNo;
    }

    public String getWorksheetAgentsFob() {
        return worksheetAgentsFob;
    }

    public void setWorksheetAgentsFob(String worksheetAgentsFob) {
        this.worksheetAgentsFob = worksheetAgentsFob;
    }

    public String getWorksheetInvoicedFob() {
        return worksheetInvoicedFob;
    }

    public void setWorksheetInvoicedFob(String worksheetInvoicedFob) {
        this.worksheetInvoicedFob = worksheetInvoicedFob;
    }

    public String getWorksheetAgentsFreight() {
        return worksheetAgentsFreight;
    }

    public void setWorksheetAgentsFreight(String worksheetAgentsFreight) {
        this.worksheetAgentsFreight = worksheetAgentsFreight;
    }

    public String getWorksheetInvoicedFreight() {
        return worksheetInvoicedFreight;
    }

    public void setWorksheetInvoicedFreight(String worksheetInvoicedFreight) {
        this.worksheetInvoicedFreight = worksheetInvoicedFreight;
    }

    public String getWorksheetAgentsInsurance() {
        return worksheetAgentsInsurance;
    }

    public void setWorksheetAgentsInsurance(String worksheetAgentsInsurance) {
        this.worksheetAgentsInsurance = worksheetAgentsInsurance;
    }

    public String getWorksheetInvoicedInsurance() {
        return worksheetInvoicedInsurance;
    }

    public void setWorksheetInvoicedInsurance(String worksheetInvoicedInsurance) {
        this.worksheetInvoicedInsurance = worksheetInvoicedInsurance;
    }

    public String getWorksheetOptionsValue() {
        return worksheetOptionsValue;
    }

    public void setWorksheetOptionsValue(String worksheetOptionsValue) {
        this.worksheetOptionsValue = worksheetOptionsValue;
    }

    public String getWorksheetBlFreightCalc() {
        return worksheetBlFreightCalc;
    }

    public void setWorksheetBlFreightCalc(String worksheetBlFreightCalc) {
        this.worksheetBlFreightCalc = worksheetBlFreightCalc;
    }

    public String getWorksheetBlFreightAmount() {
        return worksheetBlFreightAmount;
    }

    public void setWorksheetBlFreightAmount(String worksheetBlFreightAmount) {
        this.worksheetBlFreightAmount = worksheetBlFreightAmount;
    }

    public String getWorksheetBlDate() {
        return worksheetBlDate;
    }

    public void setWorksheetBlDate(String worksheetBlDate) {
        this.worksheetBlDate = worksheetBlDate;
    }

    public String getWorksheetManufactureDate() {
        return worksheetManufactureDate;
    }

    public void setWorksheetManufactureDate(String worksheetManufactureDate) {
        this.worksheetManufactureDate = worksheetManufactureDate;
    }

    public String getWorksheetAgeDifference() {
        return worksheetAgeDifference;
    }

    public void setWorksheetAgeDifference(String worksheetAgeDifference) {
        this.worksheetAgeDifference = worksheetAgeDifference;
    }

    public String getWorksheetFirstRegistrationDate() {
        return worksheetFirstRegistrationDate;
    }

    public void setWorksheetFirstRegistrationDate(String worksheetFirstRegistrationDate) {
        this.worksheetFirstRegistrationDate = worksheetFirstRegistrationDate;
    }

    public String getWorksheetWebsiteValue() {
        return worksheetWebsiteValue;
    }

    public void setWorksheetWebsiteValue(String worksheetWebsiteValue) {
        this.worksheetWebsiteValue = worksheetWebsiteValue;
    }

    public String getWorksheetLocalTaxes() {
        return worksheetLocalTaxes;
    }

    public void setWorksheetLocalTaxes(String worksheetLocalTaxes) {
        this.worksheetLocalTaxes = worksheetLocalTaxes;
    }

    public String getWorksheetFifteenPercent() {
        return worksheetFifteenPercent;
    }

    public void setWorksheetFifteenPercent(String worksheetFifteenPercent) {
        this.worksheetFifteenPercent = worksheetFifteenPercent;
    }

    public String getWorksheetFobValue85() {
        return worksheetFobValue85;
    }

    public void setWorksheetFobValue85(String worksheetFobValue85) {
        this.worksheetFobValue85 = worksheetFobValue85;
    }

    public String getWorksheetLcNo() {
        return worksheetLcNo;
    }

    public void setWorksheetLcNo(String worksheetLcNo) {
        this.worksheetLcNo = worksheetLcNo;
    }

    public String getWorksheetLcAmount() {
        return worksheetLcAmount;
    }

    public void setWorksheetLcAmount(String worksheetLcAmount) {
        this.worksheetLcAmount = worksheetLcAmount;
    }

    public String getWorksheetLcBank() {
        return worksheetLcBank;
    }

    public void setWorksheetLcBank(String worksheetLcBank) {
        this.worksheetLcBank = worksheetLcBank;
    }

    public String getWorksheetLcImporter() {
        return worksheetLcImporter;
    }

    public void setWorksheetLcImporter(String worksheetLcImporter) {
        this.worksheetLcImporter = worksheetLcImporter;
    }

    public String getWorksheetLcIssueDate() {
        return worksheetLcIssueDate;
    }

    public void setWorksheetLcIssueDate(String worksheetLcIssueDate) {
        this.worksheetLcIssueDate = worksheetLcIssueDate;
    }

    public String getWorksheetLcExpiryDate() {
        return worksheetLcExpiryDate;
    }

    public void setWorksheetLcExpiryDate(String worksheetLcExpiryDate) {
        this.worksheetLcExpiryDate = worksheetLcExpiryDate;
    }

    public String getWorksheetLcAmendmentDate() {
        return worksheetLcAmendmentDate;
    }

    public void setWorksheetLcAmendmentDate(String worksheetLcAmendmentDate) {
        this.worksheetLcAmendmentDate = worksheetLcAmendmentDate;
    }

    public String getWorksheetClearingAgent() {
        return worksheetClearingAgent;
    }

    public void setWorksheetClearingAgent(String worksheetClearingAgent) {
        this.worksheetClearingAgent = worksheetClearingAgent;
    }

    public String getWorksheetFiscalFob() {
        return worksheetFiscalFob;
    }

    public void setWorksheetFiscalFob(String worksheetFiscalFob) {
        this.worksheetFiscalFob = worksheetFiscalFob;
    }

    public String getWorksheetFiscalFreight() {
        return worksheetFiscalFreight;
    }

    public void setWorksheetFiscalFreight(String worksheetFiscalFreight) {
        this.worksheetFiscalFreight = worksheetFiscalFreight;
    }

    public String getWorksheetFiscalInsurance() {
        return worksheetFiscalInsurance;
    }

    public void setWorksheetFiscalInsurance(String worksheetFiscalInsurance) {
        this.worksheetFiscalInsurance = worksheetFiscalInsurance;
    }

    public String getWorksheetFiscalOptions() {
        return worksheetFiscalOptions;
    }

    public void setWorksheetFiscalOptions(String worksheetFiscalOptions) {
        this.worksheetFiscalOptions = worksheetFiscalOptions;
    }

    public String getWorksheetFiscalTotal() {
        return worksheetFiscalTotal;
    }

    public void setWorksheetFiscalTotal(String worksheetFiscalTotal) {
        this.worksheetFiscalTotal = worksheetFiscalTotal;
    }

    public String getPage4OriginalName() {
        return page4OriginalName;
    }

    public void setPage4OriginalName(String page4OriginalName) {
        this.page4OriginalName = page4OriginalName;
    }

    public String getPage4StoredName() {
        return page4StoredName;
    }

    public void setPage4StoredName(String page4StoredName) {
        this.page4StoredName = page4StoredName;
    }

    public String getPage4ContentType() {
        return page4ContentType;
    }

    public void setPage4ContentType(String page4ContentType) {
        this.page4ContentType = page4ContentType;
    }

    public String getOcrTextPage4() {
        return ocrTextPage4;
    }

    public void setOcrTextPage4(String ocrTextPage4) {
        this.ocrTextPage4 = ocrTextPage4;
    }

    public String getBlNo() {
        return blNo;
    }

    public void setBlNo(String blNo) {
        this.blNo = blNo;
    }

    public String getDateOfBlIssue() {
        return dateOfBlIssue;
    }

    public void setDateOfBlIssue(String dateOfBlIssue) {
        this.dateOfBlIssue = dateOfBlIssue;
    }

    public String getLandingCostUsd() {
        return landingCostUsd;
    }

    public void setLandingCostUsd(String landingCostUsd) {
        this.landingCostUsd = landingCostUsd;
    }

    public String getBlExchangeRate() {
        return blExchangeRate;
    }

    public void setBlExchangeRate(String blExchangeRate) {
        this.blExchangeRate = blExchangeRate;
    }

    public String getLandingCostLkr() {
        return landingCostLkr;
    }

    public void setLandingCostLkr(String landingCostLkr) {
        this.landingCostLkr = landingCostLkr;
    }

    public String getPage5OriginalName() {
        return page5OriginalName;
    }

    public void setPage5OriginalName(String page5OriginalName) {
        this.page5OriginalName = page5OriginalName;
    }

    public String getPage5StoredName() {
        return page5StoredName;
    }

    public void setPage5StoredName(String page5StoredName) {
        this.page5StoredName = page5StoredName;
    }

    public String getPage5ContentType() {
        return page5ContentType;
    }

    public void setPage5ContentType(String page5ContentType) {
        this.page5ContentType = page5ContentType;
    }

    public String getOcrTextPage5() {
        return ocrTextPage5;
    }

    public void setOcrTextPage5(String ocrTextPage5) {
        this.ocrTextPage5 = ocrTextPage5;
    }
}
