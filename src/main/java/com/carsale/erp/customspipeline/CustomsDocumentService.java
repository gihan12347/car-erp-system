package com.carsale.erp.customspipeline;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.customspipeline.CustomsDocument;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.customspipeline.CustomsDocumentRepository;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class CustomsDocumentService {

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

        Vehicle vehicle = vehicleRepository.findById(incoming.getChassisNo().trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + incoming.getChassisNo()));

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
    public CustomsDocument saveOdometerCertificate(CustomsDocument incoming) {
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
        return customsDocumentRepository.save(existing);
    }

    @Transactional
    public void savePageDocument(String chassisNo, int page, String originalName, String storedName, String contentType) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        if (page < 1 || page > 4) {
            throw new IllegalArgumentException("Invalid clearance page number.");
        }
        if (storedName == null || storedName.trim().isEmpty()) {
            throw new IllegalArgumentException("Stored document name is required.");
        }

        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        CustomsDocument record = customsDocumentRepository.findById(chassisNo.trim()).orElse(null);
        if (record == null) {
            record = new CustomsDocument();
            record.setChassisNo(chassisNo.trim());
            prefillFromVehicle(record, vehicle);
        }

        if (page == 1) {
            deleteReplacedPage(record.getPage1StoredName(), storedName);
            record.setPage1OriginalName(originalName);
            record.setPage1StoredName(storedName);
            record.setPage1ContentType(contentType);
        } else if (page == 2) {
            deleteReplacedPage(record.getPage2StoredName(), storedName);
            record.setPage2OriginalName(originalName);
            record.setPage2StoredName(storedName);
            record.setPage2ContentType(contentType);
        } else if (page == 3) {
            deleteReplacedPage(record.getPage3StoredName(), storedName);
            record.setPage3OriginalName(originalName);
            record.setPage3StoredName(storedName);
            record.setPage3ContentType(contentType);
        } else {
            deleteReplacedPage(record.getPage4StoredName(), storedName);
            record.setPage4OriginalName(originalName);
            record.setPage4StoredName(storedName);
            record.setPage4ContentType(contentType);
        }

        customsDocumentRepository.save(record);
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
        target.setJevicEngineCapacity(source.getJevicEngineCapacity());
        target.setJevicFirstRegistration(source.getJevicFirstRegistration());
        target.setJevicEngineNo(source.getJevicEngineNo());
        target.setJevicRemarks(source.getJevicRemarks());
        target.setJevicAuctionReading(source.getJevicAuctionReading());
        target.setJevicAuctionReadingDate(source.getJevicAuctionReadingDate());
        target.setJevicDealerReading(source.getJevicDealerReading());
        target.setJevicDealerReadingDate(source.getJevicDealerReadingDate());
        target.setJevicDeregistrationReading(source.getJevicDeregistrationReading());
        target.setJevicDeregistrationReadingDate(source.getJevicDeregistrationReadingDate());
        target.setJevicAuthorizedBy(source.getJevicAuthorizedBy());
        target.setJevicOrgAddress(source.getJevicOrgAddress());
        target.setJevicTel(source.getJevicTel());
        target.setJevicWebsite(source.getJevicWebsite());
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
        if (vehicle.getYear() != null && vehicle.getYear().matches(".*\\d{4}.*")) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{4})").matcher(vehicle.getYear());
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
        return ("USED " + make + " " + model).trim().replaceAll("\\s{2,}", " ");
    }

    private boolean hasStoredName(String storedName) {
        return storedName != null && !storedName.trim().isEmpty();
    }
}
