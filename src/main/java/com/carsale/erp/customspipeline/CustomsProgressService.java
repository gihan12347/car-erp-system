package com.carsale.erp.customspipeline;

import com.carsale.erp.importpipeline.ImportProgressService;
import com.carsale.erp.shared.pipeline.PipelineProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
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
public class CustomsProgressService {

    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final ImportProgressService importProgressService;
    private final CustomsDocumentService customsDocumentService;

    public CustomsProgressService(
            VehicleRepository vehicleRepository,
            VehicleService vehicleService,
            ImportProgressService importProgressService,
            CustomsDocumentService customsDocumentService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleService = vehicleService;
        this.importProgressService = importProgressService;
        this.customsDocumentService = customsDocumentService;
    }

    public CustomsProgress progressFor(Vehicle vehicle) {
        if (vehicle == null) {
            return new CustomsProgress(false, false, false);
        }
        return progressFor(vehicle.getChassisNo());
    }

    public CustomsProgress progressFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new CustomsProgress(false, false, false);
        }
        return new CustomsProgress(
                customsDocumentService.hasDeclaration(chassisNo),
                customsDocumentService.hasAssessment(chassisNo),
                customsDocumentService.hasWorksheet(chassisNo)
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
        return importProgressService.progressFor(vehicle).isPipelineCompleted();
    }

    public List<Vehicle> listEligible(String query) {
        List<Vehicle> rows = new ArrayList<>();
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
        CustomsProgress status = progressFor(vehicle);
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

    public String redirectWhenNotEligible(Vehicle vehicle) {
        if (vehicle == null || vehicle.getChassisNo() == null) {
            return "/import";
        }
        return "/auction/" + UriUtils.encodePathSegment(vehicle.getChassisNo().trim(), java.nio.charset.StandardCharsets.UTF_8);
    }

    public static final class CustomsProgress implements PipelineProgress {
        private final boolean declarationReady;
        private final boolean assessmentReady;
        private final boolean worksheetReady;

        public CustomsProgress(boolean declarationReady, boolean assessmentReady, boolean worksheetReady) {
            this.declarationReady = declarationReady;
            this.assessmentReady = assessmentReady;
            this.worksheetReady = worksheetReady;
        }

        public boolean isDeclarationReady() {
            return declarationReady;
        }

        public boolean isAssessmentReady() {
            return assessmentReady;
        }

        public boolean isWorksheetReady() {
            return worksheetReady;
        }

        @Override
        public boolean hasAnyCompletedStage() {
            return declarationReady || assessmentReady || worksheetReady;
        }

        public int completedCount() {
            return (declarationReady ? 1 : 0) + (assessmentReady ? 1 : 0) + (worksheetReady ? 1 : 0);
        }

        public boolean isClearanceComplete() {
            return declarationReady && assessmentReady && worksheetReady;
        }

        public boolean isStageComplete(String stageKey) {
            if (FlowStage.ASSESSMENT.getStageKey().equals(stageKey)) {
                return assessmentReady;
            }
            if (FlowStage.WORKSHEET.getStageKey().equals(stageKey)) {
                return worksheetReady;
            }
            return declarationReady;
        }

        @Override
        public boolean isPipelineCompleted() {
            return assessmentReady && worksheetReady && declarationReady;
        }

        public String firstIncompleteStageKey(List<String> keys) {
            if (keys == null) {
                return null;
            }
            for (String key : keys) {
                if (!isStageComplete(key)) {
                    return key;
                }
            }
            return null;
        }
    }
}
