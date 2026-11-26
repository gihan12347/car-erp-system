package com.carsale.erp.importpipeline.service;

import com.carsale.erp.importpipeline.model.InspectionCertificate;
import com.carsale.erp.importpipeline.repository.InspectionCertificateRepository;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.document.document.CertificateOfInspectionParser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.utils.Utils;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
@RequiredArgsConstructor
public class CertificateOfInspectionService {

    private final VehicleRepository vehicleRepository;
    private final InspectionCertificateRepository certificateRepository;
    private final SheetDocumentStorageService documentStorageService;

    public InspectionCertificate findByChassisNo(String chassisNo) {
        return Utils.findByChassisNo(chassisNo, certificateRepository, this::normalizeInspectionDate);
    }

    public InspectionCertificate prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        InspectionCertificate record = certificateRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = newBlank();
            record.setChassisNo(chassisNo);
            prefillFromVehicle(record, vehicle);
        } else {
            ensureInspectionDate(record);
        }
        return record;
    }

    public InspectionCertificate newBlank() {
        InspectionCertificate record = new InspectionCertificate();
        ensureInspectionDate(record);
        return record;
    }

    private void ensureInspectionDate(InspectionCertificate record) {
        normalizeInspectionDate(record);
        if (record.getInspectionDate() == null || record.getInspectionDate().trim().isEmpty()) {
            record.setInspectionDate(java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")));
        }
    }

    private void normalizeInspectionDate(InspectionCertificate record) {
        if (record == null || record.getInspectionDate() == null) {
            return;
        }
        String normalized = CertificateOfInspectionParser.toSlashDate(record.getInspectionDate());
        if (normalized != null && normalized.matches("\\d{4}/\\d{2}/\\d{2}")) {
            record.setInspectionDate(normalized);
        }
    }

    public boolean hasCertificate(String chassisNo) {
        InspectionCertificate record = findByChassisNo(chassisNo);
        return record != null
                && record.getDocumentStoredName() != null
                && !record.getDocumentStoredName().trim().isEmpty();
    }

    @Transactional
    public InspectionCertificate save(InspectionCertificate incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }

        Vehicle vehicle = vehicleRepository.findById(incoming.getChassisNo().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle not found for chassis " + incoming.getChassisNo()));

        InspectionCertificate existing = certificateRepository.findById(incoming.getChassisNo().trim()).orElse(null);
        if (existing != null) {
            keepStoredDocument(incoming, existing);
            if (incoming.getDocumentStoredName() != null
                    && existing.getDocumentStoredName() != null
                    && !existing.getDocumentStoredName().equals(incoming.getDocumentStoredName())) {
                documentStorageService.deleteCoiIfExists(existing.getDocumentStoredName());
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
        InspectionCertificate saved = certificateRepository.save(existing);
        applyToAuctionLot(saved, vehicle);
        vehicleRepository.save(vehicle);
        return saved;
    }

    private void keepStoredDocument(InspectionCertificate incoming, InspectionCertificate existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    private void applyToAuctionLot(InspectionCertificate certificate, Vehicle vehicle) {
        setIfPresent(vehicle::setEngineSize, certificate.getEngineCapacity(), 40);
        setIfPresent(vehicle::setModelCode, certificate.getModel(), 40);
        setIfPresent(vehicle::setModel, vehicleName(certificate.getMake(), certificate.getModel()), 80);
    }

    private static String vehicleName(String make, String model) {
        String makeText = trimToNull(make);
        String modelText = trimToNull(model);
        if (makeText == null || modelText == null) {
            return null;
        }
        if (modelText.equalsIgnoreCase(makeText)
                || modelText.regionMatches(true, 0, makeText + " ", 0, makeText.length() + 1)) {
            return modelText;
        }
        return makeText + " " + modelText;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void setIfPresent(java.util.function.Consumer<String> setter, String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return;
        }
        if (trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength);
        }
        setter.accept(trimmed);
    }

    private void prefillFromVehicle(InspectionCertificate record, Vehicle vehicle) {
        setIfBlank(record.getChassisVin(), record::setChassisVin, vehicle.getChassisNo());
        setIfBlank(record.getMake(), record::setMake, vehicle.getMake());
        setIfBlank(record.getModel(), record::setModel, vehicle.getModel());
        setIfBlank(record.getEngineCapacity(), record::setEngineCapacity, vehicle.getEngineSize());
        setIfBlank(record.getInspectedMileage(), record::setInspectedMileage, vehicle.getMileage());
        setIfBlank(record.getFirstRegistration(), record::setFirstRegistration, vehicle.getYear());
    }

    private static void setIfBlank(String current, java.util.function.Consumer<String> setter, String value) {
        if (current != null && !current.trim().isEmpty()) {
            return;
        }
        setIfPresent(setter, value, Integer.MAX_VALUE);
    }

    private void copyFields(InspectionCertificate source, InspectionCertificate target) {
        target.setCertificateNo(source.getCertificateNo());
        target.setInspectionBranch(source.getInspectionBranch());
        target.setMake(source.getMake());
        target.setModel(source.getModel());
        target.setEngineCapacity(source.getEngineCapacity());
        target.setFirstRegistration(source.getFirstRegistration());
        target.setChassisVin(source.getChassisVin());
        target.setEngineNo(source.getEngineNo());
        target.setInspectedMileage(source.getInspectedMileage());
        target.setInspectionDate(source.getInspectionDate());
        target.setRemarks(source.getRemarks());
        target.setOcrText(source.getOcrText());
        target.setDocumentOriginalName(source.getDocumentOriginalName());
        target.setDocumentStoredName(source.getDocumentStoredName());
        target.setDocumentContentType(source.getDocumentContentType());
    }
}
