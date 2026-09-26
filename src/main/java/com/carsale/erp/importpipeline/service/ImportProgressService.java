package com.carsale.erp.importpipeline.service;

import com.carsale.erp.customspipeline.service.CustomsDocumentService;
import com.carsale.erp.shared.pipeline.PipelineProgress;
import com.carsale.erp.shared.pipeline.FlowStage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.shared.vehicle.VehicleRepository;

import java.util.List;

@Service
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

    public ImportProgressService(
            VehicleRepository vehicleRepository,
            PreShipmentService preShipmentService,
            EquipmentInspectionService equipmentInspectionService,
            CustomsDocumentService customsDocumentService,
            CertificateOfInspectionService certificateOfInspectionService,
            StandardsCertificateService standardsCertificateService,
            ExportCertificateService exportCertificateService,
            GradeSearchService gradeSearchService,
            VehiclePhotoService vehiclePhotoService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.preShipmentService = preShipmentService;
        this.equipmentInspectionService = equipmentInspectionService;
        this.customsDocumentService = customsDocumentService;
        this.certificateOfInspectionService = certificateOfInspectionService;
        this.standardsCertificateService = standardsCertificateService;
        this.exportCertificateService = exportCertificateService;
        this.gradeSearchService = gradeSearchService;
        this.vehiclePhotoService = vehiclePhotoService;
    }

    public ImportProgress progressFor(Vehicle vehicle) {
        if (vehicle == null) {
            return new ImportProgress(false, false, false, false, false, false, false, false, false);
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
                gradeSearchService.hasDocument(chassisNo));
    }

    public ImportProgress progressFor(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return new ImportProgress(false, false, false, false, false, false, false, false, false);
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

    public static final class ImportProgress implements PipelineProgress {
        private final boolean auctionReady;
        private final boolean preshipReady;
        private final boolean equipmentReady;
        private final boolean jevicReady;
        private final boolean coiReady;
        private final boolean standardsReady;
        private final boolean exportReady;
        private final boolean photosReady;
        private final boolean gradeReady;

        public ImportProgress(
                boolean auctionReady,
                boolean preshipReady,
                boolean equipmentReady,
                boolean jevicReady,
                boolean coiReady,
                boolean standardsReady,
                boolean exportReady,
                boolean photosReady,
                boolean gradeReady
        ) {
            this.auctionReady = auctionReady;
            this.preshipReady = preshipReady;
            this.equipmentReady = equipmentReady;
            this.jevicReady = jevicReady;
            this.coiReady = coiReady;
            this.standardsReady = standardsReady;
            this.exportReady = exportReady;
            this.photosReady = photosReady;
            this.gradeReady = gradeReady;
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

        public boolean isOdometerReady() {
            return jevicReady;
        }

        public boolean isCoiReady() {
            return coiReady;
        }

        public boolean isStandardsReady() {
            return standardsReady;
        }

        public boolean isExportReady() {
            return exportReady;
        }

        public boolean isPhotosReady() {
            return photosReady;
        }

        public boolean isGradeReady() {
            return gradeReady;
        }

        @Override
        public boolean hasAnyCompletedStage() {
            return auctionReady || preshipReady || equipmentReady || jevicReady || coiReady
                    || standardsReady || exportReady || photosReady || gradeReady;
        }

        @Override
        public int completedCount() {
            return (auctionReady ? 1 : 0) + (preshipReady ? 1 : 0) + (equipmentReady ? 1 : 0)
                    + (jevicReady ? 1 : 0) + (coiReady ? 1 : 0) + (standardsReady ? 1 : 0)
                    + (exportReady ? 1 : 0) + (photosReady ? 1 : 0) + (gradeReady ? 1 : 0);
        }

        @Override
        public boolean isPipelineCompleted() {
            return auctionReady && preshipReady && equipmentReady && jevicReady && coiReady
                    && standardsReady && exportReady && photosReady && gradeReady;
        }

        public boolean isImportComplete() {
            return isPipelineCompleted();
        }

        @Override
        public boolean isStageComplete(String stageKey) {
            if (FlowStage.PRESHIP.getStageKey().equals(stageKey)) {
                return preshipReady;
            }
            if (FlowStage.EQUIPMENT.getStageKey().equals(stageKey)) {
                return equipmentReady;
            }
            if (FlowStage.JEVIC.getStageKey().equals(stageKey)) {
                return jevicReady;
            }
            if (FlowStage.COI.getStageKey().equals(stageKey)) {
                return coiReady;
            }
            if (FlowStage.STANDARDS.getStageKey().equals(stageKey)) {
                return standardsReady;
            }
            if (FlowStage.EXPORT.getStageKey().equals(stageKey)) {
                return exportReady;
            }
            if (FlowStage.GRADE.getStageKey().equals(stageKey)) {
                return gradeReady;
            }
            if (FlowStage.PHOTOS.getStageKey().equals(stageKey)) {
                return photosReady;
            }
            return auctionReady;
        }

        public String firstIncompleteStageKey(List<String> keys) {
            for (String key : keys) {
                if (!isStageComplete(key)) {
                    return key;
                }
            }
            return null;
        }
    }
}
