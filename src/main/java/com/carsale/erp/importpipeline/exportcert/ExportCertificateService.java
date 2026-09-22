package com.carsale.erp.importpipeline.exportcert;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class ExportCertificateService {

    private final VehicleRepository vehicleRepository;
    private final ExportCertificateRepository certificateRepository;
    private final SheetDocumentStorageService documentStorageService;

    public ExportCertificateService(
            VehicleRepository vehicleRepository,
            ExportCertificateRepository certificateRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.certificateRepository = certificateRepository;
        this.documentStorageService = documentStorageService;
    }

    public ExportCertificate findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return certificateRepository.findById(chassisNo.trim()).orElse(null);
    }

    public ExportCertificate prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        ExportCertificate record = certificateRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new ExportCertificate();
            record.setChassisNo(chassisNo);
            prefillFromVehicle(record, vehicle);
        }
        return record;
    }

    public boolean hasCertificate(String chassisNo) {
        ExportCertificate record = findByChassisNo(chassisNo);
        return record != null
                && record.getDocumentStoredName() != null
                && !record.getDocumentStoredName().trim().isEmpty();
    }

    @Transactional
    public ExportCertificate save(ExportCertificate incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }

        Vehicle vehicle = vehicleRepository.findById(incoming.getChassisNo().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle not found for chassis " + incoming.getChassisNo()));

        ExportCertificate existing = certificateRepository.findById(incoming.getChassisNo().trim()).orElse(null);
        if (existing != null) {
            keepStoredDocument(incoming, existing);
            if (incoming.getDocumentStoredName() != null
                    && existing.getDocumentStoredName() != null
                    && !existing.getDocumentStoredName().equals(incoming.getDocumentStoredName())) {
                documentStorageService.deleteExportIfExists(existing.getDocumentStoredName());
            }
            copyFields(incoming, existing);
        } else {
            existing = incoming;
            existing.setChassisNo(incoming.getChassisNo().trim());
            if (existing.getMake() == null) {
                prefillFromVehicle(existing, vehicle);
                copyFields(incoming, existing);
            }
        }
        return certificateRepository.save(existing);
    }

    private void keepStoredDocument(ExportCertificate incoming, ExportCertificate existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    private void prefillFromVehicle(ExportCertificate record, Vehicle vehicle) {
        record.setChassisVin(vehicle.getChassisNo());
        record.setMake(vehicle.getMake());
        record.setModel(vehicle.getModel());
    }

    private void copyFields(ExportCertificate source, ExportCertificate target) {
        target.setIssueDate(source.getIssueDate());
        target.setRegistrationNo(source.getRegistrationNo());
        target.setRegistrationDate(source.getRegistrationDate());
        target.setFirstRegDate(source.getFirstRegDate());
        target.setChassisVin(source.getChassisVin());
        target.setVehicleClassification(source.getVehicleClassification());
        target.setUseType(source.getUseType());
        target.setPurpose(source.getPurpose());
        target.setBodyType(source.getBodyType());
        target.setSeatingCapacity(source.getSeatingCapacity());
        target.setMaxCarry(source.getMaxCarry());
        target.setWeightKg(source.getWeightKg());
        target.setGrossWeightKg(source.getGrossWeightKg());
        target.setLengthCm(source.getLengthCm());
        target.setWidthCm(source.getWidthCm());
        target.setHeightCm(source.getHeightCm());
        target.setEngineCapacity(source.getEngineCapacity());
        target.setFuelType(source.getFuelType());
        target.setSpecificationNo(source.getSpecificationNo());
        target.setClassificationNo(source.getClassificationNo());
        target.setFrontAxleWeight(source.getFrontAxleWeight());
        target.setRearAxleWeight(source.getRearAxleWeight());
        target.setFrWeight(source.getFrWeight());
        target.setRfWeight(source.getRfWeight());
        target.setUserName(source.getUserName());
        target.setUserAddress(source.getUserAddress());
        target.setOwnerName(source.getOwnerName());
        target.setOwnerAddress(source.getOwnerAddress());
        target.setLocalityOfUse(source.getLocalityOfUse());
        target.setExportScheduledDate(source.getExportScheduledDate());
        target.setDirectorGeneralLandTransportBranch(source.getDirectorGeneralLandTransportBranch());
        target.setOcrText(source.getOcrText());
        target.setDocumentOriginalName(source.getDocumentOriginalName());
        target.setDocumentStoredName(source.getDocumentStoredName());
        target.setDocumentContentType(source.getDocumentContentType());
    }
}
