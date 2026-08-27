package com.carsale.erp.service;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.EquipmentInspection;
import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.repository.EquipmentInspectionRepository;
import com.carsale.erp.repository.VehicleRepository;

@Service
public class EquipmentInspectionService {

    private final VehicleRepository vehicleRepository;
    private final EquipmentInspectionRepository equipmentRepository;
    private final SheetDocumentStorageService documentStorageService;

    public EquipmentInspectionService(
            VehicleRepository vehicleRepository,
            EquipmentInspectionRepository equipmentRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.equipmentRepository = equipmentRepository;
        this.documentStorageService = documentStorageService;
    }

    public EquipmentInspection findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return equipmentRepository.findById(chassisNo.trim()).orElse(null);
    }

    public EquipmentInspection prepareForm(String chassisNo) {
        Vehicle vehicle = vehicleRepository.findById(chassisNo).orElse(null);
        if (vehicle == null) {
            return null;
        }
        EquipmentInspection record = equipmentRepository.findById(chassisNo).orElse(null);
        if (record == null) {
            record = new EquipmentInspection();
            record.setChassisNo(chassisNo);
        }
        return record;
    }

    public boolean hasEquipmentInspection(String chassisNo) {
        EquipmentInspection record = findByChassisNo(chassisNo);
        return record != null
                && record.getDocumentStoredName() != null
                && !record.getDocumentStoredName().trim().isEmpty();
    }

    public String fieldValue(EquipmentInspection record, String key) {
        if (record == null || key == null || key.trim().isEmpty()) {
            return "";
        }
        try {
            Object value = new BeanWrapperImpl(record).getPropertyValue(key);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception ex) {
            return "";
        }
    }

    @Transactional
    public EquipmentInspection save(EquipmentInspection incoming) {
        if (incoming == null || incoming.getChassisNo() == null || incoming.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }

        vehicleRepository.findById(incoming.getChassisNo().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle not found for chassis " + incoming.getChassisNo()));

        EquipmentInspection existing = equipmentRepository.findById(incoming.getChassisNo().trim()).orElse(null);
        if (existing != null) {
            keepStoredDocument(incoming, existing);
            if (incoming.getDocumentStoredName() != null
                    && existing.getDocumentStoredName() != null
                    && !existing.getDocumentStoredName().equals(incoming.getDocumentStoredName())) {
                documentStorageService.deleteEquipmentIfExists(existing.getDocumentStoredName());
            }
            BeanUtils.copyProperties(incoming, existing);
            return equipmentRepository.save(existing);
        }
        return equipmentRepository.save(incoming);
    }

    private void keepStoredDocument(EquipmentInspection incoming, EquipmentInspection existing) {
        if (incoming.getDocumentStoredName() != null && !incoming.getDocumentStoredName().trim().isEmpty()) {
            return;
        }
        incoming.setDocumentStoredName(existing.getDocumentStoredName());
        incoming.setDocumentOriginalName(existing.getDocumentOriginalName());
        incoming.setDocumentContentType(existing.getDocumentContentType());
    }
}
