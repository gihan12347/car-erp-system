package com.carsale.erp.customspipeline.service;

import com.carsale.erp.shared.dto.CustomsProgress;
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
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class CustomsProgressService {

    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final CustomsDocumentService customsDocumentService;
    private final SkippedStageService skippedStageService;

    public CustomsProgressService(
            VehicleRepository vehicleRepository,
            VehicleService vehicleService,
            CustomsDocumentService customsDocumentService,
            SkippedStageService skippedStageService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleService = vehicleService;
        this.customsDocumentService = customsDocumentService;
        this.skippedStageService = skippedStageService;
    }

    public CustomsProgress progressFor(Vehicle vehicle) {
        if (vehicle == null) {
            return new CustomsProgress(false, false, false, false);
        }
        return progressFor(vehicle.getChassisNo());
    }

    public CustomsProgress progressFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new CustomsProgress(false, false, false, false);
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return new CustomsProgress(
                customsDocumentService.hasBillOfLading(chassisNo),
                customsDocumentService.hasDeclaration(chassisNo),
                customsDocumentService.hasAssessment(chassisNo),
                customsDocumentService.hasWorksheet(chassisNo),
                skippedStageService.matching(vehicle, PipelineStageUtils.CUSTOMS_STAGE_KEYS)
        );
    }

    public boolean isStageSatisfied(String chassisNo, String stageKey) {
        return progressFor(chassisNo).isStageComplete(stageKey);
    }

    @Transactional
    public void skipStage(String chassisNo, String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage == null || !PipelineStageUtils.CUSTOMS_STAGE_KEYS.contains(stage.getStageKey())
                || FlowStage.WORKSHEET.getStageKey().equals(stage.getStageKey())) {
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

    public List<Vehicle> listEligible(String query) {
        List<Vehicle> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicleService.search(query)) {
            if (isEligible(vehicle) && vehicle.getIntake() != VehicleIntake.LOCAL_REGISTERED) {
                rows.add(vehicle);
            }
        }
        return rows;
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
        CustomsProgress status = progressFor(vehicle);
        if (!status.isPipelineCompleted()) {
            return;
        }
        VehicleStage current = vehicle.getStage();
        if (current == null
                || current == VehicleStage.PURCHASED
                || current == VehicleStage.SHIPPING
                || current == VehicleStage.CUSTOMS) {
            vehicle.setStage(VehicleStage.WORKSHOP);
            vehicleRepository.save(vehicle);
        }
    }

    public String redirectWhenNotEligible(Vehicle vehicle) {
        if (vehicle == null || vehicle.getChassisNo() == null) {
            return "/import";
        }
        return "/auction/" + UriUtils.encodePathSegment(vehicle.getChassisNo().trim(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
