package com.carsale.erp.importpipeline.gradesearch;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeSearchService {

    private final VehicleRepository vehicleRepository;
    private final GradeSearchRepository gradeSearchRepository;
    private final SheetDocumentStorageService documentStorageService;

    public GradeSearchService(
            VehicleRepository vehicleRepository,
            GradeSearchRepository gradeSearchRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.gradeSearchRepository = gradeSearchRepository;
        this.documentStorageService = documentStorageService;
    }

    public GradeSearch findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return gradeSearchRepository.findById(chassisNo.trim()).orElse(null);
    }

    public GradeSearch prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        GradeSearch record = gradeSearchRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new GradeSearch();
            record.setChassisNo(chassisNo);
            record.setChassisNumber(vehicle.getChassisNo());
            record.setGrade(vehicle.getGrade());
        }
        return record;
    }

    public boolean hasDocument(String chassisNo) {
        GradeSearch record = findByChassisNo(chassisNo);
        return record != null
                && record.getDocumentStoredName() != null
                && !record.getDocumentStoredName().trim().isEmpty();
    }

    @Transactional
    public GradeSearch save(GradeSearch incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }

        String chassisNo = incoming.getChassisNo().trim();
        Vehicle vehicle = vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));

        GradeSearch existing = gradeSearchRepository.findById(chassisNo).orElse(null);
        if (existing != null) {
            keepStoredDocument(incoming, existing);
            if (incoming.getDocumentStoredName() != null
                    && existing.getDocumentStoredName() != null
                    && !existing.getDocumentStoredName().equals(incoming.getDocumentStoredName())) {
                documentStorageService.deleteGradeSearchIfExists(existing.getDocumentStoredName());
            }
            copyFields(incoming, existing);
        } else {
            existing = incoming;
            existing.setChassisNo(chassisNo);
        }
        GradeSearch saved = gradeSearchRepository.save(existing);
        applyGradeToVehicle(vehicle, saved.getGrade());
        return saved;
    }

    private void applyGradeToVehicle(Vehicle vehicle, String grade) {
        if (grade == null || grade.trim().isEmpty()) {
            return;
        }
        vehicle.setGrade(grade.trim());
        vehicleRepository.save(vehicle);
    }

    private void keepStoredDocument(GradeSearch incoming, GradeSearch existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }

    private void copyFields(GradeSearch source, GradeSearch target) {
        target.setChassisNumber(source.getChassisNumber());
        target.setGrade(source.getGrade());
        target.setOcrText(source.getOcrText());
        target.setDocumentOriginalName(source.getDocumentOriginalName());
        target.setDocumentStoredName(source.getDocumentStoredName());
        target.setDocumentContentType(source.getDocumentContentType());
    }
}
