package com.carsale.erp.importpipeline.coi;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.importpipeline.coi.InspectionCertificate;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.importpipeline.coi.InspectionCertificateRepository;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class CertificateOfInspectionService {

    private final VehicleRepository vehicleRepository;
    private final InspectionCertificateRepository certificateRepository;
    private final SheetDocumentStorageService documentStorageService;

    public CertificateOfInspectionService(
            VehicleRepository vehicleRepository,
            InspectionCertificateRepository certificateRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.certificateRepository = certificateRepository;
        this.documentStorageService = documentStorageService;
    }

    public InspectionCertificate findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return certificateRepository.findById(chassisNo.trim()).orElse(null);
    }

    public InspectionCertificate prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        InspectionCertificate record = certificateRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new InspectionCertificate();
            record.setChassisNo(chassisNo);
            prefillFromVehicle(record, vehicle);
        }
        return record;
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
        return certificateRepository.save(existing);
    }

    public AuctionParseResult remapParseResult(AuctionParseResult parsed) {
        if (parsed == null) {
            return new AuctionParseResult();
        }
        Map<String, String> source = parsed.getFields();
        AuctionParseResult remapped = new AuctionParseResult();
        remapped.setSuccess(parsed.isSuccess());
        remapped.setMessage(parsed.getMessage());
        remapped.setRawText(parsed.getRawText());
        remapped.put("certificateNo", value(source, "jevicCertificateNo"));
        remapped.put("issueDate", value(source, "jevicIssueDate"));
        remapped.put("inspectionBranch", value(source, "jevicLocation"));
        remapped.put("make", value(source, "jevicMake"));
        remapped.put("model", value(source, "jevicModel"));
        remapped.put("engineCapacity", value(source, "jevicEngineCapacity"));
        remapped.put("firstRegistration", value(source, "jevicFirstRegistration"));
        remapped.put("chassisVin", value(source, "jevicChassisVin"));
        remapped.put("engineNo", value(source, "jevicEngineNo"));
        remapped.put("inspectedMileage", value(source, "jevicCurrentOdometer"));
        remapped.put("inspectionDate", value(source, "jevicInspectionDate"));
        remapped.put("remarks", value(source, "jevicRemarks"));
        return remapped;
    }

    private String value(Map<String, String> source, String key) {
        if (source == null) {
            return null;
        }
        return source.get(key);
    }

    private void keepStoredDocument(InspectionCertificate incoming, InspectionCertificate existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    private void prefillFromVehicle(InspectionCertificate record, Vehicle vehicle) {
        record.setChassisVin(vehicle.getChassisNo());
        record.setMake(vehicle.getMake());
        record.setModel(vehicle.getModel());
        record.setEngineCapacity(vehicle.getEngineSize());
        record.setInspectedMileage(vehicle.getMileage());
        record.setFirstRegistration(vehicle.getYear());
    }

    private void copyFields(InspectionCertificate source, InspectionCertificate target) {
        target.setCertificateNo(source.getCertificateNo());
        target.setIssueDate(source.getIssueDate());
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
