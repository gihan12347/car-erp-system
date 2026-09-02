package com.carsale.erp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.util.UriUtils;

import com.carsale.erp.entity.Vehicle;
import com.carsale.erp.entity.VehicleStage;
import com.carsale.erp.repository.VehicleRepository;

@Service
public class PreparationPipelineService {

    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final ClearancePipelineService clearancePipelineService;
    private final WorkshopService workshopService;
    private final YardService yardService;
    private final VehicleInspectionService vehicleInspectionService;

    public PreparationPipelineService(
            VehicleRepository vehicleRepository,
            VehicleService vehicleService,
            ClearancePipelineService clearancePipelineService,
            WorkshopService workshopService,
            YardService yardService,
            VehicleInspectionService vehicleInspectionService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleService = vehicleService;
        this.clearancePipelineService = clearancePipelineService;
        this.workshopService = workshopService;
        this.yardService = yardService;
        this.vehicleInspectionService = vehicleInspectionService;
    }

    public PrepStatus statusFor(Vehicle vehicle) {
        if (vehicle == null) {
            return new PrepStatus(false, false, false, false);
        }
        return statusFor(vehicle.getChassisNo());
    }

    public PrepStatus statusFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new PrepStatus(false, false, false, false);
        }
        return new PrepStatus(
                workshopService.isComplete(chassisNo),
                yardService.isComplete(chassisNo),
                vehicleInspectionService.isComplete(chassisNo),
                workshopService.canEnterYard(chassisNo)
        );
    }

    public boolean isEligible(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }
        VehicleStage stage = vehicle.getStage();
        if (stage == VehicleStage.WORKSHOP
                || stage == VehicleStage.READY
                || stage == VehicleStage.RESERVED
                || stage == VehicleStage.SOLD) {
            return true;
        }
        return clearancePipelineService.statusFor(vehicle).isClearanceComplete();
    }

    public String ineligibleHubPath(Vehicle vehicle) {
        if (vehicle == null || vehicle.getChassisNo() == null) {
            return "/workshop-yard";
        }
        String encoded = UriUtils.encodePathSegment(vehicle.getChassisNo().trim(), java.nio.charset.StandardCharsets.UTF_8);
        if (clearancePipelineService.isEligible(vehicle)) {
            return "/customs/" + encoded;
        }
        return "/auction/" + encoded;
    }

    public String ineligibleNotice(Vehicle vehicle) {
        if (vehicle != null && clearancePipelineService.isEligible(vehicle)) {
            return "Finish the customs clearance pipeline before the preparation pipeline.";
        }
        return "Finish the import pipeline before the preparation pipeline.";
    }

    public boolean isEligibleForSale(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }
        if (statusFor(vehicle).isPrepComplete()) {
            return true;
        }
        VehicleStage stage = vehicle.getStage();
        return stage == VehicleStage.READY
                || stage == VehicleStage.RESERVED
                || stage == VehicleStage.SOLD;
    }

    public List<Vehicle> listEligible(String query) {
        return filter(vehicleService.search(query), true);
    }

    public List<Vehicle> listReadyForSale(String query) {
        return filter(vehicleService.search(query), false);
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
        PrepStatus status = statusFor(vehicle);
        VehicleStage current = vehicle.getStage();
        if (current == VehicleStage.RESERVED || current == VehicleStage.SOLD) {
            return;
        }
        if (status.isPrepComplete()) {
            if (current == null
                    || current == VehicleStage.PURCHASED
                    || current == VehicleStage.SHIPPING
                    || current == VehicleStage.CUSTOMS
                    || current == VehicleStage.WORKSHOP) {
                vehicle.setStage(VehicleStage.READY);
                vehicleRepository.save(vehicle);
            }
            return;
        }
        if (current == VehicleStage.READY && clearancePipelineService.statusFor(vehicle).isClearanceComplete()) {
            vehicle.setStage(VehicleStage.WORKSHOP);
            vehicleRepository.save(vehicle);
        }
    }

    private List<Vehicle> filter(List<Vehicle> vehicles, boolean preparationList) {
        List<Vehicle> rows = new ArrayList<Vehicle>();
        for (Vehicle vehicle : vehicles) {
            if (preparationList) {
                if (isEligible(vehicle)) {
                    rows.add(vehicle);
                }
            } else if (isEligibleForSale(vehicle)) {
                rows.add(vehicle);
            }
        }
        return rows;
    }

    public static final class PrepStatus {
        private final boolean workshopReady;
        private final boolean yardReady;
        private final boolean inspectionReady;
        private final boolean canEnterYard;

        public PrepStatus(boolean workshopReady, boolean yardReady, boolean inspectionReady, boolean canEnterYard) {
            this.workshopReady = workshopReady;
            this.yardReady = yardReady;
            this.inspectionReady = inspectionReady;
            this.canEnterYard = canEnterYard;
        }

        public boolean isWorkshopReady() {
            return workshopReady;
        }

        public boolean isYardReady() {
            return yardReady;
        }

        public boolean isInspectionReady() {
            return inspectionReady;
        }

        public boolean isCanEnterYard() {
            return canEnterYard;
        }

        public int completedCount() {
            return (workshopReady ? 1 : 0) + (yardReady ? 1 : 0) + (inspectionReady ? 1 : 0);
        }

        public boolean isPrepComplete() {
            return workshopReady && yardReady && inspectionReady;
        }
    }
}
