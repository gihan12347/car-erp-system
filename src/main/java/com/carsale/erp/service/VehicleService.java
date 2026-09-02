package com.carsale.erp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.VehicleStage;
import com.carsale.erp.repository.ClearanceDocumentRepository;
import com.carsale.erp.repository.EquipmentInspectionRepository;
import com.carsale.erp.repository.PreShipmentInspectionRepository;
import com.carsale.erp.repository.SaleListingRepository;
import com.carsale.erp.repository.VehicleInspectionRepository;
import com.carsale.erp.repository.VehicleRepository;
import com.carsale.erp.repository.WorkshopJobRepository;
import com.carsale.erp.repository.YardRecordRepository;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final SheetDocumentStorageService documentStorageService;
    private final PreShipmentInspectionRepository preShipmentInspectionRepository;
    private final ClearanceDocumentRepository clearanceDocumentRepository;
    private final EquipmentInspectionRepository equipmentInspectionRepository;
    private final WorkshopJobRepository workshopJobRepository;
    private final YardRecordRepository yardRecordRepository;
    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final SaleListingRepository saleListingRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            SheetDocumentStorageService documentStorageService,
            PreShipmentInspectionRepository preShipmentInspectionRepository,
            ClearanceDocumentRepository clearanceDocumentRepository,
            EquipmentInspectionRepository equipmentInspectionRepository,
            WorkshopJobRepository workshopJobRepository,
            YardRecordRepository yardRecordRepository,
            VehicleInspectionRepository vehicleInspectionRepository,
            SaleListingRepository saleListingRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.documentStorageService = documentStorageService;
        this.preShipmentInspectionRepository = preShipmentInspectionRepository;
        this.clearanceDocumentRepository = clearanceDocumentRepository;
        this.equipmentInspectionRepository = equipmentInspectionRepository;
        this.workshopJobRepository = workshopJobRepository;
        this.yardRecordRepository = yardRecordRepository;
        this.vehicleInspectionRepository = vehicleInspectionRepository;
        this.saleListingRepository = saleListingRepository;
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        return vehicleRepository.search(query.trim());
    }

    public Vehicle findByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return null;
        }
        return vehicleRepository.findByChassisNo(chassisNo.trim()).orElse(null);
    }

    public boolean existsByChassisNo(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return false;
        }
        return vehicleRepository.existsByChassisNo(chassisNo.trim());
    }


    public Vehicle save(Vehicle vehicle) {
        if (vehicle.getChassisNo() == null || vehicle.getChassisNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        vehicle.setChassisNo(vehicle.getChassisNo().trim());
        if (vehicle.getStage() == null) {
            vehicle.setStage(VehicleStage.PURCHASED);
        }
        VehicleFieldNormalizer.normalize(vehicle);
        Vehicle existing = vehicleRepository.findById(vehicle.getChassisNo()).orElse(null);
        boolean isNew = existing == null;
        if (!isNew) {
            preserveExistingStockNo(vehicle, existing);
            preserveExistingDocument(vehicle, existing);
        } else if (vehicle.getStockNo() == null || vehicle.getStockNo().trim().isEmpty()) {
            vehicle.setStockNo(nextAvailableStockNo());
        }
        return vehicleRepository.save(vehicle);
    }

    private void preserveExistingStockNo(Vehicle incoming, Vehicle existing) {
        if (incoming.getStockNo() == null || incoming.getStockNo().trim().isEmpty()) {
            incoming.setStockNo(existing.getStockNo());
        }
    }

    private void preserveExistingDocument(Vehicle incoming, Vehicle existing) {
        if (incoming.getSheetStoredName() == null || incoming.getSheetStoredName().trim().isEmpty()) {
            incoming.setSheetStoredName(existing.getSheetStoredName());
            incoming.setSheetOriginalName(existing.getSheetOriginalName());
            incoming.setSheetContentType(existing.getSheetContentType());
            return;
        }
        String oldStored = existing.getSheetStoredName();
        String newStored = incoming.getSheetStoredName().trim();
        if (oldStored != null && !oldStored.equals(newStored)) {
            documentStorageService.deleteIfExists(oldStored);
        }
    }

    private String nextAvailableStockNo() {
        int maxSuffix = 0;
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            maxSuffix = Math.max(maxSuffix, parseStockSuffix(vehicle.getStockNo()));
        }
        int candidate = maxSuffix + 1;
        String stockNo;
        do {
            stockNo = String.format("V%03d", candidate++);
        } while (vehicleRepository.existsByStockNo(stockNo));
        return stockNo;
    }

    private int parseStockSuffix(String stockNo) {
        if (stockNo == null || stockNo.length() < 2 || stockNo.charAt(0) != 'V') {
            return 0;
        }
        try {
            return Integer.parseInt(stockNo.substring(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @Transactional
    public boolean delete(String chassisNo) {
        return deleteFromFlow(chassisNo, PipelineStageService.FLOW_IMPORT);
    }

    @Transactional
    public boolean deleteFromFlow(String chassisNo, String flowKey) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return false;
        }
        String id = chassisNo.trim();
        Vehicle vehicle = findByChassisNo(id);
        if (vehicle == null) {
            return false;
        }
        if (PipelineStageService.FLOW_IMPORT.equals(flowKey)) {
            deleteReadyData(id);
            deletePrepData(id);
            deleteCustomsData(id);
            deleteImportData(id, vehicle);
            vehicleRepository.delete(vehicle);
            return true;
        }
        if (PipelineStageService.FLOW_CUSTOMS.equals(flowKey)) {
            deleteReadyData(id);
            deletePrepData(id);
            deleteCustomsData(id);
            setStage(vehicle, VehicleStage.CUSTOMS);
            return true;
        }
        if (PipelineStageService.FLOW_PREP.equals(flowKey)) {
            deleteReadyData(id);
            deletePrepData(id);
            setStage(vehicle, VehicleStage.WORKSHOP);
            return true;
        }
        if (PipelineStageService.FLOW_READY.equals(flowKey)) {
            deleteReadyData(id);
            setStage(vehicle, VehicleStage.READY);
            return true;
        }
        return false;
    }

    private void deleteReadyData(String chassisNo) {
        saleListingRepository.findById(chassisNo).ifPresent(saleListingRepository::delete);
    }

    private void deletePrepData(String chassisNo) {
        workshopJobRepository.findById(chassisNo).ifPresent(workshopJobRepository::delete);
        yardRecordRepository.findById(chassisNo).ifPresent(yardRecordRepository::delete);
        vehicleInspectionRepository.findById(chassisNo).ifPresent(vehicleInspectionRepository::delete);
    }

    private void deleteCustomsData(String chassisNo) {
        clearanceDocumentRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteClearanceIfExists(record.getPage1StoredName());
            documentStorageService.deleteClearanceIfExists(record.getPage2StoredName());
            documentStorageService.deleteClearanceIfExists(record.getPage3StoredName());
            clearanceDocumentRepository.delete(record);
        });
    }

    private void deleteImportData(String chassisNo, Vehicle vehicle) {
        documentStorageService.deleteIfExists(vehicle.getSheetStoredName());
        preShipmentInspectionRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deletePreShipmentIfExists(record.getDocumentStoredName());
            preShipmentInspectionRepository.delete(record);
        });
        equipmentInspectionRepository.findById(chassisNo).ifPresent(record -> {
            documentStorageService.deleteEquipmentIfExists(record.getDocumentStoredName());
            equipmentInspectionRepository.delete(record);
        });
    }

    private void setStage(Vehicle vehicle, VehicleStage stage) {
        vehicle.setStage(stage);
        vehicleRepository.save(vehicle);
    }

}
