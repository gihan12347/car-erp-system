package com.carsale.erp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

@Entity
@Table(name = "clearance_documents")
@SecondaryTable(
        name = "clearance_assessment_notices",
        pkJoinColumns = @PrimaryKeyJoinColumn(name = "chassis_no")
)
@org.hibernate.annotations.Table(appliesTo = "clearance_assessment_notices", optional = true)
public class ClearanceDocument {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    /* Page 1 — JEVIC odometer certificate */
    @Column(length = 40)
    private String jevicCertificateNo;

    @Column(length = 40)
    private String jevicChassisVin;

    @Column(length = 80)
    private String jevicMake;

    @Column(length = 80)
    private String jevicModel;

    @Column(length = 80)
    private String jevicLocation;

    @Column(length = 40)
    private String jevicInspectionDate;

    @Column(length = 40)
    private String jevicIssueDate;

    @Column(length = 40)
    private String jevicCurrentOdometer;

    @Column(length = 40)
    private String jevicAuctionReading;

    @Column(length = 40)
    private String jevicAuctionReadingDate;

    @Column(length = 40)
    private String jevicDealerReading;

    @Column(length = 40)
    private String jevicDealerReadingDate;

    @Column(length = 40)
    private String jevicDeregistrationReading;

    @Column(length = 40)
    private String jevicDeregistrationReadingDate;

    @Column(length = 80)
    private String jevicAuthorizedBy;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jevicOrgAddress;

    @Column(length = 40)
    private String jevicTel;

    @Column(length = 80)
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
    @Column(length = 40)
    private String customsReference;

    @Column(length = 20)
    private String declarationType;

    @Column(length = 20)
    private String declarationPages;

    @Column(length = 20)
    private String declarationLists;

    @Column(length = 20)
    private String declarationItems;

    @Column(length = 40)
    private String totalPackages;

    @Column(length = 120)
    private String exporterName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exporterAddress;

    @Column(length = 120)
    private String consigneeName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String consigneeAddress;

    @Column(length = 40)
    private String consigneeTin;

    @Column(length = 120)
    private String declarantName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declarantAddress;

    @Column(length = 40)
    private String declarantTin;

    @Column(length = 80)
    private String countryLastConsignment;

    @Column(length = 20)
    private String tradingCountry;

    @Column(length = 80)
    private String countryExport;

    @Column(length = 80)
    private String countryDestination;

    @Column(length = 80)
    private String countryOrigin;

    @Column(length = 80)
    private String vesselFlight;

    @Column(length = 20)
    private String deliveryTerms;

    @Column(length = 60)
    private String voyageNoDate;

    @Column(length = 80)
    private String placeLoadingDischarging;

    @Column(length = 20)
    private String currencyInvoiced;

    @Column(length = 40)
    private String totalAmountInvoiced;

    @Column(length = 20)
    private String exchangeRate;

    @Column(length = 40)
    private String paymentTerms;

    @Column(length = 20)
    private String bankCode;

    @Column(length = 80)
    private String bankName;

    @Column(length = 40)
    private String bankBranch;

    @Column(length = 60)
    private String bankReference;

    @Column(length = 40)
    private String locationOfGoods;

    @Column(length = 20)
    private String hsCode;

    @Column(length = 20)
    private String grossMassKg;

    @Column(length = 20)
    private String netMassKg;

    @Column(length = 60)
    private String blAwbNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String goodsDescription;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String modelSpec;

    @Column(length = 20)
    private String yearOfManufacture;

    @Column(length = 20)
    private String dateOfRegistration;

    @Column(length = 40)
    private String clearanceChassisNo;

    @Column(length = 40)
    private String clearanceEngineNo;

    @Column(length = 40)
    private String itemPrice;

    @Column(length = 40)
    private String valueNcy;

    @Column(length = 20)
    private String engineCapacityCc;

    @Column(length = 30)
    private String taxCid;

    @Column(length = 30)
    private String taxSur;

    @Column(length = 30)
    private String taxVat;

    @Column(length = 30)
    private String taxXid;

    @Column(length = 30)
    private String taxVel;

    @Column(length = 30)
    private String taxOther;

    @Column(length = 30)
    private String totalTaxAmount;

    @Column(length = 30)
    private String invoiceFob;

    @Column(length = 30)
    private String invoiceFreight;

    @Column(length = 30)
    private String invoiceInsurance;

    @Column(length = 30)
    private String invoiceTotal;

    @Column(length = 20)
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
}
