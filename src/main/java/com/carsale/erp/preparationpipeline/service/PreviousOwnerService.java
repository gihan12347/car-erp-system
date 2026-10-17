package com.carsale.erp.preparationpipeline.service;

import com.carsale.erp.preparationpipeline.model.PreviousOwner;
import com.carsale.erp.preparationpipeline.repository.PreviousOwnerRepository;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.utils.Utils;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PreviousOwnerService {

    private final VehicleRepository vehicleRepository;
    private final PreviousOwnerRepository previousOwnerRepository;
    private final SheetDocumentStorageService documentStorageService;

    public PreviousOwner findByChassisNo(String chassisNo) {
        return Utils.findByChassisNo(chassisNo, previousOwnerRepository, null);
    }

    public PreviousOwner prepareForm(String chassisNo) {
        if (vehicleRepository.findById(chassisNo).orElse(null) == null) {
            return null;
        }
        PreviousOwner record = previousOwnerRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new PreviousOwner();
            record.setChassisNo(chassisNo);
        }
        return record;
    }

    public boolean isSaved(String chassisNo) {
        PreviousOwner record = findByChassisNo(chassisNo);
        if (record == null) {
            return false;
        }
        return text(record.getFullName()) != null || text(record.getNicNo()) != null;
    }

    @Transactional
    public PreviousOwner save(PreviousOwner incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String chassisNo = incoming.getChassisNo().trim();
        vehicleRepository.findById(chassisNo)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for chassis " + chassisNo));
        PreviousOwner existing = previousOwnerRepository.findById(chassisNo).orElse(null);
        if (existing != null) {
            if (text(incoming.getDocumentStoredName()) == null) {
                incoming.setDocumentStoredName(existing.getDocumentStoredName());
                incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
                incoming.setDocumentContentType(existing.getDocumentContentType());
            } else if (text(existing.getDocumentStoredName()) != null
                    && !existing.getDocumentStoredName().equals(incoming.getDocumentStoredName())) {
                documentStorageService.deleteIdCardIfExists(existing.getDocumentStoredName());
            }
            existing.setFullName(incoming.getFullName());
            existing.setNicNo(incoming.getNicNo());
            existing.setAddress(incoming.getAddress());
            existing.setDateOfBirth(incoming.getDateOfBirth());
            existing.setGender(incoming.getGender());
            existing.setOcrText(incoming.getOcrText());
            existing.setDocumentOriginalName(incoming.getDocumentOriginalName());
            existing.setDocumentStoredName(incoming.getDocumentStoredName());
            existing.setDocumentContentType(incoming.getDocumentContentType());
            return previousOwnerRepository.save(existing);
        }
        incoming.setChassisNo(chassisNo);
        return previousOwnerRepository.save(incoming);
    }

    private static String text(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
