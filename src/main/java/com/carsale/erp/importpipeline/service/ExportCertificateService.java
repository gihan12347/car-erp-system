package com.carsale.erp.importpipeline.service;

import com.carsale.erp.importpipeline.model.ExportCertificate;
import com.carsale.erp.importpipeline.repository.ExportCertificateRepository;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.document.document.ExportCertificateParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsale.erp.shared.utils.Utils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
@RequiredArgsConstructor
public class ExportCertificateService {

    private final VehicleRepository vehicleRepository;
    private final ExportCertificateRepository certificateRepository;
    private final SheetDocumentStorageService documentStorageService;

    public ExportCertificate findByChassisNo(String chassisNo) {
        return Utils.findByChassisNo(chassisNo, certificateRepository, this::formatDisplayDates);
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
        formatDisplayDates(record);
        return record;
    }

    private void formatDisplayDates(ExportCertificate record) {
        if (record == null) {
            return;
        }
        record.setIssueDate(ExportCertificateParser.toSlashDate(record.getIssueDate()));
        record.setExportScheduledDate(ExportCertificateParser.toSlashDate(record.getExportScheduledDate()));
        record.setRegistrationDate(ExportCertificateParser.toSlashDate(record.getRegistrationDate()));
        record.setFirstRegDate(ExportCertificateParser.toSlashDate(record.getFirstRegDate()));
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
        formatDisplayDates(existing);
        ExportCertificate saved = certificateRepository.save(existing);
        applyToAuctionLot(saved, vehicle);
        vehicleRepository.save(vehicle);
        return saved;
    }

    private void keepStoredDocument(ExportCertificate incoming, ExportCertificate existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    private void applyToAuctionLot(ExportCertificate certificate, Vehicle vehicle) {
        setIfPresent(vehicle::setFuel, certificate.getFuelType(), 40);
        setIfPresent(vehicle::setSeats, certificate.getSeatingCapacity(), 20);
        setIfPresent(vehicle::setLengthCm, certificate.getLengthCm(), 10);
        setIfPresent(vehicle::setWidthCm, certificate.getWidthCm(), 10);
        setIfPresent(vehicle::setHeightCm, certificate.getHeightCm(), 10);
    }

    private static void setIfPresent(java.util.function.Consumer<String> setter, String value, int maxLength) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength);
        }
        setter.accept(trimmed);
    }

    private void prefillFromVehicle(ExportCertificate record, Vehicle vehicle) {
        setIfBlank(record.getChassisVin(), record::setChassisVin, vehicle.getChassisNo());
        setIfBlank(record.getMake(), record::setMake, vehicle.getMake());
        setIfBlank(record.getModel(), record::setModel, vehicle.getModel());
    }

    private static void setIfBlank(String current, java.util.function.Consumer<String> setter, String value) {
        if (current != null && !current.trim().isEmpty()) {
            return;
        }
        setIfPresent(setter, value, Integer.MAX_VALUE);
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
        target.setOcrText(source.getOcrText());
        target.setDocumentOriginalName(source.getDocumentOriginalName());
        target.setDocumentStoredName(source.getDocumentStoredName());
        target.setDocumentContentType(source.getDocumentContentType());
    }
}
