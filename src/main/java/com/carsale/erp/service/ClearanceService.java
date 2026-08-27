package com.carsale.erp.service;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.ClearanceDocument;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.repository.ClearanceDocumentRepository;
import com.carsale.erp.repository.VehicleRepository;

@Service
public class ClearanceService {

    private final VehicleRepository vehicleRepository;
    private final ClearanceDocumentRepository clearanceRepository;
    private final SheetDocumentStorageService documentStorageService;

    public ClearanceService(
            VehicleRepository vehicleRepository,
            ClearanceDocumentRepository clearanceRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.clearanceRepository = clearanceRepository;
        this.documentStorageService = documentStorageService;
    }

    public ClearanceDocument findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return clearanceRepository.findById(chassisNo.trim()).orElse(null);
    }

    public ClearanceDocument prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        ClearanceDocument record = clearanceRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new ClearanceDocument();
            record.setChassisNo(chassisNo);
            prefillFromVehicle(record, vehicle);
        }
        return record;
    }

    public boolean hasJevic(String chassisNo) {
        ClearanceDocument record = findByChassisNo(chassisNo);
        return record != null && hasStoredName(record.getPage1StoredName());
    }

    public boolean hasDeclaration(String chassisNo) {
        ClearanceDocument record = findByChassisNo(chassisNo);
        return record != null && hasStoredName(record.getPage2StoredName());
    }

    public boolean hasAssessment(String chassisNo) {
        ClearanceDocument record = findByChassisNo(chassisNo);
        return record != null && hasStoredName(record.getPage3StoredName());
    }

    public boolean hasClearance(String chassisNo) {
        return hasJevic(chassisNo) && hasDeclaration(chassisNo) && hasAssessment(chassisNo);
    }

    @Transactional
    public ClearanceDocument save(ClearanceDocument incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }

        Vehicle vehicle = vehicleRepository.findById(incoming.getChassisNo().trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + incoming.getChassisNo()));

        ClearanceDocument existing = clearanceRepository.findById(incoming.getChassisNo().trim()).orElse(null);
        if (existing != null) {
            keepStoredDocuments(incoming, existing);
            BeanUtils.copyProperties(incoming, existing);
            existing.setChassisNo(incoming.getChassisNo().trim());
        } else {
            existing = incoming;
            existing.setChassisNo(incoming.getChassisNo().trim());
        }

        return clearanceRepository.save(existing);
    }

    @Transactional
    public void savePageDocument(String chassisNo, int page, String originalName, String storedName, String contentType) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        if (page < 1 || page > 3) {
            throw new IllegalArgumentException("Invalid clearance page number.");
        }
        if (storedName == null || storedName.trim().isEmpty()) {
            throw new IllegalArgumentException("Stored document name is required.");
        }

        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        ClearanceDocument record = clearanceRepository.findById(chassisNo.trim()).orElse(null);
        if (record == null) {
            record = new ClearanceDocument();
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
        } else {
            deleteReplacedPage(record.getPage3StoredName(), storedName);
            record.setPage3OriginalName(originalName);
            record.setPage3StoredName(storedName);
            record.setPage3ContentType(contentType);
        }

        clearanceRepository.save(record);
    }

    private void keepStoredDocuments(ClearanceDocument incoming, ClearanceDocument existing) {
        incoming.setPage1OriginalName(existing.getPage1OriginalName());
        incoming.setPage1StoredName(existing.getPage1StoredName());
        incoming.setPage1ContentType(existing.getPage1ContentType());
        incoming.setPage2OriginalName(existing.getPage2OriginalName());
        incoming.setPage2StoredName(existing.getPage2StoredName());
        incoming.setPage2ContentType(existing.getPage2ContentType());
        incoming.setPage3OriginalName(existing.getPage3OriginalName());
        incoming.setPage3StoredName(existing.getPage3StoredName());
        incoming.setPage3ContentType(existing.getPage3ContentType());
    }

    private void deleteReplacedPage(String oldStored, String newStored) {
        if (oldStored != null && newStored != null && !oldStored.equals(newStored)) {
            documentStorageService.deleteClearanceIfExists(oldStored);
        }
    }

    private void prefillFromVehicle(ClearanceDocument record, Vehicle vehicle) {
        record.setChassisNo(vehicle.getChassisNo());
        setIfPresent(record::setJevicChassisVin, vehicle.getChassisNo());
        setIfPresent(record::setJevicMake, vehicle.getMake());
        setIfPresent(record::setJevicModel, vehicle.getModel());
        setIfPresent(record::setJevicCurrentOdometer, vehicle.getMileage());
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
