package com.carsale.erp.importpipeline.service;

import com.carsale.erp.customspipeline.service.CustomsDocumentService;
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.service.SkippedStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
@RequiredArgsConstructor
public class ImportProgressService {

    private final VehicleRepository vehicleRepository;
    private final PreShipmentService preShipmentService;
    private final EquipmentInspectionService equipmentInspectionService;
    private final CustomsDocumentService customsDocumentService;
    private final CertificateOfInspectionService certificateOfInspectionService;
    private final StandardsCertificateService standardsCertificateService;
    private final ExportCertificateService exportCertificateService;
    private final GradeSearchService gradeSearchService;
    private final VehiclePhotoService vehiclePhotoService;
    private final SkippedStageService skippedStageService;

    public ImportProgress progressFor(Vehicle vehicle) {
        if (vehicle == null) {
            return ImportProgress.builder()
                    .auctionReady(false)
                    .preshipReady(false)
                    .equipmentReady(false)
                    .jevicReady(false)
                    .coiReady(false)
                    .standardsReady(false)
                    .exportReady(false)
                    .photosReady(false)
                    .gradeReady(false)
                    .worksheetReady(false)
                    .build();
        }
        String chassisNo = vehicle.getChassisNo();
        return new ImportProgress(
                hasAuctionDocument(vehicle),
                preShipmentService.hasPreShipment(chassisNo),
                equipmentInspectionService.hasEquipmentInspection(chassisNo),
                customsDocumentService.hasOdometerCertificate(chassisNo),
                certificateOfInspectionService.hasCertificate(chassisNo),
                standardsCertificateService.hasCertificate(chassisNo),
                exportCertificateService.hasCertificate(chassisNo),
                vehiclePhotoService.hasPhotos(chassisNo),
                gradeSearchService.hasDocument(chassisNo),
                customsDocumentService.hasWorksheet(chassisNo),
                skippedStageService.matching(vehicle, PipelineStageUtils.IMPORT_STAGE_KEYS));
    }

    @Transactional
    public void skipStage(String chassisNo, String stageKey) {
        FlowStage stage = FlowStage.fromKey(stageKey);
        if (stage == null || !PipelineStageUtils.IMPORT_STAGE_KEYS.contains(stage.getStageKey())) {
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

    public boolean isStageSatisfied(String chassisNo, String stageKey) {
        if (chassisNo == null || chassisNo.trim().isEmpty() || stageKey == null) {
            return false;
        }
        return progressFor(chassisNo).isStageComplete(stageKey);
    }

    public String skippedToken(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return "|";
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        if (vehicle == null) {
            return "|";
        }
        StringBuilder token = new StringBuilder("|");
        for (String key : skippedStageService.matching(vehicle, PipelineStageUtils.IMPORT_STAGE_KEYS)) {
            token.append(key).append('|');
        }
        return token.toString();
    }

    public ImportProgress progressFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new ImportProgress(false, false, false, false, false, false, false, false, false, false);
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        return progressFor(vehicle);
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
        ImportProgress status = progressFor(vehicle);
        if (!status.isPipelineCompleted()) {
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

}
