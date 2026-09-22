package com.carsale.erp.customspipeline;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.regex.RegexConstants;

@Service
public class CustomsDocumentService {

    private static final String[] DECLARATION_FIELDS = {
            "exchangeRate", "valueNcy",
            "invoiceFob", "invoiceFreight", "invoiceInsurance", "invoiceOther", "invoiceTotal",
            "page2OriginalName", "page2StoredName", "page2ContentType", "ocrTextPage2"
    };

    private static final String[] ASSESSMENT_FIELDS = {
            "assessmentOffice", "assessmentNoticeRef", "assessmentModel", "assessmentCustomsReference",
            "assessmentDeclarantReference", "assessmentReference", "assessmentPackages",
            "assessmentDeclarantId", "assessmentDeclarantName", "assessmentDeclarantAddress", "assessmentDeclarantChaExp",
            "assessmentConsigneeId", "assessmentConsigneeName", "assessmentConsigneeAddress",
            "assessmentTaxOtc", "assessmentTaxCom", "assessmentTaxExm", "assessmentTaxCid", "assessmentTaxSur",
            "assessmentTaxXid", "assessmentTaxVat", "assessmentTaxVel", "assessmentTotalAssessed", "assessmentTotalPaid",
            "page3OriginalName", "page3StoredName", "page3ContentType", "ocrTextPage3"
    };

    private static final String[] WORKSHEET_FIELDS = {
            "worksheetRef", "worksheetHsCode", "worksheetVehicleType", "worksheetReferenceNo", "worksheetVesselName",
            "worksheetChassisNo", "worksheetAgentsFob", "worksheetInvoicedFob", "worksheetAgentsFreight",
            "worksheetInvoicedFreight", "worksheetAgentsInsurance", "worksheetInvoicedInsurance", "worksheetOptionsValue",
            "worksheetBlFreightCalc", "worksheetBlFreightAmount", "worksheetBlDate", "worksheetManufactureDate",
            "worksheetAgeDifference", "worksheetFirstRegistrationDate", "worksheetWebsiteValue", "worksheetLocalTaxes",
            "worksheetFifteenPercent", "worksheetFobValue85", "worksheetLcNo", "worksheetLcAmount", "worksheetLcBank",
            "worksheetLcImporter", "worksheetLcIssueDate", "worksheetLcExpiryDate", "worksheetLcAmendmentDate",
            "worksheetClearingAgent", "worksheetFiscalFob", "worksheetFiscalFreight", "worksheetFiscalInsurance",
            "worksheetFiscalOptions", "worksheetFiscalTotal",
            "page4OriginalName", "page4StoredName", "page4ContentType", "ocrTextPage4"
    };

    private static final String[] BILL_OF_LADING_FIELDS = {
            "blNo", "dateOfBlIssue", "landingCostUsd", "blExchangeRate", "landingCostLkr",
            "page5OriginalName", "page5StoredName", "page5ContentType", "ocrTextPage5"
    };

    private final VehicleRepository vehicleRepository;
    private final CustomsDocumentRepository customsDocumentRepository;
    private final SheetDocumentStorageService documentStorageService;

    public CustomsDocumentService(
            VehicleRepository vehicleRepository,
            CustomsDocumentRepository customsDocumentRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.customsDocumentRepository = customsDocumentRepository;
        this.documentStorageService = documentStorageService;
    }

