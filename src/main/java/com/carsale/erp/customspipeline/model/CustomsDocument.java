package com.carsale.erp.customspipeline.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Tables;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

@Setter
@Getter
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
@Tables({
        @org.hibernate.annotations.Table(appliesTo = "clearance_assessment_notices"),
        @org.hibernate.annotations.Table(appliesTo = "clearance_jevic_inspections"),
        @org.hibernate.annotations.Table(appliesTo = "clearance_working_sheets"),
        @org.hibernate.annotations.Table(appliesTo = "clearance_bills_of_lading")
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

    @Column()
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

    @Column()
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
    private String worksheetAgentsFobCalc;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetChaNo;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetCheckedBy;

    @Lob
    @Column(name = "worksheet_fob_value85_currency", table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFobValue85Currency;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetFiscalTotalCurrency;

    @Lob
    @Column(table = "clearance_working_sheets", columnDefinition = "TEXT")
    private String worksheetAppraiserName;

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

}
