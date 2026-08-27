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
public class ClearancePipelineService {

    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final ImportPipelineService importPipelineService;
    private final ClearanceService clearanceService;

    public ClearancePipelineService(
            VehicleRepository vehicleRepository,
            VehicleService vehicleService,
            ImportPipelineService importPipelineService,
            ClearanceService clearanceService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleService = vehicleService;
        this.importPipelineService = importPipelineService;
        this.clearanceService = clearanceService;
    }

    public ClearanceStatus statusFor(Vehicle vehicle) {
        if (vehicle == null) {
            return new ClearanceStatus(false, false, false);
        }
        return statusFor(vehicle.getChassisNo());
    }

    public ClearanceStatus statusFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new ClearanceStatus(false, false, false);
        }
        return new ClearanceStatus(
                clearanceService.hasJevic(chassisNo),
                clearanceService.hasDeclaration(chassisNo),
                clearanceService.hasAssessment(chassisNo)
        );
    }

    public boolean isEligible(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }
        VehicleStage stage = vehicle.getStage();
        if (stage == VehicleStage.CUSTOMS
                || stage == VehicleStage.WORKSHOP
                || stage == VehicleStage.READY
                || stage == VehicleStage.RESERVED
                || stage == VehicleStage.SOLD) {
            return true;
        }
        return importPipelineService.statusFor(vehicle).isImportComplete();
    }

    public List<Vehicle> listEligible(String query) {
        List<Vehicle> rows = new ArrayList<Vehicle>();
        for (Vehicle vehicle : vehicleService.search(query)) {
            if (isEligible(vehicle)) {
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
        ClearanceStatus status = statusFor(vehicle);
        if (!status.isClearanceComplete()) {
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

    public String ineligibleHubPath(Vehicle vehicle) {
        if (vehicle == null || vehicle.getChassisNo() == null) {
            return "/import";
        }
        return "/auction/" + UriUtils.encodePathSegment(vehicle.getChassisNo().trim(), java.nio.charset.StandardCharsets.UTF_8);
    }

    public static final class ClearanceStatus {
        private final boolean jevicReady;
        private final boolean declarationReady;
        private final boolean assessmentReady;

        public ClearanceStatus(boolean jevicReady, boolean declarationReady, boolean assessmentReady) {
            this.jevicReady = jevicReady;
            this.declarationReady = declarationReady;
            this.assessmentReady = assessmentReady;
        }

        public boolean isJevicReady() {
            return jevicReady;
        }

        public boolean isDeclarationReady() {
            return declarationReady;
        }

        public boolean isAssessmentReady() {
            return assessmentReady;
        }

        public int completedCount() {
            return (jevicReady ? 1 : 0) + (declarationReady ? 1 : 0) + (assessmentReady ? 1 : 0);
        }

        public boolean isClearanceComplete() {
            return jevicReady && declarationReady && assessmentReady;
        }
    }
}
