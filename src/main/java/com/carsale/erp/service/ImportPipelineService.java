package com.carsale.erp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.VehicleStage;
import com.carsale.erp.repository.VehicleRepository;

@Service
public class ImportPipelineService {

    private final VehicleRepository vehicleRepository;
    private final PreShipmentService preShipmentService;
    private final EquipmentInspectionService equipmentInspectionService;

    public ImportPipelineService(
            VehicleRepository vehicleRepository,
            PreShipmentService preShipmentService,
            EquipmentInspectionService equipmentInspectionService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.preShipmentService = preShipmentService;
        this.equipmentInspectionService = equipmentInspectionService;
    }

    public ImportStatus statusFor(Vehicle vehicle) {
        if (vehicle == null) {
            return new ImportStatus(false, false, false);
        }
        String chassisNo = vehicle.getChassisNo();
        return new ImportStatus(
                hasAuctionDocument(vehicle),
                preShipmentService.hasPreShipment(chassisNo),
                equipmentInspectionService.hasEquipmentInspection(chassisNo)
        );
    }

    public ImportStatus statusFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new ImportStatus(false, false, false);
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return statusFor(vehicle);
    }

    @Transactional
    public void syncVehicleStage(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return;
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        if (vehicle == null) {
            return;
        }
        ImportStatus status = statusFor(vehicle);
        if (!status.isImportComplete()) {
            return;
        }
        VehicleStage current = vehicle.getStage();
        if (current == null
                || current == VehicleStage.PURCHASED
                || current == VehicleStage.SHIPPING) {
            vehicle.setStage(VehicleStage.CUSTOMS);
            vehicleRepository.save(vehicle);
        }
    }

    public boolean hasAuctionDocument(Vehicle vehicle) {
        return vehicle != null
                && vehicle.getSheetStoredName() != null
                && !vehicle.getSheetStoredName().trim().isEmpty();
    }

    public static final class ImportStatus {
        private final boolean auctionReady;
        private final boolean preshipReady;
        private final boolean equipmentReady;

        public ImportStatus(boolean auctionReady, boolean preshipReady, boolean equipmentReady) {
            this.auctionReady = auctionReady;
            this.preshipReady = preshipReady;
            this.equipmentReady = equipmentReady;
        }

        public boolean isAuctionReady() {
            return auctionReady;
        }

        public boolean isPreshipReady() {
            return preshipReady;
        }

        public boolean isEquipmentReady() {
            return equipmentReady;
        }

        public boolean isAnyReady() {
            return auctionReady || preshipReady || equipmentReady;
        }

        public int completedCount() {
            return (auctionReady ? 1 : 0) + (preshipReady ? 1 : 0) + (equipmentReady ? 1 : 0);
        }

        public boolean isImportComplete() {
            return auctionReady && preshipReady && equipmentReady;
        }
    }
}