    public CustomsDocument findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return customsDocumentRepository.findById(chassisNo.trim()).orElse(null);
    }

    public CustomsDocument prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        CustomsDocument record = customsDocumentRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new CustomsDocument();
            record.setChassisNo(chassisNo);
            prefillFromVehicle(record, vehicle);
        }
        return record;
    }

    public boolean hasOdometerCertificate(String chassisNo) {
        CustomsDocument record = findByChassisNo(chassisNo);
        return record != null && hasStoredName(record.getPage1StoredName());
    }

    public boolean hasBillOfLading(String chassisNo) {
        CustomsDocument record = findByChassisNo(chassisNo);
        return record != null && hasStoredName(record.getPage5StoredName());
    }

    public boolean hasDeclaration(String chassisNo) {
        CustomsDocument record = findByChassisNo(chassisNo);
        return record != null && hasStoredName(record.getPage2StoredName());
    }

    public boolean hasAssessment(String chassisNo) {
        CustomsDocument record = findByChassisNo(chassisNo);
        return record != null && hasStoredName(record.getPage3StoredName());
    }

    public boolean hasWorksheet(String chassisNo) {
        CustomsDocument record = findByChassisNo(chassisNo);
        return record != null && hasStoredName(record.getPage4StoredName());
    }

    @Transactional
    public CustomsDocument save(CustomsDocument incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        CustomsDocument existing = customsDocumentRepository.findById(incoming.getChassisNo().trim()).orElse(null);
        if (existing != null) {
            keepStoredDocuments(incoming, existing);
            keepOdometerFields(incoming, existing);
            BeanUtils.copyProperties(incoming, existing);
            existing.setChassisNo(incoming.getChassisNo().trim());
        } else {
            existing = incoming;
            existing.setChassisNo(incoming.getChassisNo().trim());
        }

        return customsDocumentRepository.save(existing);
    }

    @Transactional
    public void saveOdometerCertificate(CustomsDocument incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }

        Vehicle vehicle = vehicleRepository.findById(incoming.getChassisNo().trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + incoming.getChassisNo()));

        CustomsDocument existing = customsDocumentRepository.findById(incoming.getChassisNo().trim()).orElse(null);
        if (existing == null) {
            existing = new CustomsDocument();
            existing.setChassisNo(incoming.getChassisNo().trim());
            prefillFromVehicle(existing, vehicle);
        } else {
            replacePage1IfChanged(existing, incoming);
        }
        copyOdometerFields(incoming, existing);
        existing.setChassisNo(incoming.getChassisNo().trim());
        customsDocumentRepository.save(existing);
    }

    @Transactional
    public void saveBillOfLading(CustomsDocument incoming) {
        CustomsDocument existing = prepareStageSave(incoming);
        deleteReplacedPage(existing.getPage5StoredName(), incoming.getPage5StoredName());
        copyFields(incoming, existing, BILL_OF_LADING_FIELDS);
        existing.setChassisNo(incoming.getChassisNo().trim());
        customsDocumentRepository.save(existing);
    }

    @Transactional
    public void saveDeclaration(CustomsDocument incoming) {
        CustomsDocument existing = prepareStageSave(incoming);
        deleteReplacedPage(existing.getPage2StoredName(), incoming.getPage2StoredName());
        copyFields(incoming, existing, DECLARATION_FIELDS);
        existing.setChassisNo(incoming.getChassisNo().trim());
        customsDocumentRepository.save(existing);
    }

    @Transactional
    public void saveAssessment(CustomsDocument incoming) {
        CustomsDocument existing = prepareStageSave(incoming);
        deleteReplacedPage(existing.getPage3StoredName(), incoming.getPage3StoredName());
        copyFields(incoming, existing, ASSESSMENT_FIELDS);
        existing.setChassisNo(incoming.getChassisNo().trim());
        customsDocumentRepository.save(existing);
    }

    @Transactional
    public void saveWorksheet(CustomsDocument incoming) {
        CustomsDocument existing = prepareStageSave(incoming);
        deleteReplacedPage(existing.getPage4StoredName(), incoming.getPage4StoredName());
        copyFields(incoming, existing, WORKSHEET_FIELDS);
        existing.setChassisNo(incoming.getChassisNo().trim());
        customsDocumentRepository.save(existing);
    }

    private CustomsDocument prepareStageSave(CustomsDocument incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        Vehicle vehicle = vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        CustomsDocument existing = customsDocumentRepository.findById(chassisNo).orElse(null);
        if (existing == null) {
            existing = new CustomsDocument();
            existing.setChassisNo(chassisNo);
            prefillFromVehicle(existing, vehicle);
        }
        return existing;
    }

    private void copyFields(CustomsDocument source, CustomsDocument target, String[] fields) {
        if (source == null || target == null || fields == null) {
            return;
        }
        BeanWrapper src = new BeanWrapperImpl(source);
        BeanWrapper dst = new BeanWrapperImpl(target);
        for (String field : fields) {
            dst.setPropertyValue(field, src.getPropertyValue(field));
        }
    }

    private void keepOdometerFields(CustomsDocument incoming, CustomsDocument existing) {
        copyOdometerFields(existing, incoming);
    }

    private void copyOdometerFields(CustomsDocument source, CustomsDocument target) {
        if (source == null || target == null) {
            return;
        }
        target.setJevicCertificateNo(source.getJevicCertificateNo());
        target.setJevicChassisVin(source.getJevicChassisVin());
        target.setJevicMake(source.getJevicMake());
        target.setJevicModel(source.getJevicModel());
        target.setJevicLocation(source.getJevicLocation());
        target.setJevicInspectionDate(source.getJevicInspectionDate());
        target.setJevicIssueDate(source.getJevicIssueDate());
        target.setJevicCurrentOdometer(source.getJevicCurrentOdometer());
        target.setPage1OriginalName(source.getPage1OriginalName());
        target.setPage1StoredName(source.getPage1StoredName());
        target.setPage1ContentType(source.getPage1ContentType());
        target.setOcrTextPage1(source.getOcrTextPage1());
    }

    private void replacePage1IfChanged(CustomsDocument existing, CustomsDocument incoming) {
        String oldStored = existing.getPage1StoredName();
        String newStored = incoming.getPage1StoredName();
        if (oldStored != null && newStored != null && !oldStored.equals(newStored)) {
            documentStorageService.deleteClearanceIfExists(oldStored);
        }
    }

    private void keepStoredDocuments(CustomsDocument incoming, CustomsDocument existing) {
        incoming.setPage1OriginalName(existing.getPage1OriginalName());
        incoming.setPage1StoredName(existing.getPage1StoredName());
        incoming.setPage1ContentType(existing.getPage1ContentType());
        incoming.setPage2OriginalName(existing.getPage2OriginalName());
        incoming.setPage2StoredName(existing.getPage2StoredName());
        incoming.setPage2ContentType(existing.getPage2ContentType());
        incoming.setPage3OriginalName(existing.getPage3OriginalName());
        incoming.setPage3StoredName(existing.getPage3StoredName());
        incoming.setPage3ContentType(existing.getPage3ContentType());
        incoming.setPage4OriginalName(existing.getPage4OriginalName());
        incoming.setPage4StoredName(existing.getPage4StoredName());
        incoming.setPage4ContentType(existing.getPage4ContentType());
        incoming.setPage5OriginalName(existing.getPage5OriginalName());
        incoming.setPage5StoredName(existing.getPage5StoredName());
        incoming.setPage5ContentType(existing.getPage5ContentType());
    }

    private void deleteReplacedPage(String oldStored, String newStored) {
        if (oldStored != null && newStored != null && !oldStored.equals(newStored)) {
            documentStorageService.deleteClearanceIfExists(oldStored);
        }
    }

    private void prefillFromVehicle(CustomsDocument record, Vehicle vehicle) {
        record.setChassisNo(vehicle.getChassisNo());
        setIfPresent(record::setJevicChassisVin, vehicle.getChassisNo());
        setIfPresent(record::setJevicMake, vehicle.getMake());
        setIfPresent(record::setJevicModel, vehicle.getModel());
        setIfPresent(record::setJevicCurrentOdometer, vehicle.getMileage());
        setIfPresent(record::setJevicEngineCapacity, vehicle.getEngineSize());
        setIfPresent(record::setClearanceChassisNo, vehicle.getChassisNo());
        String description = buildDescription(vehicle);
        if (description != null) {
            record.setGoodsDescription(description);
        }
        setIfPresent(record::setEngineCapacityCc, vehicle.getEngineSize());
        if (vehicle.getYear() != null && vehicle.getYear().matches(RegexConstants.Dates.CONTAINS_YEAR)) {
            java.util.regex.Matcher matcher = RegexConstants.Dates.YEAR_CAPTURE_PATTERN.matcher(vehicle.getYear());
            if (matcher.find()) {
                record.setYearOfManufacture(matcher.group(1));
            }
        }
    }

    private void setIfPresent(java.util.function.Consumer<String> setter, String value) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value.trim());
        }
    }

    private String buildDescription(Vehicle vehicle) {
        String make = vehicle.getMake() == null ? "" : vehicle.getMake().trim();
        String model = vehicle.getModel() == null ? "" : vehicle.getModel().trim();
        if (make.isEmpty() && model.isEmpty()) {
            return null;
        }
        return ("USED " + make + " " + model).trim().replaceAll(RegexConstants.Text.WHITESPACE_RUN, " ");
    }

    private boolean hasStoredName(String storedName) {
        return storedName != null && !storedName.trim().isEmpty();
    }
}
