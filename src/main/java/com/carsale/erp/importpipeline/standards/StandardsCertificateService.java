package com.carsale.erp.importpipeline.standards;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.importpipeline.standards.StandardsCertificate;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.importpipeline.standards.StandardsCertificateRepository;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class StandardsCertificateService {

    private final VehicleRepository vehicleRepository;
    private final StandardsCertificateRepository certificateRepository;
    private final SheetDocumentStorageService documentStorageService;

    public StandardsCertificateService(
            VehicleRepository vehicleRepository,
            StandardsCertificateRepository certificateRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.certificateRepository = certificateRepository;
        this.documentStorageService = documentStorageService;
    }

    public StandardsCertificate findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return certificateRepository.findById(chassisNo.trim()).orElse(null);
    }

    public StandardsCertificate prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        StandardsCertificate record = certificateRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new StandardsCertificate();
            record.setChassisNo(chassisNo);
            prefillFromVehicle(record, vehicle);
        }
        return record;
    }

    public boolean hasCertificate(String chassisNo) {
        StandardsCertificate record = findByChassisNo(chassisNo);
        return record != null
                && record.getDocumentStoredName() != null
                && !record.getDocumentStoredName().trim().isEmpty();
    }

    @Transactional
    public StandardsCertificate save(StandardsCertificate incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }

        Vehicle vehicle = vehicleRepository.findById(incoming.getChassisNo().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle not found for chassis " + incoming.getChassisNo()));

        StandardsCertificate existing = certificateRepository.findById(incoming.getChassisNo().trim()).orElse(null);
        if (existing != null) {
            keepStoredDocument(incoming, existing);
            if (incoming.getDocumentStoredName() != null
                    && existing.getDocumentStoredName() != null
                    && !existing.getDocumentStoredName().equals(incoming.getDocumentStoredName())) {
                documentStorageService.deleteStandardsIfExists(existing.getDocumentStoredName());
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

    private void keepStoredDocument(StandardsCertificate incoming, StandardsCertificate existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    private void prefillFromVehicle(StandardsCertificate record, Vehicle vehicle) {
        record.setChassisVin(vehicle.getChassisNo());
        record.setMake(vehicle.getMake());
        record.setModel(vehicle.getModel());
    }

    private void copyFields(StandardsCertificate source, StandardsCertificate target) {
        target.setScheduleType(source.getScheduleType());
        target.setEmissionCo(source.getEmissionCo());
        target.setEmissionNmhc(source.getEmissionNmhc());
        target.setEmissionNox(source.getEmissionNox());
        target.setEmissionPm(source.getEmissionPm());
        target.setEmissionHc(source.getEmissionHc());
        target.setEmissionHcNox(source.getEmissionHcNox());
        target.setEmissionThc(source.getEmissionThc());
        target.setEmissionCh4(source.getEmissionCh4());
        target.setEmissionSmoke(source.getEmissionSmoke());
        target.setThreePointSeatBelts(source.isThreePointSeatBelts());
        target.setTwoPointSeatBelts(source.isTwoPointSeatBelts());
        target.setDriverAirbag(source.isDriverAirbag());
        target.setPassengerAirbag(source.isPassengerAirbag());
        target.setAbsFitted(source.isAbsFitted());
        target.setMake(source.getMake());
        target.setModel(source.getModel());
        target.setChassisVin(source.getChassisVin());
        target.setPlaceOfInspection(source.getPlaceOfInspection());
        target.setInspectionDate(source.getInspectionDate());
        target.setRemarks(source.getRemarks());
        target.setOcrText(source.getOcrText());
        target.setDocumentOriginalName(source.getDocumentOriginalName());
        target.setDocumentStoredName(source.getDocumentStoredName());
        target.setDocumentContentType(source.getDocumentContentType());
    }
}
