package com.carsale.erp.preparationpipeline.service;

import com.carsale.erp.readypipeline.service.SaleListingService;
import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.shared.dto.PreparationProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.service.SkippedStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.vehicle.VehicleService;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class PreparationProgressService {

    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final CustomsProgressService customsProgressService;
    private final WorkshopService workshopService;
    private final YardService yardService;
    private final VehicleInspectionService vehicleInspectionService;
    private final SaleListingService saleListingService;
    private final SkippedStageService skippedStageService;

    public PreparationProgressService(
            VehicleRepository vehicleRepository,
            VehicleService vehicleService,
            CustomsProgressService customsProgressService,
            WorkshopService workshopService,
            YardService yardService,
            VehicleInspectionService vehicleInspectionService,
            SaleListingService saleListingService,
            SkippedStageService skippedStageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleService = vehicleService;
        this.customsProgressService = customsProgressService;
        this.workshopService = workshopService;
        this.yardService = yardService;
        this.vehicleInspectionService = vehicleInspectionService;
        this.saleListingService = saleListingService;
        this.skippedStageService = skippedStageService;
    }

    public PreparationProgress progressFor(Vehicle vehicle) {
        if (vehicle == null) {
            return new PreparationProgress(false, false, false, false, false);
        }
        return progressFor(vehicle.getChassisNo());
    }

    public PreparationProgress progressFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new PreparationProgress(false, false, false, false, false);
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return new PreparationProgress(
                workshopService.isComplete(chassisNo),
                yardService.isComplete(chassisNo),
                vehicleInspectionService.isComplete(chassisNo),
                workshopService.canEnterYard(chassisNo),
                false,
                skippedStageService.matching(vehicle, PipelineStageUtils.PREP_STAGE_KEYS)
        );
    }

    public boolean isStageSatisfied(String chassisNo, String stageKey) {
        return progressFor(chassisNo).isStageComplete(stageKey);
    }

    @Transactional
    public void skipStage(String chassisNo, String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage == null || !PipelineStageUtils.PREP_STAGE_KEYS.contains(stage.getStageKey())) {
            throw new IllegalArgumentException("That stage cannot be skipped.");
        }
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        if (progressFor(vehicle).isStageFilled(stage.getStageKey())) {
            return;
        }
        skippedStageService.add(vehicle.getChassisNo(), stage.getStageKey());
        syncVehicleStage(vehicle.getChassisNo());
    }

    public boolean isEligible(Vehicle vehicle) {
        return vehicle != null
                && vehicle.getChassisNo() != null
                && !vehicle.getChassisNo().trim().isEmpty();
    }

    public String redirectWhenNotEligible(Vehicle vehicle) {
        if (vehicle == null || vehicle.getChassisNo() == null) {
            return "/workshop-yard";
        }
        String encoded = UriUtils.encodePathSegment(vehicle.getChassisNo().trim(), java.nio.charset.StandardCharsets.UTF_8);
        if (customsProgressService.isEligible(vehicle)) {
            return "/customs/" + encoded;
        }
        return "/auction/" + encoded;
    }

    public String ineligibleNotice(Vehicle vehicle) {
        if (vehicle != null && customsProgressService.isEligible(vehicle)) {
            return "Finish the customs clearance pipeline before the preparation pipeline.";
        }
        return "Finish the import pipeline before the preparation pipeline.";
    }

    public boolean isEligibleForSale(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }
        if (saleListingService.isAssignedToSale(vehicle.getChassisNo())) {
            return true;
        }
        VehicleStage stage = vehicle.getStage();
        return stage == VehicleStage.RESERVED || stage == VehicleStage.SOLD;
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
        PreparationProgress status = progressFor(vehicle);
        VehicleStage current = vehicle.getStage();
        if (current == VehicleStage.RESERVED || current == VehicleStage.SOLD) {
            return;
        }
        if (status.isPipelineCompleted()) {
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
        if (current == VehicleStage.READY && customsProgressService.progressFor(vehicle).isPipelineCompleted()) {
            vehicle.setStage(VehicleStage.WORKSHOP);
            vehicleRepository.save(vehicle);
        }
    }

    private List<Vehicle> filter(List<Vehicle> vehicles, boolean preparationList) {
        List<Vehicle> rows = new ArrayList<>();
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
}
